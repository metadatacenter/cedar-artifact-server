package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.error.CedarErrorPack;
import org.metadatacenter.exception.CedarBadRequestException;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.exception.CedarRequestBodyMissingFieldException;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.model.core.CedarModelVocabulary;
import org.metadatacenter.model.validation.CedarValidator;
import org.metadatacenter.model.validation.ModelValidator;
import org.metadatacenter.model.validation.report.ErrorItem;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.rest.assertion.noun.CedarRequestBody;
import org.metadatacenter.rest.context.HttpRequestEmptyBody;
import org.metadatacenter.rest.context.HttpRequestJsonBody;
import org.metadatacenter.rest.exception.CedarAssertionException;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.JsonPointerValuePair;
import org.metadatacenter.util.ModelUtil;
import org.metadatacenter.util.artifact.ArtifactYamlTranscoder;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.mongo.MongoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.fasterxml.jackson.databind.node.JsonNodeType.NULL;

public class AbstractArtifactServerResource extends CedarMicroserviceResource {

  private static final Logger log = LoggerFactory.getLogger(AbstractArtifactServerResource.class);

  protected static List<String> FIELD_NAMES_EXCLUSION_LIST;

  protected AbstractArtifactServerResource(CedarConfig cedarConfig) {
    super(cedarConfig);
    FIELD_NAMES_EXCLUSION_LIST = new ArrayList<>();
    FIELD_NAMES_EXCLUSION_LIST.addAll(cedarConfig.getArtifactRESTAPI().getExcludedFields());
  }

  protected void setProvenanceAndId(CedarResourceType cedarResourceType, JsonNode element, ProvenanceInfo pi) {
    if ((element.get(LinkedData.ID) != null) && (!NULL.equals(element.get(LinkedData.ID).getNodeType()))) {
      throw new IllegalArgumentException("Specifying " + LinkedData.ID + " for new objects is not allowed");
    }
    provenanceUtil.addProvenanceInfo(element, pi);

    String id = linkedDataUtil.buildNewLinkedDataId(cedarResourceType);
    ((ObjectNode) element).put(LinkedData.ID, id);

    // add template-element-instance ids (only for instances)
    linkedDataUtil.addElementInstanceIds(element, cedarResourceType);
    // and a property IRI for every attribute the instance names and has none for
    linkedDataUtil.addAttributeValuePropertyIris(element, cedarResourceType);
    // and for every child of a template or element that has none
    linkedDataUtil.addChildPropertyIris(element, cedarResourceType);
  }

  protected Boolean ensureSummary(Optional<Boolean> summary) {
    if (summary == null || !summary.isPresent()) {
      return false;
    } else {
      return summary.get();
    }
  }

  protected static List<String> getAndCheckFieldNames(Optional<String> fieldNames, boolean summary) throws CedarAssertionException {
    if (fieldNames != null && fieldNames.isPresent()) {
      if (summary == true) {
        throw new CedarAssertionException(
            "It is no allowed to specify parameter 'field_names' and also set 'summary' to true!");
      } else if (fieldNames.get().length() > 0) {
        return Arrays.asList(fieldNames.get().split(","));
      }
    }
    return null;
  }

  protected static void checkPagingParametersAgainstTotal(Integer offset, long total) throws CedarException {
    if (offset != 0 && offset > total - 1) {
      throw new CedarBadRequestException(new CedarErrorPack()
          .message("Parameter 'offset' must be smaller than the total count of objects, which is " + total + "!")
          .parameter("offset", offset)
          .parameter("total", total));
    }
  }

  protected ValidationReport validateTemplate(JsonNode template) throws CedarException {
    try {
      return newModelValidator().validateTemplate(template);
    } catch (Exception e) {
      throw newCedarException(e.getMessage());
    }
  }

  protected ValidationReport validateTemplateElement(JsonNode templateElement) throws CedarException {
    try {
      return newModelValidator().validateTemplateElement(templateElement);
    } catch (Exception e) {
      throw newCedarException(e.getMessage());
    }
  }

  protected ValidationReport validateTemplateField(JsonNode templateField) throws CedarException {
    try {
      return newModelValidator().validateTemplateField(templateField);
    } catch (Exception e) {
      throw newCedarException(e.getMessage());
    }
  }

