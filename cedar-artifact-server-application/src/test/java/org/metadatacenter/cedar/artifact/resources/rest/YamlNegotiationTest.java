package org.metadatacenter.cedar.artifact.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end coverage of YAML content negotiation on the artifact server: the Accept header
 * selects the response representation, the Content-Type header selects the request one, and JSON
 * remains the default for clients that ask for nothing in particular.
 */
public class YamlNegotiationTest extends AbstractRestTest {

  private static final String APPLICATION_YAML = "application/yaml";
  private static final String APPLICATION_X_YAML = "application/x-yaml";

  // Reading

  @ParameterizedTest
  @ValueSource(strings = {APPLICATION_YAML, APPLICATION_X_YAML})
  public void getReturnsYamlForBothYamlMediaTypes(String yamlMediaType) throws IOException {
    String id = createTemplateFromJson();

    Response response = get(id).accept(yamlMediaType).get();

    assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
    assertTrue(response.getMediaType().toString().contains("yaml"),
        "the response echoes the YAML type the client asked for, got " + response.getMediaType());
    String body = response.readEntity(String.class);
    assertTrue(body.startsWith("type:"), "a YAML artifact opens with its type, got: " + head(body));
    assertTrue(body.contains("name:"));
  }

  @Test
  public void getDefaultsToJsonWhenNoAcceptHeaderIsSent() throws IOException {
    String id = createTemplateFromJson();

    Response response = get(id).get();

    assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
    assertEquals("application/json", stripParameters(response.getMediaType().toString()));
    assertTrue(response.readEntity(String.class).trim().startsWith("{"));
  }

  @Test
  public void getDefaultsToJsonForAWildcardAccept() throws IOException {
    String id = createTemplateFromJson();

    Response response = get(id).accept("*/*").get();

    assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
    assertEquals("application/json", stripParameters(response.getMediaType().toString()));
  }

  @Test
  public void getRejectsAnAcceptHeaderThatCanNotBeProduced() throws IOException {
    String id = createTemplateFromJson();

    Response response = get(id).accept("application/xml").get();

    assertEquals(CedarResponseStatus.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
  }

  @Test
  public void compactSelectsTheLeanYamlFormOnRead() throws IOException {
    String id = createTemplateFromJson();

    String full = get(id).accept(APPLICATION_YAML).get().readEntity(String.class);
    String compact = getWithQuery(id, "compact=true").accept(APPLICATION_YAML).get().readEntity(String.class);

    assertTrue(compact.length() < full.length(),
        "the compact form drops provenance, version and status, so it is smaller");
    assertTrue(full.contains("modelVersion:"));
    assertFalse(compact.contains("modelVersion:"), "the compact form carries no system-recorded keys");
  }

  @Test
  public void byteDifferentRepresentationsHaveDifferentStrongEtags() throws IOException {
    String id = createTemplateFromJson();

    Response jsonResponse = get(id).get();
    String jsonEtag = jsonResponse.getHeaderString("ETag");
    assertTrue(jsonResponse.getHeaderString("Vary").contains("Accept"));
    jsonResponse.close();

    Response yamlResponse = get(id).accept(APPLICATION_YAML).get();
    String yamlEtag = yamlResponse.getHeaderString("ETag");
    String yaml = yamlResponse.readEntity(String.class);
    assertTrue(yamlResponse.getHeaderString("Vary").contains("Accept"));

    Response compactResponse = getWithQuery(id, "compact=true").accept(APPLICATION_YAML).get();
    String compactEtag = compactResponse.getHeaderString("ETag");
    compactResponse.close();

    assertNotEquals(jsonEtag, yamlEtag);
    assertNotEquals(yamlEtag, compactEtag);
    assertEquals("\"1-yaml\"", yamlEtag);
    assertEquals("\"1-yaml-compact\"", compactEtag);

    Response updated = request(templateUrl(id)).header("If-Match", yamlEtag)
        .put(Entity.entity(yaml.replaceFirst("(?m)^name: .*$", "name: Updated With YAML ETag"),
            APPLICATION_YAML));
    assertEquals(CedarResponseStatus.OK.getStatusCode(), updated.getStatus());
    assertEquals("\"2\"", updated.getHeaderString("ETag"));
  }

  // Writing

  @ParameterizedTest
  @ValueSource(strings = {APPLICATION_YAML, APPLICATION_X_YAML})
  public void postAcceptsAYamlBody(String yamlMediaType) throws IOException {
    String yaml = "type: template\nname: YAML Posted Template\ndescription: created from a YAML body\n";

    Response response = request(baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix())
        .post(Entity.entity(yaml, yamlMediaType));

    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus());
    JsonNode created = JsonMapper.MAPPER.readTree(response.readEntity(String.class));
    markForCleanup(created);
    assertEquals("YAML Posted Template", created.get("schema:name").asText());
  }

