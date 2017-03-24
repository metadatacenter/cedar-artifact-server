package org.metadatacenter.cedar.template.resources;

import org.junit.Test;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TemplateElementTest extends BaseTemplateResourceTest {

  @Test
  public void shouldPassEmptyTemplate() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/empty-element.json")
    );
  }

  @Test
  public void shouldPassSingleFieldTemplate() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/multi-field-element.json")
    );
  }

  @Test
  public void shouldReportMissingContext() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/missing-element-context.json")
    );
  }

  @Test
  public void shouldReportMissingId() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/missing-element-id.json")
    );
  }

  @Test
  public void shouldReportMissingType() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/missing-element-type.json")
    );
  }

  @Test
  public void shouldReportMissingJsonType() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/missing-element-json-type.json")
    );
  }

  @Test
  public void shouldReportMissingTitle() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/missing-element-title.json")
    );
  }

  @Test
  public void shouldReportMissingDescription() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/missing-element-description.json")
    );
  }

  @Test
  public void shouldReportMissingUi() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/missing-element-ui.json")
    );
  }

  @Test
  public void shouldReportMissingProperties() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/missing-element-properties.json")
    );
  }

  @Test
  public void shouldReportMissingRequired() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/missing-element-required.json")
    );
  }

  @Test
  public void shouldReportMissingProvenance() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/missing-element-provenance.json")
    );
  }

  @Test
  public void shouldReportMissingAdditionalProperties() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/missing-element-additional-properties.json")
    );
  }

  @Test
  public void shouldReportMissingSchema() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("elements/missing-element-schema.json")
    );
  }

  private void runTestAndAssert(TemplateResource testResource) {
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