  protected ValidationReport validateTemplateInstance(JsonNode templateInstance, JsonNode instanceSchema) throws CedarException {
    try {
      return newModelValidator().validateTemplateInstance(templateInstance, instanceSchema);
    } catch (Exception e) {
      throw newCedarException(e.getMessage());
    }
  }

  private static ModelValidator newModelValidator() {
    return new CedarValidator();
  }

  protected static CedarException newCedarException(String message) {
    return new CedarException(message) {
    };
  }

  /**
   * The identifier a body must carry to be created: the key, with null in it.
   *
   * <p>Only the server assigns an identifier, so a client asks for one rather than inventing one, and
   * null is how it asks. The key has to be there for the asking to be legible — a body that leaves it
   * out cannot be told from one that forgot, and the meta-schema agrees, typing {@code @id} as
   * {@code ["string", "null"]} and marking it required. That is also what makes a body createable and
   * valid at once: an omitted key was the single shape that created here and failed validation there.
   *
   * <p>A real IRI is refused as it always was. It asserts an identity nothing can resolve, and one the
   * server is about to replace.
   */
  protected void enforceMandatoryNullOrMissingId(JsonNode jsonObject, CedarResourceType resourceType, CedarErrorKey errorKey) throws CedarBadRequestException {
    JsonNode idInRequestNode = jsonObject.get(LinkedData.ID);
    if (idInRequestNode == null) {
      CedarErrorPack errorPack = new CedarErrorPack()
          .message("The " + resourceType.getValue() + " must contain a '" + LinkedData.ID
              + "' field, with null in it: the server assigns the identifier, and null is how a client asks for one.")
          .errorKey(errorKey)
          .parameter(LinkedData.ID, "missing");
      throw new CedarBadRequestException(errorPack);
    }
    if (!idInRequestNode.isNull()) {
      String idInRequest = idInRequestNode.asText();
      if (idInRequest != null) {
        CedarErrorPack errorPack = new CedarErrorPack()
            .message("The " + resourceType.getValue() + " must not contain a non-null '" + LinkedData.ID + "' field!")
            .errorKey(errorKey)
            .parameter(LinkedData.ID, idInRequest);
        throw new CedarBadRequestException(errorPack);
      }
    }
  }

  protected void enforceMandatoryName(JsonNode jsonObject, CedarResourceType resourceType, CedarErrorKey errorKey) throws CedarBadRequestException {
    JsonPointerValuePair namePair = ModelUtil.extractNameFromResource(resourceType, jsonObject);
    if (namePair.hasEmptyValue()) {
      throw new CedarRequestBodyMissingFieldException(namePair.getPointer(), errorKey);
    }
  }

  /**
   * A child of a template or element must declare which of the two artifact kinds it is, because the
   * identifier minted for a child takes its prefix from that declaration. The meta-schemas already require
   * '@type' on a child, so this rejects the same artifacts validation would; it exists so that the minting
   * code is never asked to guess, including when validation is switched off. A multi-instance child with no
   * 'items' object is refused here for the same reason.
   */
  protected void enforceChildArtifactTypes(JsonNode jsonObject, CedarResourceType resourceType, CedarErrorKey errorKey)
      throws CedarBadRequestException {
    if (resourceType != CedarResourceType.TEMPLATE && resourceType != CedarResourceType.ELEMENT) {
      return;
    }
    JsonNode properties = jsonObject.get("properties");
    if (properties == null || !properties.isObject()) {
      return;
    }
    Iterator<Map.Entry<String, JsonNode>> it = properties.fields();
    while (it.hasNext()) {
      Map.Entry<String, JsonNode> entry = it.next();
      JsonNode candidate = entry.getValue();
      if (!candidate.isObject() || candidate.get("type") == null || ModelUtil.isSpecialField(entry.getKey())) {
        continue;
      }
      String type = candidate.get("type").asText();
      JsonNode child = candidate;
      if ("array".equals(type)) {
        child = candidate.get("items");
        if (child == null || !child.isObject()) {
          throw new CedarBadRequestException(new CedarErrorPack()
              .message("The multi-instance child '" + entry.getKey() + "' must contain an 'items' object!")
              .parameter("property", entry.getKey())
              .errorKey(errorKey));
        }
      } else if (!"object".equals(type)) {
        continue;
      }
      if (!ModelUtil.hasRecognisedChildType(child)) {
        throw new CedarBadRequestException(new CedarErrorPack()
            .message("The child '" + entry.getKey() + "' must declare a recognised '" + LinkedData.TYPE + "' of "
                + CedarResourceType.AtType.FIELD + ", " + CedarResourceType.AtType.STATIC_FIELD + " or "
                + CedarResourceType.AtType.ELEMENT + "!")
            .parameter("property", entry.getKey())
            .errorKey(errorKey));
      }
    }
  }

