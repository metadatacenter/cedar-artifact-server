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
  public void shouldFailMissingContext() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-context.json")
    );
  }

  @Test
  public void shouldPassMissingId() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-id.json")
    );
  }

  @Test
  public void shouldFailMissingType() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-type.json")
    );
  }

  @Test
  public void shouldFailMissingJsonType() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-json-type.json")
    );
  }

  @Test
  public void shouldFailMissingTitle() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-title.json")
    );
  }

  @Test
  public void shouldFailMissingDescription() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-description.json")
    );
  }

  @Test
  public void shouldFailMissingUi() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-ui.json")
    );
  }

  @Test
  public void shouldFailMissingProperties() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-properties.json")
    );
  }

  @Test
  public void shouldFailMissingRequired() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-required.json")
    );
  }

  @Test
  public void shouldFailMissingValueConstraints() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-value-constraints.json")
    );
  }

  @Test
  public void shouldFailMissingProvenance() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-provenance.json")
    );
  }

  @Test
  public void shouldFailMissingAdditionalProperties() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-additional-properties.json")
    );
  }

  @Test
  public void shouldFailMissingSchema() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-schema.json")
    );
  }

  @Test
  public void shouldFailMissingPropertiesValueFieldForNonConstrainedValue() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-properties-value-ncv.json")
    );
  }

  @Test
  public void shouldFailMissingPropertiesIdForConstrainedValue() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/missing-field-properties-id-cv.json")
    );
  }

  @Test
  public void shouldFailInvalidFieldForNonConstrainedValue_UseId() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/invalid-field-properties-ncv-1.json")
    );
  }

  @Test
  public void shouldFailInvalidFieldForConstrainedValue_UseValue() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/invalid-field-properties-cv-1.json")
    );
  }

  @Test
  public void shouldFailInvalidFieldForNonConstrainedValue_UseIdAndValue() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/invalid-field-properties-ncv-2.json")
    );
  }

  @Test
  public void shouldFailInvalidFieldForConstrainedValue_UseIdAndValue() {
    runTestAndAssert(
        TestResourcesUtils.useResource("fields/invalid-field-properties-cv-2.json")
    );
  }

  private void runTestAndAssert(TestResource testResource) {
    String payload = testResource.getContent();
    Response response = sendPostRequest(
        RequestUrls.forValidatingField(getPortNumber()),
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
