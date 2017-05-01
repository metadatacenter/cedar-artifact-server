package org.metadatacenter.cedar.template.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.jsonldjava.core.JsonLdError;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.error.CedarErrorPack;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.model.core.CedarModelVocabulary;
import org.metadatacenter.model.request.OutputFormatType;
import org.metadatacenter.model.request.OutputFormatTypeDetector;
import org.metadatacenter.model.trimmer.JsonLdDocument;
import org.metadatacenter.model.validation.report.ReportUtils;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
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

@Path("/template-instances")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateInstancesResource extends AbstractTemplateServerResource {

  private static final Logger logger = LoggerFactory.getLogger(TemplateInstancesResource.class);

  private final TemplateInstanceService<String, JsonNode> templateInstanceService;
  private final TemplateService<String, JsonNode> templateService;

  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  public TemplateInstancesResource(CedarConfig cedarConfig, TemplateInstanceService<String, JsonNode>
      templateInstanceService, TemplateService<String, JsonNode> templateService) {
    super(cedarConfig);
    this.templateInstanceService = templateInstanceService;
    this.templateService = templateService;
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(cedarConfig.getTemplateRESTAPI().getSummaries().getInstance().getFields());
  }

  @POST
  @Timed
  public Response createTemplateInstance(@QueryParam(QP_IMPORT_MODE) Optional<Boolean> importMode) throws
      CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_INSTANCE_CREATE);
    // TODO: the non-empty check is not working
    //c.must(c.request().getRequestBody()).be(NonEmpty);

    JsonNode templateInstance = c.request().getRequestBody().asJson();
    ValidationReport validationReport = validateTemplateInstance(templateInstance);
    ReportUtils.outputLogger(logger, validationReport, true);

    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    checkImportModeSetProvenanceAndId(CedarNodeType.INSTANCE, templateInstance, pi, importMode);

    JsonNode createdTemplateInstance = null;
    try {
      createdTemplateInstance = templateInstanceService.createTemplateInstance(templateInstance);
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .errorKey(CedarErrorKey.TEMPLATE_INSTANCE_NOT_CREATED)
          .errorMessage("The template instance can not be created")
          .exception(e)
          .build();
    }
    MongoUtils.removeIdField(createdTemplateInstance);

    String id = createdTemplateInstance.get("@id").asText();

    URI uri = CedarUrlUtil.getIdURI(uriInfo, id);
    return CedarResponse.created(uri)
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, validationReport.getValidationStatus())
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_REPORT, validationReport)
        .entity(createdTemplateInstance).build();
  }

  @GET
  @Timed
  @Path("/{id}")
  public Response findTemplateInstance(@PathParam(PP_ID) String id, @QueryParam(QP_FORMAT) Optional<String> format)
      throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_INSTANCE_READ);

    JsonNode templateInstance = null;
    try {
      templateInstance = templateInstanceService.findTemplateInstance(id);
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_INSTANCE_NOT_FOUND)
          .errorMessage("The template instance can not be found by id:" + id)
          .exception(e)
          .build();
    }
    if (templateInstance == null) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_INSTANCE_NOT_FOUND)
          .errorMessage("The template instance can not be found by id:" + id)
          .build();
    } else {
      OutputFormatType formatType = OutputFormatTypeDetector.detectFormat(format);
      MongoUtils.removeIdField(templateInstance);
      Response response = sendFormattedTemplateInstance(templateInstance, formatType);
      return response;
    }
  }

  @GET
  @Timed
  public Response findAllTemplateInstances(@QueryParam(QP_LIMIT) Optional<Integer> limitParam,
                                           @QueryParam(QP_OFFSET) Optional<Integer> offsetParam,
                                           @QueryParam(QP_SUMMARY) Optional<Boolean> summaryParam,
                                           @QueryParam(QP_FIELD_NAMES) Optional<String> fieldNamesParam) throws
      CedarException {

    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_INSTANCE_READ);

    PagedQuery pagedQuery = new PagedQuery(cedarConfig.getTemplateRESTAPI().getPagination())
        .limit(limitParam)
        .offset(offsetParam);
    pagedQuery.validate();

    Integer limit = pagedQuery.getLimit();
    Integer offset = pagedQuery.getOffset();
    Boolean summary = ensureSummary(summaryParam);

    List<String> fieldNameList = getAndCheckFieldNames(fieldNamesParam, summary);
    Map<String, Object> r = new HashMap<>();
    List<JsonNode> instances = null;
    try {
      if (summary) {
        instances = templateInstanceService.findAllTemplateInstances(limit, offset, FIELD_NAMES_SUMMARY_LIST,
            FieldNameInEx.INCLUDE);
      } else if (fieldNameList != null) {
        instances = templateInstanceService.findAllTemplateInstances(limit, offset, fieldNameList, FieldNameInEx
            .INCLUDE);
      } else {
        instances = templateInstanceService.findAllTemplateInstances(limit, offset, FIELD_NAMES_EXCLUSION_LIST,
            FieldNameInEx.EXCLUDE);
      }
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .errorKey(CedarErrorKey.TEMPLATE_INSTANCES_NOT_LISTED)
          .errorMessage("The template instances can not be listed")
          .exception(e)
          .build();
    }
    long total = templateInstanceService.count();
    checkPagingParametersAgainstTotal(offset, total);

    String absoluteUrl = uriInfo.getAbsolutePathBuilder().build().toString();
    String linkHeader = LinkHeaderUtil.getPagingLinkHeader(absoluteUrl, total, limit, offset);
    Response.ResponseBuilder responseBuilder = Response.ok().entity(instances);
    responseBuilder.header(CustomHttpConstants.HEADER_TOTAL_COUNT, String.valueOf(total));
    if (!linkHeader.isEmpty()) {
      responseBuilder.header(HttpConstants.HTTP_HEADER_LINK, linkHeader);
    }
    return responseBuilder.build();
  }

  @PUT
  @Timed
  @Path("/{id}")
  public Response updateTemplateInstance(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_INSTANCE_UPDATE);

    JsonNode newInstance = c.request().getRequestBody().asJson();
    ValidationReport validationReport = validateTemplateInstance(newInstance);
    ReportUtils.outputLogger(logger, validationReport, true);

    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    provenanceUtil.patchProvenanceInfo(newInstance, pi);
    JsonNode updatedTemplateInstance = null;
    try {
      updatedTemplateInstance = templateInstanceService.updateTemplateInstance(id, newInstance);
    } catch (InstanceNotFoundException e) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_INSTANCE_NOT_FOUND)
          .errorMessage("The template instance can not be found by id:" + id)
          .exception(e)
          .build();
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_INSTANCE_NOT_UPDATED)
          .errorMessage("The template instance can not be updated by id:" + id)
          .exception(e)
          .build();
    }
    MongoUtils.removeIdField(updatedTemplateInstance);
    return CedarResponse.ok()
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, validationReport.getValidationStatus())
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_REPORT, validationReport)
        .entity(updatedTemplateInstance).build();
  }

  @DELETE
  @Timed
  @Path("/{id}")
  public Response deleteTemplateInstance(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_INSTANCE_DELETE);
    try {
      templateInstanceService.deleteTemplateInstance(id);
    } catch (InstanceNotFoundException e) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_INSTANCE_NOT_FOUND)
          .errorMessage("The template instance can not be found by id:" + id)
          .exception(e)
          .build();
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_INSTANCE_NOT_DELETED)
          .errorMessage("The template instance can not be deleted by id:" + id)
          .exception(e)
          .build();
    }
    return CedarResponse.noContent().build();
  }



  private Response sendFormattedTemplateInstance(JsonNode templateInstance, OutputFormatType formatType) throws
      CedarException {
    Object responseObject = null;
    String mediaType = null;
    if (formatType == OutputFormatType.JSONLD) { // The assumption is the formatType is already a valid-and-supported
      // type
      responseObject = templateInstance;
      mediaType = MediaType.APPLICATION_JSON;
    } else if (formatType == OutputFormatType.JSON) {
      responseObject = getJsonString(templateInstance);
      mediaType = MediaType.APPLICATION_JSON;
    } else if (formatType == OutputFormatType.RDF_NQUAD) {
      responseObject = getRdfString(templateInstance);
      mediaType = "application/n-quads";
    } else {
      throw new CedarException("Programming error: no handler is programmed for format type: " + formatType) {
      };
    }
    return Response.ok(responseObject, mediaType).build();
  }

  private JsonNode getJsonString(JsonNode templateInstance) {
    return new JsonLdDocument(templateInstance).asJson();
  }

  private String getRdfString(JsonNode templateInstance) throws CedarException {
    try {
      return new JsonLdDocument(templateInstance).asRdf();
    } catch (JsonLdError e) {
      throw new CedarProcessingException("Error while converting the instance to RDF", e);
    }
  }

  private ValidationReport validateTemplateInstance(JsonNode templateInstance) throws CedarException {
    try {
      JsonNode instanceSchema = getSchemaSource(templateInstance);
      return validateTemplateInstance(templateInstance, instanceSchema);
    } catch (IOException | ProcessingException e) {
      throw newCedarException(e.getMessage());
    }
  }

  private JsonNode getSchemaSource(JsonNode templateInstance) throws IOException, ProcessingException, CedarException {
    checkInstanceSchemaExists(templateInstance);
    String templateRefId = templateInstance.get(CedarModelVocabulary.SCHEMA_IS_BASED_ON).asText();
    JsonNode template = templateService.findTemplate(templateRefId);
    MongoUtils.removeIdField(template);
    return template;
  }

  private static JsonNode checkInstanceSchemaExists(JsonNode templateInstance) throws CedarException {
    JsonNode isBasedOnNode = templateInstance.path(CedarModelVocabulary.SCHEMA_IS_BASED_ON);
    if (isBasedOnNode.isMissingNode()) {
      throw newBadRequestException(String.format("Template instance has a missing property ('%s')",
          CedarModelVocabulary.SCHEMA_IS_BASED_ON));
    }
    return templateInstance;
  }
}