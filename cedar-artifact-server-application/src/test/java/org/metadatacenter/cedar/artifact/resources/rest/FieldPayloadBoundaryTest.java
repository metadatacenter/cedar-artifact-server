package org.metadatacenter.cedar.artifact.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Field endpoint boundaries that the older template/element matrices never exercised directly.
 * Each refusal checks the collection count as well as the response, so a 400 cannot hide a write
 * that happened before parsing or model validation failed.
 */
public class FieldPayloadBoundaryTest extends AbstractRestTest {

  private static final String APPLICATION_YAML = "application/yaml";

  @Test
  public void aMinimalFieldKeepsItsIdentityAcrossAYamlJsonYamlRoundTrip() throws IOException {
    Response createdResponse = request(fieldsUrl())
        .post(Entity.entity("type: text-field\nname: Minimal Field\n", APPLICATION_YAML));
    String createdBody = createdResponse.readEntity(String.class);
    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), createdResponse.getStatus(), createdBody);

    JsonNode created = JsonMapper.STRICT_MAPPER.readTree(createdBody);
    String id = created.path(LinkedData.ID).asText();
    createdResources.put(id, CedarResourceType.FIELD);
    assertEquals("Minimal Field", created.path("schema:name").asText());
    assertEquals("textfield", created.path("_ui").path("inputType").asText());

    String url = fieldUrl(id);
    Response yamlResponse = request(url).accept(APPLICATION_YAML).get();
    assertEquals(CedarResponseStatus.OK.getStatusCode(), yamlResponse.getStatus());
    String yaml = yamlResponse.readEntity(String.class);
    assertTrue(yaml.startsWith("type: text-field"), yaml);

    Response updatedResponse = request(url).header("If-Match", currentEtag(url, authHeaderTestUser1))
        .put(Entity.entity(yaml.replaceFirst("(?m)^name:.*$", "name: Renamed Field"), APPLICATION_YAML));
    String updatedBody = updatedResponse.readEntity(String.class);
    assertEquals(CedarResponseStatus.OK.getStatusCode(), updatedResponse.getStatus(), updatedBody);

    JsonNode updated = JsonMapper.STRICT_MAPPER.readTree(updatedBody);
    assertEquals(id, updated.path(LinkedData.ID).asText());
    assertEquals("Renamed Field", updated.path("schema:name").asText());
  }

  /*
   * A fractional number is read as a BigDecimal and stored as a Decimal128. Since release 2.9.25 it came back
   * from the store as {"$numberDecimal": "1E-7"}, an object where the field had a number, so a field saved with
   * fractional bounds no longer validated and could not be opened. It must read back as it was written, as JSON
   * from the response and from a fresh read, and as YAML.
   */
  @Test
  public void fractionalBoundsReadBackAsTheNumbersThatWereWritten() throws IOException {
    Response createdResponse = request(fieldsUrl())
        .post(Entity.entity("type: numeric-field\nname: Rate\ndatatype: xsd:decimal\n", APPLICATION_YAML));
    String createdBody = createdResponse.readEntity(String.class);
    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), createdResponse.getStatus(), createdBody);
    JsonNode created = JsonMapper.STRICT_MAPPER.readTree(createdBody);
    String id = created.path(LinkedData.ID).asText();
    createdResources.put(id, CedarResourceType.FIELD);

    String url = fieldUrl(id);
    ObjectNode edited = (ObjectNode) JsonMapper.STRICT_MAPPER.readTree(request(url).get().readEntity(String.class));
    ((ObjectNode) edited.path("_valueConstraints"))
        .setAll((ObjectNode) JsonMapper.STRICT_MAPPER.readTree("{\"minValue\": 1e-07, \"maxValue\": 0.5}"));
    Response updatedResponse = request(url).header("If-Match", currentEtag(url, authHeaderTestUser1))
        .put(Entity.json(edited.toString()));
    String updatedBody = updatedResponse.readEntity(String.class);
    assertEquals(CedarResponseStatus.OK.getStatusCode(), updatedResponse.getStatus(), updatedBody);

    for (JsonNode read : new JsonNode[] {JsonMapper.STRICT_MAPPER.readTree(updatedBody),
        JsonMapper.STRICT_MAPPER.readTree(request(url).get().readEntity(String.class))}) {
      JsonNode constraints = read.path("_valueConstraints");
      assertTrue(constraints.path("minValue").isNumber(), read.toString());
      assertEquals(0, constraints.path("minValue").decimalValue().compareTo(new BigDecimal("1E-7")));
      assertEquals(0, constraints.path("maxValue").decimalValue().compareTo(new BigDecimal("0.5")));
      assertFalse(read.toString().contains("$numberDecimal"), read.toString());
    }
    Response yamlResponse = request(url).accept(APPLICATION_YAML).get();
    String yaml = yamlResponse.readEntity(String.class);
    assertEquals(CedarResponseStatus.OK.getStatusCode(), yamlResponse.getStatus(), yaml);
    assertFalse(yaml.contains("numberDecimal"), yaml);
  }

  @Test
  public void malformedFieldYamlIsRejectedWithoutChangingTheStore() {
    int before = countFields();

    Response response = request(fieldsUrl())
        .post(Entity.entity("type: text-field\n  name: [unclosed\n", APPLICATION_YAML));

    assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals(before, countFields(), "malformed YAML must not leave a field behind");
  }

  @Test
  public void invalidFieldCardinalityIsRejectedWithoutChangingTheStore() {
    int before = countFields();
    String invalid = """
        type: text-field
        name: Impossible Bounds
        configuration:
          multiple: true
          minItems: 2
          maxItems: 1
        """;

    Response response = request(fieldsUrl()).post(Entity.entity(invalid, APPLICATION_YAML));
    String body = response.readEntity(String.class);

    assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus(), body);
    assertTrue(body.contains("minItems") && body.contains("maxItems"), body);
    assertEquals(before, countFields(), "invalid cardinality must not leave a field behind");
  }

  @Test
  public void controlledTermConstraintsSurviveTheFieldEndpoint() throws IOException {
    String controlledTerm = """
        type: controlled-term-field
        name: Disease
        datatype: iri
        values:
          - type: ontology
            sourceAcronym: DOID
            sourceName: Human Disease Ontology
        """;

    Response response = request(fieldsUrl()).post(Entity.entity(controlledTerm, APPLICATION_YAML));
    String body = response.readEntity(String.class);
    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus(), body);

    JsonNode created = JsonMapper.STRICT_MAPPER.readTree(body);
    String id = created.path(LinkedData.ID).asText();
    createdResources.put(id, CedarResourceType.FIELD);
    assertTrue(created.path("_valueConstraints").toString().contains("DOID"), created.toString());

    String rendered = request(fieldUrl(id)).accept(APPLICATION_YAML).get().readEntity(String.class);
    assertTrue(rendered.contains("type: controlled-term-field"), rendered);
    assertTrue(rendered.contains("sourceAcronym: \"DOID\""), rendered);
    assertTrue(rendered.contains("sourceName: \"Human Disease Ontology\""), rendered);
  }

  private int countFields() {
    Response response = request(fieldsUrl()).get();
    try {
      assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
      return response.readEntity(JsonNode.class).get("totalCount").asInt();
    } finally {
      response.close();
    }
  }

  private String fieldsUrl() {
    return baseTestUrl + "/" + CedarResourceType.FIELD.getPrefix();
  }

  private String fieldUrl(String id) {
    return fieldsUrl() + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8);
  }

  private Invocation.Builder request(String url) {
    return testClient.target(url).request().header(AUTHORIZATION, authHeaderTestUser1);
  }
}
