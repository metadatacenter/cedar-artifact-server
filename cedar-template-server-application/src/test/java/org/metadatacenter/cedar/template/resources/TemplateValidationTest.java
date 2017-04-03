package org.metadatacenter.cedar.template.resources;

import org.junit.Test;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;

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
  public void shouldReportMissingContext() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-context.json")
    );
  }

  @Test
  public void shouldReportMissingId() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-id.json")
    );
  }

  @Test
  public void shouldReportMissingType() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-type.json")
    );
  }

  @Test
  public void shouldReportMissingJsonType() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-json-type.json")
    );
  }

  @Test
  public void shouldReportMissingTitle() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-title.json")
    );
  }

  @Test
  public void shouldReportMissingDescription() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-description.json")
    );
  }

  @Test
  public void shouldReportMissingUi() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-ui.json")
    );
  }

  @Test
  public void shouldReportMissingProperties() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-properties.json")
    );
  }

  @Test
  public void shouldReportMissingRequired() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-required.json")
    );
  }

  @Test
  public void shouldReportMissingProvenance() {
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
  public void shouldReportMissingSchema() {
    runTestAndAssert(
        TestResourcesUtils.useResource("templates/missing-template-schema.json")
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