  /**
   * A body that names an artifact is read as a stored one, and a stored one carries its model version.
   *
   * <p>The shape below — an id with none of the system-recorded keys — used to be caught by a guard of
   * its own, as the signature of the compact form, which strips those keys and could not be stored
   * without silently regenerating them. The guard is gone: compact stopped carrying the identifier, so
   * nothing emits that signature any more. The shape is still refused, by the reader rather than a
   * guard — naming an artifact is what selects the full form, and the full form requires
   * {@code modelVersion}.
   */
  @Test
  public void postRejectsAYamlBodyThatNamesAnArtifactWithoutItsModelVersion() throws IOException {
    String naming = "type: template\nname: Compact\nid: https://repo.metadatacenter.org/templates/"
        + "11111111-1111-1111-1111-111111111111\n";

    Response response = request(baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix())
        .post(Entity.entity(naming, APPLICATION_YAML));

    assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertTrue(response.readEntity(String.class).contains("modelVersion"),
        "the refusal names what the form it was read as requires");
  }

  @Test
  public void writesRejectTheCompactQueryParameter() throws IOException {
    String yaml = "type: template\nname: Compact Param\n";

    Response response = request(baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix() + "?compact=true")
        .post(Entity.entity(yaml, APPLICATION_YAML));

    assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertTrue(response.readEntity(String.class).contains("not supported on write operations"));
  }

  @Test
  public void postRejectsAnEmptyYamlBody() throws IOException {
    Response response = request(baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix())
        .post(Entity.entity("", APPLICATION_YAML));

    assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void postRejectsAMalformedYamlBody() throws IOException {
    Response response = request(baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix())
        .post(Entity.entity("type: template\n  name: [unclosed\n", APPLICATION_YAML));

    assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  public void writeResponseIsRenderedAsYamlWhenTheClientAsksForIt() throws IOException {
    String yaml = "type: template\nname: Negotiated Write\n";

    Response response = request(baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix())
        .accept(APPLICATION_YAML)
        .post(Entity.entity(yaml, APPLICATION_YAML));

    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus());
    assertEquals("\"1-yaml\"", response.getHeaderString("ETag"));
    assertTrue(response.getHeaderString("Vary").contains("Accept"));
    String body = response.readEntity(String.class);
    assertTrue(body.startsWith("type:"), "the write response honors Accept, got: " + head(body));
    markForCleanup(yamlBodyId(body));
  }

  @Test
  public void aJsonBodyStillWorksWhileTheClientAsksForYamlBack() throws IOException {
    String json = getFileContentAsString(MINIMAL_TEMPLATE_NULL_ID);

    Response response = request(baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix())
        .accept(APPLICATION_YAML)
        .post(Entity.json(json));

    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus());
    String body = response.readEntity(String.class);
    assertTrue(body.startsWith("type:"));
    markForCleanup(yamlBodyId(body));
  }

  @Test
  public void putAcceptsAYamlBody() throws IOException {
    String id = createTemplateFromJson();
    String stored = get(id).accept(APPLICATION_YAML).get().readEntity(String.class);
    String edited = stored.replaceFirst("(?m)^name: .*$", "name: Renamed Through YAML");

    String url = templateUrl(id);
    Response response = request(url).header("If-Match", currentEtag(url, authHeaderTestUser1))
        .put(Entity.entity(edited, APPLICATION_YAML));

    assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
    JsonNode reread = JsonMapper.MAPPER.readTree(get(id).get().readEntity(String.class));
    assertEquals("Renamed Through YAML", reread.get("schema:name").asText());
  }

