package org.metadatacenter.cedar.template.resources;

import org.junit.Test;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TemplateResourceTest extends BaseTemplateResourceTest {

  @Test
  public void shouldPassEmptyTemplate() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/empty-template.json")
    );
  }

  @Test
  public void shouldPassSingleFieldTemplate() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/single-field-template.json")
    );
  }

  @Test
  public void shouldPassMultiFieldTemplate() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/multi-field-template.json")
    );
  }

  @Test
  public void shouldPassNestedElementTemplate() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/nested-element-template.json")
    );
  }

  @Test
  public void shouldReportMissingTemplateContext() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/missing-template-context.json")
    );
  }

  @Test
  public void shouldReportMissingTemplateId() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/missing-template-id.json")
    );
  }

  @Test
  public void shouldReportMissingTemplateType() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/missing-template-type.json")
    );
  }

  @Test
  public void shouldReportMissingTemplateJsonType() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/missing-template-json-type.json")
    );
  }

  @Test
  public void shouldReportMissingTemplateTitle() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/missing-template-title.json")
    );
  }

  @Test
  public void shouldReportMissingTemplateDescription() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/missing-template-description.json")
    );
  }

  @Test
  public void shouldReportMissingTemplateUi() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/missing-template-ui.json")
    );
  }

  @Test
  public void shouldReportMissingTemplateProperties() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/missing-template-properties.json")
    );
  }

  @Test
  public void shouldReportMissingTemplateRequired() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/missing-template-required.json")
    );
  }

  @Test
  public void shouldReportMissingTemplateProvenance() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/missing-template-provenance.json")
    );
  }

  @Test
  public void shouldReportMissingTemplateSchema() {
    runTestAndAssert(
        TestResourcesUtils.useTemplateResource("templates/missing-template-schema.json")
    );
  }

  private void runTestAndAssert(TemplateResource testResource) {
    String payload = testResource.getContent();
    Response response = sendPostRequest(
        RequestUrl.forValidatingTemplate(getPortNumber()),
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
