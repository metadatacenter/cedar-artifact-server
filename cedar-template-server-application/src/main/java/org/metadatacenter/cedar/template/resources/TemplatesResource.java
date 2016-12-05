package org.metadatacenter.cedar.template.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.http.LinkHeaderUtil;
import org.metadatacenter.util.http.UrlUtil;
import org.metadatacenter.util.mongo.MongoUtils;
import org.metadatacenter.util.provenance.ProvenanceUtil;

import javax.management.InstanceNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.*;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;
import static org.metadatacenter.rest.assertion.GenericAssertions.NonEmpty;

@Path("/templates")
@Produces(MediaType.APPLICATION_JSON)
public class TemplatesResource extends AbstractTemplateServerResource {

  private
  @Context
  UriInfo uriInfo;

  private
  @Context
  HttpServletRequest request;

  private final TemplateService<String, JsonNode> templateService;
  private final TemplateFieldService<String, JsonNode> templateFieldService;

  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  public TemplatesResource(CedarConfig cedarConfig, TemplateService<String, JsonNode> templateService,
                           TemplateFieldService<String, JsonNode> templateFieldService) {
    super(cedarConfig);
    this.templateService = templateService;
    this.templateFieldService = templateFieldService;
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(cedarConfig.getTemplateRESTAPISummaries().getTemplate().getFields());
  }

