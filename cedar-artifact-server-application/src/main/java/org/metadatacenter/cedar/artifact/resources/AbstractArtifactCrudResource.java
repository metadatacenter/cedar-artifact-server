package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.metadatacenter.model.CreateOrUpdate;
import org.metadatacenter.model.validation.report.CedarValidationReport;
import org.metadatacenter.model.validation.report.ReportUtils;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.rest.assertion.noun.CedarRequestBody;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.dao.ArtifactRevisionConflictException;
import org.metadatacenter.server.dao.ArtifactWithRevision;
import org.metadatacenter.server.RevisionPrecondition;
import org.metadatacenter.server.jsonld.LinkedDataUtil;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.util.ModelUtil;
import org.metadatacenter.util.provenance.ProvenanceUtil;
import org.metadatacenter.util.artifact.ArtifactYamlTranscoder;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.http.CedarUrlUtil;
import org.metadatacenter.util.http.LinkHeaderUtil;
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
import java.util.Optional;

import static org.metadatacenter.rest.assertion.GenericAssertions.*;

public abstract class AbstractArtifactCrudResource extends AbstractArtifactServerResource {

  protected final List<String> FIELD_NAMES_SUMMARY_LIST;

  private final Logger logger;
  private final String artifactLabel;
  private final String artifactsLabel;
  private final boolean ensureFieldIds;

  protected AbstractArtifactCrudResource(CedarConfig cedarConfig, Logger logger, String artifactLabel,
                                         String artifactsLabel, List<String> summaryFields, boolean ensureFieldIds) {
    super(cedarConfig);
    this.logger = logger;
    this.artifactLabel = artifactLabel;
    this.artifactsLabel = artifactsLabel;
    this.ensureFieldIds = ensureFieldIds;
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(summaryFields);
  }

  protected abstract JsonNode createArtifactInService(JsonNode artifact) throws IOException;

  protected abstract JsonNode findArtifactInService(String id) throws IOException;

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

  protected abstract ValidationReport validateArtifact(JsonNode artifact) throws CedarException;

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

    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    setProvenanceAndId(resourceType, artifact, pi);

    Response response = null;
    // Always. Validation used to be switchable, by CEDAR_VALIDATION_ENABLED, and an artifact stored
    // without it is one nothing has ever checked against the model it claims to follow.
    {
      ValidationReport validationReport = validateArtifact(artifact);
      ReportUtils.outputLogger(logger, validationReport, true);
      String validationStatus = validationReport.getValidationStatus();
      if (validationStatus.equals(CedarValidationReport.IS_VALID)) {
        response = storeArtifactInDatabase(artifact, pi, notCreatedKey);
      } else {
        response = CedarResponse.badRequest()
            .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_INVALID)
            .errorKey(CedarErrorKey.INVALID_DATA)
            .errorReasonKey(CedarErrorReasonKey.VALIDATION_ERROR)
            .errorMessage(concatenateValidationMessages(validationReport))
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
      JsonNode createdArtifact = createArtifactInService(artifact);
      MongoUtils.removeIdField(createdArtifact);
      String id = createdArtifact.get(LinkedData.ID).asText();
      URI createdArtifactUri = CedarUrlUtil.getIdURI(uriInfo, id);
      return CedarResponse.created(createdArtifactUri)
          .header(HttpHeaders.ETAG, etag(1L))
          .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_VALID)
          .entity(createdArtifact).build();
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .errorKey(notCreatedKey)
          .errorMessage("The " + artifactLabel + " can not be created")
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

  protected void logLegacyArtifactRepairs(List<LinkedDataUtil.LegacyArtifactRepair> repairs, String artifactId) {
    for (LinkedDataUtil.LegacyArtifactRepair repair : repairs) {
      logger.warn("Repaired inherited defect '{}' at '{}' in {}. Previous value: {}",
          repair.issue(), repair.path(), artifactId, repair.previousValue());
    }
  }

