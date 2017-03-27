package org.metadatacenter.cedar.template.resources;

import org.junit.Test;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TemplateFieldValidationTest extends BaseTemplateResourceTest {

  @Test
  public void shouldPassBasicTextField() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/basic-text-field.json")
    );
  }

  @Test
  public void shouldPassValueConstrainedField() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/value-constrained-field.json")
    );
  }

  @Test
  public void shouldReportMissingContext() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-context.json")
    );
  }

  @Test
  public void shouldReportMissingId() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-id.json")
    );
  }

  @Test
  public void shouldReportMissingType() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-type.json")
    );
  }

  @Test
  public void shouldReportMissingJsonType() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-json-type.json")
    );
  }

  @Test
  public void shouldReportMissingTitle() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-title.json")
    );
  }

  @Test
  public void shouldReportMissingDescription() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-description.json")
    );
  }

  @Test
  public void shouldReportMissingUi() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-ui.json")
    );
  }

  @Test
  public void shouldReportMissingProperties() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-properties.json")
    );
  }

  @Test
  public void shouldReportMissingRequired() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-required.json")
    );
  }

  @Test
  public void shouldReportMissingValueConstraints() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-value-constraints.json")
    );
  }

  @Test
  public void shouldReportMissingProvenance() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-provenance.json")
    );
  }

  @Test
  public void shouldReportMissingAdditionalProperties() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-additional-properties.json")
    );
  }

  @Test
  public void shouldReportMissingSchema() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-schema.json")
    );
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
