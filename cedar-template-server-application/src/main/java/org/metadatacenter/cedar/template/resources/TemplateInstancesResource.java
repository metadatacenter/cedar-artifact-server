package org.metadatacenter.cedar.template.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.jsonldjava.core.JsonLdError;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serializer.OutputPropertyUtils;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.model.request.OutputFormatType;
import org.metadatacenter.model.request.OutputFormatTypeDetector;
import org.metadatacenter.model.validation.JsonLdDocument;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateInstanceService;
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

@Path("/template-instances")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateInstancesResource extends AbstractTemplateServerResource {

  private final TemplateInstanceService<String, JsonNode> templateInstanceService;

  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  public TemplateInstancesResource(CedarConfig cedarConfig, TemplateInstanceService<String, JsonNode>
      templateInstanceService) {
    super(cedarConfig);
    this.templateInstanceService = templateInstanceService;
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

    //TODO: test if it is not empty
    //c.must(c.request().getRequestBody()).be(NonEmpty);
    JsonNode templateInstance = c.request().getRequestBody().asJson();

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
    return Response.created(uri).entity(createdTemplateInstance).build();
  }

  @GET
  @Timed
  @Path("/{id}")
  public Response findTemplateInstance(@PathParam(PP_ID) String id, @QueryParam(QP_FORMAT) Optional<String> format)
      throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_INSTANCE_READ);

    OutputFormatType formatType = OutputFormatTypeDetector.detectFormat(format);
    if (formatType == OutputFormatType.UNKNOWN) {
      return CedarResponse.badRequest()
          .errorKey(CedarErrorKey.UNKNOWN_INSTANCE_OUTPUT_FORMAT)
          .errorMessage("Unknown requested output format: " + format.get())
          .parameter("requestedFormat", format.get())
          .parameter("expectedFormats", OutputFormatType.values())
          .build();
    }
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
      MongoUtils.removeIdField(templateInstance);
      Object responseObject = null;
      String mediaType = null;
      if (formatType == OutputFormatType.JSONLD) {
        responseObject = templateInstance;
        mediaType = MediaType.APPLICATION_JSON;
      } else if (formatType == OutputFormatType.JSON) {
        responseObject = new JsonLdDocument(templateInstance).asJson();
        mediaType = MediaType.APPLICATION_JSON;
      } else if (formatType == OutputFormatType.RDF_NQUAD) {
        try {
          responseObject = new JsonLdDocument(templateInstance).asRdf();
          mediaType = "application/n-quads"; // XXX: Create a constant
        } catch (JsonLdError e) {
          throw new CedarProcessingException("Error while converting the instance to RDF", e);
        }
      }
      return Response.ok(responseObject, mediaType).build();
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
    return Response.ok().entity(updatedTemplateInstance).build();
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

}