  protected Response findArtifact(String id, CedarPermission readPermission, CedarErrorKey notFoundKey,
                                  CedarResourceType resourceType, Optional<Boolean> compactParam) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(readPermission);
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
          .errorMessage("The " + artifactLabel + " can not be found by id:" + id)
          .exception(e)
          .build();
    }
    if (snapshot == null) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(notFoundKey)
          .errorMessage("The " + artifactLabel + " can not be found by id:" + id)
          .build();
    } else {
      JsonNode artifact = snapshot.content();
      MongoUtils.removeIdField(artifact);
      long revision = snapshot.revision();
      if (ArtifactYamlTranscoder.isJson(responseType.get())) {
        return Response.ok().header(HttpHeaders.ETAG, etag(revision)).entity(artifact).build();
      }
      return Response.ok()
          .header(HttpHeaders.ETAG, etag(revision))
          .entity(ArtifactYamlTranscoder.jsonToYaml(artifact, resourceType,
              compactParam.isPresent() && compactParam.get()))
          .type(responseType.get())
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
          .errorMessage("The " + artifactsLabel + " can not be listed")
          .exception(e)
          .build();
    }
    long total = countArtifactsInService();
    checkPagingParametersAgainstTotal(offset, total);

    String absoluteUrl = uriInfo.getAbsolutePathBuilder().build().toString();
    String linkHeader = LinkHeaderUtil.getPagingLinkHeader(absoluteUrl, total, limit, offset);
    Response.ResponseBuilder responseBuilder = Response.ok().entity(artifacts);
    responseBuilder.header(CustomHttpConstants.HEADER_TOTAL_COUNT, String.valueOf(total));
    if (!linkHeader.isEmpty()) {
      responseBuilder.header(HttpConstants.HTTP_HEADER_LINK, linkHeader);
    }
    return responseBuilder.build();
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
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
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
      Response refusal = refuseVerbatimWriteWeCannotHonour(id, resourceType);
      if (refusal != null) {
        return refusal;
      }
    }

    CedarRequestBody body = artifactRequestBody(requestBody, resourceType);
    c.must(body).be(NonEmpty);
    JsonNode newArtifact = body.asJson();

    enforceMandatoryFieldsInPut(id, newArtifact, resourceType, notUpdatedKey);
    enforceMandatoryName(newArtifact, resourceType, notUpdatedKey);
    reportMismatchedChildIdPrefixes(newArtifact, resourceType, id);

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
      enforceChildArtifactTypes(newArtifact, resourceType, notUpdatedKey);
      if (resourceType == CedarResourceType.TEMPLATE || resourceType == CedarResourceType.ELEMENT) {
        logLegacyArtifactRepairs(
            linkedDataUtil.repairInheritedDefects(newArtifact, currentArtifact, null, resourceType), id);
      }
      provenanceUtil.patchProvenanceInfo(newArtifact, pi);
      // and a property IRI for any child added during the edit
      linkedDataUtil.addChildPropertyIris(newArtifact, resourceType);
    }

    Response response = null;
    {
      ValidationReport validationReport = validateArtifact(newArtifact);
      ReportUtils.outputLogger(logger, validationReport, true);
      String validationStatus = validationReport.getValidationStatus();
      if (validationStatus.equals(CedarValidationReport.IS_VALID)) {
        response = updateOrCreateArtifactInDatabase(id, newArtifact, pi, c, notCreatedKey, notUpdatedKey,
            verbatim, currentArtifact, currentRevision);
      } else {
        response = CedarResponse.badRequest()
            .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_INVALID)
            .errorKey(CedarErrorKey.INVALID_DATA)
            .errorReasonKey(CedarErrorReasonKey.VALIDATION_ERROR)
            .errorMessage(updateValidationErrorMessage(validationReport))
            .object("validationReport", validationReport)
            .build();
      }
    }
    return negotiateArtifactResponse(response, resourceType);
  }

  /**
   * Refuses a verbatim write whose promise cannot be kept. The promise is that the stored document is the
   * one supplied, so it needs an artifact to replace -- a request naming an identifier that resolves to
   * nothing would otherwise create, which is a different operation -- and it needs a body this server has
   * not rewritten, which a YAML request is not: a transcode decides the JSON, so what is stored is the
   * transcoder's reading rather than the caller's document.
   */
  protected Response refuseVerbatimWriteWeCannotHonour(String artifactId, CedarResourceType resourceType)
      throws CedarException {
    if (ArtifactYamlTranscoder.isYaml(httpHeaders.getMediaType())) {
      return CedarResponse.badRequest()
          .id(artifactId)
          .errorKey(CedarErrorKey.VERBATIM_WRITE_REFUSED)
          .errorMessage("A verbatim write needs a JSON body: a YAML body is transcoded, so what would be "
              + "stored is not what was sent")
          .build();
    }
    JsonNode current;
    try {
      current = findArtifactInService(artifactId);
    } catch (IOException e) {
      throw new CedarProcessingException(e);
    }
    if (current == null) {
      return CedarResponse.notFound()
          .id(artifactId)
          .errorKey(CedarErrorKey.ARTIFACT_NOT_FOUND)
          .errorMessage("A verbatim write replaces an existing " + artifactLabel + "; this one does not exist")
          .build();
    }
    return null;
  }

  /**
   * Refuses a verbatim write carrying a child identifier that is not an absolute IRI. On an ordinary write
   * such an identifier is minted over; here it cannot be, because nothing may be altered, and it must not
   * be stored, because a temporary identifier resolves to nothing and no later write will replace it.
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
          .errorMessage("A verbatim write alters nothing, so every element occurrence identifier must already "
              + "be an absolute IRI. These are not: " + String.join(", ", unusableOccurrences))
          .parameter("occurrences", String.join(", ", unusableOccurrences))
          .build();
    } else if (resourceType != CedarResourceType.TEMPLATE && resourceType != CedarResourceType.ELEMENT) {
      return null;
    }
    List<String> unusable = new ArrayList<>();
    ModelUtil.forEachChild(artifact, (name, child) -> {
      if (!ModelUtil.hasUsableChildId(child)) {
        unusable.add(name);
      }
    });
    if (unusable.isEmpty()) {
      return null;
    }
    return CedarResponse.badRequest()
        .errorKey(CedarErrorKey.VERBATIM_WRITE_REFUSED)
        .errorMessage("A verbatim write alters nothing, so every child identifier must already be an "
            + "absolute IRI. These are not: " + String.join(", ", unusable))
        .parameter("children", String.join(", ", unusable))
        .build();
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

  protected Response updateOrCreateArtifactInDatabase(String artifactId, JsonNode updatedArtifact, ProvenanceInfo pi,
                                                      CedarRequestContext c, CedarErrorKey notCreatedKey,
                                                      CedarErrorKey notUpdatedKey, boolean verbatim,
                                                      JsonNode currentArtifact, Long currentRevision)
      throws CedarException {
    JsonNode outputArtifact = null;
    CreateOrUpdate createOrUpdate = null;
    try {
      if (currentArtifact != null && !verbatim) {
        provenanceUtil.preserveCreationProvenance(updatedArtifact, currentArtifact);
      }
      if (ensureFieldIds && !verbatim) {
        // The stored artifact, so each child's provenance can record what this write did to that child
        // rather than what it did to the parent. Null on the create-by-PUT path, where every child is new.
        logReplacedChildIdentifiers(
            ModelUtil.ensureFieldIdsRecursively(updatedArtifact, currentArtifact, pi, provenanceUtil,
                linkedDataUtil), artifactId);
        Response refusal = refuseInvalidNormalizedArtifact(updatedArtifact);
        if (refusal != null) {
          return refusal;
        }
      }
      if (currentArtifact != null) {
        createOrUpdate = CreateOrUpdate.UPDATE;
        outputArtifact = updateArtifactInService(artifactId, updatedArtifact, currentRevision);
      } else {
        c.must(artifactId).be(ValidId);
        createOrUpdate = CreateOrUpdate.CREATE;
        outputArtifact = createArtifactInService(updatedArtifact);
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
          .header(HttpHeaders.ETAG, etag(createOrUpdate == CreateOrUpdate.UPDATE ? currentRevision + 1L : 1L))
          .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_VALID)
          .entity(outputArtifact)
          .build();
    } catch (ArtifactRevisionConflictException e) {
      return movedOnResponse(artifactId, currentRevision);
    } catch (IOException | ArtifactServerResourceNotFoundException e) {
      CedarResponse.CedarResponseBuilder responseBuilder = CedarResponse.internalServerError()
          .id(artifactId)
          .exception(e);
      if (createOrUpdate == CreateOrUpdate.CREATE) {
        responseBuilder
            .errorKey(notCreatedKey)
            .errorMessage("The " + artifactLabel + " can not be created using id:" + artifactId);
      } else if (createOrUpdate == CreateOrUpdate.UPDATE) {
        responseBuilder
            .errorKey(notUpdatedKey)
            .errorMessage("The " + artifactLabel + " can not be updated by id:" + artifactId);
      }
      return responseBuilder.build();
    }
  }

  protected Response enforceIfMatch(String ifMatch, long currentRevision, String artifactId) {
    if (ifMatch == null || ifMatch.isBlank()) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_REQUIRED)
          .id(artifactId)
          .errorKey(CedarErrorKey.ARTIFACT_PRECONDITION_REQUIRED)
          .errorMessage("Updating an existing " + artifactLabel + " requires the ETag returned by GET in If-Match")
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
        .errorMessage("The " + artifactLabel + " has been updated since it was read")
        .parameter("currentETag", currentRevision == null ? null : etag(currentRevision))
        .build();
  }

  protected String etag(long revision) {
    return "\"" + revision + "\"";
  }

  /**
   * The first validation checks the accepted request shape. Schema writes then
   * receive server-owned child identifiers and per-child provenance, so this
   * second gate checks the exact document that is about to cross the storage
   * boundary. No normalizer is allowed to turn a valid request into an invalid
   * stored artifact.
   */
  private Response refuseInvalidNormalizedArtifact(JsonNode artifact) throws CedarException {
    ValidationReport validationReport = validateArtifact(artifact);
    ReportUtils.outputLogger(logger, validationReport, true);
    if (CedarValidationReport.IS_VALID.equals(validationReport.getValidationStatus())) {
      return null;
    }
    return CedarResponse.badRequest()
        .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_INVALID)
        .errorKey(CedarErrorKey.INVALID_DATA)
        .errorReasonKey(CedarErrorReasonKey.VALIDATION_ERROR)
        .errorMessage("Server normalization produced an invalid " + artifactLabel + ": "
            + concatenateValidationMessages(validationReport))
        .object("validationReport", validationReport)
        .build();
  }

  protected Response deleteArtifact(String id, CedarPermission deletePermission, CedarErrorKey notFoundKey,
                                    CedarErrorKey notDeletedKey) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(deletePermission);
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
          .errorMessage("The " + artifactLabel + " can not be read before deletion by id:" + id)
          .exception(e)
          .build();
    }
    if (snapshot == null) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(notFoundKey)
          .errorMessage("The " + artifactLabel + " can not be found by id:" + id)
          .build();
    }

    String ifMatch = c.getIfMatchHeader();
    if (ifMatch == null || ifMatch.isBlank()) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_REQUIRED)
          .id(id)
          .errorKey(CedarErrorKey.ARTIFACT_PRECONDITION_REQUIRED)
          .errorMessage("Deleting an existing " + artifactLabel
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
    } catch (ArtifactRevisionConflictException e) {
      try {
        ArtifactWithRevision<JsonNode> current = findArtifactWithRevisionInService(id);
        return movedOnResponse(id, current == null ? null : current.revision());
      } catch (IOException readFailure) {
        return CedarResponse.internalServerError()
            .id(id)
            .errorKey(notDeletedKey)
            .errorMessage("The " + artifactLabel + " changed while it was being deleted")
            .exception(readFailure)
            .build();
      }
    } catch (ArtifactServerResourceNotFoundException e) {
      return CedarResponse.status(CedarResponseStatus.PRECONDITION_FAILED)
          .id(id)
          .errorKey(CedarErrorKey.ARTIFACT_HAS_MOVED_ON)
          .errorMessage("The " + artifactLabel + " no longer exists")
          .build();
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(notDeletedKey)
          .errorMessage("The " + artifactLabel + " can not be deleted by id:" + id)
          .exception(e)
          .build();
    }
    return CedarResponse.noContent().build();
  }
}
