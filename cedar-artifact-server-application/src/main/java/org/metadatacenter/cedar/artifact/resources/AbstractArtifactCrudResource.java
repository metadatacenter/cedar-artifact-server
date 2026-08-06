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
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.model.CreateOrUpdate;
import org.metadatacenter.model.validation.report.CedarValidationReport;
import org.metadatacenter.model.validation.report.ReportUtils;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.rest.assertion.noun.CedarRequestBody;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.util.ModelUtil;
import org.metadatacenter.util.artifact.ArtifactYamlTranscoder;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.http.CedarUrlUtil;
import org.metadatacenter.util.http.LinkHeaderUtil;
import org.metadatacenter.util.http.PagedQuery;
import org.metadatacenter.util.mongo.MongoUtils;
import org.slf4j.Logger;

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

  protected abstract JsonNode updateArtifactInService(String id, JsonNode content) throws IOException,
      ArtifactServerResourceNotFoundException;

  protected abstract void deleteArtifactInService(String id) throws IOException,
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

    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    setProvenanceAndId(resourceType, artifact, pi);

    Response response = null;
    if (cedarConfig.getValidationConfig().isEnabled()) {
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
    } else {
      response = storeArtifactInDatabase(artifact, pi, notCreatedKey);
    }
    return negotiateArtifactResponse(response, resourceType);
  }

  protected Response storeArtifactInDatabase(JsonNode artifact, ProvenanceInfo pi, CedarErrorKey notCreatedKey) {
    try {
      if (ensureFieldIds) {
        ModelUtil.ensureFieldIdsRecursively(artifact, pi, provenanceUtil, linkedDataUtil);
      }
      JsonNode createdArtifact = createArtifactInService(artifact);
      MongoUtils.removeIdField(createdArtifact);
      String id = createdArtifact.get(LinkedData.ID).asText();
      URI createdArtifactUri = CedarUrlUtil.getIdURI(uriInfo, id);
      return CedarResponse.created(createdArtifactUri)
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

    JsonNode artifact = null;
    try {
      artifact = findArtifactInService(id);
    } catch (IOException e) {
      return CedarResponse.internalServerError()
          .id(id)
          .errorKey(notFoundKey)
          .errorMessage("The " + artifactLabel + " can not be found by id:" + id)
          .exception(e)
          .build();
    }
    if (artifact == null) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(notFoundKey)
          .errorMessage("The " + artifactLabel + " can not be found by id:" + id)
          .build();
    } else {
      MongoUtils.removeIdField(artifact);
      if (ArtifactYamlTranscoder.isJson(responseType.get())) {
        return Response.ok().entity(artifact).build();
      }
      return Response.ok()
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

  protected Response updateArtifact(String id, CedarPermission updatePermission, CedarResourceType resourceType,
                                    CedarErrorKey notUpdatedKey, CedarErrorKey notCreatedKey, String requestBody,
                                    Optional<Boolean> compactParam) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(id).be(ValidUrl);
    c.must(c.user()).have(updatePermission);
    rejectCompactOnWriteOperations(compactParam);
    if (negotiatedArtifactResponseType().isEmpty()) {
      return notAcceptableArtifactFormatResponse();
    }

    CedarRequestBody body = artifactRequestBody(requestBody, resourceType);
    c.must(body).be(NonEmpty);
    JsonNode newArtifact = body.asJson();

    enforceMandatoryFieldsInPut(id, newArtifact, resourceType, notUpdatedKey);
    enforceMandatoryName(newArtifact, resourceType, notUpdatedKey);

    ProvenanceInfo pi = provenanceUtil.build(c.getCedarUser());
    provenanceUtil.patchProvenanceInfo(newArtifact, pi);

    Response response = null;
    if (cedarConfig.getValidationConfig().isEnabled()) {
      ValidationReport validationReport = validateArtifact(newArtifact);
      ReportUtils.outputLogger(logger, validationReport, true);
      String validationStatus = validationReport.getValidationStatus();
      if (validationStatus.equals(CedarValidationReport.IS_VALID)) {
        response = updateOrCreateArtifactInDatabase(id, newArtifact, pi, c, notCreatedKey, notUpdatedKey);
      } else {
        response = CedarResponse.badRequest()
            .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_INVALID)
            .errorKey(CedarErrorKey.INVALID_DATA)
            .errorReasonKey(CedarErrorReasonKey.VALIDATION_ERROR)
            .errorMessage(updateValidationErrorMessage(validationReport))
            .object("validationReport", validationReport)
            .build();
      }
    } else {
      response = updateOrCreateArtifactInDatabase(id, newArtifact, pi, c, notCreatedKey, notUpdatedKey);
    }
    return negotiateArtifactResponse(response, resourceType);
  }

  protected Response updateOrCreateArtifactInDatabase(String artifactId, JsonNode updatedArtifact, ProvenanceInfo pi,
                                                      CedarRequestContext c, CedarErrorKey notCreatedKey,
                                                      CedarErrorKey notUpdatedKey) throws CedarException {
    JsonNode outputArtifact = null;
    CreateOrUpdate createOrUpdate = null;
    try {
      JsonNode currentArtifact = findArtifactInService(artifactId);
      if (ensureFieldIds) {
        ModelUtil.ensureFieldIdsRecursively(updatedArtifact, pi, provenanceUtil, linkedDataUtil);
      }
      if (currentArtifact != null) {
        createOrUpdate = CreateOrUpdate.UPDATE;
        outputArtifact = updateArtifactInService(artifactId, updatedArtifact);
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
          .header(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS, CedarValidationReport.IS_VALID)
          .entity(outputArtifact)
          .build();
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

  protected Response deleteArtifact(String id, CedarPermission deletePermission, CedarErrorKey notFoundKey,
                                    CedarErrorKey notDeletedKey) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(deletePermission);
    c.must(id).be(ValidUrl);

    return deleteArtifactFromDatabase(id, notFoundKey, notDeletedKey);
  }

  protected Response deleteArtifactFromDatabase(String id, CedarErrorKey notFoundKey, CedarErrorKey notDeletedKey) {
    try {
      deleteArtifactInService(id);
    } catch (ArtifactServerResourceNotFoundException e) {
      return CedarResponse.notFound()
          .id(id)
          .errorKey(notFoundKey)
          .errorMessage("The " + artifactLabel + " can not be found by id:" + id)
          .exception(e)
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
