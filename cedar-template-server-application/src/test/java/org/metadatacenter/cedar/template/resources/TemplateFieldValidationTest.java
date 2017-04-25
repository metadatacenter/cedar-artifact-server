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

public class TemplateFieldValidationTest extends BaseTemplateResourceTest {

  @Test
  public void shouldPassTextField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassCheckboxField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/checkbox-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassDateField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/date-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassEmailField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/email-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassImageField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/image-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassListField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/list-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassNumericField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/numeric-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassParagraphField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/paragraph-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassPhoneField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/phone-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassRadioField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/radio-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassRichTextField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/rich-text-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassVideoField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/video-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassLinkField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/link-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassConstrainedTextField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/constrained-text-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldFailMissingContext() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/@context");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['@context'])");
  }

  @Test
  public void shouldPassMissingId() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/@id");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldFailMissingType() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/@type");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['@type'])");
  }

  @Test
  public void shouldFailMissingJsonType() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/type");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['type'])");
  }

  @Test
  public void shouldFailMissingTitle() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/title");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['title'])");
  }

  @Test
  public void shouldFailMissingDescription() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/description");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['description'])");
  }

  @Test
  public void shouldFailMissingUi() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/_ui");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['_ui'])");
  }

  @Test
  public void shouldFailMissingProperties() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/properties");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['properties'])");
  }

  @Test
  public void shouldFailMissingRequired() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/required");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['required'])");
  }

  @Test
  public void shouldFailMissingValueConstraints() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/_valueConstraints");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['_valueConstraints'])");
  }

  @Test
  public void shouldFailMissingCreatedOn() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/pav:createdOn");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['pav:createdOn'])");
  }

  @Test
  public void shouldFailMissingCreatedBy() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/pav:createdBy");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['pav:createdBy'])");
  }

  @Test
  public void shouldFailMissingLastUpdatedOn() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/pav:lastUpdatedOn");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['pav:lastUpdatedOn'])");
  }

  @Test
  public void shouldFailMissingModifiedBy() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/oslc:modifiedBy");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['oslc:modifiedBy'])");
  }

  @Test
  public void shouldFailMissingAdditionalProperties() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/additionalProperties");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['additionalProperties'])");
  }

  @Test
  public void shouldFailMissingSchema() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/$schema");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['$schema'])");
  }

  @Test
  public void shouldFailMissingProperties_Value() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/properties/@value");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['@value'])");
  }

  @Test
  public void shouldFailMissingProperties_Id() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/constrained-text-field.json");
    fieldString = JsonUtils.removeFieldFromDocument(fieldString, "/properties/@id");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['@id'])");
  }

  @Test
  public void shouldFailMisplacedIdProperty_InTextField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/value-constraints/invalid-text-field-1.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['@value'])");
  }

  @Test
  public void shouldFailMisplacedValueProperty_InConstrainedTextField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/value-constraints/invalid-constrained-text-field-1.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['@id'])");
  }

  @Test
  public void shouldFailMisplaceValueIdProperty_InTextField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/value-constraints/invalid-text-field-2.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has invalid properties (['@id'])");
  }

  @Test
  public void shouldFailMisplaceValueIdProperty_InConstrainedTextField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/value-constraints/invalid-constrained-text-field-2.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has invalid properties (['@value'])");
  }

  @Test
  public void shouldFailMisplacedIdProperty_InRadioField() {
    // Arrange
    String fieldString = TestResourcesUtils.getStringContent("fields/value-constraints/invalid-radio-field.json");
    // Act
    JsonNode responseMessage = runValidation(fieldString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['@value'])");
  }

  private JsonNode runValidation(String payload) {
    Response response = sendPostRequest(
        RequestUrls.forValidatingField(getPortNumber()),
        payload);
    checkStatusOk(response);
    JsonNode responseMessage = getJsonResponseMessage(response);
    return responseMessage;
  }

  private static void assertValidationMessage(JsonNode responseMessage, String expectedValue) {
    assertThat(printReason(responseMessage), responseMessage.get("errors").get(0).get("message").asText(), is(expectedValue));
  }

  private static void assertValidationStatus(JsonNode responseMessage, String expectedValue) {
    assertThat(printReason(responseMessage), responseMessage.get("validates").asText(), is(expectedValue));
  }

  private static String printReason(JsonNode responseMessage) {
    return "The server is returning a different validation report.\n(application/json): " + responseMessage.toString();
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
