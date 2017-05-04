package org.metadatacenter.cedar.template.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.error.CedarErrorReasonKey;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.model.validation.report.ReportUtils;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.server.service.TemplateInstanceService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.http.CedarUrlUtil;
import org.metadatacenter.util.http.LinkHeaderUtil;
import org.metadatacenter.util.http.PagedQuery;
import org.metadatacenter.util.mongo.MongoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import static org.metadatacenter.rest.assertion.GenericAssertions.NonEmpty;

@Path("/templates")
@Produces(MediaType.APPLICATION_JSON)
public class TemplatesResource extends AbstractTemplateServerResource {

  private static final Logger logger = LoggerFactory.getLogger(TemplatesResource.class);

  private final TemplateService<String, JsonNode> templateService;
  private final TemplateFieldService<String, JsonNode> templateFieldService;
  private final TemplateInstanceService<String, JsonNode> templateInstanceService;

  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  public TemplatesResource(CedarConfig cedarConfig, TemplateService<String, JsonNode> templateService,
                           TemplateFieldService<String, JsonNode> templateFieldService,
                           TemplateInstanceService<String, JsonNode> templateInstanceService) {
    super(cedarConfig);
    this.templateService = templateService;
    this.templateFieldService = templateFieldService;
    this.templateInstanceService = templateInstanceService;
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(cedarConfig.getTemplateRESTAPI().getSummaries().getTemplate().getFields());
  }

  @POST
  @Timed
  public Response createTemplate(@QueryParam(QP_IMPORT_MODE) Optional<Boolean> importMode) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_CREATE);
    // TODO: the non-empty check is not working
    //c.must(c.request().getRequestBody()).be(NonEmpty);

    JsonNode template = c.request().getRequestBody().asJson();
    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    checkImportModeSetProvenanceAndId(CedarNodeType.TEMPLATE, template, pi, importMode);

    ValidationReport validationReport = validateTemplate(template);
    ReportUtils.outputLogger(logger, validationReport, true);
    JsonNode createdTemplate = null;
    try {
      templateFieldService.saveNewFieldsAndReplaceIds(template, pi, provenanceUtil, linkedDataUtil);
      createdTemplate = templateService.createTemplate(template);
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .errorKey(CedarErrorKey.TEMPLATE_NOT_CREATED)
          .errorMessage("The template can not be created")
          .exception(e)
          .build();
    }
    MongoUtils.removeIdField(createdTemplate);

    String id = createdTemplate.get("@id").asText();

    URI createdTemplateUri = CedarUrlUtil.getIdURI(uriInfo, id);
    return CedarResponse.created(createdTemplateUri)
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, validationReport.getValidationStatus())
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_REPORT, validationReport)
        .entity(createdTemplate).build();
  }

  @GET
  @Timed
  @Path("/{id}")
  public Response findTemplate(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_READ);

    JsonNode template = null;
    try {
      template = templateService.findTemplate(id);
    } catch (IOException | ProcessingException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_NOT_FOUND)
          .errorMessage("The template can not be found by id:" + id)
          .exception(e)
          .build();
    }
    if (template == null) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_NOT_FOUND)
          .errorMessage("The template can not be found by id:" + id)
          .build();
    } else {
      MongoUtils.removeIdField(template);
      return Response.ok().entity(template).build();
    }
  }

  @GET
  @Timed
  public Response findAllTemplates(@QueryParam(QP_LIMIT) Optional<Integer> limitParam,
                                   @QueryParam(QP_OFFSET) Optional<Integer> offsetParam,
                                   @QueryParam(QP_SUMMARY) Optional<Boolean> summaryParam,
                                   @QueryParam(QP_FIELD_NAMES) Optional<String> fieldNamesParam) throws
      CedarException {

    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_READ);

    PagedQuery pagedQuery = new PagedQuery(cedarConfig.getTemplateRESTAPI().getPagination())
        .limit(limitParam)
        .offset(offsetParam);
    pagedQuery.validate();

    Integer limit = pagedQuery.getLimit();
    Integer offset = pagedQuery.getOffset();
    Boolean summary = ensureSummary(summaryParam);

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
      return CedarResponse.internalServerError()
          .errorKey(CedarErrorKey.TEMPLATES_NOT_LISTED)
          .errorMessage("The templates can not be listed")
          .exception(e)
          .build();
    }
    long total = templateService.count();
    checkPagingParametersAgainstTotal(offset, total);

    String absoluteUrl = uriInfo.getAbsolutePathBuilder().build().toString();
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
  public Response updateTemplate(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_UPDATE);

    JsonNode newTemplate = c.request().getRequestBody().asJson();
    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    provenanceUtil.patchProvenanceInfo(newTemplate, pi);

    ValidationReport validationReport = validateTemplate(newTemplate);
    ReportUtils.outputLogger(logger, validationReport, true);
    JsonNode updatedTemplate = null;
    try {
      templateFieldService.saveNewFieldsAndReplaceIds(newTemplate, pi, provenanceUtil, linkedDataUtil);
      updatedTemplate = templateService.updateTemplate(id, newTemplate);
    } catch (InstanceNotFoundException e) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_NOT_FOUND)
          .errorMessage("The template can not be found by id:" + id)
          .exception(e)
          .build();
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_NOT_UPDATED)
          .errorMessage("The template can not be updated by id:" + id)
          .exception(e)
          .build();
    }
    MongoUtils.removeIdField(updatedTemplate);
    return CedarResponse.ok()
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, validationReport.getValidationStatus())
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_REPORT, validationReport)
        .entity(updatedTemplate).build();
  }

  @DELETE
  @Timed
  @Path("/{id}")
  public Response deleteTemplate(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_DELETE);

    long referenceCount = templateInstanceService.countReferencingTemplate(id);

    if (referenceCount != 0) {
      return CedarResponse.badRequest()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_NOT_DELETED)
          .errorReasonKey(CedarErrorReasonKey.TEMPLATE_REFERENCED_IN_INSTANCES)
          .errorMessage("The template can not be deleted since there are instances using it")
          .parameter("referenceCount", referenceCount)
          .build();
    }

    try {
      templateService.deleteTemplate(id);
    } catch (InstanceNotFoundException e) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_NOT_FOUND)
          .errorMessage("The template can not be found by id:" + id)
          .exception(e)
          .build();
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_NOT_DELETED)
          .errorMessage("The template can not be deleted by id:" + id)
          .exception(e)
          .build();
    }
    return CedarResponse.noContent().build();
  }
}