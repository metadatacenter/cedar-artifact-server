package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.artifact.ArtifactServerApplication;
import org.metadatacenter.cedar.artifact.ArtifactServerConfiguration;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.EmbeddedCedarMongo;
import org.metadatacenter.util.test.TestAuthUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Proves the deployed exception boundary against the real MongoDB driver, not a mocked DAO. */
class ArtifactMongoOutageTest {

  static {
    Map<String, String> environment = new HashMap<>(CedarEnvironmentSource.getAll());
    environment.put("CEDAR_ARTIFACT_HTTP_PORT", "0");
    environment.put("CEDAR_ARTIFACT_ADMIN_PORT", "0");
    environment.put("CEDAR_ARTIFACT_STOP_PORT", "0");
    environment.put("CEDAR_MONGO_HOST", "127.0.0.1");
    environment.put("CEDAR_MONGO_PORT", "1");
    environment.put("CEDAR_NEO4J_HOST", "127.0.0.1");
    environment.put("CEDAR_NEO4J_BOLT_PORT", "1");
    environment.put("CEDAR_REDIS_PERSISTENT_PORT", "1");
    CedarEnvironmentSource.setOverride(environment);
  }

  private static final DropwizardTestSupport<ArtifactServerConfiguration> SERVER =
      new DropwizardTestSupport<>(ArtifactServerApplication.class,
          ResourceHelpers.resourceFilePath("test-config.yml"));

  private static final HttpClient CLIENT = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();

  private static String authorization;

  @BeforeAll
  static void startServer() throws Exception {
    SERVER.before();
    CedarConfig cedarConfig = CedarConfig.getInstance(
        CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_ARTIFACT));
    TestAuthUtil.installInMemoryUserService(cedarConfig);
    authorization = TestAuthUtil.getTestUser1AuthHeader(cedarConfig);
  }

  @AfterAll
  static void stopServer() {
    SERVER.after();
    // This suite reuses one JVM. Restore the ordinary embedded-store environment so a class which
    // boots after this one cannot inherit the deliberately dead client configuration.
    EmbeddedCedarMongo.startAndRedirectEnvironment(Map.of(
        "CEDAR_ARTIFACT_HTTP_PORT", "0",
        "CEDAR_ARTIFACT_ADMIN_PORT", "0",
        "CEDAR_ARTIFACT_STOP_PORT", "0",
        "CEDAR_REDIS_PERSISTENT_PORT", "1"));
  }

  @Test
  void artifactReadReturnsSanitizedServiceUnavailable() throws Exception {
    String id = URLEncoder.encode("https://example.org/templates/mongo-outage", StandardCharsets.UTF_8);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + "/templates/" + id))
        .timeout(Duration.ofSeconds(5))
        .header("Authorization", authorization)
        .GET()
        .build();

    HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

    Assertions.assertEquals(503, response.statusCode(), response.body());
    JsonNode error = JsonMapper.MAPPER.readTree(response.body());
    Assertions.assertEquals("SERVICE_UNAVAILABLE", error.path("status").asText(), response.body());
    Assertions.assertEquals("MongoDB is unavailable", error.path("message").asText(), response.body());
    Assertions.assertTrue(error.path("originalException").isMissingNode()
        || error.path("originalException").isNull(), response.body());
    Assertions.assertTrue(error.path("sourceException").isMissingNode()
        || error.path("sourceException").isNull(), response.body());
    Assertions.assertFalse(response.body().contains("127.0.0.1"), response.body());
  }
}