  /**
   * Reports children whose identifier prefix contradicts their '@type', which earlier writes produced by
   * minting every child as a field. The write is not refused: the identifier is what other artifacts
   * already refer to, so replacing it belongs to a repair pass that can record what it changed. Logged so
   * that the affected artifacts can be found without reading the whole corpus first.
   */
  protected void reportMismatchedChildIdPrefixes(JsonNode jsonObject, CedarResourceType resourceType, String id) {
    if (resourceType != CedarResourceType.TEMPLATE && resourceType != CedarResourceType.ELEMENT) {
      return;
    }
    List<String> mismatched = ModelUtil.childrenWithMismatchedIdPrefix(jsonObject);
    if (!mismatched.isEmpty()) {
      log.warn("Artifact {} has {} child(ren) whose '@id' prefix contradicts their '{}': {}",
          id, mismatched.size(), LinkedData.TYPE, String.join(", ", mismatched));
    }
  }

  protected void enforceMandatoryFieldsInPut(String id, JsonNode jsonObject, CedarResourceType resourceType, CedarErrorKey errorKey) throws CedarBadRequestException {
    JsonNode idInRequestNode = jsonObject.get(LinkedData.ID);
    String idInRequest = null;
    if (idInRequestNode != null && !idInRequestNode.isNull()) {
      idInRequest = idInRequestNode.asText();
    }
    if (idInRequest == null) {
      CedarErrorPack errorPack = new CedarErrorPack()
          .message("The " + resourceType.getValue() + " must contain a non-null '" + LinkedData.ID + "' field!")
          .errorKey(errorKey);
      throw new CedarBadRequestException(errorPack);
    }
    if (!idInRequest.equals(id)) {
      CedarErrorPack errorPack = new CedarErrorPack()
          .message("The " + LinkedData.ID + " in the body must match the id in the URL!")
          .parameter("idInURL", id)
          .parameter("idInBody", idInRequest)
          .errorKey(errorKey);
      throw new CedarBadRequestException(errorPack);
    }
  }

  protected static JsonNode getSchemaSource(TemplateService<String, JsonNode> templateService, JsonNode templateInstance) throws IOException,
      CedarException {
    checkInstanceSchemaExists(templateInstance);
    String templateRefId = templateInstance.get(CedarModelVocabulary.SCHEMA_IS_BASED_ON).asText();
    JsonNode template = templateService.findTemplate(templateRefId);
    if (template == null) {
      throw new CedarBadRequestException(
          new CedarErrorPack()
              .message("The artifact that this instance is based on can not be found.")
              .parameter(CedarModelVocabulary.SCHEMA_IS_BASED_ON, templateRefId)
              .errorKey(CedarErrorKey.INVALID_INPUT)
      );
    }
    MongoUtils.removeIdField(template);
    return template;
  }

  protected static JsonNode checkInstanceSchemaExists(JsonNode templateInstance) throws CedarException {
    JsonNode isBasedOnNode = templateInstance.path(CedarModelVocabulary.SCHEMA_IS_BASED_ON);
    if (isBasedOnNode.isMissingNode()) {
      throw new CedarRequestBodyMissingFieldException(CedarModelVocabulary.SCHEMA_IS_BASED_ON, CedarErrorKey
          .INVALID_INPUT);
    }
    return templateInstance;
  }

