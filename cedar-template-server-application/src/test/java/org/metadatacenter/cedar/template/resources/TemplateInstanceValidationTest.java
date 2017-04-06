package org.metadatacenter.cedar.template.resources;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TemplateInstanceValidationTest extends BaseTemplateResourceTest {

  private String templateExampleId1;
  private String templateExampleId2;
  private String templateExampleId3;

  private static String templateExample1;
  private static String templateExample2;
  private static String templateExample3;

  @BeforeClass
  public static void loadTemplateExamples() {
    templateExample1 = TestResourcesUtils.getStringContent("templates/single-field-template.json");
    templateExample2 = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    templateExample3 = TestResourcesUtils.getStringContent("templates/nested-element-template.json");
  }

  @Before
  public void uploadTemplates() {
    templateExampleId1 = uploadTemplate(templateExample1);
    templateExampleId2 = uploadTemplate(templateExample2);
    templateExampleId3 = uploadTemplate(templateExample3);
  }

  @After
  public void removeTemplates() {
    removeTemplate(templateExampleId1);
    removeTemplate(templateExampleId2);
    removeTemplate(templateExampleId3);
  }

  @Test
  public void shouldPassSingleFieldInstance() {
    runTestAndAssert(
        TestResourcesUtils.useResource("instances/single-field-instance.jsonld")
    );
  }

  @Test
  public void shouldPassMultiFieldInstance() {
    runTestAndAssert(
        TestResourcesUtils.useResource("instances/multi-field-instance.jsonld")
    );
  }

  @Test
  public void shouldPassNestedElementInstance() {
    runTestAndAssert(
        TestResourcesUtils.useResource("instances/nested-element-instance.jsonld")
    );
  }

  @Test
  public void shouldFailMissingContext() {
    runTestAndAssert(
        TestResourcesUtils.useResource("instances/missing-instance-context.jsonld")
    );
  }

  @Test
  public void shouldFailMissingId() {
    runTestAndAssert(
        TestResourcesUtils.useResource("instances/missing-instance-id.jsonld")
    );
  }

  @Test
  public void shouldFailMissingProvenance() {
    runTestAndAssert(
        TestResourcesUtils.useResource("instances/missing-instance-provenance.jsonld")
    );
  }

  @Test
  public void shouldFailMissingFields() {
    runTestAndAssert(
        TestResourcesUtils.useResource("instances/missing-instance-fields.jsonld")
    );
  }

  @Test
  public void shouldFailMissingValueInRequiredProperty() {
    runTestAndAssert(
        TestResourcesUtils.useResource("instances/missing-value-in-required-property.jsonld")
    );
  }

  private void runTestAndAssert(TestResource testResource) {
    String payload = testResource.getContent();
    Response response = sendPostRequest(
        RequestUrls.forValidatingInstance(getPortNumber()),
        payload);
    checkStatusOk(response);
    // Assert
    String responseMessage = response.readEntity(String.class);
    assertThat(responseMessage, is(testResource.getExpected()));
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

  private static String toPlainText(String s) {
    try {
      return URLDecoder.decode(s, StandardCharsets.UTF_8.displayName());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e.getMessage());
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