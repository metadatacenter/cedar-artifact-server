package org.metadatacenter.cedar.artifact.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.jsonldjava.core.JsonLdError;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.error.CedarErrorReasonKey;
import org.metadatacenter.exception.ArtifactServerResourceNotFoundException;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.model.CreateOrUpdate;
import org.metadatacenter.model.request.OutputFormatType;
import org.metadatacenter.model.request.OutputFormatTypeDetector;
import org.metadatacenter.model.trimmer.JsonLdDocument;
import org.metadatacenter.model.validation.report.CedarValidationReport;
import org.metadatacenter.model.validation.report.ReportUtils;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.rest.assertion.noun.CedarRequestBody;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.dao.ArtifactRevisionConflictException;
import org.metadatacenter.server.dao.ArtifactWithRevision;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateInstanceService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.artifact.ArtifactYamlTranscoder;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.http.CedarUrlUtil;
import org.metadatacenter.util.mongo.MongoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.metadatacenter.constant.CedarPathParameters.PP_ID;
import static org.metadatacenter.constant.CedarQueryParameters.*;
import static org.metadatacenter.rest.assertion.GenericAssertions.*;

@Path("/template-instances")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateInstancesResource extends AbstractArtifactCrudResource {

  private static final Logger logger = LoggerFactory.getLogger(TemplateInstancesResource.class);

  private final TemplateInstanceService<String, JsonNode> templateInstanceService;
  private final TemplateService<String, JsonNode> templateService;

  public TemplateInstancesResource(CedarConfig cedarConfig, TemplateInstanceService<String, JsonNode> templateInstanceService,
                                   TemplateService<String, JsonNode> templateService) {
    super(cedarConfig, logger, "artifact instance", "artifact instances",
        cedarConfig.getArtifactRESTAPI().getSummaries().getInstance().getFields(), false);
    this.templateInstanceService = templateInstanceService;
    this.templateService = templateService;
  }

  @POST
  @Timed
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  @Consumes({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  public Response createTemplateInstance(@QueryParam(QP_SKIP_VALIDATION) Optional<Boolean> skipValidation,
                                         @QueryParam("compact") Optional<Boolean> compactParam,
                                         String requestBody) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_INSTANCE_CREATE);
    rejectCompactOnWriteOperations(compactParam);
    if (negotiatedArtifactResponseType().isEmpty()) {
      return notAcceptableArtifactFormatResponse();
    }

    CedarRequestBody body = artifactRequestBody(requestBody, CedarResourceType.INSTANCE, templateService::findTemplate);
    c.must(body).be(NonEmpty);
    JsonNode templateInstance = body.asJson();

    enforceMandatoryNullOrMissingId(templateInstance, CedarResourceType.INSTANCE, CedarErrorKey.TEMPLATE_INSTANCE_NOT_CREATED);
    enforceMandatoryName(templateInstance, CedarResourceType.INSTANCE, CedarErrorKey.TEMPLATE_INSTANCE_NOT_CREATED);

    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    setProvenanceAndId(CedarResourceType.INSTANCE, templateInstance, pi);

    // Kept in the signature for wire compatibility only. Artifact validation
    // is unconditional; honoring this legacy switch would reopen a path for an
    // unchecked instance to enter the repository. It previously also left the
    // response null when true, so it neither skipped nor stored coherently.
    ValidationReport validationReport = validateArtifact(templateInstance);
    ReportUtils.outputLogger(logger, validationReport, true);
    String validationStatus = validationReport.getValidationStatus();
    Response response;
    if (validationStatus.equals(CedarValidationReport.IS_VALID)) {
      response = storeArtifactInDatabase(templateInstance, pi, CedarErrorKey.TEMPLATE_INSTANCE_NOT_CREATED);
    } else {
      response = CedarResponse.badRequest()
          .errorMessage(concatenateValidationMessages(validationReport))
          .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_INVALID)
          .errorKey(CedarErrorKey.INVALID_DATA)
          .errorReasonKey(CedarErrorReasonKey.VALIDATION_ERROR)
          .errorMessage("There was an error while validating the artifact")
          .object("validationReport", validationReport)
          .build();
    }
    return negotiateArtifactResponse(response, CedarResourceType.INSTANCE);
  }

  @GET
  @Timed
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml",
      "application/n-quads"})
  public Response findTemplateInstance(@PathParam(PP_ID) String id, @QueryParam(QP_FORMAT) Optional<String> format,
                                       @QueryParam("compact") Optional<Boolean> compactParam) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(id).be(ValidUrl);
    c.must(c.user()).have(CedarPermission.TEMPLATE_INSTANCE_READ);

    Optional<MediaType> responseType = Optional.empty();
    if (format.isEmpty()) {
      responseType = negotiatedArtifactResponseType();
      if (responseType.isEmpty()) {
        return notAcceptableArtifactFormatResponse();
      }
    }

    ArtifactWithRevision<JsonNode> snapshot;
    try {
      snapshot = findArtifactWithRevisionInService(id);
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_INSTANCE_NOT_FOUND)
          .errorMessage("The artifact instance can not be found by id:" + id)
          .exception(e)
          .build();
    }
    if (snapshot == null) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_INSTANCE_NOT_FOUND)
          .errorMessage("The artifact instance can not be found by id:" + id)
          .build();
    } else {
      JsonNode templateInstance = snapshot.content();
      MongoUtils.removeIdField(templateInstance);
      long revision = snapshot.revision();
      // An explicit format names the representation, so it wins over Accept negotiation.
      if (format.isEmpty() && !ArtifactYamlTranscoder.isJson(responseType.get())) {
        return Response.ok()
            .header(HttpHeaders.ETAG, etag(revision))
            .entity(ArtifactYamlTranscoder.jsonToYaml(templateInstance, CedarResourceType.INSTANCE,
                compactParam.isPresent() && compactParam.get()))
            .type(responseType.get())
            .build();
      }
      OutputFormatType formatType = OutputFormatTypeDetector.detectFormat(format);
      return Response.fromResponse(sendFormattedTemplateInstance(templateInstance, formatType))
          .header(HttpHeaders.ETAG, etag(revision))
          .build();
    }
  }

  @GET
  @Timed
  public Response findAllTemplateInstances(@QueryParam(QP_LIMIT) Optional<Integer> limitParam,
                                           @QueryParam(QP_OFFSET) Optional<Integer> offsetParam,
                                           @QueryParam(QP_SUMMARY) Optional<Boolean> summaryParam,
                                           @QueryParam(QP_FIELD_NAMES) Optional<String> fieldNamesParam) throws CedarException {
    return findAllArtifacts(limitParam, offsetParam, summaryParam, fieldNamesParam,
        CedarPermission.TEMPLATE_INSTANCE_READ, CedarErrorKey.TEMPLATE_INSTANCES_NOT_LISTED);
  }

  @PUT
  @Timed
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  @Consumes({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  public Response updateTemplateInstance(@PathParam(PP_ID) String id,
                                         @QueryParam("compact") Optional<Boolean> compactParam,
                                         @QueryParam(QP_VERBATIM) Optional<Boolean> verbatimParam,
                                         String requestBody) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(id).be(ValidUrl);
    rejectCompactOnWriteOperations(compactParam);
    if (negotiatedArtifactResponseType().isEmpty()) {
      return notAcceptableArtifactFormatResponse();
    }

    JsonNode currentTemplateInstance;
    Long currentRevision = null;
    try {
      ArtifactWithRevision<JsonNode> snapshot = findArtifactWithRevisionInService(id);
      if (snapshot == null) {
        currentTemplateInstance = null;
        c.must(c.user()).have(CedarPermission.TEMPLATE_INSTANCE_CREATE);
      } else {
        currentTemplateInstance = snapshot.content();
        c.must(c.user()).have(CedarPermission.TEMPLATE_INSTANCE_UPDATE);
        currentRevision = snapshot.revision();
        Response preconditionFailure = enforceIfMatch(c.getIfMatchHeader(), currentRevision, id);
        if (preconditionFailure != null) {
          return preconditionFailure;
        }
      }
    } catch (IOException e) {
      throw new CedarProcessingException(e);
    }

    boolean verbatim = verbatimParam != null && verbatimParam.isPresent() && verbatimParam.get();
    if (verbatim) {
      c.must(c.user()).have(CedarPermission.WRITE_ARTIFACT_VERBATIM);
      Response refusal = refuseVerbatimWriteWeCannotHonour(id, CedarResourceType.INSTANCE);
      if (refusal != null) {
        return refusal;
      }
    }

    CedarRequestBody body = artifactRequestBody(requestBody, CedarResourceType.INSTANCE, templateService::findTemplate);
    c.must(body).be(NonEmpty);
    JsonNode newInstance = body.asJson();

    enforceMandatoryFieldsInPut(id, newInstance, CedarResourceType.INSTANCE, CedarErrorKey.TEMPLATE_INSTANCE_NOT_UPDATED);
    enforceMandatoryName(newInstance, CedarResourceType.INSTANCE, CedarErrorKey.TEMPLATE_INSTANCE_NOT_CREATED);

    if (verbatim) {
      Response refusal = refuseVerbatimChildIdentifiers(newInstance, CedarResourceType.INSTANCE);
      if (refusal != null) {
        return refusal;
      }
    }

    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    if (!verbatim) {
      JsonNode instanceSchema;
      try {
        instanceSchema = getSchemaSource(templateService, newInstance);
      } catch (IOException e) {
        throw new CedarProcessingException(e);
      }
      logLegacyArtifactRepairs(
          linkedDataUtil.repairInheritedDefects(newInstance, currentTemplateInstance, instanceSchema,
              CedarResourceType.INSTANCE), id);
      provenanceUtil.patchProvenanceInfo(newInstance, pi);

      // add template-element-instance ids if needed. For instance, this may be needed if new items are added to an
      // array
      // of template-element instances
      linkedDataUtil.addElementInstanceIds(newInstance, CedarResourceType.INSTANCE);
      // and name any attribute added during the edit
      linkedDataUtil.addAttributeValuePropertyIris(newInstance, CedarResourceType.INSTANCE);
    }

    {
      ValidationReport validationReport = validateArtifact(newInstance);
      ReportUtils.outputLogger(logger, validationReport, true);
      if (!CedarValidationReport.IS_VALID.equals(validationReport.getValidationStatus())) {
        Response response = CedarResponse.badRequest()
            .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_INVALID)
            .errorKey(CedarErrorKey.INVALID_DATA)
            .errorReasonKey(CedarErrorReasonKey.VALIDATION_ERROR)
            .errorMessage(updateValidationErrorMessage(validationReport))
            .object("validationReport", validationReport)
            .build();
        return negotiateArtifactResponse(response, CedarResourceType.INSTANCE);
      }
    }

    JsonNode outputTemplateInstance = null;
    CreateOrUpdate createOrUpdate = null;
    try {
      if (currentTemplateInstance != null) {
        createOrUpdate = CreateOrUpdate.UPDATE;
        outputTemplateInstance = templateInstanceService.updateTemplateInstance(id, newInstance, currentRevision);
      } else {
        c.must(id).be(ValidId);
        createOrUpdate = CreateOrUpdate.CREATE;
        outputTemplateInstance = templateInstanceService.createTemplateInstance(newInstance);
      }
    } catch (ArtifactRevisionConflictException e) {
      return movedOnResponse(id, currentRevision);
    } catch (IOException | ArtifactServerResourceNotFoundException e) {
      CedarResponse.CedarResponseBuilder responseBuilder = CedarResponse.internalServerError()
          .id(id)
          .exception(e);
      if (createOrUpdate == CreateOrUpdate.CREATE) {
        responseBuilder
            .errorKey(CedarErrorKey.TEMPLATE_INSTANCE_NOT_CREATED)
            .errorMessage("The artifact instance can not be created using id:" + id);
      } else if (createOrUpdate == CreateOrUpdate.UPDATE) {
        responseBuilder
            .errorKey(CedarErrorKey.TEMPLATE_INSTANCE_NOT_UPDATED)
            .errorMessage("The artifact instance can not be updated by id:" + id);
      }
      return responseBuilder.build();
    }
    MongoUtils.removeIdField(outputTemplateInstance);
    CedarResponse.CedarResponseBuilder responseBuilder = null;
    if (createOrUpdate == CreateOrUpdate.UPDATE) {
      responseBuilder = CedarResponse.ok();
    } else {
      URI createdTemplateUri = CedarUrlUtil.getURI(uriInfo);
      responseBuilder = CedarResponse.created(createdTemplateUri);
    }
    responseBuilder
        .header(HttpHeaders.ETAG, etag(createOrUpdate == CreateOrUpdate.UPDATE ? currentRevision + 1L : 1L))
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_VALID)
        .entity(outputTemplateInstance);
    return negotiateArtifactResponse(responseBuilder.build(), CedarResourceType.INSTANCE);
  }

  @DELETE
  @Timed
  @Path("/{id}")
  public Response deleteTemplateInstance(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(id).be(ValidUrl);
    c.must(c.user()).have(CedarPermission.TEMPLATE_INSTANCE_DELETE);
    return deleteArtifactFromDatabase(c, id, CedarErrorKey.TEMPLATE_INSTANCE_NOT_FOUND,
        CedarErrorKey.TEMPLATE_INSTANCE_NOT_DELETED);
  }

  @Override
  protected JsonNode createArtifactInService(JsonNode templateInstance) throws IOException {
    return templateInstanceService.createTemplateInstance(templateInstance);
  }

  @Override
  protected JsonNode findArtifactInService(String id) throws IOException {
    return templateInstanceService.findTemplateInstance(id);
  }

  @Override
  protected ArtifactWithRevision<JsonNode> findArtifactWithRevisionInService(String id) throws IOException {
    return templateInstanceService.findTemplateInstanceWithRevision(id);
  }

  @Override
  protected JsonNode updateArtifactInService(String id, JsonNode content, long expectedRevision) throws IOException,
      ArtifactServerResourceNotFoundException {
    return templateInstanceService.updateTemplateInstance(id, content, expectedRevision);
  }

  @Override
  protected void deleteArtifactInService(String id) throws IOException, ArtifactServerResourceNotFoundException {
    templateInstanceService.deleteTemplateInstance(id);
  }

  @Override
  protected void deleteArtifactInService(String id, long expectedRevision)
      throws IOException, ArtifactServerResourceNotFoundException {
    templateInstanceService.deleteTemplateInstance(id, expectedRevision);
  }

  @Override
  protected List<JsonNode> findAllArtifactsInService(Integer limit, Integer offset, List<String> fieldNames,
                                                     FieldNameInEx includeExclude) throws IOException {
    return templateInstanceService.findAllTemplateInstances(limit, offset, fieldNames, includeExclude);
  }

  @Override
  protected long countArtifactsInService() {
    return templateInstanceService.count();
  }

  /**
   * Validates the instance against the template it is based on, and prunes what that template shows to
   * be dead.
   *
   * <p>The prune belongs here because this is where the template is: it is fetched to validate against,
   * on create and on update alike, and an instance whose template cannot be found is refused by that
   * fetch rather than stored. Removing a term needs the template — only it tells an attribute nobody
   * names any more from a child the instance has not filled in — so doing it anywhere else would mean
   * loading the template twice or reaching for one from a class that has no way to.
   *
   * <p>It runs before validation rather than after, because a term for an attribute that no longer
   * exists is not something the instance should be judged on.
   */
  @Override
  protected ValidationReport validateArtifact(JsonNode templateInstance) throws CedarException {
    try {
      JsonNode instanceSchema = getSchemaSource(templateService, templateInstance);
      linkedDataUtil.pruneOrphanPropertyIris(templateInstance, instanceSchema, CedarResourceType.INSTANCE);
      return validateTemplateInstance(templateInstance, instanceSchema);
    } catch (IOException e) {
      throw newCedarException(e.getMessage());
    }
  }

  private Response sendFormattedTemplateInstance(JsonNode templateInstance, OutputFormatType formatType) throws CedarException {
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

}