  /**
   * The regression this negotiation is most likely to hide: static image and YouTube fields carry
   * their display box as _ui._size, and the YAML serialization puts it in the child's
   * configuration block, so a round trip that only reads it at the field level loses it.
   */
  @Test
  public void aYamlRoundTripPreservesTheStaticFieldDisplaySize() throws IOException {
    String yaml = """
        type: template
        name: Static Size Template
        children:
          - key: logo
            type: static-image
            name: Logo
            content: https://example.org/logo.png
            configuration:
              width: 300
              height: 200
        """;

    Response created = request(baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix())
        .post(Entity.entity(yaml, APPLICATION_YAML));
    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), created.getStatus());
    JsonNode stored = JsonMapper.MAPPER.readTree(created.readEntity(String.class));
    markForCleanup(stored);
    String id = stored.get(LinkedData.ID).asText();

    JsonNode sizeBefore = findFirstUiSize(stored);
    assertEquals(300, sizeBefore.get("width").asInt(), "the YAML write path dropped the width");
    assertEquals(200, sizeBefore.get("height").asInt(), "the YAML write path dropped the height");

    // Read it back as YAML and store that verbatim: the size must survive both directions.
    String rendered = get(id).accept(APPLICATION_YAML).get().readEntity(String.class);
    String url = templateUrl(id);
    Response put = request(url).header("If-Match", currentEtag(url, authHeaderTestUser1))
        .put(Entity.entity(rendered, APPLICATION_YAML));
    assertEquals(CedarResponseStatus.OK.getStatusCode(), put.getStatus());

    JsonNode after = JsonMapper.MAPPER.readTree(get(id).get().readEntity(String.class));
    assertEquals(sizeBefore, findFirstUiSize(after), "the YAML round trip dropped _ui._size");
  }

  // Helpers

  private String createTemplateFromJson() throws IOException {
    return createTemplateFromJson(MINIMAL_TEMPLATE_NULL_ID);
  }

  private String createTemplateFromJson(String fixture) throws IOException {
    Response response = request(baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix())
        .post(Entity.json(getFileContentAsString(fixture)));
    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus(),
        "the fixture could not be created, so the negotiation assertions would be meaningless");
    JsonNode created = JsonMapper.MAPPER.readTree(response.readEntity(String.class));
    markForCleanup(created);
    return created.get(LinkedData.ID).asText();
  }

  private void markForCleanup(JsonNode created) {
    createdResources.put(created.get(LinkedData.ID).asText(), CedarResourceType.TEMPLATE);
  }

  private void markForCleanup(String id) {
    createdResources.put(id, CedarResourceType.TEMPLATE);
  }

  private String yamlBodyId(String yamlBody) {
    for (String line : yamlBody.split("\n")) {
      if (line.startsWith("id:")) {
        return line.substring("id:".length()).trim().replaceAll("^\"|\"$", "");
      }
    }
    throw new IllegalStateException("The created artifact carries no id: " + head(yamlBody));
  }

  private Invocation.Builder request(String url) {
    return testClient.target(url).request().header(AUTHORIZATION, authHeaderTestUser1);
  }

  private Invocation.Builder get(String id) throws UnsupportedEncodingException {
    return request(templateUrl(id));
  }

  private Invocation.Builder getWithQuery(String id, String query) throws UnsupportedEncodingException {
    return request(templateUrl(id) + "?" + query);
  }

  private String templateUrl(String id) throws UnsupportedEncodingException {
    return baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix() + "/" + URLEncoder.encode(id, "UTF-8");
  }

  private static String stripParameters(String mediaType) {
    int semicolon = mediaType.indexOf(';');
    return semicolon < 0 ? mediaType : mediaType.substring(0, semicolon);
  }

  private static String head(String body) {
    return body.length() <= 200 ? body : body.substring(0, 200) + "...";
  }

  private static JsonNode findFirstUiSize(JsonNode node) {
    if (node == null || !(node.isObject() || node.isArray())) {
      return null;
    }
    JsonNode ui = node.get("_ui");
    if (ui != null && ui.get("_size") != null) {
      return ui.get("_size");
    }
    for (JsonNode child : node) {
      JsonNode found = findFirstUiSize(child);
      if (found != null) {
        return found;
      }
    }
    return null;
  }
}