  protected String concatenateValidationMessages(ValidationReport validationReport) {
    StringBuilder sb = new StringBuilder();
    if (!validationReport.getErrors().isEmpty()) {
      for (ErrorItem ei : validationReport.getErrors()) {
        sb.append(ei.getMessage()).append("\n");
      }
    }
    return sb.toString();
  }

  // YAML content negotiation. Storage is JSON only: YAML is a request and response
  // representation, transcoded per request by ArtifactYamlTranscoder.

  protected Optional<MediaType> negotiatedArtifactResponseType() {
    return ArtifactYamlTranscoder.negotiateResponseType(httpHeaders.getAcceptableMediaTypes());
  }

  protected Response notAcceptableArtifactFormatResponse() {
    return CedarResponse.notAcceptable()
        .errorMessage("None of the media types in the Accept header can be produced")
        .parameter("allowed media types",
            Arrays.toString(new String[]{MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML}))
        .build();
  }

  /**
   * Reads a POST/PUT body into the JSON the storage layer expects. A body sent with Content-Type
   * application/yaml (or application/x-yaml) is transcoded; any other body is parsed as JSON.
   * The result is returned as a CedarRequestBody so callers keep asserting NonEmpty on it, with
   * the same outcome an empty or malformed body produced when the body came off the request
   * context.
   */
  protected CedarRequestBody artifactRequestBody(String requestBody, CedarResourceType resourceType)
      throws CedarException {
    if (requestBody == null || requestBody.trim().isEmpty()) {
      return new HttpRequestEmptyBody();
    }
    if (ArtifactYamlTranscoder.isYaml(httpHeaders.getMediaType())) {
      try {
        String json = ArtifactYamlTranscoder.yamlToJsonString(requestBody, resourceType);
        return new HttpRequestJsonBody(JsonMapper.MAPPER.readTree(json));
      } catch (ArtifactYamlTranscoder.CompactYamlBodyException e) {
        throw new CedarBadRequestException(e.getMessage(), e);
      } catch (Exception e) {
        throw new CedarBadRequestException("There was an error converting the YAML request body to JSON", e);
      }
    }
    try {
      return new HttpRequestJsonBody(JsonMapper.MAPPER.readTree(requestBody));
    } catch (Exception e) {
      throw new CedarBadRequestException("There was an error deserializing the request body", e);
    }
  }

  /**
   * Applies Accept-header negotiation to a write response. When the client asked for YAML and the
   * response carries the stored artifact, the entity is re-rendered as YAML. Responses whose
   * entity is not artifact JSON — errors, validation reports — are returned unchanged.
   */
  protected Response negotiateArtifactResponse(Response jsonResponse, CedarResourceType resourceType) {
    Optional<MediaType> responseType = negotiatedArtifactResponseType();
    if (responseType.isEmpty() || ArtifactYamlTranscoder.isJson(responseType.get())) {
      return jsonResponse;
    }
    if (Response.Status.Family.familyOf(jsonResponse.getStatus()) != Response.Status.Family.SUCCESSFUL
        || !(jsonResponse.getEntity() instanceof JsonNode artifactNode)) {
      return jsonResponse;
    }
    try {
      return Response.fromResponse(jsonResponse)
          .entity(ArtifactYamlTranscoder.jsonToYaml(artifactNode, resourceType, false))
          .type(responseType.get())
          .build();
    } catch (Exception e) {
      log.warn("The artifact could not be rendered as YAML; returning the JSON response", e);
      return jsonResponse;
    }
  }

  /**
   * Rejects the compact query parameter on write operations. On a read it selects the lossy
   * compact YAML rendering; on a write it can only signal a misunderstanding, since write
   * responses always render the full form and compact bodies are rejected.
   */
  protected void rejectCompactOnWriteOperations(Optional<Boolean> compactParam) throws CedarBadRequestException {
    if (compactParam.isPresent()) {
      throw new CedarBadRequestException(new CedarErrorPack()
          .message("The compact parameter is not supported on write operations: write responses always render "
              + "the full form, and the compact form can not be stored. "
              + "See https://metadatacenter.readthedocs.io/en/latest/yaml-spec/minimal-and-full/"));
    }
  }
}
