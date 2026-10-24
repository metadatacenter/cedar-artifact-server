package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoTimeoutException;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.metadatacenter.artifacts.model.core.TemplateSchemaArtifact;
import org.metadatacenter.artifacts.model.core.TextField;
import org.metadatacenter.artifacts.model.renderer.JsonArtifactRenderer;
import org.metadatacenter.cedar.artifact.ArtifactServerApplication;
import org.metadatacenter.cedar.artifact.ArtifactServerConfiguration;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.mongo.MongoUtils;
import org.metadatacenter.util.test.EmbeddedCedarMongo;
import org.metadatacenter.util.test.TestAuthUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Named.named;

/**
 * When the store fails under a write, or another writer commits between the request's read and its
 * write, the answer must say what happened and storage must agree with it.
 *
 * <p>The tests use real HTTP and embedded Mongo. The template and element services are decorated at the
 * boundary the resources call. An interruption either fails one call with the exception the store would
 * raise, or lets another writer act through an undecorated service at that moment. No decorator alters
 * a result. Every interruption is spent by the first call it matches, so it must be
 * armed after the GET that supplies the request's ETag.
 */
@Tag("integration")
public class ArtifactWriteFaultTest {
  static {
    EmbeddedCedarMongo.startAndRedirectEnvironment(Map.of(
        "CEDAR_ARTIFACT_HTTP_PORT", "0", "CEDAR_ARTIFACT_ADMIN_PORT", "0",
        "CEDAR_ARTIFACT_STOP_PORT", "0", "CEDAR_REDIS_PERSISTENT_PORT", "1",
        "CEDAR_TERMINOLOGY_SERVER_HOST", "127.0.0.1", "CEDAR_TERMINOLOGY_HTTP_PORT", "1",
        "CEDAR_BRIDGE_SERVER_HOST", "127.0.0.1", "CEDAR_BRIDGE_HTTP_PORT", "1"));
  }

