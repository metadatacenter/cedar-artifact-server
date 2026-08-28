package org.metadatacenter.cedar.artifact.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Persisted post-state checks for rejected template and element payloads. */
public class SchemaPayloadPostStateTest extends AbstractRestTest {

  private static final String APPLICATION_YAML = "application/yaml";

  @ParameterizedTest
  @EnumSource(value = CedarResourceType.class, names = {"TEMPLATE", "ELEMENT"})
  public void malformedJsonDoesNotChangeTheSchemaCollection(CedarResourceType type) {
    int before = count(type);

    Response response = request(collectionUrl(type)).post(Entity.entity("{", MediaType.APPLICATION_JSON_TYPE));

    assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals(before, count(type), "malformed JSON must not leave a " + type + " behind");
  }

  @ParameterizedTest
  @EnumSource(value = CedarResourceType.class, names = {"TEMPLATE", "ELEMENT"})
  public void invalidNestedCardinalityDoesNotChangeTheSchemaCollection(CedarResourceType type) {
    int before = count(type);
    String invalid = """
        type: %s
        name: Invalid Nested Bounds
        children:
          - key: impossible
            type: text-field
            name: Impossible Child
            configuration:
              multiple: true
              minItems: 3
              maxItems: 2
        """.formatted(type == CedarResourceType.TEMPLATE ? "template" : "element");

    Response response = request(collectionUrl(type)).post(Entity.entity(invalid, APPLICATION_YAML));
    String body = response.readEntity(String.class);

    assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus(), body);
    assertTrue(body.contains("minItems") && body.contains("maxItems"), body);
    assertEquals(before, count(type), "invalid nested cardinality must not leave a " + type + " behind");
  }

  @ParameterizedTest
  @EnumSource(value = CedarResourceType.class, names = {"TEMPLATE", "ELEMENT"})
  public void aMinimalSchemaArtifactCrossesTheYamlJsonBoundary(CedarResourceType type) throws IOException {
    String kind = type == CedarResourceType.TEMPLATE ? "template" : "element";
    String name = type == CedarResourceType.TEMPLATE ? "Minimal Template" : "Minimal Element";
    String yaml = "type: " + kind + "\nname: " + name + "\n";

    Response response = request(collectionUrl(type)).post(Entity.entity(yaml, APPLICATION_YAML));
    String body = response.readEntity(String.class);
    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus(), body);

    JsonNode created = JsonMapper.MAPPER.readTree(body);
    String id = created.path(LinkedData.ID).asText();
    createdResources.put(id, type);
    assertEquals(name, created.path("schema:name").asText());
    assertTrue(created.path(LinkedData.TYPE).asText().endsWith(
        type == CedarResourceType.TEMPLATE ? "/Template" : "/TemplateElement"), created.toString());
  }

  private int count(CedarResourceType type) {
    Response response = request(collectionUrl(type)).get();
    try {
      assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
      return Integer.parseInt(response.getHeaderString(CustomHttpConstants.HEADER_TOTAL_COUNT));
    } finally {
      response.close();
    }
  }

  private String collectionUrl(CedarResourceType type) {
    return baseTestUrl + "/" + type.getPrefix();
  }

  private Invocation.Builder request(String url) {
    return testClient.target(url).request().header(AUTHORIZATION, authHeaderTestUser1);
  }
}
