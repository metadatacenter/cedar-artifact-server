package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.metadatacenter.cedar.artifact.resources.JsonSchemaTitleAndDescription;
import org.metadatacenter.cedar.artifact.resources.TemplateElementsResource;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.exception.ArtifactServerResourceNotFoundException;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.security.model.user.CedarUser;
import org.metadatacenter.server.dao.ArtifactRevisionConflictException;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.util.test.TestAuthUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.util.List;
import java.util.UUID;

import static org.metadatacenter.cedar.artifact.resources.utils.TestConstants.LAST_UPDATED_ON_FIELD;
import static org.metadatacenter.model.ModelNodeNames.SCHEMA_IS_BASED_ON;

public class UpdateResourceTest extends AbstractResourceCrudTest {

  private static final String ELEMENT_NAME = "An Element";
  private static final String FIELD_NAME = "A Field";
  private static final String ATTRIBUTE_VALUE_FIELD_NAME = "Additional Information";
  private static final String SAFE_ATTRIBUTE_NAME = "safe";
  private static final String DUPLICATE_ATTRIBUTE_NAME = "duplicate";
  private static final String PROPERTY_IRI_PREFIX = "https://schema.metadatacenter.org/properties/";
  private static final String OCCURRENCE_IRI_PREFIX =
      "https://repo.metadatacenter.orgx/template-element-instances/";

  /**
   * 'UPDATE' TESTS
   */

  @ParameterizedTest
  @MethodSource("getCommonParams1")
  public void updateResourceTest(JsonNode sampleResource, CedarResourceType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
    // Create a artifact
    try {
      JsonNode createdResource = createResource(sampleResource, resourceType);
      createdResources.put(createdResource.get(LinkedData.ID).asText(), resourceType);
      String createdResourceId = createdResource.get(LinkedData.ID).asText();
      // Update the artifact
      // The name is the one field every artifact kind lets a client change: an instance is
      // constrained by its template schema, and a schema artifact's JSON Schema title and
      // description are derived from the name by the server rather than taken from the client.
      String fieldName = "schema:name";
      String fieldNewValue = "This is a new name";
      JsonNode updatedResource = ((ObjectNode) createdResource).put(fieldName, fieldNewValue);
      // Service invocation - Update
      Response responseUpdate = testClient.target(url + "/" + URLEncoder.encode(createdResourceId, "UTF-8")).
          request().header("Authorization", authHeader)
          .header("If-Match", currentEtag(url + "/" + URLEncoder.encode(createdResourceId, "UTF-8"), authHeader))
          .put(Entity.json(updatedResource));
      // Check HTTP response
      Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), responseUpdate.getStatus());
      // Retrieve updated element
      Response responseFind = testClient.target(url + "/" + URLEncoder.encode(createdResourceId, "UTF-8")).
          request().header("Authorization", authHeader).get();
      JsonNode actual = responseFind.readEntity(JsonNode.class);
      // Check that the modifications have been done correctly
      Assertions.assertNotNull(actual.get(fieldName));
      Assertions.assertEquals(fieldNewValue, actual.get(fieldName).asText());
      // Check that all the other fields contain the expected values, the derived pair having
      // followed the name
      if (resourceType != CedarResourceType.INSTANCE) {
        String previous = createdResource.path("description").asText(null);
        ((ObjectNode) createdResource).put("title", JsonSchemaTitleAndDescription.title(fieldNewValue, resourceType));
        ((ObjectNode) createdResource).put("description",
            JsonSchemaTitleAndDescription.description(fieldNewValue, resourceType, previous));
      }
      ((ObjectNode) createdResource).remove(fieldName);
      ((ObjectNode) actual).remove(fieldName);
      // Remove the lastUpdatedOn field
      ((ObjectNode) createdResource).remove(LAST_UPDATED_ON_FIELD);
      ((ObjectNode) actual).remove(LAST_UPDATED_ON_FIELD);
      Assertions.assertEquals(createdResource, actual);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @ParameterizedTest
  @MethodSource("getCommonParams1")
  public void updateRequiresIfMatch(JsonNode sampleResource, CedarResourceType resourceType) throws Exception {
    JsonNode prepared = setSchemaIsBasedOn(sampleTemplate.deepCopy(), sampleResource.deepCopy(), resourceType);
    JsonNode created = createResource(prepared, resourceType);
    String id = created.get(LinkedData.ID).asText();
    createdResources.put(id, resourceType);
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType) + "/" + URLEncoder.encode(id, "UTF-8");

