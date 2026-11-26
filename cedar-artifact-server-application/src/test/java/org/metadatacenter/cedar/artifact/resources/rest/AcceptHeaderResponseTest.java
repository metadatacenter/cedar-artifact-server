package org.metadatacenter.cedar.artifact.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.model.Filters;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.config.MongoConfig;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a caller gets from each negotiated artifact route for each Accept header that matters: no header,
 * wildcards, the types the server produces, types it does not, a header that is not a media type, and
 * quality values, including a malformed one.
 *
 * <p>Every resource method declares the types it produces, and the server chooses among exactly those
 * types. Jersey therefore answers an Accept header that names none of them with its own 406 before the
 * method runs, and a method that runs always finds a representation. The refusal is the JSON error
 * envelope with no message or parameters, and a refused write stores nothing. Jersey also does not treat
 * a quality of zero as a refusal, so such a header is served like one without the quality.
 */
public class AcceptHeaderResponseTest extends AbstractRestTest {

  /** What a route answers with. */
  private enum Outcome {
    JSON("application/json", ""), YAML("application/yaml", "-yaml"), X_YAML("application/x-yaml", "-yaml"),
    NQUADS("application/n-quads", "-rdf-nquad"), REFUSED(null, null), MALFORMED(null, null);

    private final String mediaType;
    private final String etagSuffix;

    Outcome(String mediaType, String etagSuffix) {
      this.mediaType = mediaType;
      this.etagSuffix = etagSuffix;
    }
  }

  /**
   * An Accept header, with what a schema artifact's routes and an instance's writes answer, what a
   * negotiated instance read answers, and what an instance read naming its format answers. A null header
   * sends none.
   */
  private record Accept(String header, Outcome write, Outcome instanceRead, Outcome formatRead) {
    @Override
    public String toString() {
      return header == null ? "no Accept" : "Accept '" + header + "'";
    }
  }

  private static final List<Accept> ACCEPTS = List.of(
      new Accept(null, Outcome.JSON, Outcome.JSON, Outcome.JSON),
      new Accept("", Outcome.JSON, Outcome.JSON, Outcome.JSON),
      new Accept("*/*", Outcome.JSON, Outcome.JSON, Outcome.JSON),
      new Accept("application/*;q=0.5", Outcome.JSON, Outcome.JSON, Outcome.JSON),
      new Accept("application/json", Outcome.JSON, Outcome.JSON, Outcome.JSON),
      new Accept("application/yaml", Outcome.YAML, Outcome.YAML, Outcome.JSON),
      new Accept("application/x-yaml", Outcome.X_YAML, Outcome.X_YAML, Outcome.JSON),
      new Accept("application/n-quads", Outcome.REFUSED, Outcome.NQUADS, Outcome.JSON),
      new Accept("text/plain", Outcome.REFUSED, Outcome.REFUSED, Outcome.REFUSED),
      new Accept("text/*", Outcome.REFUSED, Outcome.REFUSED, Outcome.REFUSED),
      new Accept("garbage", Outcome.REFUSED, Outcome.REFUSED, Outcome.REFUSED),
      new Accept("application/garbage", Outcome.REFUSED, Outcome.REFUSED, Outcome.REFUSED),
      new Accept("application/json, text/plain", Outcome.JSON, Outcome.JSON, Outcome.JSON),
      new Accept("text/plain, application/json;q=0.1", Outcome.JSON, Outcome.JSON, Outcome.JSON),
      new Accept("text/plain;q=1.0, application/yaml;q=0.2", Outcome.YAML, Outcome.YAML, Outcome.JSON),
      new Accept("application/json;q=0", Outcome.JSON, Outcome.JSON, Outcome.JSON),
      new Accept("application/json;q=0, text/plain", Outcome.JSON, Outcome.JSON, Outcome.JSON),
      new Accept("*/*;q=0", Outcome.JSON, Outcome.JSON, Outcome.JSON),
      new Accept("application/yaml;q=0, application/json;q=0", Outcome.YAML, Outcome.YAML, Outcome.JSON),
      new Accept("application/json;q=abc", Outcome.MALFORMED, Outcome.MALFORMED, Outcome.MALFORMED));

