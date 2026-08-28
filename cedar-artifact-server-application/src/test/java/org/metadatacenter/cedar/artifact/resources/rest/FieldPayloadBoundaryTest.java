package org.metadatacenter.cedar.artifact.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    JsonNode created = JsonMapper.MAPPER.readTree(createdBody);
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

    JsonNode updated = JsonMapper.MAPPER.readTree(updatedBody);
    assertEquals(id, updated.path(LinkedData.ID).asText());
    assertEquals("Renamed Field", updated.path("schema:name").asText());
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

    JsonNode created = JsonMapper.MAPPER.readTree(body);
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
      return Integer.parseInt(response.getHeaderString(CustomHttpConstants.HEADER_TOTAL_COUNT));
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