    Response response = testClient.target(url).request().header(HttpHeaders.AUTHORIZATION, authHeader)
        .put(Entity.json(created));

    Assertions.assertEquals(CedarResponseStatus.PRECONDITION_REQUIRED.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest
  @MethodSource("getCommonParams1")
  public void staleConcurrentUpdateIsRejected(JsonNode sampleResource, CedarResourceType resourceType) throws Exception {
    JsonNode prepared = setSchemaIsBasedOn(sampleTemplate.deepCopy(), sampleResource.deepCopy(), resourceType);
    ObjectNode created = (ObjectNode) createResource(prepared, resourceType);
    String id = created.get(LinkedData.ID).asText();
    createdResources.put(id, resourceType);
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType) + "/" + URLEncoder.encode(id, "UTF-8");
    String originalEtag = currentEtag(url, authHeader);
    String editableName = "schema:name";

    ObjectNode firstEditor = created.deepCopy().put(editableName, "first editor");
    ObjectNode secondEditor = created.deepCopy().put(editableName, "second editor");
    Response firstResponse = testClient.target(url).request().header(HttpHeaders.AUTHORIZATION, authHeader)
        .header(HttpHeaders.IF_MATCH, originalEtag).put(Entity.json(firstEditor));
    Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), firstResponse.getStatus());
    Assertions.assertNotEquals(originalEtag, firstResponse.getHeaderString(HttpHeaders.ETAG));

    Response staleResponse = testClient.target(url).request().header(HttpHeaders.AUTHORIZATION, authHeader)
        .header(HttpHeaders.IF_MATCH, originalEtag).put(Entity.json(secondEditor));
    Assertions.assertEquals(CedarResponseStatus.PRECONDITION_FAILED.getStatusCode(), staleResponse.getStatus());

    JsonNode stored = testClient.target(url).request().header(HttpHeaders.AUTHORIZATION, authHeader)
        .get().readEntity(JsonNode.class);
    Assertions.assertEquals("first editor", stored.get(editableName).asText());
  }

  @ParameterizedTest
  @MethodSource("getCommonParams1")
  public void conditionalPutCannotRecreateADeletedArtifact(JsonNode sampleResource,
                                                            CedarResourceType resourceType) throws Exception {
    JsonNode prepared = setSchemaIsBasedOn(sampleTemplate.deepCopy(), sampleResource.deepCopy(), resourceType);
    JsonNode created = createResource(prepared, resourceType);
    String id = created.get(LinkedData.ID).asText();
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType) + "/" + URLEncoder.encode(id, "UTF-8");
    String etag = currentEtag(url, authHeader);

    Response deleted = testClient.target(url).request().header(HttpHeaders.AUTHORIZATION, authHeader)
        .header(HttpHeaders.IF_MATCH, etag).delete();
    Assertions.assertEquals(CedarResponseStatus.NO_CONTENT.getStatusCode(), deleted.getStatus());

    for (String ifMatch : List.of(etag, "*")) {
      Response recreate = testClient.target(url).request().header(HttpHeaders.AUTHORIZATION, authHeader)
          .header(HttpHeaders.IF_MATCH, ifMatch).put(Entity.json(created));
      Assertions.assertEquals(CedarResponseStatus.PRECONDITION_FAILED.getStatusCode(), recreate.getStatus());
      Response stillGone = testClient.target(url).request().header(HttpHeaders.AUTHORIZATION, authHeader).get();
      Assertions.assertEquals(CedarResponseStatus.NOT_FOUND.getStatusCode(), stillGone.getStatus());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void deletionReportedByMongoAfterUpdatePreflightReturnsPreconditionFailed() throws Exception {
    ObjectNode created = (ObjectNode) createResource(sampleElement.deepCopy(), CedarResourceType.ELEMENT);
    String id = created.get(LinkedData.ID).asText();
    createdResources.put(id, CedarResourceType.ELEMENT);
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, CedarResourceType.ELEMENT) + "/"
        + URLEncoder.encode(id, "UTF-8");
    String etag = currentEtag(url, authHeader);

    TemplateElementService<String, JsonNode> original = TestUtil.templateElementService;
    TemplateElementService<String, JsonNode> disappearing =
        (TemplateElementService<String, JsonNode>) Proxy.newProxyInstance(
            TemplateElementService.class.getClassLoader(),
            new Class<?>[]{TemplateElementService.class},
            (proxy, method, arguments) -> {
              if ("updateTemplateElement".equals(method.getName())) {
                throw new ArtifactServerResourceNotFoundException();
              }
              try {
                return method.invoke(original, arguments);
              } catch (InvocationTargetException e) {
                throw e.getCause();
              }
            });

    // TemplateElementsResource holds the service in a static field shared by the registered Jersey
    // instance. Swap in a deterministic DAO race for this request, then restore the embedded Mongo
    // service before cleanup and the next test.
    new TemplateElementsResource(TestUtil.getCedarConfig(), disappearing);
    try {
      ObjectNode update = created.deepCopy().put("schema:name", "update that loses to deletion");
      Response response = testClient.target(url).request().header(HttpHeaders.AUTHORIZATION, authHeader)
          .header(HttpHeaders.IF_MATCH, etag).put(Entity.json(update));
      Assertions.assertEquals(CedarResponseStatus.PRECONDITION_FAILED.getStatusCode(), response.getStatus());
    } finally {
      new TemplateElementsResource(TestUtil.getCedarConfig(), original);
    }
  }

  @Test
  public void mongoCompareAndSwapRejectsAStaleRevision() throws Exception {
    ObjectNode created = (ObjectNode) createResource(sampleTemplate.deepCopy(), CedarResourceType.TEMPLATE);
    String id = created.get(LinkedData.ID).asText();
    createdResources.put(id, CedarResourceType.TEMPLATE);
    long originalRevision = TestUtil.templateService.getTemplateRevision(id);

    ObjectNode firstEditor = created.deepCopy().put("schema:name", "first editor");
    ObjectNode secondEditor = created.deepCopy().put("schema:name", "second editor");
    TestUtil.templateService.updateTemplate(id, firstEditor, originalRevision);

    Assertions.assertThrows(ArtifactRevisionConflictException.class,
        () -> TestUtil.templateService.updateTemplate(id, secondEditor, originalRevision));
    Assertions.assertEquals("first editor", TestUtil.templateService.findTemplate(id).get("schema:name").asText());
  }

  @ParameterizedTest
  @MethodSource("getCommonParams1")
  public void updateOnlyPermissionCannotCreateByPut(JsonNode sampleResource, CedarResourceType resourceType)
      throws Exception {
    JsonNode prepared = setSchemaIsBasedOn(sampleTemplate.deepCopy(), sampleResource.deepCopy(), resourceType);
    String id = "https://repo.metadatacenter.org/artifacts/" + UUID.randomUUID();
    ((ObjectNode) prepared).put(LinkedData.ID, id);
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType) + "/" + URLEncoder.encode(id, "UTF-8");
    CedarUser updateOnlyUser = TestAuthUtil.getTestUser2(TestUtil.getCedarConfig());
    List<String> originalPermissions = List.copyOf(updateOnlyUser.getPermissions());
    CedarPermission updatePermission = CedarPermission.getUpdateForArtifactType(resourceType);
    updateOnlyUser.setPermissions(List.of(CedarPermission.LOGGED_IN.getPermissionName(),
        updatePermission.getPermissionName()));
    try {
      Response response = testClient.target(url).request()
          .header(HttpHeaders.AUTHORIZATION, TestAuthUtil.getTestUser2AuthHeader(TestUtil.getCedarConfig()))
          .put(Entity.json(prepared));
      Assertions.assertEquals(CedarResponseStatus.FORBIDDEN.getStatusCode(), response.getStatus());
    } finally {
      updateOnlyUser.setPermissions(originalPermissions);
    }
  }

  @Test
  public void ordinaryPutRepairsAnInheritedUnusableTemplatePropertyIri() throws Exception {
    ObjectNode created = createTemplateWithField();
    String id = created.get(LinkedData.ID).asText();
    ObjectNode brokenStored = created.deepCopy();
    propertyMapping(brokenStored, FIELD_NAME).putArray("enum").add("");
    // The Mongo DAO escapes '$' keys in-place; isolate that storage-only mutation from the HTTP body.
    TestUtil.templateService.updateTemplate(id, brokenStored.deepCopy(), TestUtil.templateService.getTemplateRevision(id));

    ObjectNode submitted = brokenStored.deepCopy();
    submitted.put("schema:name", "Edited old template");
    Response response = put(submitted, id, CedarResourceType.TEMPLATE);

    Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
    JsonNode repaired = response.readEntity(JsonNode.class);
    Assertions.assertTrue(propertyMapping((ObjectNode) repaired, FIELD_NAME).get("enum").get(0).asText()
        .startsWith(PROPERTY_IRI_PREFIX));
  }

  @Test
  public void ordinaryPutAcceptsAClientThatDropsAnInheritedUnusableTemplatePropertyIri() throws Exception {
    ObjectNode created = createTemplateWithField();
    String id = created.get(LinkedData.ID).asText();
    ObjectNode brokenStored = created.deepCopy();
    propertyMapping(brokenStored, FIELD_NAME).putArray("enum").add("");
    TestUtil.templateService.updateTemplate(id, brokenStored.deepCopy(), TestUtil.templateService.getTemplateRevision(id));

    // The hardened Designer does not own repository property IRIs. If it
    // canonicalizes an old unusable mapping by omitting it, the update must
    // still reach the server's normal minting path rather than strand the
    // production artifact because the submitted defect is no longer byte-for-
    // byte identical to the stored one.
    ObjectNode submitted = brokenStored.deepCopy();
    ((ObjectNode) submitted.get("properties").get(LinkedData.CONTEXT).get("properties")).remove(FIELD_NAME);
    submitted.put("schema:name", "Edited old template through a hardened client");

    Response response = put(submitted, id, CedarResourceType.TEMPLATE);

    Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
    JsonNode repaired = response.readEntity(JsonNode.class);
    Assertions.assertTrue(propertyMapping((ObjectNode) repaired, FIELD_NAME).get("enum").get(0).asText()
        .startsWith(PROPERTY_IRI_PREFIX));
  }

  @Test
  public void ordinaryPutRejectsANewUnusableTemplatePropertyIri() throws Exception {
    ObjectNode created = createTemplateWithField();
    String id = created.get(LinkedData.ID).asText();
    ObjectNode submitted = created.deepCopy();
    submitted.put("schema:name", "Edited template");
    propertyMapping(submitted, FIELD_NAME).putArray("enum").add("");

    Response response = put(submitted, id, CedarResourceType.TEMPLATE);

    Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    response.close();
  }

  @Test
  public void ordinaryPutRestoresAnInheritedMissingChildSchema() throws Exception {
    ObjectNode created = createTemplateWithField();
    String id = created.get(LinkedData.ID).asText();
    ObjectNode brokenStored = created.deepCopy();
    ((ObjectNode) brokenStored.path("properties").path(FIELD_NAME)).remove("$schema");
    TestUtil.templateService.updateTemplate(id, brokenStored.deepCopy(), TestUtil.templateService.getTemplateRevision(id));

    ObjectNode submitted = brokenStored.deepCopy();
    submitted.put("schema:name", "Edited legacy template");
    Response response = put(submitted, id, CedarResourceType.TEMPLATE);

    Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
    JsonNode repaired = response.readEntity(JsonNode.class);
    Assertions.assertEquals("http://json-schema.org/draft-04/schema#",
        repaired.path("properties").path(FIELD_NAME).path("$schema").asText());
  }

  @Test
  public void ordinaryPutRejectsANewMissingChildSchema() throws Exception {
    ObjectNode created = createTemplateWithField();
    String id = created.get(LinkedData.ID).asText();
    ObjectNode submitted = created.deepCopy();
    submitted.put("schema:name", "Removed a required child declaration");
    ((ObjectNode) submitted.path("properties").path(FIELD_NAME)).remove("$schema");

    Response response = put(submitted, id, CedarResourceType.TEMPLATE);

    Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    response.close();
  }

  @Test
  public void verbatimPutDoesNotRepairAnInheritedMissingChildSchema() throws Exception {
    ObjectNode created = createTemplateWithField();
    String id = created.get(LinkedData.ID).asText();
    ObjectNode brokenStored = created.deepCopy();
    ((ObjectNode) brokenStored.path("properties").path(FIELD_NAME)).remove("$schema");
    TestUtil.templateService.updateTemplate(id, brokenStored.deepCopy(), TestUtil.templateService.getTemplateRevision(id));

    ObjectNode submitted = brokenStored.deepCopy();
    Response response = verbatimPut(submitted, id, CedarResourceType.TEMPLATE);

    Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    response.close();
  }

  @Test
  public void ordinaryPutRepairsInheritedEmptyDerivedFromRecursively() throws Exception {
    ObjectNode created = createTemplateWithField();
    String id = created.get(LinkedData.ID).asText();
    ObjectNode brokenStored = created.deepCopy();
    brokenStored.put("pav:derivedFrom", "");
    ((ObjectNode) brokenStored.path("properties").path(FIELD_NAME)).put("pav:derivedFrom", "");
    TestUtil.templateService.updateTemplate(id, brokenStored.deepCopy(), TestUtil.templateService.getTemplateRevision(id));

    ObjectNode submitted = brokenStored.deepCopy();
    submitted.put("schema:name", "Edited old template provenance");
    Response response = put(submitted, id, CedarResourceType.TEMPLATE);

    Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
    JsonNode repaired = response.readEntity(JsonNode.class);
    Assertions.assertFalse(repaired.has("pav:derivedFrom"));
    Assertions.assertFalse(repaired.path("properties").path(FIELD_NAME).has("pav:derivedFrom"));
  }

  @Test
  public void ordinaryPutAcceptsAClientThatDropsInheritedEmptyDerivedFrom() throws Exception {
    ObjectNode created = createTemplateWithField();
    String id = created.get(LinkedData.ID).asText();
    ObjectNode brokenStored = created.deepCopy();
    brokenStored.put("pav:derivedFrom", "");
    ((ObjectNode) brokenStored.path("properties").path(FIELD_NAME)).put("pav:derivedFrom", "");
    TestUtil.templateService.updateTemplate(id, brokenStored.deepCopy(), TestUtil.templateService.getTemplateRevision(id));

    // The compatibility reader maps the legacy spelling to absence, and its
    // writer omits the optional key before the ordinary update reaches here.
    ObjectNode submitted = brokenStored.deepCopy();
    submitted.remove("pav:derivedFrom");
    ((ObjectNode) submitted.path("properties").path(FIELD_NAME)).remove("pav:derivedFrom");
    submitted.put("schema:name", "Edited through compatibility reader");
    Response response = put(submitted, id, CedarResourceType.TEMPLATE);

    Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
    JsonNode repaired = response.readEntity(JsonNode.class);
    Assertions.assertFalse(repaired.has("pav:derivedFrom"));
    Assertions.assertFalse(repaired.path("properties").path(FIELD_NAME).has("pav:derivedFrom"));
  }

  @Test
  public void ordinaryPutRejectsANewEmptyDerivedFrom() throws Exception {
    ObjectNode created = createTemplateWithField();
    String id = created.get(LinkedData.ID).asText();
    ObjectNode submitted = created.deepCopy();
    submitted.put("schema:name", "Introduced invalid provenance");
    submitted.put("pav:derivedFrom", "");

    Response response = put(submitted, id, CedarResourceType.TEMPLATE);

    Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    response.close();
  }

  @Test
  public void ordinaryPutRepairsAnInheritedUnusableElementOccurrenceId() throws Exception {
    ObjectNode template = createTemplateWithElement();
    ObjectNode created = createInstanceWithElement(template);
    String id = created.get(LinkedData.ID).asText();
    ObjectNode brokenStored = created.deepCopy();
    ((ObjectNode) brokenStored.get(ELEMENT_NAME)).put(LinkedData.ID, "");
    TestUtil.templateInstanceService.updateTemplateInstance(id, brokenStored.deepCopy(),
        TestUtil.templateInstanceService.getTemplateInstanceRevision(id));

    ObjectNode submitted = brokenStored.deepCopy();
    submitted.put("schema:name", "Edited old instance");
    Response response = put(submitted, id, CedarResourceType.INSTANCE);

    Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
    JsonNode repaired = response.readEntity(JsonNode.class);
    Assertions.assertTrue(repaired.get(ELEMENT_NAME).get(LinkedData.ID).asText()
        .startsWith(OCCURRENCE_IRI_PREFIX));
  }

  @Test
  public void ordinaryPutAcceptsAClientThatCanonicalizesAnInheritedUnusableOccurrenceId() throws Exception {
    ObjectNode template = createTemplateWithElement();
    ObjectNode created = createInstanceWithElement(template);
    String id = created.get(LinkedData.ID).asText();
    ObjectNode brokenStored = created.deepCopy();
    ((ObjectNode) brokenStored.get(ELEMENT_NAME)).put(LinkedData.ID, "");
    TestUtil.templateInstanceService.updateTemplateInstance(id, brokenStored.deepCopy(),
        TestUtil.templateInstanceService.getTemplateInstanceRevision(id));

    // CEE's compatibility reader opens the legacy empty string and its writer
    // emits null, the canonical request for server assignment. The differential
    // repair boundary must allow that safe client-side normalization even
    // though it no longer equals the stored spelling.
    ObjectNode submitted = brokenStored.deepCopy();
    ((ObjectNode) submitted.get(ELEMENT_NAME)).putNull(LinkedData.ID);
    submitted.put("schema:name", "Edited old instance through a hardened client");

    Response response = put(submitted, id, CedarResourceType.INSTANCE);

    Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
    JsonNode repaired = response.readEntity(JsonNode.class);
    Assertions.assertTrue(repaired.get(ELEMENT_NAME).get(LinkedData.ID).asText()
        .startsWith(OCCURRENCE_IRI_PREFIX));
  }

  @Test
  public void ordinaryPutRejectsANewUnusableElementOccurrenceId() throws Exception {
    ObjectNode template = createTemplateWithElement();
    ObjectNode created = createInstanceWithElement(template);
    String id = created.get(LinkedData.ID).asText();
    ObjectNode submitted = created.deepCopy();
    submitted.put("schema:name", "Edited instance");
    ((ObjectNode) submitted.get(ELEMENT_NAME)).put(LinkedData.ID, "");

    Response response = put(submitted, id, CedarResourceType.INSTANCE);

    Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    response.close();
  }

  @Test
  public void ordinaryPutRepairsInheritedInvalidAttributeValueNames() throws Exception {
    ObjectNode template = createTemplateWithAttributeValueField();
    ObjectNode created = createInstanceWithAttributeValueField(template);
    String id = created.get(LinkedData.ID).asText();
    ObjectNode brokenStored = withInvalidAttributeValueNames(created.deepCopy());
    TestUtil.templateInstanceService.updateTemplateInstance(id, brokenStored.deepCopy(),
        TestUtil.templateInstanceService.getTemplateInstanceRevision(id));

    ObjectNode submitted = brokenStored.deepCopy();
    submitted.put("schema:name", "Edited old attribute-value instance");
    Response response = put(submitted, id, CedarResourceType.INSTANCE);

    Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
    JsonNode repaired = response.readEntity(JsonNode.class);
    ArrayNode repairedNames = (ArrayNode) repaired.get(ATTRIBUTE_VALUE_FIELD_NAME);
    Assertions.assertEquals(1, repairedNames.size());
    Assertions.assertEquals(DUPLICATE_ATTRIBUTE_NAME, repairedNames.get(0).asText());
  }

  @Test
  public void ordinaryPutRejectsNewInvalidAttributeValueNames() throws Exception {
    ObjectNode template = createTemplateWithAttributeValueField();
    ObjectNode created = createInstanceWithAttributeValueField(template);
    String id = created.get(LinkedData.ID).asText();
    ObjectNode submitted = withInvalidAttributeValueNames(created.deepCopy());
    submitted.put("schema:name", "Edited attribute-value instance");

    Response response = put(submitted, id, CedarResourceType.INSTANCE);

    Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    response.close();
  }

  private ObjectNode createTemplateWithElement() {
    return createTemplateWithChild(ELEMENT_NAME, sampleElement.deepCopy());
  }

  private ObjectNode createTemplateWithField() {
    return createTemplateWithChild(FIELD_NAME, textField(FIELD_NAME));
  }

  private ObjectNode textField(String fieldName) {
    ObjectNode field = sampleElement.deepCopy();
    field.put("$schema", "http://json-schema.org/draft-04/schema#");
    field.put("@type", "https://schema.metadatacenter.org/core/TemplateField");
    field.put("schema:name", fieldName);
    field.put("schema:schemaVersion", "1.5.0");
    field.remove("pav:version");
    field.remove("bibo:status");
    ObjectNode fieldContext = (ObjectNode) field.get(LinkedData.CONTEXT);
    fieldContext.put("skos", "http://www.w3.org/2004/02/skos/core#");
    fieldContext.putObject("skos:prefLabel").put("@type", "xsd:string");
    fieldContext.putObject("skos:altLabel").put("@type", "xsd:string");
    ObjectNode fieldUi = field.putObject("_ui");
    fieldUi.put("inputType", "textfield");
    field.putObject("_valueConstraints").put("requiredValue", false);
    ObjectNode fieldProperties = field.putObject("properties");
    ObjectNode typeProperty = fieldProperties.putObject("@type");
    com.fasterxml.jackson.databind.node.ArrayNode typeAlternatives = typeProperty.putArray("oneOf");
    typeAlternatives.addObject().put("type", "string").put("format", "uri");
    ObjectNode typeArray = typeAlternatives.addObject();
    typeArray.put("type", "array");
    typeArray.put("minItems", 1);
    typeArray.putObject("items").put("type", "string").put("format", "uri");
    typeArray.put("uniqueItems", true);
    fieldProperties.putObject("rdfs:label").putArray("type").add("string").add("null");
    fieldProperties.putObject("@value").putArray("type").add("string").add("null");
    ObjectNode language = fieldProperties.putObject("@language");
    language.putArray("type").add("string").add("null");
    language.put("minLength", 1);
    field.putArray("required").add("@value");
    return field;
  }

  private ObjectNode attributeValueField(String fieldName) {
    ObjectNode field = sampleElement.deepCopy();
    field.put("@type", "https://schema.metadatacenter.org/core/TemplateField");
    field.put("type", "string");
    field.put("schema:name", fieldName);
    field.put("schema:schemaVersion", "1.5.0");
    field.remove("properties");
    field.remove("required");
    field.remove("pav:version");
    field.remove("bibo:status");
    ObjectNode fieldContext = (ObjectNode) field.get(LinkedData.CONTEXT);
    fieldContext.put("skos", "http://www.w3.org/2004/02/skos/core#");
    fieldContext.putObject("skos:prefLabel").put("@type", "xsd:string");
    fieldContext.putObject("skos:altLabel").put("@type", "xsd:string");
    ObjectNode fieldUi = field.putObject("_ui");
    fieldUi.put("inputType", "attribute-value");

    ObjectNode wrapper = ((ObjectNode) sampleTemplate).objectNode();
    wrapper.put("type", "array");
    wrapper.put("minItems", 0);
    wrapper.set("items", field);
    return wrapper;
  }

  private ObjectNode createTemplateWithAttributeValueField() {
    ObjectNode template = sampleTemplate.deepCopy();
    addTemplateChild(template, FIELD_NAME, textField(FIELD_NAME));
    addTemplateChild(template, ATTRIBUTE_VALUE_FIELD_NAME, attributeValueField(ATTRIBUTE_VALUE_FIELD_NAME));

    ObjectNode contextSchema = (ObjectNode) template.get("properties").get(LinkedData.CONTEXT);
    ObjectNode contextAdditionalProperties = contextSchema.putObject("additionalProperties");
    contextAdditionalProperties.put("type", "string");
    contextAdditionalProperties.put("format", "uri");

    ObjectNode valueSchema = template.putObject("additionalProperties");
    valueSchema.put("type", "object");
    ObjectNode valueProperties = valueSchema.putObject("properties");
    valueProperties.putObject("@value").putArray("type").add("string").add("null");
    valueProperties.putObject("@type").put("type", "string").put("format", "uri");
    valueSchema.putArray("required").add("@value");
    valueSchema.put("additionalProperties", false);
    return createTemplate(template);
  }

  private ObjectNode createTemplateWithChild(String childName, ObjectNode child) {
    ObjectNode template = sampleTemplate.deepCopy();
    addTemplateChild(template, childName, child);
    return createTemplate(template);
  }

  private void addTemplateChild(ObjectNode template, String childName, ObjectNode child) {
    ObjectNode ui = (ObjectNode) template.get("_ui");
    ((ArrayNode) ui.get("order")).add(childName);
    ((ObjectNode) ui.get("propertyLabels")).put(childName, childName);
    ((ObjectNode) ui.get("propertyDescriptions")).put(childName, "");
    ((ObjectNode) template.get("properties")).set(childName, child);
  }

  private ObjectNode createTemplate(ObjectNode template) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, CedarResourceType.TEMPLATE);
    Response createResponse = testClient.target(url)
        .property(ClientProperties.READ_TIMEOUT, 15000)
        .request().header("Authorization", authHeader).post(Entity.json(template));
    Assertions.assertEquals(CedarResponseStatus.CREATED.getStatusCode(), createResponse.getStatus());
    ObjectNode created = (ObjectNode) createResponse.readEntity(JsonNode.class);
    createdResources.put(created.get(LinkedData.ID).asText(), CedarResourceType.TEMPLATE);
    return created;
  }

  private ObjectNode createInstanceWithElement(ObjectNode template) {
    ObjectNode instance = sampleInstance.deepCopy();
    instance.put(SCHEMA_IS_BASED_ON, template.get(LinkedData.ID).asText());
    ((ObjectNode) instance.get(LinkedData.CONTEXT)).put(ELEMENT_NAME,
        propertyMapping(template, ELEMENT_NAME).get("enum").get(0).asText());
    ObjectNode occurrence = instance.putObject(ELEMENT_NAME);
    occurrence.putObject(LinkedData.CONTEXT);
    occurrence.putNull(LinkedData.ID);

    ObjectNode created = (ObjectNode) createResource(instance, CedarResourceType.INSTANCE);
    createdResources.put(created.get(LinkedData.ID).asText(), CedarResourceType.INSTANCE);
    return created;
  }

  private ObjectNode createInstanceWithAttributeValueField(ObjectNode template) {
    ObjectNode instance = sampleInstance.deepCopy();
    instance.put(SCHEMA_IS_BASED_ON, template.get(LinkedData.ID).asText());
    ((ObjectNode) instance.get(LinkedData.CONTEXT)).put(FIELD_NAME,
        propertyMapping(template, FIELD_NAME).get("enum").get(0).asText());
    instance.putArray(ATTRIBUTE_VALUE_FIELD_NAME).add(SAFE_ATTRIBUTE_NAME);
    instance.putObject(SAFE_ATTRIBUTE_NAME).put("@value", "a value");

    ObjectNode created = (ObjectNode) createResource(instance, CedarResourceType.INSTANCE);
    createdResources.put(created.get(LinkedData.ID).asText(), CedarResourceType.INSTANCE);
    return created;
  }

  private ObjectNode withInvalidAttributeValueNames(ObjectNode instance) {
    instance.putArray(ATTRIBUTE_VALUE_FIELD_NAME)
        .add(LinkedData.CONTEXT)
        .add(FIELD_NAME)
        .add(DUPLICATE_ATTRIBUTE_NAME)
        .add(DUPLICATE_ATTRIBUTE_NAME);
    ObjectNode context = (ObjectNode) instance.get(LinkedData.CONTEXT);
    context.remove(SAFE_ATTRIBUTE_NAME);
    context.put(DUPLICATE_ATTRIBUTE_NAME, PROPERTY_IRI_PREFIX + "legacy-duplicate");
    instance.remove(SAFE_ATTRIBUTE_NAME);
    instance.putObject(DUPLICATE_ATTRIBUTE_NAME).put("@value", "a value");
    return instance;
  }

  private ObjectNode propertyMapping(ObjectNode template, String childName) {
    return (ObjectNode) template.get("properties").get(LinkedData.CONTEXT).get("properties").get(childName);
  }

  private Response put(JsonNode artifact, String id, CedarResourceType resourceType) throws IOException {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    return testClient.target(url + "/" + URLEncoder.encode(id, "UTF-8"))
        .request().header("Authorization", authHeader)
        .header("If-Match", currentEtag(url + "/" + URLEncoder.encode(id, "UTF-8"), authHeader))
        .put(Entity.json(artifact));
  }

  private Response verbatimPut(JsonNode artifact, String id, CedarResourceType resourceType) throws IOException {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    String adminAuthHeader = TestAuthUtil.getAdminUserAuthHeader(TestUtil.getCedarConfig());
    return testClient.target(url + "/" + URLEncoder.encode(id, "UTF-8"))
        .queryParam("verbatim", true)
        .request().header("Authorization", adminAuthHeader)
        .header("If-Match", currentEtag(url + "/" + URLEncoder.encode(id, "UTF-8"), adminAuthHeader))
        .put(Entity.json(artifact));
  }

}
