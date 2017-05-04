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
import org.metadatacenter.model.validation.report.ReportUtils;
import org.metadatacenter.model.validation.report.ValidationReport;
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

@Path("/template-elements")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateElementsResource extends AbstractTemplateServerResource {

  private static final Logger logger = LoggerFactory.getLogger(TemplateInstancesResource.class);

  private static TemplateElementService<String, JsonNode> templateElementService;
  private static TemplateFieldService<String, JsonNode> templateFieldService;

  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  public TemplateElementsResource(CedarConfig cedarConfig, TemplateElementService<String, JsonNode>
      templateElementService, TemplateFieldService<String, JsonNode> templateFieldService) {
    super(cedarConfig);
    TemplateElementsResource.templateElementService = templateElementService;
    TemplateElementsResource.templateFieldService = templateFieldService;
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(cedarConfig.getTemplateRESTAPI().getSummaries().getElement().getFields());
  }

  @POST
  @Timed
  public Response createTemplateElement(@QueryParam(QP_IMPORT_MODE) Optional<Boolean> importMode) throws
      CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_ELEMENT_CREATE);
    // TODO: not working
    //c.must(c.request().getRequestBody()).be(NonEmpty);

    JsonNode templateElement = c.request().getRequestBody().asJson();
    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    checkImportModeSetProvenanceAndId(CedarNodeType.ELEMENT, templateElement, pi, importMode);

    ValidationReport validationReport = validateTemplateElement(templateElement);
    ReportUtils.outputLogger(logger, validationReport, true);
    JsonNode createdTemplateElement = null;
    try {
      templateFieldService.saveNewFieldsAndReplaceIds(templateElement, pi, provenanceUtil, linkedDataUtil);
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

    URI createdElementUri = CedarUrlUtil.getIdURI(uriInfo, id);
    return CedarResponse.created(createdElementUri)
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, validationReport.getValidationStatus())
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_REPORT, validationReport)
        .entity(createdTemplateElement).build();
  }

  @GET
  @Timed
  @Path("/{id}")
  public Response findTemplateElement(@PathParam(PP_ID) String id) throws CedarException {
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
  public Response findAllTemplateElements(@QueryParam(QP_LIMIT) Optional<Integer> limitParam,
                                          @QueryParam(QP_OFFSET) Optional<Integer> offsetParam,
                                          @QueryParam(QP_SUMMARY) Optional<Boolean> summaryParam,
                                          @QueryParam(QP_FIELD_NAMES) Optional<String> fieldNamesParam) throws
      CedarException {

    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_ELEMENT_READ);

    PagedQuery pagedQuery = new PagedQuery(cedarConfig.getTemplateRESTAPI().getPagination())
        .limit(limitParam)
        .offset(offsetParam);
    pagedQuery.validate();

    Integer limit = pagedQuery.getLimit();
    Integer offset = pagedQuery.getOffset();
    Boolean summary = ensureSummary(summaryParam);

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
  public Response updateTemplateElement(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_ELEMENT_UPDATE);

    JsonNode newElement = c.request().getRequestBody().asJson();
    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    provenanceUtil.patchProvenanceInfo(newElement, pi);

    ValidationReport validationReport = validateTemplateElement(newElement);
    ReportUtils.outputLogger(logger, validationReport, true);
    JsonNode updatedTemplateElement = null;
    try {
      templateFieldService.saveNewFieldsAndReplaceIds(newElement, pi, provenanceUtil, linkedDataUtil);
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
    return CedarResponse.ok()
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, validationReport.getValidationStatus())
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_REPORT, validationReport)
        .entity(updatedTemplateElement).build();
  }

  @DELETE
  @Timed
  @Path("/{id}")
  public Response deleteTemplateElement(@PathParam(PP_ID) String id) throws CedarException {
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