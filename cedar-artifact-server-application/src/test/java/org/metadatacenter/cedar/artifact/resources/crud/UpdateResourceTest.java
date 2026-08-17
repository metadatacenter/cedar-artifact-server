package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;

import java.io.IOException;
import java.net.URLEncoder;

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
      // Template instances are JSON-LD documents constrained by their template schema. Update an
      // allowed metadata field so this generic CRUD test remains valid when update validation is on.
      String fieldName = resourceType == CedarResourceType.INSTANCE ? "schema:name" : "title";
      String fieldNewValue = "This is a new title";
      JsonNode updatedResource = ((ObjectNode) createdResource).put(fieldName, fieldNewValue);
      // Service invocation - Update
      Response responseUpdate = testClient.target(url + "/" + URLEncoder.encode(createdResourceId, "UTF-8")).
          request().header("Authorization", authHeader).put(Entity.json(updatedResource));
      // Check HTTP response
      Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), responseUpdate.getStatus());
      // Retrieve updated element
      Response responseFind = testClient.target(url + "/" + URLEncoder.encode(createdResourceId, "UTF-8")).
          request().header("Authorization", authHeader).get();
      JsonNode actual = responseFind.readEntity(JsonNode.class);
      // Check that the modifications have been done correctly
      Assertions.assertNotNull(actual.get(fieldName));
      Assertions.assertEquals(fieldNewValue, actual.get(fieldName).asText());
      // Check that all the other fields contain the expected values
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

  @Test
  public void ordinaryPutRepairsAnInheritedUnusableTemplatePropertyIri() throws Exception {
    ObjectNode created = createTemplateWithField();
    String id = created.get(LinkedData.ID).asText();
    ObjectNode brokenStored = created.deepCopy();
    propertyMapping(brokenStored, FIELD_NAME).putArray("enum").add("");
    // The Mongo DAO escapes '$' keys in-place; isolate that storage-only mutation from the HTTP body.
    TestUtil.templateService.updateTemplate(id, brokenStored.deepCopy());

    ObjectNode submitted = brokenStored.deepCopy();
    submitted.put("schema:name", "Edited old template");
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
  public void ordinaryPutRepairsAnInheritedUnusableElementOccurrenceId() throws Exception {
    ObjectNode template = createTemplateWithElement();
    ObjectNode created = createInstanceWithElement(template);
    String id = created.get(LinkedData.ID).asText();
    ObjectNode brokenStored = created.deepCopy();
    ((ObjectNode) brokenStored.get(ELEMENT_NAME)).put(LinkedData.ID, "");
    TestUtil.templateInstanceService.updateTemplateInstance(id, brokenStored.deepCopy());

    ObjectNode submitted = brokenStored.deepCopy();
    submitted.put("schema:name", "Edited old instance");
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
    TestUtil.templateInstanceService.updateTemplateInstance(id, brokenStored.deepCopy());

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
        .request().header("Authorization", authHeader).put(Entity.json(artifact));
  }

}
