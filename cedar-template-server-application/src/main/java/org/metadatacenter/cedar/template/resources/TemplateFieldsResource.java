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
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.http.CedarUrlUtil;
import org.metadatacenter.util.http.LinkHeaderUtil;
import org.metadatacenter.util.http.PagedQuery;
import org.metadatacenter.util.mongo.MongoUtils;

import javax.management.InstanceNotFoundException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import static org.metadatacenter.constant.CedarPathParameters.PP_ID;
import static org.metadatacenter.constant.CedarQueryParameters.*;
import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/template-fields")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateFieldsResource extends AbstractTemplateServerResource {

  private final TemplateFieldService<String, JsonNode> templateFieldService;

  protected static List<String> FIELD_NAMES_SUMMARY_LIST;


  public TemplateFieldsResource(CedarConfig cedarConfig, TemplateFieldService<String, JsonNode> templateFieldService) {
    super(cedarConfig);
    this.templateFieldService = templateFieldService;
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(cedarConfig.getTemplateRESTAPI().getSummaries().getField().getFields());
  }

  @POST
  @Timed
  public Response createTemplateField(@QueryParam(QP_IMPORT_MODE) Optional<Boolean> importMode) throws
      CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_FIELD_CREATE);

    //TODO: test if it is not empty
    //c.must(c.request().getRequestBody()).be(NonEmpty);
    JsonNode templateField = c.request().getRequestBody().asJson();

    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    checkImportModeSetProvenanceAndId(CedarNodeType.FIELD, templateField, pi, importMode);

    JsonNode createdTemplateField = null;
    try {
      createdTemplateField = templateFieldService.createTemplateField(templateField);
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .errorKey(CedarErrorKey.TEMPLATE_FIELD_NOT_CREATED)
          .errorMessage("The template instance can not be created")
          .exception(e)
          .build();
    }
    MongoUtils.removeIdField(createdTemplateField);

    String id = createdTemplateField.get("@id").asText();

    URI uri = CedarUrlUtil.getIdURI(uriInfo, id);
    return Response.created(uri).entity(createdTemplateField).build();
  }

  @GET
  @Timed
  @Path("/{id}")
  public Response findTemplateField(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_FIELD_READ);

    JsonNode templateField = null;
    try {
      templateField = templateFieldService.findTemplateField(id);
    } catch (IOException | ProcessingException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_FIELD_NOT_FOUND)
          .errorMessage("The template field can not be found by id:" + id)
          .exception(e)
          .build();
    }
    if (templateField == null) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_FIELD_NOT_FOUND)
          .errorMessage("The template field can not be found by id:" + id)
          .build();
    } else {
      MongoUtils.removeIdField(templateField);
      return Response.ok().entity(templateField).build();
    }
  }

  @GET
  @Timed
  public Response findAllTemplateFields(@QueryParam(QP_LIMIT) Optional<Integer> limitParam,
                                        @QueryParam(QP_OFFSET) Optional<Integer> offsetParam,
                                        @QueryParam(QP_SUMMARY) Optional<Boolean> summaryParam,
                                        @QueryParam(QP_FIELD_NAMES) Optional<String> fieldNamesParam) throws
      CedarException {

    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_FIELD_READ);

    PagedQuery pagedQuery = new PagedQuery(cedarConfig.getTemplateRESTAPI().getPagination())
        .limit(limitParam)
        .offset(offsetParam);
    pagedQuery.validate();

    Integer limit = pagedQuery.getLimit();
    Integer offset = pagedQuery.getOffset();
    Boolean summary = ensureSummary(summaryParam);

    List<String> fieldNameList = getAndCheckFieldNames(fieldNamesParam, summary);
    Map<String, Object> r = new HashMap<>();
    List<JsonNode> fields = null;
    try {
      if (summary) {
        fields = templateFieldService.findAllTemplateFields(limit, offset, FIELD_NAMES_SUMMARY_LIST, FieldNameInEx
            .INCLUDE);
      } else if (fieldNameList != null) {
        fields = templateFieldService.findAllTemplateFields(limit, offset, fieldNameList, FieldNameInEx.INCLUDE);
      } else {
        fields = templateFieldService.findAllTemplateFields(limit, offset, FIELD_NAMES_EXCLUSION_LIST, FieldNameInEx
            .EXCLUDE);
      }
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .errorKey(CedarErrorKey.TEMPLATE_FIELDS_NOT_LISTED)
          .errorMessage("The template fields can not be listed")
          .exception(e)
          .build();
    }
    long total = templateFieldService.count();
    checkPagingParametersAgainstTotal(offset, total);

    String absoluteUrl = uriInfo.getAbsolutePathBuilder().build().toString();
    String linkHeader = LinkHeaderUtil.getPagingLinkHeader(absoluteUrl, total, limit, offset);
    Response.ResponseBuilder responseBuilder = Response.ok().entity(fields);
    responseBuilder.header(CustomHttpConstants.HEADER_TOTAL_COUNT, String.valueOf(total));
    if (!linkHeader.isEmpty()) {
      responseBuilder.header(HttpConstants.HTTP_HEADER_LINK, linkHeader);
    }
    return responseBuilder.build();
  }

  @PUT
  @Timed
  @Path("/{id}")
  public Response updateTemplateField(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_FIELD_UPDATE);

    JsonNode newField = c.request().getRequestBody().asJson();
    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    provenanceUtil.patchProvenanceInfo(newField, pi);
    JsonNode updatedTemplateField = null;
    try {
      updatedTemplateField = templateFieldService.updateTemplateField(id, newField);
    } catch (InstanceNotFoundException e) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_FIELD_NOT_FOUND)
          .errorMessage("The template field can not be found by id:" + id)
          .exception(e)
          .build();
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_FIELD_NOT_UPDATED)
          .errorMessage("The template field can not be updated by id:" + id)
          .exception(e)
          .build();
    }
    MongoUtils.removeIdField(updatedTemplateField);
    return Response.ok().entity(updatedTemplateField).build();
  }

  @DELETE
  @Timed
  @Path("/{id}")
  public Response deleteTemplateField(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_FIELD_DELETE);

    try {
      templateFieldService.deleteTemplateField(id);
    } catch (InstanceNotFoundException e) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_FIELD_NOT_FOUND)
          .errorMessage("The template field can not be found by id:" + id)
          .exception(e)
          .build();
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_FIELD_NOT_DELETED)
          .errorMessage("The template field can not be deleted by id:" + id)
          .exception(e)
          .build();
    }
    return CedarResponse.noContent().build();
  }

}