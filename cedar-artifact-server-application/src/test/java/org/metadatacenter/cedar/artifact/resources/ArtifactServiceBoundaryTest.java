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

  @Test
  void countsRequireBothCredentialsAndMonitorPermissionAndIncludeGraphlessDocuments() throws Exception {
    var config = TestUtil.getCedarConfig();
    String key = config.getArtifactService().requireApiKey();
    String admin = org.metadatacenter.util.test.TestAuthUtil.getAdminUserAuthHeader(config);
    String path = "/monitor/artifact-counts";
    assertEquals(401, send("GET", path, null, admin).statusCode());
    assertEquals(401, send("GET", path, key, null).statusCode());
    assertEquals(403, send("GET", path, key, authHeaderTestUser1).statusCode());
    var initial = send("GET", path, key, admin);
    assertEquals(200, initial.statusCode(), initial.body());
    var before = org.metadatacenter.util.json.JsonMapper.STRICT_MAPPER.readTree(initial.body());
    var mongo = config.getArtifactServerConfig();
    var database = org.metadatacenter.bridge.CedarDataServices.getInstance()
        .getMongoClientFactoryForDocuments().getClient().getDatabase(mongo.getDatabaseName());
    // Deliberately no graph nodes: diagnostics must count orphaned storage records too.
    var types = List.of(org.metadatacenter.model.CedarResourceType.FIELD,
        org.metadatacenter.model.CedarResourceType.ELEMENT, org.metadatacenter.model.CedarResourceType.TEMPLATE,
        org.metadatacenter.model.CedarResourceType.INSTANCE);
    String marker = java.util.UUID.randomUUID().toString();
    try {
      for (var type : types) {
        database.getCollection(mongo.getMongoCollectionName(type))
            .insertOne(new org.bson.Document("_id", marker));
      }
      var result = send("GET", path, key, admin);
      assertEquals(200, result.statusCode(), result.body());
      var counts = org.metadatacenter.util.json.JsonMapper.STRICT_MAPPER.readTree(result.body());
      for (String kind : List.of("field", "element", "template", "instance")) {
        assertEquals(before.path(kind).longValue() + 1, counts.path(kind).longValue(), kind);
      }
    } finally {
      for (var type : types) database.getCollection(mongo.getMongoCollectionName(type))
          .deleteOne(new org.bson.Document("_id", marker));
    }
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
