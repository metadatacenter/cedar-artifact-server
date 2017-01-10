package org.metadatacenter.cedar.template.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.http.CedarUrlUtil;
import org.metadatacenter.util.http.LinkHeaderUtil;
import org.metadatacenter.util.mongo.MongoUtils;
import org.metadatacenter.util.provenance.ProvenanceUtil;

import javax.management.InstanceNotFoundException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/template-elements")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateElementsResource extends AbstractTemplateServerResource {

  private final TemplateElementService<String, JsonNode> templateElementService;
  private static TemplateFieldService<String, JsonNode> templateFieldService;

  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  public TemplateElementsResource(CedarConfig cedarConfig, TemplateElementService<String, JsonNode>
      templateElementService, TemplateFieldService<String, JsonNode> templateFieldService) {
    super(cedarConfig);
    this.templateElementService = templateElementService;
    this.templateFieldService = templateFieldService;
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(cedarConfig.getTemplateRESTAPISummaries().getElement().getFields());
  }

  @POST
  @Timed
  public Response createTemplateElement(@QueryParam("importMode") Optional<Boolean> importMode) throws
      CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_ELEMENT_CREATE);

    //TODO: test if it is not empty
    //c.must(c.request().getRequestBody()).be(NonEmpty);
    JsonNode templateElement = c.request().getRequestBody().asJson();

    ProvenanceInfo pi = ProvenanceUtil.build(cedarConfig, c.getCedarUser());
    checkImportModeSetProvenanceAndId(CedarNodeType.ELEMENT, templateElement, pi, importMode);

    JsonNode createdTemplateElement = null;
    try {
      templateFieldService.saveNewFieldsAndReplaceIds(templateElement, pi,
          cedarConfig.getLinkedDataPrefix(CedarNodeType.FIELD));
      createdTemplateElement = templateElementService.createTemplateElement(templateElement);
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .errorKey(CedarErrorKey.TEMPLATE_ELEMENT_NOT_CREATED)
          .errorMessage("The template element can not be created")
          .exception(e)
          .build();
    }
    MongoUtils.removeIdField(createdTemplateElement);

    String id = createdTemplateElement.get("@id").asText();

    URI uri = CedarUrlUtil.getIdURI(uriInfo, id);
    return Response.created(uri).entity(createdTemplateElement).build();
  }

  @GET
  @Timed
  @Path("/{id}")
  public Response findTemplateElement(@PathParam("id") String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_ELEMENT_READ);

    JsonNode templateElement = null;
    try {
      templateElement = templateElementService.findTemplateElement(id);
    } catch (IOException | ProcessingException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_ELEMENT_NOT_FOUND)
          .errorMessage("The template element can not be found by id:" + id)
          .exception(e)
          .build();
    }
    if (templateElement == null) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_ELEMENT_NOT_FOUND)
          .errorMessage("The template element can not be found by id:" + id)
          .build();
    } else {
      MongoUtils.removeIdField(templateElement);
      return Response.ok().entity(templateElement).build();
    }
  }

  @GET
  @Timed
  public Response findAllTemplateElements(@QueryParam("limit") Optional<Integer> limitParam,
                                          @QueryParam("offset") Optional<Integer> offsetParam,
                                          @QueryParam("summary") Optional<Boolean> summaryParam,
                                          @QueryParam("fieldNames") Optional<String> fieldNamesParam) throws
      CedarException {

    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_ELEMENT_READ);

    Integer limit = ensureLimit(limitParam);
    Integer offset = ensureOffset(offsetParam);
    Boolean summary = ensureSummary(summaryParam);

    checkPagingParameters(limit, offset);
    List<String> fieldNameList = getAndCheckFieldNames(fieldNamesParam, summary);
    Map<String, Object> r = new HashMap<>();
    List<JsonNode> elements = null;
    try {
      if (summary) {
        elements = templateElementService.findAllTemplateElements(limit, offset, FIELD_NAMES_SUMMARY_LIST, FieldNameInEx
            .INCLUDE);
      } else if (fieldNameList != null) {
        elements = templateElementService.findAllTemplateElements(limit, offset, fieldNameList, FieldNameInEx.INCLUDE);
      } else {
        elements = templateElementService.findAllTemplateElements(limit, offset, FIELD_NAMES_EXCLUSION_LIST,
            FieldNameInEx.EXCLUDE);
      }
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .errorKey(CedarErrorKey.TEMPLATE_ELEMENTS_NOT_LISTED)
          .errorMessage("The template elements can not be listed")
          .exception(e)
          .build();
    }
    long total = templateElementService.count();
    checkPagingParametersAgainstTotal(offset, total);

    String absoluteUrl = uriInfo.getAbsolutePathBuilder().build().toString();
    String linkHeader = LinkHeaderUtil.getPagingLinkHeader(absoluteUrl, total, limit, offset);
    Response.ResponseBuilder responseBuilder = Response.ok().entity(elements);
    responseBuilder.header(CustomHttpConstants.HEADER_TOTAL_COUNT, String.valueOf(total));
    if (!linkHeader.isEmpty()) {
      responseBuilder.header(HttpConstants.HTTP_HEADER_LINK, linkHeader);
    }
    return responseBuilder.build();
  }

  @PUT
  @Timed
  @Path("/{id}")
  public Response updateTemplateElement(@PathParam("id") String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_ELEMENT_UPDATE);

    JsonNode newElement = c.request().getRequestBody().asJson();
    ProvenanceInfo pi = ProvenanceUtil.build(cedarConfig, c.getCedarUser());
    ProvenanceUtil.patchProvenanceInfo(newElement, pi);
    JsonNode updatedTemplateElement = null;
    try {
      templateFieldService.saveNewFieldsAndReplaceIds(newElement, pi,
          cedarConfig.getLinkedDataPrefix(CedarNodeType.FIELD));
      updatedTemplateElement = templateElementService.updateTemplateElement(id, newElement);
    } catch (InstanceNotFoundException e) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_ELEMENT_NOT_FOUND)
          .errorMessage("The template element can not be found by id:" + id)
          .exception(e)
          .build();
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_ELEMENT_NOT_UPDATED)
          .errorMessage("The template element can not be updated by id:" + id)
          .exception(e)
          .build();
    }
    MongoUtils.removeIdField(updatedTemplateElement);
    return Response.ok().entity(updatedTemplateElement).build();
  }

  @DELETE
  @Timed
  @Path("/{id}")
  public Response deleteTemplateElement(@PathParam("id") String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_ELEMENT_DELETE);

    try {
      templateElementService.deleteTemplateElement(id);
    } catch (InstanceNotFoundException e) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_ELEMENT_NOT_FOUND)
          .errorMessage("The template element can not be found by id:" + id)
          .exception(e)
          .build();
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_ELEMENT_NOT_DELETED)
          .errorMessage("The template element can not be deleted by id:" + id)
          .exception(e)
          .build();
    }
    return CedarResponse.noContent().build();
  }

}