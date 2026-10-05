package org.metadatacenter.cedar.artifact.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.config.MongoConfig;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

/**
 * Every way a client can ask the artifact server for an artifact, for each kind of artifact, stored
 * readable, stored in a form the artifact library cannot read, or not stored at all.
 *
 * <p>The server returns an artifact's JSON as it is stored and produces every other representation
 * through the artifact library, so what it can answer depends on the stored document as well as on the
 * request. A YAML read of an artifact the library could not read answered 500, even when the request
 * accepted the JSON that was there to serve. An instance read declared N-Quads among the types it
 * produces and refused an Accept that named it.
 *
 * <p>A read must answer with the representation the Accept header prefers among those the server can
 * produce, labelled as what it is, varying on Accept, with a validator for that representation; or 406
 * when there is none. A YAML read of an unreadable artifact falls back to JSON where the header admits
 * JSON, and is refused, saying why, where it does not.
 *
 * <p>The validate command and a write must also agree on every body. A YAML instance is completed
 * against its template when it is written, and the command did not complete it, so it reported as
 * invalid an instance the write would store.
 */
public class RepresentationMatrixTest extends AbstractRestTest {

  /** A representation a read can answer with, by the media type it is labelled with. */
  private enum Representation {
    JSON("application/json"), YAML("application/yaml"), X_YAML("application/x-yaml"),
    NQUADS("application/n-quads"), NONE(null);

    private final String mediaType;

    Representation(String mediaType) {
      this.mediaType = mediaType;
    }

    boolean isYaml() {
      return this == YAML || this == X_YAML;
    }
  }

  /**
   * An Accept header, with the representation a schema artifact and an instance should be read in,
   * and whether the header admits JSON at all. A null header sends none.
   */
  private record Accept(String header, Representation schema, Representation instance, boolean admitsJson) {
    @Override
    public String toString() {
      return header == null ? "no Accept" : "Accept " + header;
    }
  }

  private static final List<Accept> ACCEPTS = List.of(
      new Accept(null, Representation.JSON, Representation.JSON, true),
      new Accept("*/*", Representation.JSON, Representation.JSON, true),
      new Accept("application/*", Representation.JSON, Representation.JSON, true),
      new Accept("application/json", Representation.JSON, Representation.JSON, true),
      new Accept("application/yaml", Representation.YAML, Representation.YAML, false),
      new Accept("application/x-yaml", Representation.X_YAML, Representation.X_YAML, false),
      new Accept("application/yaml, application/json;q=0.5", Representation.YAML, Representation.YAML, true),
      new Accept("application/yaml, */*;q=0.1", Representation.YAML, Representation.YAML, true),
      new Accept("application/json;q=0.5, application/yaml", Representation.YAML, Representation.YAML, true),
      new Accept("application/n-quads", Representation.NONE, Representation.NQUADS, false),
      new Accept("application/n-quads, application/json;q=0.5", Representation.JSON, Representation.NQUADS, true),
      new Accept("text/html", Representation.NONE, Representation.NONE, false),
      new Accept("text/html, */*;q=0.8", Representation.JSON, Representation.JSON, true));

  private static final List<CedarResourceType> KINDS = List.of(
      CedarResourceType.TEMPLATE, CedarResourceType.ELEMENT, CedarResourceType.FIELD, CedarResourceType.INSTANCE);

  /** How the artifact a read names is stored. */
  private enum Stored { READABLE, UNREADABLE, ABSENT }

  private static final String TEMPLATE_YAML = """
      type: template
      name: Representation Matrix Template
      children:
        - key: filled
          type: text-field
          name: Filled
        - key: omitted
          type: text-field
          name: Omitted
      """;

