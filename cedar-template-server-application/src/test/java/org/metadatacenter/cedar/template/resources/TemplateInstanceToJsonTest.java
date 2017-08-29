package org.metadatacenter.cedar.template.resources;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.metadatacenter.model.request.OutputFormatType;

import javax.annotation.Nonnull;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.Response.Status.Family;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TemplateInstanceToJsonTest extends BaseServerTest {

  private String templateExampleId;
  private String instanceExampleId;

  private static String templateExample;
  private static String instanceExample;

  @BeforeClass
  public static void loadTestPayload() {
    templateExample = TestResourcesUtils.getStringContent("instances/usecase-template.json");
    instanceExample = TestResourcesUtils.getStringContent("instances/usecase-instance.jsonld");
  }

  @Before
  public void uploadResources() {
    templateExampleId = uploadTemplate(templateExample);
    instanceExampleId = uploadInstance(instanceExample);
  }

  @After
  public void removeResources() {
    removeInstance(instanceExampleId);
    removeTemplate(templateExampleId);
  }

  @Test
  public void shouldGetJsonOutput() {
    Response response = sendGetRequest(
        TestRequestUrls.forFindingInstance(getPortNumber(), instanceExampleId,
            OutputFormatType.JSON.getValue()));
    checkStatusOk(response);
    // Assert header
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE), is(MediaType.APPLICATION_JSON));
    // Assert content
    String responseContent = response.readEntity(String.class);
    System.out.println(responseContent);
  }

  private String uploadTemplate(String templateDocument) {
    Response response = sendPostRequest(
        TestRequestUrls.forCreatingTemplate(getPortNumber()),
        templateDocument);
    checkStatusOk(response);
    return extractId(response);
  }

  private String uploadInstance(String instanceDocument) {
    Response response = sendPostRequest(
        TestRequestUrls.forCreatingInstances(getPortNumber()),
        instanceDocument);
    checkStatusOk(response);
    return extractId(response);
  }

  private void removeTemplate(String templateId) {
    Response response = sendDeleteRequest(
        TestRequestUrls.forDeletingTemplate(getPortNumber(), templateId));
    checkStatusOk(response);
  }

  private void removeInstance(String instanceId) {
    Response response = sendDeleteRequest(
        TestRequestUrls.forDeletingInstance(getPortNumber(), instanceId));
    checkStatusOk(response);
  }

  private static String extractId(final Response response) {
    String urlString = response.getLocation().toString();
    return urlString.substring(urlString.lastIndexOf("/") + 1);
  }

  private static void checkStatusOk(@Nonnull Response response) {
    checkNotNull(response);
    int responseCode = response.getStatus();
    if (Family.familyOf(responseCode) == Family.CLIENT_ERROR) {
      throw new RuntimeException("Request contains bad syntax or cannot be fulfilled:\n" + response.toString());
    } else if (Family.familyOf(responseCode) == Family.SERVER_ERROR) {
      throw new RuntimeException("Server failed to fulfill the request:\n" + response.toString());
    }
  }
}
