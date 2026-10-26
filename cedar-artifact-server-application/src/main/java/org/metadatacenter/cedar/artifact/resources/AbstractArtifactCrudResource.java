package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoException;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.error.CedarErrorReasonKey;
import org.metadatacenter.exception.ArtifactServerResourceNotFoundException;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.exception.CedarProcessingException;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.model.request.OutputFormatType;
import org.metadatacenter.model.CreateOrUpdate;
import org.metadatacenter.model.validation.report.CedarValidationReport;
import org.metadatacenter.model.validation.report.ReportUtils;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.rest.assertion.noun.CedarRequestBody;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.dao.ArtifactRevisionConflictException;
import org.metadatacenter.server.dao.TemplateReferencedException;
import org.metadatacenter.server.dao.ArtifactWithRevision;
import org.metadatacenter.server.RevisionPrecondition;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.util.ModelUtil;
import org.metadatacenter.util.provenance.ProvenanceUtil;
import org.metadatacenter.util.artifact.ArtifactYamlTranscoder;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.http.CedarUrlUtil;
import org.metadatacenter.util.http.PagedQuery;
import org.metadatacenter.util.http.RevisionPreconditionParser;
import org.metadatacenter.util.mongo.MongoUtils;
import org.slf4j.Logger;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.metadatacenter.rest.assertion.GenericAssertions.*;

public abstract class AbstractArtifactCrudResource extends AbstractArtifactServerResource {

  protected final List<String> FIELD_NAMES_SUMMARY_LIST;

  private final CedarResourceType artifactType;
  private final Logger logger;
  private final String artifactLabel;
  private final String artifactsLabel;
  private final boolean ensureFieldIds;

  protected AbstractArtifactCrudResource(CedarConfig cedarConfig, CedarResourceType artifactType, Logger logger, String artifactLabel,
                                         String artifactsLabel, List<String> summaryFields, boolean ensureFieldIds) {
    super(cedarConfig);
    this.artifactType = artifactType;
    this.logger = logger;
    this.artifactLabel = artifactLabel;
    this.artifactsLabel = artifactsLabel;
    this.ensureFieldIds = ensureFieldIds;
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(summaryFields);
  }

  protected abstract ArtifactWithRevision<JsonNode> createArtifactWithRevisionInService(JsonNode artifact) throws IOException;

  protected abstract ArtifactWithRevision<JsonNode> findArtifactWithRevisionInService(String id) throws IOException;

  protected abstract JsonNode updateArtifactInService(String id, JsonNode content, long expectedRevision) throws IOException,
      ArtifactServerResourceNotFoundException;

  protected abstract void deleteArtifactInService(String id) throws IOException,
      ArtifactServerResourceNotFoundException;

  protected abstract void deleteArtifactInService(String id, long expectedRevision) throws IOException,
      ArtifactServerResourceNotFoundException;

  protected abstract List<JsonNode> findAllArtifactsInService(Integer limit, Integer offset, List<String> fieldNames,
                                                              FieldNameInEx includeExclude) throws IOException;

  protected abstract long countArtifactsInService();

  /**
   * Judges the artifact this write is about to store.
   *
   * <p>An implementation may normalize the document on its way through, since validating one shape and
   * storing another is how a stored artifact ends up unchecked. A verbatim write is the exception: it
   * stores the document the caller stated, so nothing may be altered when {@code verbatim} is true, and
   * a document the normalization would have rescued is refused rather than quietly rewritten.
   */
  protected abstract ValidationReport validateArtifact(JsonNode artifact, boolean verbatim) throws CedarException;

  protected String updateValidationErrorMessage(ValidationReport validationReport) {
    return concatenateValidationMessages(validationReport);
  }

  protected Response createArtifact(CedarPermission createPermission, CedarResourceType resourceType,
                                    CedarErrorKey notCreatedKey, String requestBody,
                                    Optional<Boolean> compactParam) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(createPermission);
    rejectCompactOnWriteOperations(compactParam);
    if (negotiatedArtifactResponseType().isEmpty()) {
      return notAcceptableArtifactFormatResponse();
    }

    CedarRequestBody body = artifactRequestBody(requestBody, resourceType);
    c.must(body).be(NonEmpty);
    JsonNode artifact = body.asJson();

    enforceMandatoryNullOrMissingId(artifact, resourceType, notCreatedKey);
    enforceMandatoryName(artifact, resourceType, notCreatedKey);
    enforceChildArtifactTypes(artifact, resourceType, notCreatedKey);
    JsonSchemaTitleAndDescription.derive(artifact, resourceType);

    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    setProvenanceAndId(resourceType, artifact, pi);

