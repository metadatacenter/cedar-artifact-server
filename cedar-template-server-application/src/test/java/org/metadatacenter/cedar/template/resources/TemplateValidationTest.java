package org.metadatacenter.cedar.template.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TemplateValidationTest extends BaseTemplateResourceTest {

  @Test
  public void shouldPassEmptyTemplate() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/empty-template.json")
    );
  }

  @Test
  public void shouldPassSingleFieldTemplate() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/single-field-template.json")
    );
  }

  @Test
  public void shouldPassMultiFieldTemplate() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/multi-field-template.json")
    );
  }

  @Test
  public void shouldPassNestedElementTemplate() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/nested-element-template.json")
    );
  }

  @Test
  public void shouldFailMissingContext() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-context.json")
    );
  }

  @Test
  public void shouldFailMissingId() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-id.json")
    );
  }

  @Test
  public void shouldFailMissingType() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-type.json")
    );
  }

  @Test
  public void shouldFailMissingJsonType() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-json-type.json")
    );
  }

  @Test
  public void shouldFailMissingTitle() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-title.json")
    );
  }

  @Test
  public void shouldFailMissingDescription() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-description.json")
    );
  }

  @Test
  public void shouldFailMissingUi() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-ui.json")
    );
  }

  @Test
  public void shouldFailMissingProperties() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-properties.json")
    );
  }

  @Test
  public void shouldFailMissingRequired() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-required.json")
    );
  }

  @Test
  public void shouldFailMissingProvenance() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-provenance.json")
    );
  }

  @Test
  public void shouldPassMissingAdditionalProperties() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-additional-properties.json")
    );
  }

  @Test
  public void shouldFailInvalidValueAdditionalProperties() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/invalid-value-additional-properties.json")
    );
  }

  @Test
  public void shouldFailMissingSchema() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-schema.json")
    );
  }

  @Test
  public void shouldFailMissingProperties_Context() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/@context");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@context\"])");
  }

  @Test
  public void shouldFailMissingProperties_Id() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/@id");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@id\"])");
  }

  @Test
  public void shouldFailMissingProperties_Type() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/@type");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@type\"])");
  }

  @Test
  public void shouldFailMissingProperties_IsBasedOn() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/schema:isBasedOn");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"schema:isBasedOn\"])");
  }

  @Test
  public void shouldFailMissingProperties_Name() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/schema:name");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"schema:name\"])");
  }

  @Test
  public void shouldFailMissingProperties_Description() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/schema:description");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"schema:description=\"])");
  }

  @Test
  public void shouldFailMissingProperties_CreatedOn() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/pav:createdOn");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:createdOn\"])");
  }

  @Test
  public void shouldFailMissingProperties_CreatedBy() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/pav:createdBy");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:createdBy\"])");
  }

  @Test
  public void shouldFailMissingProperties_LastUpdatedOn() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/pav:lastUpdatedOn");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:lastUpdatedOn\"])");
  }

  @Test
  public void shouldFailMissingProperties_ModifiedBy() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/oslc:modifiedBy");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"oslc:modifiedBy\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Type() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/@type");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "");
  }

  @Test
  public void shouldFailMissingProperties_Field_Context() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/@context");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@context\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_JsonType() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/type");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"type\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Title() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/title");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"title\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Description() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/description");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"description\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Required() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/required");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"required\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_CreatedOn() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/pav:createdOn");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:createdOn\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_CreatedBy() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/pav:createdBy");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:createdBy\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_LastUpdatedOn() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/pav:lastUpdatedOn");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:lastUpdatedOn\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_ModifiedBy() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/oslc:modifiedBy");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"oslc:modifiedBy\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_AdditionalProperties() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/additionalProperties");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"additionalProperties\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Id() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/@id");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "");
  }

  @Test
  public void shouldFailMissingProperties_Field_Schema() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/$schema");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"$schema\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Properties() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/properties");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "No JSON Schema properties field in artifact at path /");
  }

  @Test
  public void shouldFailMissingProperties_Field_Properties_Type() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/properties/@type");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@type\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Properties_Value() {
    // Arrange
    String templateString = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateString = JsonUtils.removeFieldFromDocument(templateString, "/properties/studyName/properties/@value");
    // Act
    JsonNode responseMessage = runValidation(templateString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "A template field without value constraints must have a '@value' field at path /properties/studyName/properties/");
  }

  private void runTestAndAssert(TestResource testResource) {
    String payload = testResource.getContent();
    Response response = sendPostRequest(
        RequestUrls.forValidatingTemplate(getPortNumber()),
        payload);
    checkStatusOk(response);
    // Assert
    String responseMessage = response.readEntity(String.class);
    assertThat(responseMessage, is(testResource.getExpected()));
  }

  private JsonNode runValidation(String payload) {
    Response response = sendPostRequest(
        RequestUrls.forValidatingTemplate(getPortNumber()),
        payload);
    checkStatusOk(response);
    JsonNode responseMessage = getJsonResponseMessage(response);
    return responseMessage;
  }

  private static void assertValidationMessage(JsonNode responseMessage, String expectedValue) {
    assertThat(responseMessage.get("errors").get(0).asText(), is(expectedValue));
  }

  private static void assertValidationStatus(JsonNode responseMessage, String expectedValue) {
    assertThat(responseMessage.get("validates").asText(), is(expectedValue));
  }

  private JsonNode getJsonResponseMessage(Response response) {
    try {
      return new ObjectMapper().readTree(response.readEntity(String.class));
    } catch (IOException e) {
      throw new RuntimeException("Programming error", e);
    }
  }

  private static void checkStatusOk(@Nonnull Response response) {
    checkNotNull(response);
    int responseCode = response.getStatus();
    if (Response.Status.Family.familyOf(responseCode) == Response.Status.Family.CLIENT_ERROR) {
      throw new RuntimeException("Request contains bad syntax or cannot be fulfilled:\n" + response.toString());
    } else if (Response.Status.Family.familyOf(responseCode) == Response.Status.Family.SERVER_ERROR) {
      throw new RuntimeException("Server failed to fulfill the request:\n" + response.toString());
    }
  }
}
