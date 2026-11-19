package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

/** Proves the deployed exception boundary against the real MongoDB driver, not a mocked DAO. */
@Tag("integration")
class ArtifactMongoOutageTest {

  static {
    Map<String, String> environment = new HashMap<>(CedarEnvironmentSource.getAll());
    environment.put("CEDAR_ARTIFACT_HTTP_PORT", "0");
    environment.put("CEDAR_ARTIFACT_ADMIN_PORT", "0");
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
        "CEDAR_REDIS_PERSISTENT_PORT", "1"));
  }

  @Test
  void artifactReadReturnsSanitizedServiceUnavailable() throws Exception {
    String id = URLEncoder.encode("https://example.org/templates/mongo-outage", StandardCharsets.UTF_8);
    HttpResponse<String> response = send("GET", "/templates/" + id, null, null, null);
    assertSanitizedServiceUnavailable(response);
  }

  /** The route of each kind, and a body a create of that kind accepts. */
  private static Stream<Arguments> writes() {
    Map<String, String> routes = Map.of(
        "template", "/templates",
        "element", "/template-elements",
        "text-field", "/template-fields",
        "instance", "/template-instances");
    Map<String, String> bodies = Map.of(
        "template", "type: template\nname: Outage Template\n",
        "element", "type: element\nname: Outage Element\n",
        "text-field", "type: text-field\nname: Outage Field\n",
        "instance", "type: instance\nname: Outage Instance\nisBasedOn: https://example.org/templates/mongo-outage\n");
    return routes.keySet().stream().sorted().flatMap(kind -> {
      String route = routes.get(kind);
      String id = URLEncoder.encode("https://example.org" + route + "/mongo-outage", StandardCharsets.UTF_8);
      return Stream.of(
          Arguments.of(kind + " POST", "POST", route, bodies.get(kind), null),
          Arguments.of(kind + " conditional PUT", "PUT", route + "/" + id, bodies.get(kind), "\"1\""),
          Arguments.of(kind + " conditional DELETE", "DELETE", route + "/" + id, null, "\"1\""));
    });
  }

  /**
   * The write handlers catch IOException, which the real driver never throws for an outage. What a
   * caller sees then rests on the shared exception mapper alone, and a catch broadened to Exception
   * around a write would turn the 503 a caller may retry into a 500.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("writes")
  void artifactWriteReturnsSanitizedServiceUnavailable(String name, String method, String path, String body,
                                                       String ifMatch) throws Exception {
    HttpResponse<String> response = send(method, path, body, body == null ? null : "application/yaml", ifMatch);
    assertSanitizedServiceUnavailable(response);
  }

  private static HttpResponse<String> send(String method, String path, String body, String contentType,
                                           String ifMatch) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + path))
        .timeout(Duration.ofSeconds(5))
        .header("Authorization", authorization)
        .header(org.metadatacenter.config.ArtifactServiceConfig.HEADER,
            CedarConfig.getInstance(CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_ARTIFACT))
                .getArtifactService().requireApiKey())
        .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
    if (contentType != null) {
      request.header("Content-Type", contentType);
    }
    if (ifMatch != null) {
      request.header("If-Match", ifMatch);
    }
    return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }

  private static void assertSanitizedServiceUnavailable(HttpResponse<String> response) throws Exception {
    Assertions.assertEquals(503, response.statusCode(), response.body());
    JsonNode error = JsonMapper.STRICT_MAPPER.readTree(response.body());
    Assertions.assertEquals("SERVICE_UNAVAILABLE", error.path("status").asText(), response.body());
    Assertions.assertEquals("MongoDB is unavailable", error.path("message").asText(), response.body());
    Assertions.assertTrue(error.path("originalException").isMissingNode()
        || error.path("originalException").isNull(), response.body());
    Assertions.assertTrue(error.path("sourceException").isMissingNode()
        || error.path("sourceException").isNull(), response.body());
    Assertions.assertFalse(response.body().contains("127.0.0.1"), response.body());
  }
}