  private static final DropwizardTestSupport<ArtifactServerConfiguration> SERVER =
      new DropwizardTestSupport<>(FaultApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));
  private static final String APPLICATION_YAML = "application/yaml";
  private static final Map<String, Interruption> BEFORE = new ConcurrentHashMap<>();
  private static final Map<String, Interruption> AFTER = new ConcurrentHashMap<>();
  private static Client testClient;
  private static String userAuthorization;
  private static String adminAuthorization;

  @BeforeAll
  static void start() throws Exception {
    SERVER.before();
    var config = TestUtil.getCedarConfig();
    TestAuthUtil.installInMemoryUserService(config);
    userAuthorization = TestAuthUtil.getTestUser1AuthHeader(config);
    adminAuthorization = TestAuthUtil.getAdminUserAuthHeader(config);
    testClient = ClientBuilder.newClient();
    testClient.register((jakarta.ws.rs.client.ClientRequestFilter) request ->
        request.getHeaders().putSingle(org.metadatacenter.config.ArtifactServiceConfig.HEADER,
            config.getArtifactService().requireApiKey()));
  }

  @AfterAll
  static void stop() {
    if (testClient != null) testClient.close();
    SERVER.after();
  }

  @AfterEach
  void disarm() {
    BEFORE.clear();
    AFTER.clear();
  }

  /** One store call's worth of trouble: a failure, or another writer's commit. */
  @FunctionalInterface
  private interface Interruption {
    void run() throws Exception;
  }

  private static void before(String method, Interruption interruption) {
    BEFORE.put(method, interruption);
  }

  private static void after(String method, Interruption interruption) {
    AFTER.put(method, interruption);
  }

  private static Interruption storeFailure() {
    return () -> {
      throw new IOException("the store failed");
    };
  }

  public static class FaultApplication extends ArtifactServerApplication {
    @Override
    public void initializeApp() {
      super.initializeApp();
      templateService = interrupted(TemplateService.class, templateService);
      templateElementService = interrupted(TemplateElementService.class, templateElementService);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T interrupted(Class<T> service, T original) {
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service},
        (proxy, method, arguments) -> {
          Interruption before = BEFORE.remove(method.getName());
          if (before != null) before.run();
          Object result;
          try {
            result = method.invoke(original, arguments);
          } catch (InvocationTargetException error) {
            throw error.getCause();
          }
          Interruption after = AFTER.remove(method.getName());
          if (after != null) after.run();
          return result;
        });
  }

  private record Reply(int status, JsonNode body, String etag) {
    String errorKey() {
      return body.path("errorKey").asText();
    }

    JsonNode currentETag() {
      return body.path("parameters").path("currentETag");
    }
  }

  private record Stored(String kind, String id, String url, String etag, ObjectNode body) {}

  private static String base() {
    return "http://localhost:" + SERVER.getLocalPort();
  }

  private static Reply reply(Response response) throws IOException {
    try (response) {
      String entity = response.hasEntity() ? response.readEntity(String.class) : "";
      JsonNode body = entity.isBlank() ? JsonMapper.STRICT_MAPPER.createObjectNode()
          : JsonMapper.STRICT_MAPPER.readTree(entity);
      return new Reply(response.getStatus(), body, response.getHeaderString("ETag"));
    }
  }

  private static Invocation.Builder request(String url, String authorization, String ifMatch) {
    Invocation.Builder request = testClient.target(url).request("application/json")
        .header("Authorization", authorization);
    return ifMatch == null ? request : request.header("If-Match", ifMatch);
  }

  private static Stored create(String kind, String name) throws IOException {
    String yaml = "type: " + (kind.equals("templates") ? "template" : "element") + "\nname: " + name + "\n";
    Reply created = reply(request(base() + "/" + kind, userAuthorization, null)
        .post(Entity.entity(yaml, APPLICATION_YAML)));
    assertEquals(201, created.status(), created.body().toString());
    String id = created.body().path("@id").asText();
    return new Stored(kind, id, base() + "/" + kind + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8),
        created.etag(), (ObjectNode) created.body());
  }

  private static Reply get(Stored artifact) throws IOException {
    return reply(request(artifact.url(), userAuthorization, null).get());
  }

  private static Reply delete(Stored artifact) throws IOException {
    return reply(request(artifact.url(), userAuthorization, artifact.etag()).delete());
  }

  private static Reply put(Stored artifact, String suffix, JsonNode body) throws IOException {
    return reply(request(artifact.url() + suffix, userAuthorization, artifact.etag()).put(Entity.json(body)));
  }

  private static ObjectNode renamed(Stored artifact, String name) {
    ObjectNode body = artifact.body().deepCopy();
    body.put("schema:name", name);
    return body;
  }

  /** Reads the artifact as storage holds it, past the decorated services, or null when there is none. */
  private static JsonNode stored(Stored artifact) throws IOException {
    JsonNode document = artifact.kind().equals("templates")
        ? TestUtil.templateService.findTemplate(artifact.id())
        : TestUtil.templateElementService.findTemplateElement(artifact.id());
    if (document != null) {
      MongoUtils.removeIdField(document);
    }
    return document;
  }

  /** Another writer replaces the artifact, as a client with the current ETag would. */
  private static Interruption anotherWriterRenames(Stored artifact, String name) {
    return () -> {
      ObjectNode body = renamed(artifact, name);
      if (artifact.kind().equals("templates")) {
        TestUtil.templateService.updateTemplate(artifact.id(), body,
            TestUtil.templateService.getTemplateRevision(artifact.id()));
      } else {
        TestUtil.templateElementService.updateTemplateElement(artifact.id(), body,
            TestUtil.templateElementService.getTemplateElementRevision(artifact.id()));
      }
    };
  }

  private static Interruption anotherWriterDeletes(Stored artifact) {
    return () -> {
      if (artifact.kind().equals("templates")) {
        TestUtil.templateService.deleteTemplate(artifact.id());
      } else {
        TestUtil.templateElementService.deleteTemplateElement(artifact.id());
      }
    };
  }

  private static void assertMovedOnTo(Reply reply, Reply current) {
    assertEquals(412, reply.status(), reply.body().toString());
    assertEquals(CedarErrorKey.ARTIFACT_HAS_MOVED_ON.getValue(), reply.errorKey(), reply.body().toString());
    assertTrue(reply.body().path("errorReasonKey").isMissingNode() || reply.body().path("errorReasonKey").isNull(),
        "a race is not a refusal for references: " + reply.body());
    assertEquals(current.etag(), reply.currentETag().asText(null),
        "a refused write names the revision that is current, so a client can read it and decide again");
  }

  private static void assertNoLongerExists(Reply reply) {
    assertEquals(412, reply.status(), reply.body().toString());
    assertEquals(CedarErrorKey.ARTIFACT_HAS_MOVED_ON.getValue(), reply.errorKey(), reply.body().toString());
    assertTrue(reply.body().path("message").asText().endsWith("no longer exists"), reply.body().toString());
    assertTrue(reply.currentETag().isMissingNode() || reply.currentETag().isNull(),
        "no revision exists for the answer to name: " + reply.body());
  }

  private static void assertServerError(Reply reply, CedarErrorKey errorKey, Stored artifact) {
    assertEquals(500, reply.status(), reply.body().toString());
    assertEquals(errorKey.getValue(), reply.errorKey(), reply.body().toString());
    if (artifact != null) {
      assertEquals(artifact.id(), reply.body().path("parameters").path("id").asText(), reply.body().toString());
    }
    assertFalse(reply.body().toString().contains("the store failed"),
        "the failure is logged under the error id, not returned: " + reply.body());
  }

  // Deletion

  @Test
  void aStoreFailureReadingBeforeADeletionAnswers500AndDeletesNothing() throws Exception {
    Stored element = create("template-elements", "Unreadable before deletion");
    before("findTemplateElementWithRevision", storeFailure());

    assertServerError(delete(element), CedarErrorKey.TEMPLATE_ELEMENT_NOT_DELETED, element);
    assertNotNull(stored(element));
  }

  @Test
  void aStoreFailureWhileDeletingAnswers500() throws Exception {
    Stored element = create("template-elements", "Undeletable");
    before("deleteTemplateElement", storeFailure());

    assertServerError(delete(element), CedarErrorKey.TEMPLATE_ELEMENT_NOT_DELETED, element);
    assertNotNull(stored(element));
  }

  @ParameterizedTest
  @ValueSource(strings = {"templates", "template-elements"})
  void aDeletionThatLosesARaceToAnUpdateIsRefusedAndNamesTheWinner(String kind) throws Exception {
    Stored artifact = create(kind, "Deleted while renamed");
    before(kind.equals("templates") ? "deleteTemplate" : "deleteTemplateElement",
        anotherWriterRenames(artifact, "Renamed by another writer"));

    Reply deleted = delete(artifact);

    assertMovedOnTo(deleted, get(artifact));
    assertEquals("Renamed by another writer", stored(artifact).path("schema:name").asText());
  }

  @ParameterizedTest
  @ValueSource(strings = {"templates", "template-elements"})
  void aDeletionWhoseArtifactAnotherWriterDeletedAnswersThatItNoLongerExists(String kind) throws Exception {
    Stored artifact = create(kind, "Deleted twice");
    before(kind.equals("templates") ? "deleteTemplate" : "deleteTemplateElement", anotherWriterDeletes(artifact));

    assertNoLongerExists(delete(artifact));
  }

  /**
   * The store refused the write because the artifact moved on, so nothing was written and the answer is
   * a refusal. Failing to read the revision that won only leaves it unnamed, whether the read fails as
   * an IOException or as the driver's own exception. Answering 500 or 503 instead would report a
   * definite refusal as an outcome nobody knows, which the resource server keeps for a background retry
   * rather than abandoning.
   */
  @ParameterizedTest
  @MethodSource("refusalsWhoseWinnerCannotBeRead")
  void aWriteRefusedForAConflictIsRefusedEvenWhenTheWinnerCannotBeRead(String write, Interruption readFailure)
      throws Exception {
    Stored element = create("template-elements", "Conflict unread");
    boolean deleting = write.equals("DELETE");
    before(deleting ? "deleteTemplateElement" : "updateTemplateElement", () -> {
      anotherWriterRenames(element, "Renamed by another writer").run();
      before("findTemplateElementWithRevision", readFailure);
    });

    Reply refused = deleting ? delete(element) : put(element, "", renamed(element, "Renamed by this request"));

    assertEquals(412, refused.status(), refused.body().toString());
    assertEquals(CedarErrorKey.ARTIFACT_HAS_MOVED_ON.getValue(), refused.errorKey(), refused.body().toString());
    assertTrue(refused.currentETag().isMissingNode() || refused.currentETag().isNull(),
        "a revision the server could not read is not named: " + refused.body());
    assertEquals("Renamed by another writer", stored(element).path("schema:name").asText());
  }

  private static Stream<Arguments> refusalsWhoseWinnerCannotBeRead() {
    Interruption mongoTimeout = () -> {
      throw new MongoTimeoutException("Timed out while waiting for a server");
    };
    return Stream.of("DELETE", "PUT").flatMap(write -> Stream.of(
        Arguments.of(write, named("an IOException", storeFailure())),
        Arguments.of(write, named("a MongoDB timeout", mongoTimeout))));
  }

  // Replacement and creation

  @Test
  void anUpdateThatLosesARaceIsRefusedAndNamesTheRevisionThatWon() throws Exception {
    Stored element = create("template-elements", "Raced");
    before("updateTemplateElement", anotherWriterRenames(element, "Renamed by another writer"));

    Reply updated = put(element, "", renamed(element, "Renamed by this request"));

    assertMovedOnTo(updated, get(element));
    assertEquals("Renamed by another writer", stored(element).path("schema:name").asText());
  }

  /**
   * A template write that replaces children passes the reference fence. When another writer changes the
   * template first, the fence refuses it as the race it is, and not as a template instances reference.
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "/inclusion"})
  void aFencedTemplateUpdateThatLosesARaceAnswersAsARace(String suffix) throws Exception {
    Stored template = create("templates", "Restructured while renamed");
    before("updateTemplateIfUnreferenced", anotherWriterRenames(template, "Renamed by another writer"));

    Reply updated = put(template, suffix, withANewChild(template));

    assertMovedOnTo(updated, get(template));
    assertEquals("Renamed by another writer", stored(template).path("schema:name").asText());
  }

  /** A replacement that adds a child, which an ordinary PUT sends through the reference fence. */
  private static ObjectNode withANewChild(Stored template) {
    return new JsonArtifactRenderer().renderTemplateSchemaArtifact(TemplateSchemaArtifact.builder()
        .withName("Restructured by this request").withJsonLdId(java.net.URI.create(template.id()))
        .withFieldSchema(TextField.builder().withName("New field").build()).build());
  }

  /**
   * An element vanishes under an ordinary update. A template vanishes under an inclusion update, which
   * the reference fence refuses as a conflict rather than as a missing artifact. Both describe the same
   * event, so they answer alike.
   */
  @ParameterizedTest
  @ValueSource(strings = {"templates", "template-elements"})
  void anUpdateWhoseArtifactAnotherWriterDeletedAnswersThatItNoLongerExists(String kind) throws Exception {
    Stored artifact = create(kind, "Updated while deleted");
    boolean template = kind.equals("templates");
    before(template ? "updateTemplateIfUnreferenced" : "updateTemplateElement", anotherWriterDeletes(artifact));

    Reply updated = put(artifact, template ? "/inclusion" : "", renamed(artifact, "Renamed after deletion"));

    assertNoLongerExists(updated);
    assertNull(stored(artifact), "a refused write must not recreate what another writer deleted");
  }

  @Test
  void aStoreFailureWhileUpdatingAnswers500() throws Exception {
    Stored element = create("template-elements", "Not updatable");
    before("updateTemplateElement", storeFailure());

    assertServerError(put(element, "", renamed(element, "Never stored")),
        CedarErrorKey.TEMPLATE_ELEMENT_NOT_UPDATED, element);
    assertEquals("Not updatable", stored(element).path("schema:name").asText());
  }

  @Test
  void aStoreFailureWhileCreatingByPutAnswers500() throws Exception {
    Stored source = create("template-elements", "Source for a new identifier");
    String id = TestUtil.getCedarConfig().getLinkedDataUtil()
        .buildNewLinkedDataId(org.metadatacenter.model.CedarResourceType.ELEMENT);
    Stored absent = new Stored("template-elements", id,
        base() + "/template-elements/" + URLEncoder.encode(id, StandardCharsets.UTF_8), null,
        source.body().deepCopy().put("@id", id));
    before("createTemplateElementWithRevision", storeFailure());

    assertServerError(put(absent, "", absent.body()), CedarErrorKey.TEMPLATE_ELEMENT_NOT_CREATED, absent);
    assertNull(stored(absent));
  }

  /**
   * The read found nothing, and another writer inserted the identifier before this request did, so the
   * unique index refused the insert. The answer names the revision that won, as an update's refusal does.
   */
  @Test
  void aCreateByPutThatLosesTheInsertRaceIsRefusedAndNamesTheRevisionThatWon() throws Exception {
    Stored source = create("template-elements", "Source for a contested identifier");
    String id = TestUtil.getCedarConfig().getLinkedDataUtil()
        .buildNewLinkedDataId(org.metadatacenter.model.CedarResourceType.ELEMENT);
    Stored absent = new Stored("template-elements", id,
        base() + "/template-elements/" + URLEncoder.encode(id, StandardCharsets.UTF_8), null,
        source.body().deepCopy().put("@id", id));
    before("createTemplateElementWithRevision", () -> TestUtil.templateElementService.createTemplateElement(
        absent.body().deepCopy().put("schema:name", "Created by another writer")));

    Reply created = put(absent, "", absent.body());

    assertMovedOnTo(created, get(absent));
    assertEquals("Created by another writer", stored(absent).path("schema:name").asText());
  }

  @Test
  void aStoreFailureWhileCreatingByPostAnswers500() throws Exception {
    long before = TestUtil.templateElementService.count();
    before("createTemplateElementWithRevision", storeFailure());

    Reply created = reply(request(base() + "/template-elements", userAuthorization, null)
        .post(Entity.entity("type: element\nname: Never created\n", APPLICATION_YAML)));

    assertServerError(created, CedarErrorKey.TEMPLATE_ELEMENT_NOT_CREATED, null);
    assertEquals(before, TestUtil.templateElementService.count());
  }

  /**
   * A verbatim write needs an artifact to replace. The request's own read decides whether there is
   * one: whatever other writers do after it, a verbatim write that found nothing is refused, and never
   * becomes a create.
   */
  @Test
  void aVerbatimWriteThatFoundNothingIsRefusedWhateverOtherWritersDoMeanwhile() throws Exception {
    Stored source = create("template-elements", "Verbatim source");
    String id = TestUtil.getCedarConfig().getLinkedDataUtil()
        .buildNewLinkedDataId(org.metadatacenter.model.CedarResourceType.ELEMENT);
    ObjectNode submitted = source.body().deepCopy().put("@id", id).put("schema:name", "Submitted verbatim");
    ObjectNode concurrent = submitted.deepCopy().put("schema:name", "Created by another writer");
    after("findTemplateElementWithRevision", () -> {
      TestUtil.templateElementService.createTemplateElement(concurrent.deepCopy());
      after("findTemplateElement", () -> TestUtil.templateElementService.deleteTemplateElement(id));
    });

    Reply written = reply(testClient.target(base() + "/template-elements/" + URLEncoder.encode(id, StandardCharsets.UTF_8))
        .queryParam("verbatim", true).request("application/json")
        .header("Authorization", adminAuthorization).put(Entity.json(submitted)));

    assertEquals(404, written.status(), written.body().toString());
    assertEquals(CedarErrorKey.ARTIFACT_NOT_FOUND.getValue(), written.errorKey(), written.body().toString());
    JsonNode stored = TestUtil.templateElementService.findTemplateElement(id);
    assertTrue(stored == null || !"Submitted verbatim".equals(stored.path("schema:name").asText()),
        "a verbatim write created an artifact: " + stored);
  }

  // The version-predecessor projection

  private static Reply projectPredecessor(Stored template, String previous) throws IOException {
    ObjectNode patch = JsonMapper.STRICT_MAPPER.createObjectNode().put("previousVersion", previous);
    return reply(request(template.url() + "/version-predecessor", adminAuthorization, template.etag())
        .put(Entity.json(patch.toString())));
  }

  @Test
  void aProjectionThatLosesARaceIsRefusedAndNamesTheRevisionThatWon() throws Exception {
    Stored template = create("templates", "Projected while renamed");
    before("updateTemplate", anotherWriterRenames(template, "Renamed by another writer"));

    Reply projected = projectPredecessor(template, "https://example.org/predecessor");

    assertMovedOnTo(projected, get(template));
    assertFalse(stored(template).has("pav:previousVersion"));
  }

  @Test
  void aProjectionWhoseTemplateAnotherWriterDeletedAnswersThatItNoLongerExists() throws Exception {
    Stored template = create("templates", "Projected while deleted");
    before("updateTemplate", anotherWriterDeletes(template));

    assertNoLongerExists(projectPredecessor(template, "https://example.org/predecessor"));
    assertNull(stored(template));
  }

  /**
   * The store renders each document it reads as JSON, so a document it cannot render fails as a
   * JsonProcessingException. That is the store's failure, and the caller's well-formed patch is not
   * at fault.
   */
  @Test
  void aStoreFailureDuringAProjectionIsNotBlamedOnThePatch() throws Exception {
    Stored template = create("templates", "Projected while unreadable");
    before("findTemplateWithRevision", () -> {
      throw new JsonParseException((JsonParser) null, "the stored document is not JSON");
    });

    Reply projected = projectPredecessor(template, "https://example.org/predecessor");

    assertEquals(500, projected.status(), projected.body().toString());
    assertFalse(stored(template).has("pav:previousVersion"));
  }
}
