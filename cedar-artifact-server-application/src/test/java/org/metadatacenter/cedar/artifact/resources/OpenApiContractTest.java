package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiContractTest {

  private static final String SCHEMA_ARTIFACT = "#/components/schemas/SchemaArtifactDocument";
  private static final String INSTANCE_ARTIFACT = "#/components/schemas/InstanceArtifactDocument";
  private static final String CEDAR_ERROR = "#/components/schemas/CedarError";

  @Test
  void artifactRoutesPublishTheSchemaSharedWithTheOtherArtifactServices() throws IOException {
    JsonNode spec = readSpec();
    for (String path : new String[]{"/templates", "/template-elements", "/template-fields"}) {
      assertArtifactRoutes(spec, path, SCHEMA_ARTIFACT);
    }
    assertArtifactRoutes(spec, "/template-instances", INSTANCE_ARTIFACT);
  }

  @Test
  void theArtifactSchemasStayOpenAndCarryTheModelKeys() throws IOException {
    JsonNode spec = readSpec();
    for (String name : new String[]{"ArtifactDocument", "SchemaArtifactDocument", "InstanceArtifactDocument"}) {
      JsonNode artifact = spec.at("/components/schemas/" + name);
      assertTrue(artifact.path("additionalProperties").asBoolean(), name + " stays open");
      assertTrue(artifact.path("properties").has("@id"), name + " documents @id");
      assertTrue(artifact.path("properties").has("schema:name"), name + " documents schema:name");
    }
    JsonNode schemaArtifact = spec.at("/components/schemas/SchemaArtifactDocument");
    assertTrue(schemaArtifact.path("properties").path("bibo:status").path("enum").isArray(),
        "a schema artifact publishes its publication statuses");
    assertTrue(schemaArtifact.path("properties").has("_ui"), "a schema artifact documents its UI directives");
    assertTrue(spec.at("/components/schemas/InstanceArtifactDocument").path("properties")
        .has("schema:isBasedOn"), "an instance documents the template it is based on");
  }

  @Test
  void validationReportsAndTheirRequestsAreDescribed() throws IOException {
    JsonNode spec = readSpec();
    JsonNode validate = spec.path("paths").path("/command/validate").path("post");
    assertEquals("#/components/schemas/ValidationTarget",
        validate.path("requestBody").path("content").path("application/json").path("schema").path("$ref").asText());
    assertEquals("#/components/schemas/ArtifactDocument",
        validate.path("requestBody").path("content").path("application/x-yaml").path("schema").path("$ref").asText(),
        "a YAML body is read as the artifact itself");
    assertEquals("#/components/schemas/ValidationReport",
        validate.path("responses").path("200").path("content").path("application/json").path("schema").path("$ref").asText());

    JsonNode report = spec.at("/components/schemas/ValidationReport");
    assertTrue(report.path("properties").has("validates"),
        "the documented key is the one Jackson writes, which the interface's getter does not name");
    assertEquals("[\"true\",\"false\"]",
        report.path("properties").path("validates").path("enum").toString().replace(" ", ""));
    assertTrue(report.path("properties").path("errors").path("items").path("$ref").asText().endsWith("ErrorItem"));
    assertTrue(report.path("properties").path("warnings").path("items").path("$ref").asText().endsWith("WarningItem"));
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
    // The envelope carries only its declared fields: nothing emits an undeclared one.
    assertFalse(error.path("additionalProperties").asBoolean());
  }

  /**
   * Every representation the route negotiates carries the same schema: JSON and YAML are two
   * serializations of one artifact, so a client generated from either describes the same document.
   */
  private static void assertArtifactRoutes(JsonNode spec, String path, String schemaRef) {
    JsonNode listing = spec.path("paths").path(path).path("get").path("responses").path("200")
        .path("content").path("application/json").path("schema");
    assertEquals("array", listing.path("type").asText(), path);
    assertEquals(schemaRef, listing.path("items").path("$ref").asText(), path);

    JsonNode created = spec.path("paths").path(path).path("post");
    assertRepresentations(created.path("requestBody"), schemaRef, "POST " + path + " request body");
    assertRepresentations(created.path("responses").path("201"), schemaRef, "POST " + path + " 201");

    String item = path + "/{id}";
    assertRepresentations(spec.path("paths").path(item).path("get").path("responses").path("200"),
        schemaRef, "GET " + item + " 200");
    JsonNode replaced = spec.path("paths").path(item).path("put");
    assertRepresentations(replaced.path("requestBody"), schemaRef, "PUT " + item + " request body");
    assertRepresentations(replaced.path("responses").path("200"), schemaRef, "PUT " + item + " 200");
    assertRepresentations(replaced.path("responses").path("201"), schemaRef, "PUT " + item + " 201");
  }

  private static void assertRepresentations(JsonNode payload, String schemaRef, String coordinate) {
    for (String mediaType : new String[]{"application/json", "application/x-yaml", "application/yaml"}) {
      assertEquals(schemaRef, payload.path("content").path(mediaType).path("schema").path("$ref").asText(),
          coordinate + " (" + mediaType + ")");
    }
  }

  private static JsonNode readSpec() throws IOException {
    try (InputStream input = readSpecStream()) {
      assertNotNull(input, "generated OpenAPI document");
      return JsonMapper.STRICT_MAPPER.readTree(input);
    }
  }

  private static InputStream readSpecStream() {
    return OpenApiContractTest.class.getResourceAsStream("/assets/swagger-api/swagger.json");
  }
}