    Response response = null;
    // Always. Validation used to be switchable, by CEDAR_VALIDATION_ENABLED, and an artifact stored
    // without it is one nothing has ever checked against the model it claims to follow.
    {
      ValidationReport validationReport = validateArtifact(artifact, false);
      ReportUtils.outputLogger(logger, validationReport, true);
      boolean canSave = validationReport.canSave();
      if (canSave) {
        response = storeArtifactInDatabase(artifact, pi, notCreatedKey);
      } else {
        response = CedarResponse.badRequest()
            .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_INVALID)
            .errorKey(CedarErrorKey.INVALID_DATA)
            .errorReasonKey(CedarErrorReasonKey.VALIDATION_ERROR)
            .message(concatenateValidationMessages(validationReport))
            .object("validationReport", validationReport)
            .build();
      }
    }
    return negotiateArtifactResponse(response, resourceType);
  }

  protected Response storeArtifactInDatabase(JsonNode artifact, ProvenanceInfo pi, CedarErrorKey notCreatedKey)
      throws CedarException {
    try {
      if (ensureFieldIds) {
        logReplacedChildIdentifiers(
            ModelUtil.ensureFieldIdsRecursively(artifact, pi, provenanceUtil, linkedDataUtil), null);
        Response refusal = refuseInvalidNormalizedArtifact(artifact);
        if (refusal != null) {
          return refusal;
        }
      }
      var created = createArtifactWithRevisionInService(artifact);
      JsonNode createdArtifact = created.content();
      MongoUtils.removeIdField(createdArtifact);
      String id = createdArtifact.get(LinkedData.ID).asText();
      URI createdArtifactUri = CedarUrlUtil.getIdURI(uriInfo, linkedDataUtil.resourcePathId(artifactType, id));
      return CedarResponse.created(createdArtifactUri)
          .header(HttpHeaders.ETAG, etag(created.revision()))
          .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_VALID)
          .entity(createdArtifact).build();
    } catch (ArtifactRevisionConflictException e) {
      return movedOnResponse(artifact.path(LinkedData.ID).asText(), null);
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .errorKey(notCreatedKey)
          .message("The " + artifactLabel + " can not be created")
          .exception(e)
          .build();
    }
  }

  /**
   * Records each child identifier this write replaced with a minted one. Only a child that arrived holding
   * a value is reported: replacing something that is not an absolute IRI destroys it, and nothing else in
   * the stack keeps what it was, so without this a repair pass cannot afterwards say which artifacts it
   * altered or what they held. A child arriving with no identifier loses nothing and would only add noise,
   * since that is the ordinary path for a field added in the designer.
   */
  private void logReplacedChildIdentifiers(List<ModelUtil.MintedChildId> minted, String artifactId) {
    for (ModelUtil.MintedChildId mint : minted) {
      if (mint.destroyedAValue()) {
        logger.warn("Replaced the identifier of child '{}' in {}: '{}' was not an absolute IRI, minted {}",
            mint.property(), artifactId == null ? "a new " + artifactLabel : artifactId,
            mint.replaced(), mint.minted());
      }
    }
  }

  protected Response findArtifact(String id, CedarPermission readPermission, CedarErrorKey notFoundKey,
                                  CedarResourceType resourceType, Optional<Boolean> compactParam) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(readPermission);
    id = linkedDataUtil.resolveResourceId(artifactType, id);
    c.must(id).be(ValidUrl);

    Optional<MediaType> responseType = negotiatedArtifactResponseType();
    if (responseType.isEmpty()) {
      return notAcceptableArtifactFormatResponse();
    }

    ArtifactWithRevision<JsonNode> snapshot;
    try {
      snapshot = findArtifactWithRevisionInService(id);
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(notFoundKey)
          .message("The " + artifactLabel + " can not be found by id:" + id)
          .exception(e)
          .build();
    }
    if (snapshot == null) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(notFoundKey)
          .message("The " + artifactLabel + " can not be found by id:" + id)
          .build();
    } else {
      JsonNode artifact = snapshot.content();
      MongoUtils.removeIdField(artifact);
      long revision = snapshot.revision();
      boolean compact = compactParam.isPresent() && compactParam.get();
      if (!ArtifactYamlTranscoder.isJson(responseType.get())) {
        try {
          String yaml = ArtifactYamlTranscoder.jsonToYaml(artifact, resourceType, compact);
          return Response.ok()
              .header(HttpHeaders.ETAG, etag(revision, responseType.get(), compact))
              .header(HttpHeaders.VARY, HttpHeaders.ACCEPT)
              .entity(yaml)
              .type(responseType.get())
              .build();
        } catch (ArtifactYamlTranscoder.UnreadableArtifactException e) {
          if (!acceptsJson()) {
            return noYamlFormResponse(id, resourceType, e);
          }
        }
      }
      return Response.ok()
          .header(HttpHeaders.ETAG, etag(revision))
          .header(HttpHeaders.VARY, HttpHeaders.ACCEPT)
          .entity(artifact)
          .build();
    }
  }

  protected Response findAllArtifacts(Optional<Integer> limitParam, Optional<Integer> offsetParam,
                                      Optional<Boolean> summaryParam, Optional<String> fieldNamesParam,
                                      CedarPermission readPermission, CedarErrorKey notListedKey) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(readPermission);

    PagedQuery pagedQuery = new PagedQuery(cedarConfig.getArtifactRESTAPI().getPagination())
        .limit(limitParam)
        .offset(offsetParam);
    pagedQuery.validate();

    Integer limit = pagedQuery.getLimit();
    Integer offset = pagedQuery.getOffset();
    Boolean summary = ensureSummary(summaryParam);

    List<String> fieldNameList = getAndCheckFieldNames(fieldNamesParam, summary);
    List<JsonNode> artifacts = null;
    try {
      if (summary) {
        artifacts = findAllArtifactsInService(limit, offset, FIELD_NAMES_SUMMARY_LIST, FieldNameInEx.INCLUDE);
      } else if (fieldNameList != null) {
        artifacts = findAllArtifactsInService(limit, offset, fieldNameList, FieldNameInEx.INCLUDE);
      } else {
        artifacts = findAllArtifactsInService(limit, offset, FIELD_NAMES_EXCLUSION_LIST, FieldNameInEx.EXCLUDE);
      }
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .errorKey(notListedKey)
          .message("The " + artifactsLabel + " can not be listed")
          .exception(e)
          .build();
    }
    long total = countArtifactsInService();

    // An offset past the end is an empty page, as it is on every other CEDAR listing. The links keep
    // the request's own summary and field_names, replacing only the offset and limit.
    return Response.ok()
        .entity(new ArtifactPage(artifacts, uriInfo.getRequestUri().toString(), total, limit, offset))
        .build();
  }

  protected Response updateArtifact(String id, CedarPermission createPermission, CedarPermission updatePermission,
                                    CedarResourceType resourceType,
                                    CedarErrorKey notUpdatedKey, CedarErrorKey notCreatedKey, String requestBody,
                                    Optional<Boolean> compactParam) throws CedarException {
    return updateArtifact(id, createPermission, updatePermission, resourceType, notUpdatedKey, notCreatedKey, requestBody,
        compactParam, Optional.empty());
  }

  protected Response updateArtifact(String id, CedarPermission createPermission, CedarPermission updatePermission,
                                    CedarResourceType resourceType,
                                    CedarErrorKey notUpdatedKey, CedarErrorKey notCreatedKey, String requestBody,
                                    Optional<Boolean> compactParam, Optional<Boolean> verbatimParam)
      throws CedarException {
    return updateArtifact(id, createPermission, updatePermission, resourceType, notUpdatedKey, notCreatedKey,
        requestBody, compactParam, verbatimParam, false);
  }

  protected Response updateArtifact(String id, CedarPermission createPermission, CedarPermission updatePermission,
                                    CedarResourceType resourceType, CedarErrorKey notUpdatedKey,
                                    CedarErrorKey notCreatedKey, String requestBody, Optional<Boolean> compactParam,
                                    Optional<Boolean> verbatimParam, boolean requireNoInstances) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    id = linkedDataUtil.resolveResourceId(artifactType, id);
    c.must(id).be(ValidUrl);
    rejectCompactOnWriteOperations(compactParam);
    if (negotiatedArtifactResponseType().isEmpty()) {
      return notAcceptableArtifactFormatResponse();
    }

    JsonNode currentArtifact;
    Long currentRevision = null;
    try {
      ArtifactWithRevision<JsonNode> snapshot = findArtifactWithRevisionInService(id);
      if (snapshot == null) {
        currentArtifact = null;
        c.must(c.user()).have(createPermission);
        if (c.getIfMatchHeader() != null && !c.getIfMatchHeader().isBlank()) {
          return movedOnResponse(id, null);
        }
      } else {
        currentArtifact = snapshot.content();
        c.must(c.user()).have(updatePermission);
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
      Response refusal = refuseVerbatimWriteWeCannotHonour(id, currentArtifact);
      if (refusal != null) {
        return refusal;
      }
    }

    CedarRequestBody body = artifactRequestBody(requestBody, resourceType);
    c.must(body).be(NonEmpty);
    JsonNode newArtifact = body.asJson();

    enforceReplacementShape(id, newArtifact, resourceType, notUpdatedKey);

    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    if (verbatim) {
      // Nothing is assigned: the document is stored as stated, which is the whole point. Every child
      // identifier must already be an absolute IRI, since minting one would break that promise and
      // storing a temporary one would let the value through the single door that does not check.
      Response refusal = refuseVerbatimChildIdentifiers(newArtifact, resourceType);
      if (refusal != null) {
        return refusal;
      }
    } else {
      assignWhatAWriteAssigns(newArtifact, currentArtifact, pi, resourceType, notUpdatedKey);
    }

    Response response = refuseInvalidReplacement(newArtifact, verbatim);
    if (response == null) {
      response = updateOrCreateArtifactInDatabase(id, newArtifact, pi, c, notCreatedKey, notUpdatedKey,
          verbatim, currentArtifact, currentRevision, requireNoInstances);
    }
    return negotiateArtifactResponse(response, resourceType);
  }

  /** The refusals a PUT makes of the document's shape before it changes anything in it. */
  private void enforceReplacementShape(String id, JsonNode newArtifact, CedarResourceType resourceType,
                                       CedarErrorKey errorKey) throws CedarException {
    enforceMandatoryFieldsInPut(id, newArtifact, resourceType, errorKey);
    enforceMandatoryName(newArtifact, resourceType, errorKey);
    reportMismatchedChildIdPrefixes(newArtifact, resourceType, id);
  }

  /**
   * What an ordinary PUT assigns before it validates: the child types are checked, the title and
   * description are derived, the provenance this write owns is stamped, and a property IRI is given to
   * any child added during the edit. {@code currentArtifact} is null when the write creates.
   */
  private void assignWhatAWriteAssigns(JsonNode newArtifact, JsonNode currentArtifact, ProvenanceInfo pi,
                                       CedarResourceType resourceType, CedarErrorKey errorKey) throws CedarException {
    enforceChildArtifactTypes(newArtifact, resourceType, errorKey);
    JsonSchemaTitleAndDescription.derive(newArtifact, resourceType);
    stampProvenanceForPut(newArtifact, currentArtifact, pi);
    linkedDataUtil.addChildPropertyIris(newArtifact, resourceType);
  }

  /** The PUT's first validation, of the document as received and assigned; null when it is valid. */
  private Response refuseInvalidReplacement(JsonNode newArtifact, boolean verbatim) throws CedarException {
    ValidationReport validationReport = validateArtifact(newArtifact, verbatim);
    ReportUtils.outputLogger(logger, validationReport, true);
    if (validationReport.canSave()) {
      return null;
    }
    return CedarResponse.badRequest()
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_INVALID)
        .errorKey(CedarErrorKey.INVALID_DATA)
        .errorReasonKey(CedarErrorReasonKey.VALIDATION_ERROR)
        .message(updateValidationErrorMessage(validationReport))
        .object("validationReport", validationReport)
        .build();
  }

  /**
   * Judges a replacement for an artifact that does not exist yet, exactly as the PUT that replaces it
   * will once a create has stored it, and stores nothing.
   *
   * <p>A command that creates an artifact and then replaces it learns whether the replacement is valid
   * only after the create has committed. This lets it ask first. The body holds two documents:
   * {@code created}, the document the create will send, and {@code replacement}, the document the PUT
   * will send. The create's own shape checks and assignments are applied to {@code created}, to give
   * the document the PUT will find stored. The replacement then takes the PUT's path against it: its
   * shape checks, its assignments with update semantics, both validations and the child identifiers and
   * provenance assigned between them. Only the storing is left out, so a replacement this answers as
   * valid is one the PUT accepts, and one it refuses is refused with the PUT's own answer.
   *
   * <p>{@code created} is not validated here. The create validates what it is sent, and a command may
   * change that document between asking and creating; a publication, for instance, pins terminology
   * versions. Its validity is the create's to judge when it happens.
   *
   * <p>An identifier that already names an artifact answers 412: the create would not be stored, so
   * no replacement of it can be.
   */
  protected Response judgeReplacementOfArtifactToBeCreated(String id, CedarPermission createPermission,
                                                           CedarPermission updatePermission,
                                                           CedarResourceType resourceType,
                                                           CedarErrorKey notUpdatedKey,
                                                           String requestBody) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(createPermission);
    c.must(c.user()).have(updatePermission);
    id = linkedDataUtil.resolveResourceId(artifactType, id);
    c.must(id).be(ValidUrl);
    c.must(id).be(ValidId);

    JsonNode body;
    try {
      body = requestBody == null || requestBody.isBlank() ? null
          : org.metadatacenter.util.json.JsonMapper.STRICT_MAPPER.readTree(requestBody);
    } catch (IOException e) {
      body = null;
    }
    if (body == null || !body.path("created").isObject() || !body.path("replacement").isObject() || body.size() != 2) {
      return CedarResponse.badRequest()
          .errorKey(CedarErrorKey.INVALID_INPUT)
          .message("Supply the document a create will store, as 'created', and the document that will replace "
              + "it, as 'replacement', and nothing else")
          .build();
    }

    try {
      if (findArtifactWithRevisionInService(id) != null) {
        return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
            .id(id)
            .errorKey(CedarErrorKey.NODE_ALREADY_PRESENT)
            .message("The identifier already names " + artifactLabel + "; a create there would not be stored")
            .build();
      }
    } catch (IOException e) {
      throw new CedarProcessingException(e);
    }

    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());

    // A create by PUT runs the same shape checks under the same key as the replacement that follows it.
    JsonNode stored = body.get("created").deepCopy();
    enforceReplacementShape(id, stored, resourceType, notUpdatedKey);
    assignWhatAWriteAssigns(stored, null, pi, resourceType, notUpdatedKey);
    if (ensureFieldIds) {
      ModelUtil.ensureFieldIdsRecursively(stored, null, pi, provenanceUtil, linkedDataUtil);
    }

    JsonNode replacement = body.get("replacement").deepCopy();
    enforceReplacementShape(id, replacement, resourceType, notUpdatedKey);
    assignWhatAWriteAssigns(replacement, stored, pi, resourceType, notUpdatedKey);
    Response refusal = refuseInvalidReplacement(replacement, false);
    if (refusal == null) {
      refusal = normalizeForStorage(id, replacement, stored, pi, false);
    }
    if (refusal != null) {
      return refusal;
    }
    return CedarResponse.noContent()
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_VALID)
        .build();
  }

  /** Internal lifecycle projection: changes one link, preserving even legacy document shapes. */
  protected Response updateVersionPredecessor(String id, String requestBody) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    id = linkedDataUtil.resolveResourceId(artifactType, id);
    c.must(id).be(ValidUrl);
    // The internal-service filter also authenticates this route. Reuse the existing filesystem
    // administrator authority; this service does not need access to the administrator's secret.
    c.must(c.user()).have(CedarPermission.WRITE_NOT_WRITABLE_NODE);
    // The patch is parsed apart from the store calls below. A store read can fail as a JSON processing
    // error, and that failure is not the caller's.
    JsonNode patch;
    try {
      patch = requestBody == null || requestBody.isBlank() ? null
          : org.metadatacenter.util.json.JsonMapper.STRICT_MAPPER.readTree(requestBody);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      return CedarResponse.badRequest().message("Invalid lifecycle patch JSON").build();
    }
    if (patch == null || !patch.isObject() || patch.size() != 1 || !patch.has("previousVersion")
        || !(patch.get("previousVersion").isNull() || patch.get("previousVersion").isTextual())) {
      return CedarResponse.badRequest().message("Supply only previousVersion, as an absolute URL or null").build();
    }
    String previous = patch.get("previousVersion").isNull() ? null : patch.get("previousVersion").asText();
    if (previous != null) c.must(previous).be(ValidUrl);
    try {
      var snapshot = findArtifactWithRevisionInService(id);
      if (snapshot == null) return CedarResponse.notFound().build();
      if ("*".equals(c.getIfMatchHeader())) {
        return CedarResponse.badRequest().message("A specific artifact revision is required").build();
      }
      Response failure = enforceIfMatch(c.getIfMatchHeader(), snapshot.revision(), id);
      if (failure != null) return failure;
      var document = (com.fasterxml.jackson.databind.node.ObjectNode) snapshot.content().deepCopy();
      if (previous == null) document.remove("pav:previousVersion");
      else document.put("pav:previousVersion", previous);
      MongoUtils.removeIdField(document);
      updateArtifactInService(id, document, snapshot.revision());
      return CedarResponse.ok().header(HttpHeaders.ETAG, etag(snapshot.revision() + 1L)).build();
    } catch (ArtifactRevisionConflictException e) {
      return movedOnSinceRead(id);
    } catch (ArtifactServerResourceNotFoundException e) {
      return disappearedDuringConditionalUpdate(id);
    } catch (IOException e) {
      throw new CedarProcessingException(e);
    }
  }

  /**
   * Stamps the provenance that belongs to this PUT before validation. A PUT can create when its
   * identifier does not exist, and that path must establish creation provenance from the
   * authenticated caller just as POST does. A replacement stamps only modification provenance;
   * {@link #updateOrCreateArtifactInDatabase} then restores its creation provenance from storage.
   */
  protected void stampProvenanceForPut(JsonNode artifact, JsonNode currentArtifact, ProvenanceInfo pi) {
    if (currentArtifact == null) {
      provenanceUtil.addProvenanceInfo(artifact, pi);
    } else {
      provenanceUtil.patchProvenanceInfo(artifact, pi);
    }
  }

  /**
   * Refuses a verbatim write whose promise cannot be kept. The promise is that the stored document is the
   * one supplied, so it needs an artifact to replace -- a request naming an identifier that resolves to
   * nothing would otherwise create, which is a different operation -- and it needs a body this server has
   * not rewritten, which a YAML request is not: a transcode decides the JSON, so what is stored is the
   * transcoder's reading rather than the caller's document.
   *
   * <p>{@code currentArtifact} is what the request's own read found, and it alone decides whether there
   * is an artifact to replace. The write takes the create path whenever that read found nothing, so a
   * second read here that found an artifact another writer had just inserted would let a verbatim write
   * create one.
   */
  protected Response refuseVerbatimWriteWeCannotHonour(String artifactId, JsonNode currentArtifact) {
    if (ArtifactYamlTranscoder.isYaml(httpHeaders.getMediaType())) {
      return CedarResponse.badRequest()
          .id(artifactId)
          .errorKey(CedarErrorKey.VERBATIM_WRITE_REFUSED)
          .message("A verbatim write needs a JSON body: a YAML body is transcoded, so what would be "
              + "stored is not what was sent")
          .build();
    }
    if (currentArtifact == null) {
      return CedarResponse.notFound()
          .id(artifactId)
          .errorKey(CedarErrorKey.ARTIFACT_NOT_FOUND)
          .message("A verbatim write replaces an existing " + artifactLabel + "; this one does not exist")
          .build();
    }
    return null;
  }

  /**
   * Refuses a verbatim write carrying a child identifier that is not an absolute IRI. On an ordinary write
   * such an identifier is minted over; here it cannot be, because nothing may be altered, and it must not
   * be stored, because a temporary identifier resolves to nothing and no later write will replace it.
   * An ordinary write mints at every depth, so every depth is checked, and a nested child is reported by
   * its path through the elements that hold it.
   */
  protected Response refuseVerbatimChildIdentifiers(JsonNode artifact, CedarResourceType resourceType) {
    if (resourceType == CedarResourceType.INSTANCE) {
      List<String> unusableOccurrences = new ArrayList<>();
      artifact.fields().forEachRemaining(field -> {
        if (!LinkedData.CONTEXT.equals(field.getKey())) {
          collectUnusableElementOccurrenceIds(field.getValue(), "/" + field.getKey(), unusableOccurrences);
        }
      });
      if (unusableOccurrences.isEmpty()) {
        return null;
      }
      return CedarResponse.badRequest()
          .errorKey(CedarErrorKey.VERBATIM_WRITE_REFUSED)
          .message("A verbatim write alters nothing, so every element occurrence identifier must already "
              + "be an absolute IRI. These are not: " + String.join(", ", unusableOccurrences))
          .parameter("occurrences", String.join(", ", unusableOccurrences))
          .build();
    } else if (resourceType != CedarResourceType.TEMPLATE && resourceType != CedarResourceType.ELEMENT) {
      return null;
    }
    List<String> unusable = new ArrayList<>();
    collectUnusableChildIds(artifact, "", unusable);
    if (unusable.isEmpty()) {
      return null;
    }
    return CedarResponse.badRequest()
        .errorKey(CedarErrorKey.VERBATIM_WRITE_REFUSED)
        .message("A verbatim write alters nothing, so every child identifier must already be an "
            + "absolute IRI. These are not: " + String.join(", ", unusable))
        .parameter("children", String.join(", ", unusable))
        .build();
  }

  private static void collectUnusableChildIds(JsonNode parent, String parentPath, List<String> unusable) {
    ModelUtil.forEachChild(parent, (name, child) -> {
      String path = parentPath.isEmpty() ? name : parentPath + "/" + name;
      if (!ModelUtil.hasUsableChildId(child)) {
        unusable.add(path);
      }
      collectUnusableChildIds(child, path, unusable);
    });
  }

  private void collectUnusableElementOccurrenceIds(JsonNode node, String path, List<String> unusable) {
    if (node == null) {
      return;
    }
    if (node.isArray()) {
      for (int index = 0; index < node.size(); index++) {
        collectUnusableElementOccurrenceIds(node.get(index), path + "/" + index, unusable);
      }
      return;
    }
    if (!node.isObject()) {
      return;
    }
    if (node.has(LinkedData.CONTEXT) && !ModelUtil.hasUsableChildId(node)) {
      unusable.add(path);
    }
    node.fields().forEachRemaining(field -> {
      if (!LinkedData.CONTEXT.equals(field.getKey()) && !LinkedData.ID.equals(field.getKey())) {
        collectUnusableElementOccurrenceIds(field.getValue(), path + "/" + field.getKey(), unusable);
      }
    });
  }

  protected JsonNode updateUnreferencedArtifactInService(String id, JsonNode body, long revision)
      throws IOException, ArtifactServerResourceNotFoundException {
    throw new UnsupportedOperationException("Only templates support reference-fenced inclusion updates");
  }

  protected Response updateOrCreateArtifactInDatabase(String id, JsonNode body, ProvenanceInfo pi,
                                                      CedarRequestContext c, CedarErrorKey notCreated,
                                                      CedarErrorKey notUpdated, boolean verbatim,
                                                      JsonNode current, Long revision) throws CedarException {
    return updateOrCreateArtifactInDatabase(id, body, pi, c, notCreated, notUpdated, verbatim, current, revision, false);
  }

  protected Response updateOrCreateArtifactInDatabase(String artifactId, JsonNode updatedArtifact, ProvenanceInfo pi,
                                                      CedarRequestContext c, CedarErrorKey notCreatedKey,
                                                      CedarErrorKey notUpdatedKey, boolean verbatim,
                                                      JsonNode currentArtifact, Long currentRevision, boolean requireNoInstances)
      throws CedarException {
    JsonNode outputArtifact = null;
    CreateOrUpdate createOrUpdate = null;
    long outputRevision;
    try {
      Response refusal = normalizeForStorage(artifactId, updatedArtifact, currentArtifact, pi, verbatim);
      if (refusal != null) {
        return refusal;
      }
      if (currentArtifact != null) {
        createOrUpdate = CreateOrUpdate.UPDATE;
        // Browser assessments are advisory. Fence a changed child version against both stored
        // and in-flight instances at the actual write, including callers that skip the assessment.
        // Administrative verbatim repairs retain their explicit separate contract.
        if (!verbatim && artifactType == CedarResourceType.TEMPLATE
            && TemplateValidationShape.replacesChildren(currentArtifact, updatedArtifact)
            && !TemplateValidationShape.equivalent(currentArtifact, updatedArtifact)) {
          requireNoInstances = true;
        }
        outputArtifact = requireNoInstances
            ? updateUnreferencedArtifactInService(artifactId, updatedArtifact, currentRevision)
            : updateArtifactInService(artifactId, updatedArtifact, currentRevision);
        outputRevision = currentRevision + 1L;
      } else {
        if (requireNoInstances) return movedOnResponse(artifactId, null);
        c.must(artifactId).be(ValidId);
        createOrUpdate = CreateOrUpdate.CREATE;
        var created = createArtifactWithRevisionInService(updatedArtifact);
        outputArtifact = created.content();
        outputRevision = created.revision();
      }
      MongoUtils.removeIdField(outputArtifact);
      CedarResponse.CedarResponseBuilder responseBuilder = null;
      if (createOrUpdate == CreateOrUpdate.UPDATE) {
        responseBuilder = CedarResponse.ok();
      } else {
        URI createdArtifactUri = CedarUrlUtil.getURI(uriInfo);
        responseBuilder = CedarResponse.created(createdArtifactUri);
      }
      return responseBuilder
          .header(HttpHeaders.ETAG, etag(outputRevision))
          .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_VALID)
          .entity(outputArtifact)
          .build();
    } catch (TemplateReferencedException e) {
      return referencedTemplateRefusal(artifactId, notUpdatedKey, "updated");
    } catch (ArtifactRevisionConflictException e) {
      // On the create path the read found nothing and another writer inserted the identifier first, so the
      // unique index refused this insert. On the update path the revision this request read is no longer
      // current. Either way the answer names the revision that won, and a retry with it in If-Match
      // becomes an update of what that writer stored.
      return movedOnSinceRead(artifactId);
    } catch (ArtifactServerResourceNotFoundException e) {
      // Only a replacement reports a missing artifact. An insert has nothing to miss.
      return disappearedDuringConditionalUpdate(artifactId);
    } catch (IOException e) {
      return artifactWriteFailureResponse(artifactId, createOrUpdate, notCreatedKey, notUpdatedKey, e);
    }
  }

  /**
   * What a write does to a validated document just before storing it: an update keeps the creation
   * provenance already stored, every child is given an identifier and provenance describing what this
   * write did to it, and the result is validated again. Null when the document is ready to store.
   *
   * <p>A field or an instance takes no normalization here beyond its creation provenance, so it is
   * validated again only when the stored values replaced what the request stated. A stored value the
   * model refuses would otherwise reach storage through a request that was valid, which is what the
   * second validation exists to prevent.
   */
  private Response normalizeForStorage(String artifactId, JsonNode updatedArtifact, JsonNode currentArtifact,
                                       ProvenanceInfo pi, boolean verbatim) throws CedarException {
    boolean creationProvenanceReplaced = false;
    if (currentArtifact != null && !verbatim) {
      JsonNode statedOn = updatedArtifact.get(ProvenanceUtil.PAV_CREATED_ON);
      JsonNode statedBy = updatedArtifact.get(ProvenanceUtil.PAV_CREATED_BY);
      provenanceUtil.preserveCreationProvenance(updatedArtifact, currentArtifact);
      creationProvenanceReplaced = !Objects.equals(statedOn, updatedArtifact.get(ProvenanceUtil.PAV_CREATED_ON))
          || !Objects.equals(statedBy, updatedArtifact.get(ProvenanceUtil.PAV_CREATED_BY));
    }
    if (ensureFieldIds && !verbatim) {
      // The stored artifact, so each child's provenance can record what this write did to that child
      // rather than what it did to the parent. Null on the create-by-PUT path, where every child is new.
      logReplacedChildIdentifiers(
          ModelUtil.ensureFieldIdsRecursively(updatedArtifact, currentArtifact, pi, provenanceUtil,
              linkedDataUtil), artifactId);
      return refuseInvalidNormalizedArtifact(updatedArtifact);
    }
    return creationProvenanceReplaced ? refuseInvalidNormalizedArtifact(updatedArtifact) : null;
  }

  /** Answers a store failure during the insert or the replacement, the only calls that can raise one. */
  private Response artifactWriteFailureResponse(String artifactId, CreateOrUpdate createOrUpdate,
                                                CedarErrorKey notCreatedKey, CedarErrorKey notUpdatedKey,
                                                Exception exception) {
    CedarResponse.CedarResponseBuilder responseBuilder = CedarResponse.internalServerError()
        .id(artifactId)
        .exception(exception);
    if (createOrUpdate == CreateOrUpdate.CREATE) {
      responseBuilder
          .errorKey(notCreatedKey)
          .message("The " + artifactLabel + " can not be created using id:" + artifactId);
    } else {
      responseBuilder
          .errorKey(notUpdatedKey)
          .message("The " + artifactLabel + " can not be updated by id:" + artifactId);
    }
    return responseBuilder.build();
  }

  /**
   * Answers a conditional write the store refused because the artifact moved on after this request read
   * it. The store is read again so the answer can name the revision that is now current, or say that
   * the artifact is gone. The refusal stands when that read fails, whether as an IOException or as the
   * driver's own exception: the write did not happen, and only the name of the revision that won is
   * unknown. A 500 or 503 would report the definite refusal as an unknown outcome, and a caller may
   * retry an unknown outcome once whatever blocked the write has gone.
   */
  private Response movedOnSinceRead(String artifactId) {
    ArtifactWithRevision<JsonNode> current;
    try {
      current = findArtifactWithRevisionInService(artifactId);
    } catch (IOException | MongoException e) {
      logger.warn("Could not read the current revision of {} after the store refused a write to it", artifactId, e);
      return movedOnResponse(artifactId, null);
    }
    return current == null ? disappearedDuringConditionalUpdate(artifactId)
        : movedOnResponse(artifactId, current.revision());
  }

  /**
   * Answers a template write the store's reference fence refused because instances reference the
   * template, stored ones or one being written. The status stays 412, since the precondition the request
   * was sent under no longer holds. The body is the deletion pre-check's, so a caller can tell this
   * refusal from a race: the operation's own key, and the reason that instances reference the template.
   * A retry against the revision it names is refused again until the references are gone.
   */
  private Response referencedTemplateRefusal(String templateId, CedarErrorKey errorKey, String operation) {
    Long currentRevision = null;
    try {
      ArtifactWithRevision<JsonNode> current = findArtifactWithRevisionInService(templateId);
      currentRevision = current == null ? null : current.revision();
    } catch (IOException | MongoException e) {
      logger.warn("Could not read the current revision of {} after the store refused a write to it", templateId, e);
    }
    return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
        .id(templateId)
        .errorKey(errorKey)
        .errorReasonKey(CedarErrorReasonKey.TEMPLATE_REFERENCED_IN_INSTANCES)
        .message("Instances reference the template, so it can not be " + operation)
        .parameter("currentETag", currentRevision == null ? null : etag(currentRevision))
        .build();
  }

  protected Response disappearedDuringConditionalUpdate(String artifactId) {
    return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
        .id(artifactId)
        .errorKey(CedarErrorKey.ARTIFACT_HAS_MOVED_ON)
        .message("The " + artifactLabel + " no longer exists")
        .build();
  }

  protected Response enforceIfMatch(String ifMatch, long currentRevision, String artifactId) {
    if (RevisionPreconditionParser.isAbsent(ifMatch)) {
      return CedarResponse.preconditionRequired()
          .id(artifactId)
          .errorKey(CedarErrorKey.ARTIFACT_PRECONDITION_REQUIRED)
          .message("Updating an existing " + artifactLabel + " requires the ETag returned by GET in If-Match")
          .build();
    }
    if (RevisionPreconditionParser.parse(ifMatch).matches(currentRevision)) {
      return null;
    }
    return movedOnResponse(artifactId, currentRevision);
  }

  protected Response movedOnResponse(String artifactId, Long currentRevision) {
    return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
        .id(artifactId)
        .errorKey(CedarErrorKey.ARTIFACT_HAS_MOVED_ON)
        .message("The " + artifactLabel + " has been updated since it was read")
        .parameter("currentETag", currentRevision == null ? null : etag(currentRevision))
        .build();
  }

  protected String etag(long revision) {
    return RevisionPreconditionParser.format(revision);
  }

  protected String etag(long revision, MediaType responseType, boolean compact) {
    if (ArtifactYamlTranscoder.isYaml(responseType)) {
      // The compact renderer's bytes changed when nested repository identifiers left the form.
      // Version the strong validator so an unchanged stored revision cannot validate stale bytes.
      return RevisionPreconditionParser.format(revision, compact ? "yaml-compact-v2" : "yaml");
    }
    return etag(revision);
  }

  protected String etag(long revision, OutputFormatType formatType) {
    return switch (formatType) {
      case JSONLD -> etag(revision);
      case JSON -> RevisionPreconditionParser.format(revision, "json");
      case RDF_NQUAD -> RevisionPreconditionParser.format(revision, "rdf-nquad");
    };
  }

  /**
   * The first validation checks the accepted request shape. Schema writes then
   * receive server-owned child identifiers and per-child provenance, so this
   * second gate checks the exact document that is about to cross the storage
   * boundary. No normalizer is allowed to turn a valid request into an invalid
   * stored artifact.
   */
  private Response refuseInvalidNormalizedArtifact(JsonNode artifact) throws CedarException {
    ValidationReport validationReport = validateArtifact(artifact, false);
    ReportUtils.outputLogger(logger, validationReport, true);
    if (validationReport.canSave()) {
      return null;
    }
    return CedarResponse.badRequest()
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_INVALID)
        .errorKey(CedarErrorKey.INVALID_DATA)
        .errorReasonKey(CedarErrorReasonKey.VALIDATION_ERROR)
        .message("Server normalization produced an invalid " + artifactLabel + ": "
            + concatenateValidationMessages(validationReport))
        .object("validationReport", validationReport)
        .build();
  }

  protected Response deleteArtifact(String id, CedarPermission deletePermission, CedarErrorKey notFoundKey,
                                    CedarErrorKey notDeletedKey) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(deletePermission);
    id = linkedDataUtil.resolveResourceId(artifactType, id);
    c.must(id).be(ValidUrl);

    return deleteArtifactFromDatabase(c, id, notFoundKey, notDeletedKey);
  }

  protected Response deleteArtifactFromDatabase(CedarRequestContext c, String id, CedarErrorKey notFoundKey,
                                                CedarErrorKey notDeletedKey) {
    ArtifactWithRevision<JsonNode> snapshot;
    try {
      snapshot = findArtifactWithRevisionInService(id);
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(notDeletedKey)
          .message("The " + artifactLabel + " can not be read before deletion by id:" + id)
          .exception(e)
          .build();
    }
    if (snapshot == null) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(notFoundKey)
          .message("The " + artifactLabel + " can not be found by id:" + id)
          .build();
    }

    String ifMatch = c.getIfMatchHeader();
    if (RevisionPreconditionParser.isAbsent(ifMatch)) {
      return CedarResponse.preconditionRequired()
          .id(id)
          .errorKey(CedarErrorKey.ARTIFACT_PRECONDITION_REQUIRED)
          .message("Deleting an existing " + artifactLabel
              + " requires the ETag returned by GET in If-Match")
          .build();
    }
    RevisionPrecondition precondition = RevisionPreconditionParser.parse(ifMatch);
    if (!precondition.matches(snapshot.revision())) {
      return movedOnResponse(id, snapshot.revision());
    }

    try {
      if (precondition.anyCurrentRevision()) {
        deleteArtifactInService(id);
      } else {
        deleteArtifactInService(id, snapshot.revision());
      }
    } catch (TemplateReferencedException e) {
      return referencedTemplateRefusal(id, notDeletedKey, "deleted");
    } catch (ArtifactRevisionConflictException e) {
      // The template store's reference fence reports a template that has gone as a conflict too.
      return movedOnSinceRead(id);
    } catch (ArtifactServerResourceNotFoundException e) {
      return disappearedDuringConditionalUpdate(id);
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(notDeletedKey)
          .message("The " + artifactLabel + " can not be deleted by id:" + id)
          .exception(e)
          .build();
    }
    return CedarResponse.noContent().build();
  }
}
