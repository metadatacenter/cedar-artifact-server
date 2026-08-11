package org.metadatacenter.cedar.artifact.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The update precondition: naming the 'pav:lastUpdatedOn' a caller read makes the write conditional on
 * the artifact still being at that version. Nothing else in the stack compares versions, since a write
 * replaces the whole document, so this is what keeps a read-modify-write from overwriting a concurrent
 * save.
 * <p>
 * The case worth the most care is an identifier that resolves to nothing. A PUT there creates, which is
 * what a client choosing its own identifier wants; naming a version asserts the opposite, so the two
 * cannot both be honoured and the request is refused.
 */
public class UpdatePreconditionTest extends AbstractRestTest {

  private static final String EXPECTED = "expected_last_updated_on";

  @Test
  public void anUpdateNamingTheCurrentVersionIsAccepted() throws IOException {
    String id = createTemplate();
    JsonNode stored = read(id);

    Response response = putWithExpectation(id, stored, stored.get("pav:lastUpdatedOn").asText());

    assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus(),
        "the artifact is still at the version the caller read, so the write proceeds");
  }

  @Test
  public void anUpdateNamingAStaleVersionIsRefused() throws IOException {
    String id = createTemplate();
    JsonNode stored = read(id);

    Response response = putWithExpectation(id, stored, "2019-01-01T00:00:00-07:00");

    assertEquals(CedarResponseStatus.CONFLICT.getStatusCode(), response.getStatus());
    assertTrue(response.readEntity(String.class).contains("artifactHasMovedOn"),
        "the refusal names the reason, so a caller can retry rather than guess");
  }

  @Test
  public void anUpdateNamingNoVersionIsAcceptedAsBefore() throws IOException {
    String id = createTemplate();
    JsonNode stored = read(id);

    Response response = request(templateUrl(id)).put(Entity.json(stored.toString()));

    assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus(),
        "a caller that names nothing keeps the behaviour it had");
  }

  /**
   * The fix this class exists for. Without it the precondition is dropped, the PUT is treated as a
   * create, and a mistyped identifier answers 201 with a valid-looking copy of the artifact under an
   * identifier nothing refers to — which a repair script would record as success.
   */
  @Test
  public void namingAVersionForAnArtifactThatDoesNotExistIsRefused() throws IOException {
    String existingId = createTemplate();
    JsonNode body = read(existingId);
    // Derived from an identifier this server issued, so the request reaches the create-by-PUT path
    // rather than being turned away earlier for naming a base the server does not own.
    String absentId = existingId.substring(0, existingId.lastIndexOf('/') + 1) + UUID.randomUUID();
    JsonNode retargeted = retarget(body, absentId);

    Response response = putWithExpectation(absentId, retargeted, "2026-01-01T00:00:00-07:00");

    assertEquals(CedarResponseStatus.NOT_FOUND.getStatusCode(), response.getStatus(),
        "the caller asserted the artifact exists at a version; it does not exist at all");
    assertTrue(response.readEntity(String.class).contains("artifactNotFound"),
        "the refusal says the identifier is wrong, not that someone else edited it");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  public void anEmptyExpectationIsTreatedAsNoneRatherThanAsAVersion(String blank) throws IOException {
    String id = createTemplate();
    JsonNode stored = read(id);

    Response response = putWithExpectation(id, stored, blank);

    assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus(),
        "a blank parameter cannot match any version, so it must not be compared as one");
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private String createTemplate() throws IOException {
    Response response = request(baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix())
        .post(Entity.json(getFileContentAsString(MINIMAL_TEMPLATE_NO_ID)));
    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus(),
        "the fixture could not be created, so the precondition assertions would be meaningless");
    JsonNode created = JsonMapper.MAPPER.readTree(response.readEntity(String.class));
    createdResources.put(created.get(LinkedData.ID).asText(), CedarResourceType.TEMPLATE);
    return created.get(LinkedData.ID).asText();
  }

  private JsonNode read(String id) throws IOException {
    Response response = request(templateUrl(id)).get();
    assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
    return JsonMapper.MAPPER.readTree(response.readEntity(String.class));
  }

  private JsonNode retarget(JsonNode body, String id) {
    JsonNode copy = body.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) copy).put(LinkedData.ID, id);
    return copy;
  }

  private Response putWithExpectation(String id, JsonNode body, String expectation)
      throws UnsupportedEncodingException {
    String url = templateUrl(id) + "?" + EXPECTED + "=" + URLEncoder.encode(expectation, "UTF-8");
    return request(url).put(Entity.json(body.toString()));
  }

  private Invocation.Builder request(String url) {
    return testClient.target(url).request().header(AUTHORIZATION, authHeaderTestUser1);
  }

  private String templateUrl(String id) throws UnsupportedEncodingException {
    return baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix() + "/" + URLEncoder.encode(id, "UTF-8");
  }
}
