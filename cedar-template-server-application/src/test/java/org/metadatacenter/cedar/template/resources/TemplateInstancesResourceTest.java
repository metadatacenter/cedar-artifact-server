package org.metadatacenter.cedar.template.resources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.metadatacenter.model.request.OutputFormatType;

import javax.annotation.Nonnull;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.Response.Status.Family;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TemplateInstancesResourceTest extends BaseTemplateResourceTest {

  private String testInstanceId;

  @Before
  public void addTestInstances() {
    Response response = sendPostRequest(
        RequestUrl.forCreatingInstances(getPortNumber(), "false"),
        Payloads.useExampleInstance001());
    checkStatusOk(response);
    extractAndBroadcastTestInstanceId(response);
  }

  @After
  public void deleteTestInstances() {
    Response response = sendDeleteRequest(
        RequestUrl.forDeletingInstance(getPortNumber(), testInstanceId));
    checkStatusOk(response);
  }

  @Test
  public void shouldGetTemplateInstanceInJsonLd() {
    Response response = sendGetRequest(
        RequestUrl.forFindingInstance(getPortNumber(),
            testInstanceId,
            OutputFormatType.JSONLD.getValue()));
    checkStatusOk(response);
    // Asserts
    System.out.println(response.getHeaders());
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE), is(MediaType.APPLICATION_JSON));
    assertThat(response.readEntity(String.class), is(notNullValue()));
  }

  @Test
  public void shouldGetTemplateInstanceInJson() {
    Response response = sendGetRequest(
        RequestUrl.forFindingInstance(getPortNumber(),
            testInstanceId,
            OutputFormatType.JSON.getValue()));
    checkStatusOk(response);
    // Asserts
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE), is(MediaType.APPLICATION_JSON));
    assertThat(response.readEntity(String.class), is(notNullValue()));
  }

  @Test
  public void shouldGetTemplateInstanceInRdf() {
    Response response = sendGetRequest(
        RequestUrl.forFindingInstance(getPortNumber(),
            testInstanceId,
            OutputFormatType.RDF_NQUAD.getValue()));
    checkStatusOk(response);
    // Asserts
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE), is("application/n-quads"));
    assertThat(response.readEntity(String.class), is(notNullValue()));
  }

  @Test
  public void shouldThrowUnknownFormatError() {
    Response response = sendGetRequest(
        RequestUrl.forFindingInstance(getPortNumber(),
            testInstanceId,
            "xml"));
    // Asserts
    assertThat(response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
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
