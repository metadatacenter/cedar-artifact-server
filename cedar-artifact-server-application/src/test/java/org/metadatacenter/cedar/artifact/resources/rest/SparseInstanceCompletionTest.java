package org.metadatacenter.cedar.artifact.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An instance may be written carrying only the fields that hold a value. The stored JSON must carry
 * every field its template declares, so the server completes it: the requirement belongs to the JSON
 * serialization, where the template's schema marks each property required, and not to the model,
 * which regards an instance that omits an empty field as whole. YAML cannot express an empty field
 * at all — it refuses both an empty mapping and a null — so without this a YAML-authored instance
 * could not be written. What an empty field looks like once written is the JSON's business, and
 * differs by the kind of value the field takes.
 *
 * <p>The completion is the YAML path's alone. A JSON instance is stored as it was sent, and a field
 * it omits is still refused: the two bodies describe the same document, and the serialization is
 * the only thing that says whether an absent field means "empty" or "deleted".
 */
public class SparseInstanceCompletionTest extends AbstractRestTest {

  private static final String APPLICATION_YAML = "application/yaml";

  /**
   * A template an instance can fill one field of and leave the rest empty. The empty ones are of
   * both kinds, because they are not written the same way: see
   * {@link #anEmptyFieldIsWrittenAsItsKindAllows}.
   */
  private static final String TWO_FIELD_TEMPLATE = """
      type: template
      name: Sparse Completion Template
      children:
        - key: filled
          type: text-field
          name: Filled
        - key: omitted
          type: text-field
          name: Omitted
        - key: term
          type: controlled-term-field
          name: Term
          datatype: iri
          values:
            - type: ontology
              sourceAcronym: DOID
              sourceName: Human Disease Ontology
      """;

  @Test
  public void aSparseYamlInstanceIsStoredComplete() throws IOException {
    String templateId = createTemplate();

    JsonNode stored = createInstance(sparseInstanceYaml(templateId), APPLICATION_YAML);

    assertTrue(stored.has("filled"), "the value the instance carried is stored: " + stored);
    assertEquals("Alice", stored.path("filled").path("@value").asText());
    assertTrue(stored.has("omitted"),
        "the field the instance omitted is materialized, since the JSON form requires it: " + stored);
    assertTrue(stored.path("omitted").path("@value").isNull(),
        "a materialized field is empty, not invented: " + stored.path("omitted"));
  }

