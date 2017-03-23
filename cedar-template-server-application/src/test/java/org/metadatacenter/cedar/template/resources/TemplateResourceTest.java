package org.metadatacenter.cedar.template.resources;

import org.junit.Test;

import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkNotNull;

public class TemplateResourceTest extends BaseTemplateResourceTest {

  @Test
  public void shouldValidateEmptyTemplate() {
    String payload = TestResourcesUtils.getStringContent("templates/empty-template.json");
    Response response = sendPostRequest(
        RequestUrl.forValidatingTemplate(getPortNumber()),
        payload);
    checkStatusOk(response);
  }

  @Test
  public void shouldValidateSingleFieldTemplate() {
    String payload = TestResourcesUtils.getStringContent("templates/single-field-template.json");
    Response response = sendPostRequest(
        RequestUrl.forValidatingTemplate(getPortNumber()),
        payload);
    checkStatusOk(response);
  }

  @Test
  public void shouldValidateMultiFieldTemplate() {
    String payload = TestResourcesUtils.getStringContent("templates/multi-field-template.json");
    Response response = sendPostRequest(
        RequestUrl.forValidatingTemplate(getPortNumber()),
        payload);
    checkStatusOk(response);
  }

  @Test
  public void shouldValidateNestedElementTemplate() {
    String payload = TestResourcesUtils.getStringContent("templates/nested-element-template.json");
    Response response = sendPostRequest(
        RequestUrl.forValidatingTemplate(getPortNumber()),
        payload);
    checkStatusOk(response);
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