  static Stream<Arguments> reads() {
    List<Arguments> cases = new ArrayList<>();
    for (CedarResourceType kind : KINDS) {
      for (Stored stored : Stored.values()) {
        for (Accept accept : ACCEPTS) {
          cases.add(Arguments.of(kind.getValue(), stored, accept));
        }
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "a {0} stored {1}, read with {2}")
  @MethodSource("reads")
  public void aReadAnswersWithTheRepresentationTheClientPrefersAndTheServerCanProduce(
      String kindName, Stored stored, Accept accept) throws IOException {
    CedarResourceType kind = CedarResourceType.forValue(kindName);
    String id = stored == Stored.ABSENT ? linkedDataUtil.buildNewLinkedDataId(kind) : create(kind);
    if (stored == Stored.UNREADABLE) {
      makeUnreadable(kind, id);
    }
    String url = url(kind, id);

    Representation preferred = kind == CedarResourceType.INSTANCE ? accept.instance() : accept.schema();
    Representation expected = preferred;
    if (stored == Stored.UNREADABLE && preferred.isYaml()) {
      expected = accept.admitsJson() ? Representation.JSON : Representation.NONE;
    }

    Invocation.Builder request = request(url);
    if (accept.header() != null) {
      request = request.header("Accept", accept.header());
    }
    Response response = request.get();
    String body = response.readEntity(String.class);
    String mediaType = response.getMediaType() == null ? null : stripParameters(response.getMediaType().toString());

    if (expected == Representation.NONE) {
      Assertions.assertEquals(406, response.getStatus(), body);
      Assertions.assertEquals("application/json", mediaType, "a refusal is the JSON error envelope: " + body);
      if (stored == Stored.UNREADABLE && preferred.isYaml()) {
        Assertions.assertTrue(body.contains("no YAML form"), "the refusal should say why: " + body);
      }
      return;
    }
    if (stored == Stored.ABSENT) {
      Assertions.assertEquals(404, response.getStatus(), body);
      Assertions.assertEquals("application/json", mediaType, "an error is the JSON error envelope: " + body);
      return;
    }

    Assertions.assertEquals(200, response.getStatus(), body);
    Assertions.assertEquals(expected.mediaType, mediaType, "the representation is labelled as what it is");
    Assertions.assertTrue(String.valueOf(response.getHeaderString("Vary")).contains("Accept"),
        "a negotiated read varies on Accept");
    String etag = response.getHeaderString("ETag");
    Assertions.assertNotNull(etag, "a read carries a validator");
    String jsonEtag = jsonEtag(url);
    switch (expected) {
      case JSON -> {
        Assertions.assertEquals(id, JsonMapper.STRICT_MAPPER.readTree(body).path(LinkedData.ID).asText());
        Assertions.assertEquals(jsonEtag, etag, "the JSON carries the JSON's validator");
      }
      case YAML, X_YAML -> {
        Assertions.assertTrue(body.startsWith("type:"), "a YAML artifact opens with its type: " + head(body));
        Assertions.assertNotEquals(jsonEtag, etag, "the YAML carries a validator of its own");
      }
      case NQUADS -> {
        Assertions.assertTrue(body.contains("<" + id + ">"), "the N-Quads describe the instance: " + head(body));
        Assertions.assertNotEquals(jsonEtag, etag, "the N-Quads carry a validator of their own");
      }
      default -> Assertions.fail("no representation to check");
    }
  }

  /** A body sent to the validate command and to a write, and how it is sent. */
  private record Body(String label, CedarResourceType kind, String mediaType, Body.Source source) {
    interface Source {
      String body(RepresentationMatrixTest test, String templateId) throws IOException;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  private static final List<Body> BODIES = List.of(
      new Body("a template in YAML", CedarResourceType.TEMPLATE, "application/yaml", (t, template) -> TEMPLATE_YAML),
      new Body("an element in YAML", CedarResourceType.ELEMENT, "application/yaml",
          (t, template) -> "type: element\nname: Representation Matrix Element\n"),
      new Body("a field in YAML", CedarResourceType.FIELD, "application/yaml",
          (t, template) -> "type: text-field\nname: Representation Matrix Field\n"),
      new Body("an instance in YAML carrying one of its two fields", CedarResourceType.INSTANCE, "application/yaml",
          (t, template) -> instanceYaml(template, true, false)),
      new Body("an instance in YAML carrying both fields", CedarResourceType.INSTANCE, "application/yaml",
          (t, template) -> instanceYaml(template, true, true)),
      new Body("an instance in YAML naming a template the server does not hold", CedarResourceType.INSTANCE,
          "application/yaml", (t, template) -> instanceYaml(
              "https://repo.metadatacenter.org/templates/00000000-0000-0000-0000-000000000000", true, false)),
      new Body("an instance in JSON as the server stores it", CedarResourceType.INSTANCE, "application/json",
          (t, template) -> t.storedInstanceJson(template).toString()),
      new Body("an instance in JSON missing a field", CedarResourceType.INSTANCE, "application/json",
          (t, template) -> {
            ObjectNode instance = t.storedInstanceJson(template);
            instance.remove("omitted");
            return instance.toString();
          }));

  static Stream<Arguments> bodies() {
    return BODIES.stream().map(Arguments::of);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("bodies")
  public void theValidateCommandAndAWriteAgree(Body body) throws IOException {
    String templateId = create(CedarResourceType.TEMPLATE);
    String content = body.source().body(this, templateId);

    Response validated = request(baseTestUrl + "/command/validate?resource_type=" + body.kind().getValue())
        .post(Entity.entity(content, body.mediaType()));
    String validation = validated.readEntity(String.class);
    boolean validates = validated.getStatus() == 200
        && "true".equals(JsonMapper.STRICT_MAPPER.readTree(validation).path("validates").asText());
    Assertions.assertTrue(validated.getStatus() == 200 || validated.getStatus() == 400,
        "the command answers with a report or a refusal: " + validated.getStatus() + " " + validation);

    Response written = request(baseTestUrl + "/" + body.kind().getPrefix()).post(Entity.entity(content, body.mediaType()));
    String writing = written.readEntity(String.class);
    if (written.getStatus() == 201) {
      createdResources.put(JsonMapper.STRICT_MAPPER.readTree(writing).path(LinkedData.ID).asText(), body.kind());
    }
    Assertions.assertTrue(written.getStatus() == 201 || written.getStatus() == 400,
        "the write stores the body or refuses it: " + written.getStatus() + " " + writing);

    Assertions.assertEquals(written.getStatus() == 201, validates,
        "the command said " + (validates ? "valid" : "invalid") + " (" + validation + ") and the write answered "
            + written.getStatus() + " (" + head(writing) + ")");
  }

  // Fixtures

  private String create(CedarResourceType kind) throws IOException {
    String body = switch (kind) {
      case TEMPLATE -> TEMPLATE_YAML;
      case ELEMENT -> "type: element\nname: Representation Matrix Element\n";
      case FIELD -> "type: text-field\nname: Representation Matrix Field\n";
      case INSTANCE -> instanceYaml(create(CedarResourceType.TEMPLATE), true, false);
      default -> throw new IllegalArgumentException(kind.getValue());
    };
    Response response = request(baseTestUrl + "/" + kind.getPrefix()).post(Entity.entity(body, "application/yaml"));
    String created = response.readEntity(String.class);
    Assertions.assertEquals(201, response.getStatus(), "the fixture could not be created: " + created);
    String id = JsonMapper.STRICT_MAPPER.readTree(created).path(LinkedData.ID).asText();
    createdResources.put(id, kind);
    return id;
  }

  /** The JSON the server stores for an instance of the template, both fields filled. */
  private ObjectNode storedInstanceJson(String templateId) throws IOException {
    Response response = request(baseTestUrl + "/" + CedarResourceType.INSTANCE.getPrefix())
        .post(Entity.entity(instanceYaml(templateId, true, true), "application/yaml"));
    ObjectNode stored = (ObjectNode) JsonMapper.STRICT_MAPPER.readTree(response.readEntity(String.class));
    Assertions.assertEquals(201, response.getStatus(), stored.toString());
    String id = stored.path(LinkedData.ID).asText();
    removeResource(id, CedarResourceType.INSTANCE);
    for (String assigned : List.of(LinkedData.ID, "pav:createdOn", "pav:createdBy", "pav:lastUpdatedOn",
        "oslc:modifiedBy")) {
      stored.remove(assigned);
    }
    return stored;
  }

  private static String instanceYaml(String templateId, boolean filled, boolean omitted) {
    StringBuilder yaml = new StringBuilder("type: instance\nname: Representation Matrix Instance\nisBasedOn: ")
        .append(templateId).append("\nchildren:\n");
    if (filled) {
      yaml.append("  filled:\n    value: Alice\n");
    }
    if (omitted) {
      yaml.append("  omitted:\n    value: Bob\n");
    }
    return yaml.toString();
  }

  /**
   * Stores the artifact as the server would never write it, but as older data may hold it: with a
   * creation time the artifact library can not read. The JSON is otherwise the server's own.
   */
  private static void makeUnreadable(CedarResourceType kind, String id) {
    MongoConfig store = TestUtil.getCedarConfig().getArtifactServerConfig();
    long changed = CedarDataServices.getInstance().getMongoClientFactoryForDocuments().getClient()
        .getDatabase(store.getDatabaseName())
        .getCollection(store.getMongoCollectionName(kind))
        .updateOne(Filters.eq(LinkedData.ID, id), Updates.set("pav:createdOn", "yesterday"))
        .getModifiedCount();
    Assertions.assertEquals(1, changed, "the stored " + kind.getValue() + " was not found to change");
  }

  private static String jsonEtag(String url) {
    Response response = testClient.target(url).request().header(AUTHORIZATION, authHeaderTestUser1)
        .header("Accept", "application/json").get();
    response.close();
    return response.getHeaderString("ETag");
  }

  private static String url(CedarResourceType kind, String id) {
    return baseTestUrl + "/" + kind.getPrefix() + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8);
  }

  private static Invocation.Builder request(String url) {
    return testClient.target(url).request().header(AUTHORIZATION, authHeaderTestUser1);
  }

  private static String stripParameters(String mediaType) {
    int semicolon = mediaType.indexOf(';');
    return semicolon < 0 ? mediaType : mediaType.substring(0, semicolon);
  }

  private static String head(String body) {
    return body.length() <= 300 ? body : body.substring(0, 300) + "...";
  }
}