  @Test
  public void aSparseJsonInstanceIsStillRefused() throws IOException {
    // The JSON is the server's own: an instance is written, read back, and one field removed from
    // what it returned. Hand-authoring CEDAR JSON-LD would test the fixture's @context and property
    // IRIs rather than the rule under test.
    String templateId = createTemplate();
    JsonNode stored = createInstance(sparseInstanceYaml(templateId), APPLICATION_YAML);
    String instanceId = stored.get(LinkedData.ID).asText();

    ObjectNode withoutTheEmptyField = stored.deepCopy();
    withoutTheEmptyField.remove("omitted");
    Response response = request(instanceUrl(instanceId)).put(Entity.json(withoutTheEmptyField));
    String body = response.readEntity(String.class);

    assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus(),
        "a JSON instance is stored as it was sent, so an omitted field is still missing: " + body);
    assertTrue(body.contains("missing required properties"), body);
  }

  /**
   * How an empty field is written depends on what kind of value the field takes, and the two are not
   * interchangeable. A literal-valued field carries {@code {"@value": null}}. An IRI-valued one — a
   * controlled term, a link, an external identifier — carries {@code {}}, because the key it would
   * otherwise null is {@code @id}, and {@code "@id": null} is not legal JSON-LD: {@code @id} takes
   * an IRI. An empty object is what is left to say "this field is here and holds nothing".
   *
   * <p>Worth pinning down. Completion invents these documents, so a change to the renderer that
   * gave every empty field the same shape would write instances that are invalid as JSON-LD, or
   * that the template's own schema refuses — an empty object will not do for a literal field, whose
   * schema requires {@code @value}.
   */
  @Test
  public void anEmptyFieldIsWrittenAsItsKindAllows() throws IOException {
    String templateId = createTemplate();

    JsonNode stored = createInstance(sparseInstanceYaml(templateId), APPLICATION_YAML);

    JsonNode literal = stored.path("omitted");
    assertTrue(literal.has("@value"), "an empty literal field carries @value: " + literal);
    assertTrue(literal.path("@value").isNull(), "and it is null: " + literal);

    JsonNode iri = stored.path("term");
    assertTrue(iri.isObject(), "the IRI-valued field is present: " + stored);
    assertTrue(iri.isEmpty(), "and empty, since @id can not be null in JSON-LD: " + iri);
  }

  @Test
  public void anUpdateCompletesTheInstanceAndKeepsItsIdentity() throws IOException {
    String templateId = createTemplate();
    JsonNode created = createInstance(sparseInstanceYaml(templateId), APPLICATION_YAML);
    String instanceId = created.get(LinkedData.ID).asText();

    // Read it back as YAML — which drops the empty field again — and write that straight back.
    String sparseAgain = request(instanceUrl(instanceId)).accept(APPLICATION_YAML).get()
        .readEntity(String.class);
    assertFalse(sparseAgain.contains("omitted"),
        "the YAML rendering carries no empty field, which is what makes this a round trip: " + sparseAgain);

    Response updated = request(instanceUrl(instanceId))
        .put(Entity.entity(sparseAgain.replace("Alice", "Alice Smith"), APPLICATION_YAML));
    assertEquals(CedarResponseStatus.OK.getStatusCode(), updated.getStatus());

    JsonNode reread = JsonMapper.MAPPER.readTree(
        request(instanceUrl(instanceId)).get().readEntity(String.class));
    assertEquals(instanceId, reread.get(LinkedData.ID).asText(), "the update keeps the identity");
    assertEquals("Alice Smith", reread.path("filled").path("@value").asText());
    assertTrue(reread.has("omitted"), "the update stores a complete instance: " + reread);
  }

  @Test
  public void anInstanceNamingATemplateTheServerDoesNotHoldIsRefused() throws IOException {
    String unknown = "https://repo.metadatacenter.org/templates/00000000-0000-0000-0000-000000000000";

    Response response = request(instancesUrl())
        .post(Entity.entity(sparseInstanceYaml(unknown), APPLICATION_YAML));

    assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertTrue(response.readEntity(String.class).contains("isBasedOn"),
        "the refusal names the link that could not be followed");
  }

  // Helpers

  private static String sparseInstanceYaml(String templateId) {
    return """
        type: instance
        name: Sparse Instance
        isBasedOn: %s
        children:
          filled:
            value: Alice
        """.formatted(templateId);
  }

  private String createTemplate() throws IOException {
    Response response = request(baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix())
        .post(Entity.entity(TWO_FIELD_TEMPLATE, APPLICATION_YAML));
    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus(),
        "the fixture template could not be created, so the assertions would be meaningless");
    JsonNode created = JsonMapper.MAPPER.readTree(response.readEntity(String.class));
    String id = created.get(LinkedData.ID).asText();
    createdResources.put(id, CedarResourceType.TEMPLATE);
    return id;
  }

  private JsonNode createInstance(String body, String mediaType) throws IOException {
    Response response = request(instancesUrl()).post(Entity.entity(body, mediaType));
    String responseBody = response.readEntity(String.class);
    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus(),
        "a sparse instance must be accepted, got: " + responseBody);
    JsonNode created = JsonMapper.MAPPER.readTree(responseBody);
    createdResources.put(created.get(LinkedData.ID).asText(), CedarResourceType.INSTANCE);
    return created;
  }

  private String instancesUrl() {
    return baseTestUrl + "/" + CedarResourceType.INSTANCE.getPrefix();
  }

  private String instanceUrl(String id) throws UnsupportedEncodingException {
    return instancesUrl() + "/" + URLEncoder.encode(id, "UTF-8");
  }

  private Invocation.Builder request(String url) {
    return testClient.target(url).request().header(AUTHORIZATION, authHeaderTestUser1);
  }
}
