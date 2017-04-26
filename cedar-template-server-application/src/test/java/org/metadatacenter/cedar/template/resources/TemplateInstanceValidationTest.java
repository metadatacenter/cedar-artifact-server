package org.metadatacenter.cedar.template.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TemplateInstanceValidationTest extends BaseTemplateResourceTest {

  private String singleFieldTemplateId;
  private String multiFieldTemplateId;
  private String multivaluedFieldTemplateId;
  private String multivaluedElementTemplateId;
  private String nestedElementTemplateId;

  private static String singleFieldTemplate;
  private static String multiFieldTemplate;
  private static String multivaluedFieldTemplate;
  private static String multivaluedElementTemplate;
  private static String nestedElementTemplate;

  @BeforeClass
  public static void loadTemplateExamples() {
    singleFieldTemplate = TestResourcesUtils.getStringContent("templates/single-field-template.json");
    multiFieldTemplate = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    multivaluedFieldTemplate = TestResourcesUtils.getStringContent("templates/multivalued-field-template.json");
    multivaluedElementTemplate = TestResourcesUtils.getStringContent("templates/multivalued-element-template.json");
    nestedElementTemplate = TestResourcesUtils.getStringContent("templates/nested-element-template.json");
  }

  @Before
  public void uploadTemplates() {
    singleFieldTemplateId = uploadTemplate(singleFieldTemplate);
    multiFieldTemplateId = uploadTemplate(multiFieldTemplate);
    multivaluedFieldTemplateId = uploadTemplate(multivaluedFieldTemplate);
    multivaluedElementTemplateId = uploadTemplate(multivaluedElementTemplate);
    nestedElementTemplateId = uploadTemplate(nestedElementTemplate);
  }

  @After
  public void removeTemplates() {
    removeTemplate(singleFieldTemplateId);
    removeTemplate(multiFieldTemplateId);
    removeTemplate(multivaluedFieldTemplateId);
    removeTemplate(multivaluedElementTemplateId);
    removeTemplate(nestedElementTemplateId);
  }

  @Test
  public void shouldPassSingleFieldInstance() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/single-field-instance.jsonld");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassMultiFieldInstance() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/multi-field-instance.jsonld");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassMultivaluedFieldInstance() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/multivalued-field-instance.jsonld");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassMultivaluedElementInstance() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/multivalued-element-instance.jsonld");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldPassNestedElementInstance() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/nested-element-instance.jsonld");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "true");
  }

  @Test
  public void shouldFailMissingContext() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/multi-field-instance.jsonld");
    instanceString = JsonUtils.removeFieldFromDocument(instanceString, "/@context");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['@context'])");
  }

  @Test
  public void shouldFailMissingId() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/multi-field-instance.jsonld");
    instanceString = JsonUtils.removeFieldFromDocument(instanceString, "/@id");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['@id'])");
  }

  @Test(expected=RuntimeException.class) // 400: Bad Request
  public void shouldFailMissingIsBasedOn() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/multi-field-instance.jsonld");
    instanceString = JsonUtils.removeFieldFromDocument(instanceString, "/schema:isBasedOn");
    // Act
    runValidation(instanceString);
  }

  @Test
  public void shouldFailMissingName() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/multi-field-instance.jsonld");
    instanceString = JsonUtils.removeFieldFromDocument(instanceString, "/schema:name");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['schema:name'])");
  }

  @Test
  public void shouldFailMissingDescription() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/multi-field-instance.jsonld");
    instanceString = JsonUtils.removeFieldFromDocument(instanceString, "/schema:description");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['schema:description'])");
  }

  @Test
  public void shouldFailMissingCreatedOn() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/multi-field-instance.jsonld");
    instanceString = JsonUtils.removeFieldFromDocument(instanceString, "/pav:createdOn");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['pav:createdOn'])");
  }

  @Test
  public void shouldFailMissingCreatedBy() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/multi-field-instance.jsonld");
    instanceString = JsonUtils.removeFieldFromDocument(instanceString, "/pav:createdBy");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['pav:createdBy'])");
  }

  @Test
  public void shouldFailMissingLastUpdatedOn() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/multi-field-instance.jsonld");
    instanceString = JsonUtils.removeFieldFromDocument(instanceString, "/pav:lastUpdatedOn");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['pav:lastUpdatedOn'])");
  }

  @Test
  public void shouldFailMissingModifiedBy() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/multi-field-instance.jsonld");
    instanceString = JsonUtils.removeFieldFromDocument(instanceString, "/oslc:modifiedBy");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['oslc:modifiedBy'])");
  }

  @Test
  public void shouldFailMissingFields() {
    // Arrange
    String instanceString = TestResourcesUtils.getStringContent("instances/multi-field-instance.jsonld");
    instanceString = JsonUtils.removeFieldFromDocument(instanceString, "/studyName");
    // Act
    JsonNode responseMessage = runValidation(instanceString);
    // Assert
    assertValidationStatus(responseMessage, "false");
    assertValidationMessage(responseMessage, "object has missing required properties (['studyName'])");
  }

  private JsonNode runValidation(String payload) {
    Response response = sendPostRequest(
        RequestUrls.forValidatingInstance(getPortNumber()),
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

  private String uploadTemplate(String templateDocument) {
    Response response = sendPostRequest(
        RequestUrls.forCreatingTemplate(getPortNumber(), "true"),
        templateDocument);
    checkStatusOk(response);
    return extractTemplateId(response);
  }

  private void removeTemplate(String templateId) {
    Response response = sendDeleteRequest(
        RequestUrls.forDeletingTemplate(getPortNumber(), templateId));
    checkStatusOk(response);
  }

  private static String extractTemplateId(final Response response) {
    String urlString = response.getLocation().toString();
    return urlString.substring(urlString.lastIndexOf("/") + 1);
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