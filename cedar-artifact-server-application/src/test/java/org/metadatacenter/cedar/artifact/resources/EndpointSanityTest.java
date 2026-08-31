package org.metadatacenter.cedar.artifact.resources;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.metadatacenter.constant.HttpConstants.HTTP_HEADER_AUTHORIZATION;

public class EndpointSanityTest extends BaseServerTest {

  @Test
  public void shouldRejectValidationWithoutResourceTypeAsBadRequest() {
    Response response = testClient.target("http://localhost:" + getPortNumber() + "/command/validate")
        .request()
        .header(HTTP_HEADER_AUTHORIZATION, authHeaderValue)
        .post(jakarta.ws.rs.client.Entity.json("{}"));

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  /**
   * The artifact server ships an API spec, so it advertises the documentation links and serves the
   * document.
   *
   * <p>It shipped none until its resource classes were annotated, and the shared library gates both
   * the asset bundle and the index links on whether the document is on the classpath. This holds the
   * server on the side of that gate its annotations put it on: losing the generated spec would
   * quietly take the documentation with it.
   */
  @Test
  public void shouldAdvertiseAndServeItsApiSpec() {
    String index = testClient.target("http://localhost:" + getPortNumber() + "/")
        .request().get(String.class);
    assertTrue(index.contains("apiDocs"), "The artifact server should advertise its documentation: " + index);

    Response spec = testClient.target("http://localhost:" + getPortNumber() + "/swagger-api/swagger.json")
        .request().get();
    assertEquals(Response.Status.OK.getStatusCode(), spec.getStatus());
    assertTrue(spec.readEntity(String.class).contains("\"openapi\""),
        "The advertised spec path should serve an OpenAPI document");
  }

  @Test
  public void shouldRejectOffsetBeyondCollectionAsBadRequest() {
    Response response = testClient.target("http://localhost:" + getPortNumber() + "/templates")
        .queryParam("offset", Integer.MAX_VALUE)
        .request()
        .header(HttpHeaders.AUTHORIZATION, authHeaderValue)
        .get();

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
}