  /** A negotiated route. */
  private enum Route { POST, GET, GET_WITH_FORMAT, PUT_EXISTING, PUT_NEW }

  static Stream<Arguments> requests() {
    List<Arguments> cases = new ArrayList<>();
    for (CedarResourceType kind : List.of(CedarResourceType.TEMPLATE, CedarResourceType.INSTANCE)) {
      for (Route route : Route.values()) {
        if (route == Route.GET_WITH_FORMAT && kind != CedarResourceType.INSTANCE) {
          continue;
        }
        for (Accept accept : ACCEPTS) {
          cases.add(Arguments.of(kind.getValue(), route, accept));
        }
      }
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "{1} a {0} with {2}")
  @MethodSource("requests")
  public void aRouteAnswersEachAcceptHeaderAsItDoesToday(String kindName, Route route, Accept accept)
      throws IOException {
    CedarResourceType kind = CedarResourceType.forValue(kindName);
    String templateId = create(CedarResourceType.TEMPLATE, null);
    String id = kind == CedarResourceType.TEMPLATE ? templateId : create(kind, templateId);
    String name = "Accept Probe " + UUID.randomUUID();

    Outcome expected = accept.write();
    Response response;
    String newId = null;
    switch (route) {
      case POST -> response = withAccept(request(baseTestUrl + "/" + kind.getPrefix()), accept)
          .post(Entity.entity(yaml(kind, name, templateId), "application/yaml"));
      case GET -> {
        expected = kind == CedarResourceType.INSTANCE ? accept.instanceRead() : accept.write();
        response = withAccept(request(url(kind, id)), accept).get();
      }
      case GET_WITH_FORMAT -> {
        expected = Outcome.JSON;
        response = withAccept(request(url(kind, id) + "?format=jsonld"), accept).get();
      }
      case PUT_EXISTING -> response = withAccept(request(url(kind, id)).header("If-Match", "\"1\""), accept)
          .put(Entity.json(storedJson(kind, id).put("schema:name", name).toString()));
      case PUT_NEW -> {
        newId = linkedDataUtil.buildNewLinkedDataId(kind);
        ObjectNode body = storedJson(kind, id).put(LinkedData.ID, newId).put("schema:name", name);
        createdResources.put(newId, kind);
        response = withAccept(request(url(kind, newId)), accept).put(Entity.json(body.toString()));
      }
      default -> throw new IllegalArgumentException(route.name());
    }
    String body = response.readEntity(String.class);
    String mediaType = response.getMediaType() == null ? null : stripParameters(response.getMediaType().toString());
    List<String> vary = response.getHeaderString("Vary") == null ? List.of()
        : Arrays.stream(response.getHeaderString("Vary").split(",")).map(String::trim).toList();

    switch (expected) {
      case MALFORMED -> {
        assertEquals(400, response.getStatus(), body);
        assertNull(mediaType, "a malformed Accept header is refused before any resource answers: " + body);
        assertEquals("", body);
        assertNothingWritten(kind, route, id, newId, name);
      }
      case REFUSED -> {
        assertEquals(406, response.getStatus(), body);
        assertEquals("application/json", mediaType, "a refusal is the JSON error envelope: " + body);
        JsonNode error = JsonMapper.STRICT_MAPPER.readTree(body);
        assertEquals("NOT_ACCEPTABLE", error.path("status").asText(), body);
        assertEquals(406, error.path("statusCode").asInt(), body);
        assertTrue(error.path("message").isNull(), "Jersey's refusal carries no message: " + body);
        assertEquals(0, error.path("parameters").size(), "Jersey's refusal carries no parameters: " + body);
        assertFalse(vary.contains("Accept"), "Jersey's refusal does not vary on Accept: " + vary);
        assertNothingWritten(kind, route, id, newId, name);
      }
      default -> {
        int status = route == Route.POST || route == Route.PUT_NEW ? 201 : 200;
        assertEquals(status, response.getStatus(), body);
        assertEquals(expected.mediaType, mediaType, "the representation is labelled as what it is: " + head(body));
        String revision = route == Route.PUT_EXISTING ? "2" : "1";
        assertEquals("\"" + revision + expected.etagSuffix + "\"", response.getHeaderString("ETag"));
        assertEquals(route != Route.GET_WITH_FORMAT, vary.contains("Accept"),
            "only a negotiated answer varies on Accept: " + vary);
        switch (expected) {
          case JSON -> {
            String answered = JsonMapper.STRICT_MAPPER.readTree(body).path(LinkedData.ID).asText();
            createdResources.put(answered, kind);
            if (route != Route.POST) {
              assertEquals(route == Route.PUT_NEW ? newId : id, answered);
            }
          }
          case YAML, X_YAML -> {
            assertTrue(body.startsWith("type: " + kind.getValue()), "a YAML artifact opens with its type: "
                + head(body));
            if (route == Route.POST) {
              createdResources.put(storedIdByName(kind, name), kind);
            }
          }
          case NQUADS -> assertTrue(body.contains("<" + id + ">"), "the N-Quads describe the instance: "
              + head(body));
          default -> throw new IllegalStateException(expected.name());
        }
      }
    }
  }

  /** A refused or malformed write leaves the store as it was. */
  private void assertNothingWritten(CedarResourceType kind, Route route, String id, String newId, String name)
      throws IOException {
    switch (route) {
      case POST -> assertNull(storedIdByName(kind, name), "a refused create stored an artifact");
      case PUT_EXISTING -> {
        Response stored = request(url(kind, id)).header("Accept", "application/json").get();
        stored.close();
        assertEquals("\"1\"", stored.getHeaderString("ETag"), "a refused replacement changed the artifact");
      }
      case PUT_NEW -> assertNull(storedIdByName(kind, name), "a refused create stored an artifact at " + newId);
      default -> {
      }
    }
  }

  // Fixtures

  private String create(CedarResourceType kind, String templateId) throws IOException {
    Response response = request(baseTestUrl + "/" + kind.getPrefix())
        .post(Entity.entity(yaml(kind, "Accept Probe Fixture", templateId), "application/yaml"));
    String created = response.readEntity(String.class);
    assertEquals(201, response.getStatus(), "the fixture could not be created: " + created);
    String id = JsonMapper.STRICT_MAPPER.readTree(created).path(LinkedData.ID).asText();
    createdResources.put(id, kind);
    return id;
  }

  private static String yaml(CedarResourceType kind, String name, String templateId) {
    return kind == CedarResourceType.TEMPLATE ? "type: template\nname: " + name + "\n"
        : "type: instance\nname: " + name + "\nisBasedOn: " + templateId + "\n";
  }

  private ObjectNode storedJson(CedarResourceType kind, String id) throws IOException {
    Response response = request(url(kind, id)).header("Accept", "application/json").get();
    return (ObjectNode) JsonMapper.STRICT_MAPPER.readTree(response.readEntity(String.class));
  }

  /** The identifier of the stored artifact with this name, or null when none is stored. */
  private static String storedIdByName(CedarResourceType kind, String name) {
    MongoConfig store = TestUtil.getCedarConfig().getArtifactServerConfig();
    org.bson.Document found = CedarDataServices.getInstance().getMongoClientFactoryForDocuments().getClient()
        .getDatabase(store.getDatabaseName())
        .getCollection(store.getMongoCollectionName(kind))
        .find(Filters.eq("schema:name", name))
        .first();
    return found == null ? null : found.getString(LinkedData.ID);
  }

  private static Invocation.Builder withAccept(Invocation.Builder request, Accept accept) {
    return accept.header() == null ? request : request.header("Accept", accept.header());
  }

  private String url(CedarResourceType kind, String id) {
    return baseTestUrl + "/" + kind.getPrefix() + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8);
  }

  private Invocation.Builder request(String url) {
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
