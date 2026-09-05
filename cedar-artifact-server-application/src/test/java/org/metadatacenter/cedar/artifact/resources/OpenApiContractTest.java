package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiContractTest {

  private static final String ARTIFACT = "#/components/schemas/ArtifactDocument";
  private static final String CEDAR_ERROR = "#/components/schemas/CedarError";

  @Test
  void artifactGetsAndListingsPublishOpenJsonLdSchemas() throws IOException {
    JsonNode spec = readSpec();
    for (String path : new String[]{"/templates", "/template-elements", "/template-fields",
        "/template-instances"}) {
      JsonNode response = spec.path("paths").path(path).path("get").path("responses").path("200")
          .path("content").path("application/json").path("schema");
      assertEquals("array", response.path("type").asText(), path);
      assertEquals(ARTIFACT, response.path("items").path("$ref").asText(), path);

      JsonNode single = spec.path("paths").path(path + "/{id}").path("get").path("responses").path("200")
          .path("content");
      assertEquals(ARTIFACT, single.path("application/json").path("schema").path("$ref").asText(), path);
      assertEquals(ARTIFACT, single.path("application/x-yaml").path("schema").path("$ref").asText(), path);
    }

    JsonNode artifact = spec.at("/components/schemas/ArtifactDocument");
    assertTrue(artifact.path("additionalProperties").asBoolean());
    assertTrue(artifact.path("properties").has("@id"));
    assertTrue(artifact.path("properties").has("schema:name"));
  }

  @Test
  void conditionalWritePublishesTheCommonErrorSchema() throws IOException {
    JsonNode spec = readSpec();
    JsonNode response = spec.path("paths").path("/template-elements/{id}").path("put")
        .path("responses").path("412").path("content").path("application/json").path("schema");
    assertEquals(CEDAR_ERROR, response.path("$ref").asText());

    JsonNode error = spec.at("/components/schemas/CedarError");
    assertEquals("object", error.path("type").asText());
    assertTrue(error.path("required").toString().contains("status"));
    assertTrue(error.path("required").toString().contains("statusCode"));
    assertTrue(error.path("properties").path("errorKey").path("enum").isArray());
    assertTrue(error.path("properties").path("errorKey").path("enum").size() > 100);
    assertTrue(error.path("additionalProperties").asBoolean());
  }

  private static JsonNode readSpec() throws IOException {
    try (InputStream input = OpenApiContractTest.class.getResourceAsStream("/assets/swagger-api/swagger.json")) {
      assertNotNull(input, "generated OpenAPI document");
      return JsonMapper.MAPPER.readTree(input);
    }
  }
}
