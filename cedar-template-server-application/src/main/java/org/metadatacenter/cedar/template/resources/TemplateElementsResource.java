package org.metadatacenter.cedar.template.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.exception.TemplateServerResourceNotFoundException;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.model.CreateOrUpdate;
import org.metadatacenter.model.validation.report.ReportUtils;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.util.ModelUtil;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.http.CedarUrlUtil;
import org.metadatacenter.util.http.LinkHeaderUtil;
import org.metadatacenter.util.http.PagedQuery;
import org.metadatacenter.util.mongo.MongoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import static org.metadatacenter.constant.CedarPathParameters.PP_ID;
import static org.metadatacenter.constant.CedarQueryParameters.*;
import static org.metadatacenter.rest.assertion.GenericAssertions.*;

@Path("/template-elements")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateElementsResource extends AbstractTemplateServerResource {

  private static final Logger logger = LoggerFactory.getLogger(TemplateInstancesResource.class);

  private static TemplateElementService<String, JsonNode> templateElementService;

  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  public TemplateElementsResource(CedarConfig cedarConfig, TemplateElementService<String, JsonNode>
      templateElementService) {
    super(cedarConfig);
    TemplateElementsResource.templateElementService = templateElementService;
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(cedarConfig.getTemplateRESTAPI().getSummaries().getElement().getFields());
  }

  @POST
  @Timed
  public Response createTemplateElement() throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_ELEMENT_CREATE);
    c.must(c.request().getRequestBody()).be(NonEmpty);

    JsonNode templateElement = c.request().getRequestBody().asJson();

    enforceMandatoryNullOrMissingId(templateElement, CedarNodeType.ELEMENT, CedarErrorKey.TEMPLATE_ELEMENT_NOT_CREATED);
    enforceMandatoryName(templateElement, CedarNodeType.ELEMENT, CedarErrorKey.TEMPLATE_ELEMENT_NOT_CREATED);

    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    setProvenanceAndId(CedarNodeType.ELEMENT, templateElement, pi);

    ValidationReport validationReport = validateTemplateElement(templateElement);
    ReportUtils.outputLogger(logger, validationReport, true);
    JsonNode createdTemplateElement = null;
    try {
      ModelUtil.ensureFieldIdsRecursively(templateElement, pi, provenanceUtil, linkedDataUtil);
      createdTemplateElement = templateElementService.createTemplateElement(templateElement);
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .errorKey(CedarErrorKey.TEMPLATE_ELEMENT_NOT_CREATED)
          .errorMessage("The template element can not be created")
          .exception(e)
          .build();
    }
    MongoUtils.removeIdField(createdTemplateElement);

    String id = createdTemplateElement.get(LinkedData.ID).asText();

    URI createdElementUri = CedarUrlUtil.getIdURI(uriInfo, id);
    return CedarResponse.created(createdElementUri)
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, validationReport.getValidationStatus())
        .entity(createdTemplateElement).build();
  }

  @GET
  @Timed
  @Path("/{id}")
  public Response findTemplateElement(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_ELEMENT_READ);
    c.must(id).be(ValidUrl);

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
    c.must(id).be(ValidUrl);
    c.must(c.user()).have(CedarPermission.TEMPLATE_ELEMENT_UPDATE);
    c.must(c.request().getRequestBody()).be(NonEmpty);

    JsonNode newElement = c.request().getRequestBody().asJson();

    enforceMandatoryFieldsInPut(id, newElement, CedarNodeType.ELEMENT, CedarErrorKey.TEMPLATE_ELEMENT_NOT_UPDATED);
    enforceMandatoryName(newElement, CedarNodeType.ELEMENT, CedarErrorKey.TEMPLATE_ELEMENT_NOT_UPDATED);

    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    provenanceUtil.patchProvenanceInfo(newElement, pi);

    ValidationReport validationReport = validateTemplateElement(newElement);
    ReportUtils.outputLogger(logger, validationReport, true);
    JsonNode outputTemplateElement = null;
    CreateOrUpdate createOrUpdate = null;
    try {
      JsonNode currentTemplateElement = templateElementService.findTemplateElement(id);
      ModelUtil.ensureFieldIdsRecursively(newElement, pi, provenanceUtil, linkedDataUtil);
      if (currentTemplateElement != null) {
        createOrUpdate = CreateOrUpdate.UPDATE;
        outputTemplateElement = templateElementService.updateTemplateElement(id, newElement);
      } else {
        c.must(id).be(ValidId);
        createOrUpdate = CreateOrUpdate.CREATE;
        outputTemplateElement = templateElementService.createTemplateElement(newElement);
      }
    } catch (IOException | ProcessingException | TemplateServerResourceNotFoundException e) {
      CedarResponse.CedarResponseBuilder responseBuilder = CedarResponse.internalServerError()
          .id(id)
          .exception(e);
      if (createOrUpdate == CreateOrUpdate.CREATE) {
        responseBuilder
            .errorKey(CedarErrorKey.TEMPLATE_ELEMENT_NOT_CREATED)
            .errorMessage("The template element can not be created using id:" + id);
      } else if (createOrUpdate == CreateOrUpdate.UPDATE) {
        responseBuilder
            .errorKey(CedarErrorKey.TEMPLATE_ELEMENT_NOT_UPDATED)
            .errorMessage("The template element can not be updated by id:" + id);
      }
      return responseBuilder.build();
    }
    MongoUtils.removeIdField(outputTemplateElement);
    CedarResponse.CedarResponseBuilder responseBuilder = null;
    if (createOrUpdate == CreateOrUpdate.UPDATE) {
      responseBuilder = CedarResponse.ok();
    } else {
      URI createdTemplateElementUri = CedarUrlUtil.getURI(uriInfo);
      responseBuilder = CedarResponse.created(createdTemplateElementUri);
    }
    responseBuilder
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, validationReport.getValidationStatus())
        .entity(outputTemplateElement);
    return responseBuilder.build();
  }

  @DELETE
  @Timed
  @Path("/{id}")
  public Response deleteTemplateElement(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_ELEMENT_DELETE);
    c.must(id).be(ValidUrl);

    try {
      templateElementService.deleteTemplateElement(id);
    } catch (TemplateServerResourceNotFoundException e) {
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