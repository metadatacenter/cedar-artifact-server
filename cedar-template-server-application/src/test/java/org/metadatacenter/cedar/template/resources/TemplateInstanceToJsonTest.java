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
import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.Response.Status.Family;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class TemplateInstanceToJsonTest extends BaseTemplateResourceTest {

  private String testInstanceId;

  private static TestResource testResource;

  @BeforeClass
  public static void loadTestPayload() {
    testResource = TestResourcesUtils.useResource(
        "instances/example-001.jsonld",
        "instances/example-001-json.out");
  }

  @Before
  public void addTestInstances() {
    String payload = testResource.getContent();
    Response response = sendPostRequest(
        RequestUrls.forCreatingInstances(getPortNumber(), "false"),
        payload);
    checkStatusOk(response);
    extractAndBroadcastTestInstanceId(response);
  }

  @After
  public void deleteTestInstances() {
    Response response = sendDeleteRequest(
        RequestUrls.forDeletingInstance(getPortNumber(), testInstanceId));
    checkStatusOk(response);
  }

  @Test
  public void shouldGetJsonOutput() {
    Response response = sendGetRequest(
        RequestUrls.forFindingInstance(getPortNumber(),
            testInstanceId,
            OutputFormatType.JSON.getValue()));
    checkStatusOk(response);
    // Assert header
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE), is(MediaType.APPLICATION_JSON));
    // Assert content
    String responseContent = response.readEntity(String.class);
    assertThat(responseContent, is(notNullValue()));
    for (String keyword : testResource.getExpected().split("\n")) {
      assertThat(responseContent, containsString(keyword));
    }
  }

  private void extractAndBroadcastTestInstanceId(final Response response) {
    URI resourceLocation = response.getLocation();
    String testInstanceId = extractId(resourceLocation);
    this.testInstanceId = testInstanceId;
  }

  private static String extractId(URI resourceLocation) {
    String asciiEncoded = resourceLocation.toASCIIString();
    return asciiEncoded.substring(asciiEncoded.lastIndexOf("/") + 1);
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
