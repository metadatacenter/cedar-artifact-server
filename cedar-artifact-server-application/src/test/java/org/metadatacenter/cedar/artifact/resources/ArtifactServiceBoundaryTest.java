package org.metadatacenter.cedar.artifact.resources;

import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.artifact.resources.rest.AbstractRestTest;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.config.ArtifactServiceConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Uses a raw client deliberately: the legacy CRUD harness supplies internal authentication. */
class ArtifactServiceBoundaryTest extends AbstractRestTest {
  private static final HttpClient RAW = HttpClient.newHttpClient();

  @Test
  void ordinaryUserKeysCannotReadListOrMutateAnyArtifactFamily() throws Exception {
    for (String family : List.of("templates", "template-elements", "template-fields", "template-instances")) {
      for (String method : List.of("GET", "POST", "PUT", "DELETE")) {
        for (String path : List.of("/" + family, "/" + family + "/12345678-1234-1234-1234-123456789abc")) {
          // Only methods actually defined by the route are authenticated; unmatched methods are 405.
          HttpResponse<String> response = send(method, path, null, authHeaderTestUser1);
          assertTrue(response.statusCode() == 401 || response.statusCode() == 405,
              method + " " + path + ": " + response.statusCode());
        }
      }
    }
    assertEquals(401, send("POST", "/command/validate?resource_type=instance", null, authHeaderTestUser1).statusCode());
  }

  @Test
  void requiresUserAuthenticationEvenWithTheServiceKey() throws Exception {
    String key = TestUtil.getCedarConfig().getArtifactService().requireApiKey();
    assertEquals(401, send("GET", "/templates", key, null).statusCode());
    assertEquals(401, send("GET", "/templates", "wrong-key", authHeaderTestUser1).statusCode());
    assertEquals(200, send("GET", "/templates", key, authHeaderTestUser1).statusCode());
  }

  @Test
  void indexRemainsAvailableForReadiness() throws Exception {
    assertEquals(200, send("GET", "/", null, null).statusCode());
  }

  private HttpResponse<String> send(String method, String path, String serviceKey, String authorization) throws Exception {
    var request = HttpRequest.newBuilder(URI.create(baseTestUrl + path)).timeout(Duration.ofSeconds(10))
        .header("Content-Type", "application/json");
    if (serviceKey != null) request.header(ArtifactServiceConfig.HEADER, serviceKey);
    if (authorization != null) request.header("Authorization", authorization);
    request.method(method, method.equals("POST") || method.equals("PUT")
        ? HttpRequest.BodyPublishers.ofString("{}") : HttpRequest.BodyPublishers.noBody());
    return RAW.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }
}