  @POST
  @Timed
  public Response createTemplate(@QueryParam("importMode") Optional<Boolean> importMode) throws
      CedarAssertionException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_CREATE);

    c.must(c.request().getRequestBody()).be(NonEmpty);
    JsonNode template = c.request().getRequestBody().asJson();


    ProvenanceInfo pi = ProvenanceUtil.build(cedarConfig, c.getCedarUser());
    checkImportModeSetProvenanceAndId(CedarNodeType.TEMPLATE, template, pi, importMode);

    JsonNode createdTemplate = null;
    try {
      templateFieldService.saveNewFieldsAndReplaceIds(template, pi,
          cedarConfig.getLinkedDataPrefix(CedarNodeType.FIELD));
      createdTemplate = templateService.createTemplate(template);
    } catch (IOException e) {
      Map<String, Object> errorParams = new HashMap<>();
      errorParams.put("errorId", "templateNotUpdated");
      errorParams.put("errorMessage", "The template can not be created");
      errorParams.put("exception", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorParams).build();
    }
    MongoUtils.removeIdField(createdTemplate);

    String id = createdTemplate.get("@id").asText();
    // TODO: this is way too much for a URL ENCODE
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    URI uri = null;
    try {
      uri = builder.path(URLEncoder.encode(id, "UTF-8")).build();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return Response.created(uri).entity(createdTemplate).build();
  }

  @GET
  @Timed
  @Path("/{id}")
  public Response findTemplate(@PathParam("id") String id) throws CedarAssertionException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_READ);

    JsonNode template = null;
    try {
      template = templateService.findTemplate(id);
    } catch (IOException | ProcessingException e) {
      Map<String, Object> errorParams = new HashMap<>();
      errorParams.put("id", id);
      errorParams.put("errorId", "templateNotUpdated");
      errorParams.put("errorMessage", "The template can not be found by id:" + id);
      errorParams.put("exception", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorParams).build();
    }
    if (template == null) {
      Map<String, Object> errorParams = new HashMap<>();
      errorParams.put("id", id);
      errorParams.put("errorId", "templateNotFound");
      errorParams.put("errorMessage", "The template can not be found by id:" + id);
      return Response.status(Response.Status.NOT_FOUND).entity(errorParams).build();
    } else {
      // Remove autogenerated _id field to avoid exposing it
      MongoUtils.removeIdField(template);
      return Response.ok().entity(template).build();
    }
  }

  @GET
  @Timed
  public Response findAllTemplates(@QueryParam("limit") Optional<Integer> limitParam,
                                   @QueryParam("offset") Optional<Integer> offsetParam,
                                   @QueryParam("summary") Optional<Boolean> summaryParam,
                                   @QueryParam("fieldNames") Optional<String> fieldNamesParam) throws
      CedarAssertionException {

    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_READ);

    Integer limit = ensureLimit(limitParam);
    Integer offset = ensureOffset(offsetParam);
    Boolean summary = ensureSummary(summaryParam);

    checkPagingParameters(limit, offset);
    List<String> fieldNameList = getAndCheckFieldNames(fieldNamesParam, summary);
    Map<String, Object> r = new HashMap<>();
    List<JsonNode> templates = null;
    try {
      if (summary) {
        templates = templateService.findAllTemplates(limit, offset, FIELD_NAMES_SUMMARY_LIST, FieldNameInEx.INCLUDE);
      } else if (fieldNameList != null) {
        templates = templateService.findAllTemplates(limit, offset, fieldNameList, FieldNameInEx.INCLUDE);
      } else {
        templates = templateService.findAllTemplates(limit, offset, FIELD_NAMES_EXCLUSION_LIST, FieldNameInEx.EXCLUDE);
      }
    } catch (IOException e) {
      Map<String, Object> errorParams = new HashMap<>();
      errorParams.put("errorMessage", "The templates can not be listed");
      errorParams.put("exception", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorParams).build();
    }
    long total = templateService.count();
    checkPagingParametersAgainstTotal(offset, total);

    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    URI absoluteURI = builder.queryParam("summary", false).build();
    String absoluteUrl = UrlUtil.trimUrlParameters(absoluteURI.toString());
    String linkHeader = LinkHeaderUtil.getPagingLinkHeader(absoluteUrl, total, limit, offset);
    Response.ResponseBuilder responseBuilder = Response.ok().entity(templates);
    responseBuilder.header(CustomHttpConstants.HEADER_TOTAL_COUNT, String.valueOf(total));
    if (!linkHeader.isEmpty()) {
      responseBuilder.header(HttpConstants.HTTP_HEADER_LINK, linkHeader);
    }
    return responseBuilder.build();
  }

  @PUT
  @Timed
  @Path("/{id}")
  public Response updateTemplate(@PathParam("id") String id) throws CedarAssertionException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_UPDATE);

    JsonNode newTemplate = c.request().getRequestBody().asJson();
    ProvenanceInfo pi = ProvenanceUtil.build(cedarConfig, c.getCedarUser());
    ProvenanceUtil.patchProvenanceInfo(newTemplate, pi);
    JsonNode updatedTemplate = null;
    try {
      templateFieldService.saveNewFieldsAndReplaceIds(newTemplate, pi,
          cedarConfig.getLinkedDataPrefix(CedarNodeType.FIELD));
      updatedTemplate = templateService.updateTemplate(id, newTemplate);
    } catch (IOException e) {
      Map<String, Object> errorParams = new HashMap<>();
      errorParams.put("id", id);
      errorParams.put("errorId", "templateNotUpdated");
      errorParams.put("errorMessage", "The template can not be updated by id:" + id);
      errorParams.put("exception", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorParams).build();
    } catch (InstanceNotFoundException e) {
      Map<String, Object> errorParams = new HashMap<>();
      errorParams.put("id", id);
      errorParams.put("errorId", "templateNotFound");
      errorParams.put("errorMessage", "The template can not be found by id:" + id);
      errorParams.put("exception", e);
      return Response.status(Response.Status.NOT_FOUND).entity(errorParams).build();
    }
    // Remove autogenerated _id field to avoid exposing it
    MongoUtils.removeIdField(updatedTemplate);
    return Response.ok().entity(updatedTemplate).build();
  }

  @DELETE
  @Timed
  @Path("/{id}")
  public Response deleteTemplate(@PathParam("id") String id) throws CedarAssertionException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_DELETE);

    try {
      templateService.deleteTemplate(id);
    } catch (InstanceNotFoundException e) {
      Map<String, Object> errorParams = new HashMap<>();
      errorParams.put("id", id);
      errorParams.put("errorId", "templateNotFound");
      errorParams.put("errorMessage", "The template can not be found by id:" + id);
      errorParams.put("exception", e);
      return Response.status(Response.Status.NOT_FOUND).entity(errorParams).build();
    } catch (IOException e) {
      Map<String, Object> errorParams = new HashMap<>();
      errorParams.put("id", id);
      errorParams.put("errorId", "templateNotDeleted");
      errorParams.put("errorMessage", "The template can not be delete by id:" + id);
      errorParams.put("exception", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorParams).build();
    }
    return Response.status(Response.Status.NO_CONTENT).build();
  }

}