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

public class TemplateElementValidationTest extends BaseTemplateResourceTest {

  @Test
  public void shouldPassEmptyElement() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/empty-element.json");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassMultiFieldElement() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassNestedElement() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/nested-element.json");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldFailMissingContext() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/@context");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@context\"])");
  }

  @Test
  public void shouldFailMissingId() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/@id");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@id\"])");
  }

  @Test
  public void shouldFailMissingType() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/@type");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@type\"])");
  }

  @Test
  public void shouldFailMissingJsonType() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/type");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"type\"])");
  }

  @Test
  public void shouldFailMissingTitle() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/title");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"title\"])");
  }

  @Test
  public void shouldFailMissingDescription() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/description");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"description\"])");
  }

  @Test
  public void shouldFailMissingUi() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/_ui");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"_ui\"])");
  }

  @Test
  public void shouldFailMissingProperties() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"properties\"])");
  }

  @Test
  public void shouldPassMissingRequired() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/required");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldFailMissingCreatedOn() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/pav:createdOn");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:createdOn\"])");
  }

  @Test
  public void shouldFailMissingCreatedBy() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/pav:createdBy");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:createdBy\"])");
  }

  @Test
  public void shouldFailMissingLastUpdatedOn() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/pav:lastUpdatedOn");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:lastUpdatedOn\"])");
  }

  @Test
  public void shouldFailMissingModifiedBy() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/oslc:modifiedBy");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"oslc:modifiedBy\"])");
  }

  @Test
  public void shouldPassMissingAdditionalProperties() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/additionalProperties");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldFailMissingSchema() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/$schema");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"$schema\"])");
  }

  @Test
  public void shouldFailMissingProperties_Context() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/@context");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@context\"])");
  }

  @Test
  public void shouldFailMissingProperties_Id() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/@id");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@id\"])");
  }

  @Test
  public void shouldFailMissingProperties_Type() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/@type");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@type\"])");
  }

  @Test
  public void shouldFailMissingProperties_CreatedOn() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/pav:createdOn");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:createdOn\"])");
  }

  @Test
  public void shouldFailMissingProperties_CreatedBy() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/pav:createdBy");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:createdBy\"])");
  }

  @Test
  public void shouldFailMissingProperties_LastUpdatedOn() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/pav:lastUpdatedOn");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:lastUpdatedOn\"])");
  }

  @Test
  public void shouldFailMissingProperties_ModifiedBy() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/oslc:modifiedBy");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"oslc:modifiedBy\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Type() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/@type");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@type\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Context() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/@context");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@context\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_JsonType() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/type");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"type\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Title() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/title");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"title\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Description() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/description");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"description\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Required() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/required");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"required\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_CreatedOn() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/pav:createdOn");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:createdOn\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_CreatedBy() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/pav:createdBy");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:createdBy\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_LastUpdatedOn() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/pav:lastUpdatedOn");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"pav:lastUpdatedOn\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_ModifiedBy() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/oslc:modifiedBy");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"oslc:modifiedBy\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_AdditionalProperties() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/additionalProperties");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"additionalProperties\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Id() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/@id");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@id\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Schema() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/$schema");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"$schema\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Properties() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/properties");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "No JSON Schema properties field in artifact at path /");
  }

  @Test
  public void shouldFailMissingProperties_Field_Properties_Type() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/properties/@type");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties ([\"@type\"])");
  }

  @Test
  public void shouldFailMissingProperties_Field_Properties_Value() {
    // Arrange
    String elementString = TestResourcesUtils.getStringContent("elements/multi-field-element.json");
    elementString = JsonUtils.removeFieldFromDocument(elementString, "/properties/firstName/properties/@value");
    // Act
    JsonNode responseMessage = runValidation(elementString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "A template field without value constraints must have a '@value' field at path /properties/firstName/properties/");
  }

  private JsonNode runValidation(String payload) {
    Response response = sendPostRequest(
        RequestUrls.forValidatingElement(getPortNumber()),
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
