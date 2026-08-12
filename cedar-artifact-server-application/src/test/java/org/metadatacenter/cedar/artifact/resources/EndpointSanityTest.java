package org.metadatacenter.cedar.artifact.resources;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
