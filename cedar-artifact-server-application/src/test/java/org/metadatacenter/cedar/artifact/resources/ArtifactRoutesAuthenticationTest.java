package org.metadatacenter.cedar.artifact.resources;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.config.ArtifactServiceConfig;
import org.metadatacenter.util.test.RouteSurface;
import org.metadatacenter.util.test.TestAuthUtil;
import org.metadatacenter.util.test.TestHttpClient;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Probes both authentication layers independently on every registered business endpoint. */
class ArtifactRoutesAuthenticationTest extends BaseServerTest {

  private static final String INVALID_SERVICE_KEY = "route-inventory-invalid-service-key";
  private static final String INVALID_USER = "apiKey 00000000-0000-0000-0000-000000000000";

  private record Credentials(String name, String serviceKey, String user) { }

  private List<RouteSurface.Endpoint> businessEndpoints() {
    var config = SERVER_APPLICATION.getEnvironment().jersey().getResourceConfig();
    List<Object> components = new ArrayList<>();
    components.addAll(config.getInstances());
    components.addAll(config.getSingletons());
    components.addAll(config.getClasses());
    components.addAll(config.getResources());

    // Shared index, health and diagnostic resources are outside this business-resource package.
    // No business endpoint is exempt from either authentication layer.
    List<Class<?>> resources = RouteSurface.registeredResourceClasses(components,
        "org.metadatacenter.cedar.artifact.resources");
    assertTrue(resources.containsAll(List.of(TemplateFieldsResource.class, TemplateElementsResource.class,
        TemplatesResource.class, TemplateInstancesResource.class, CommandResource.class, ArtifactCountsResource.class)),
        "An existing business resource disappeared from Jersey registration: " + resources);
    List<RouteSurface.Endpoint> endpoints = RouteSurface.endpoints(resources);
    assertFalse(endpoints.isEmpty(), "The authentication inventory must not pass without probing routes");
    assertEquals(endpoints.size(), endpoints.stream().map(RouteSurface.Endpoint::key).distinct().count(),
        "Business endpoints must have unique method/path identities");
    return endpoints;
  }

  @TestFactory
  Stream<DynamicTest> everyBusinessRouteRequiresBothCredentials() {
    ArtifactServiceConfig service = TestUtil.getCedarConfig().getArtifactService();
    String validServiceKey = service.requireApiKey();
    assertNotEquals(INVALID_SERVICE_KEY, validServiceKey);
    assertNotEquals(INVALID_SERVICE_KEY, service.getPreviousApiKey());
    List<Credentials> cases = List.of(
        new Credentials("missing service key, valid user", null, authHeaderValue),
        new Credentials("invalid service key, valid user", INVALID_SERVICE_KEY, authHeaderValue),
        new Credentials("valid service key, missing user", validServiceKey, null),
        new Credentials("valid service key, invalid user", validServiceKey, INVALID_USER));
    return businessEndpoints().stream().flatMap(endpoint -> cases.stream().map(credentials ->
        DynamicTest.dynamicTest(endpoint.key() + " / " + credentials.name(), () ->
            assertEquals(401, probe(endpoint, credentials), endpoint.key() + " / " + credentials.name()))));
  }

  private int probe(RouteSurface.Endpoint endpoint, Credentials credentials) throws Exception {
    HttpRequest.Builder request = request(RouteSurface.resolvedPath(endpoint), credentials)
        .header("Content-Type", RouteSurface.contentTypeFor(endpoint));
    boolean hasBody = List.of("POST", "PUT", "PATCH").contains(endpoint.verb);
    // Jersey binds this List<String> before entering the endpoint; an object would answer 400
    // without reaching the user check. This is a valid-input override, not an auth exemption.
    String body = endpoint.key().equals("POST /templates/deletion-references") ? "[]" : "{}";
    request.method(endpoint.verb, hasBody ? HttpRequest.BodyPublishers.ofString(body)
        : HttpRequest.BodyPublishers.noBody());
    return TestHttpClient.send(request.build()).statusCode();
  }

  private HttpRequest.Builder request(String path, Credentials credentials) {
    // Do not use BaseServerTest.testClient: its filter silently supplies the service key.
    HttpRequest.Builder request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + getPortNumber() + path))
        .timeout(Duration.ofSeconds(5));
    if (credentials.serviceKey() != null) {
      request.header(ArtifactServiceConfig.HEADER, credentials.serviceKey());
    }
    if (credentials.user() != null) {
      request.header("Authorization", credentials.user());
    }
    return request;
  }

  @Test
  void validCredentialsReachTheUserPermissionCheckAndTheResource() throws Exception {
    var config = TestUtil.getCedarConfig();
    String key = config.getArtifactService().requireApiKey();
    String path = "/monitor/artifact-counts";
    assertEquals(403, TestHttpClient.send(request(path,
        new Credentials("normal user", key, authHeaderValue)).GET().build()).statusCode());
    var admin = new Credentials("administrator", key, TestAuthUtil.getAdminUserAuthHeader(config));
    assertEquals(200, TestHttpClient.send(request(path, admin).GET().build()).statusCode());
  }
}
