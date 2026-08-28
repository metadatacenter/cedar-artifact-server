package org.metadatacenter.cedar.artifact.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Instance parsing and controlled-term boundaries, with persisted post-state checks on refusal. */
public class InstancePayloadBoundaryTest extends AbstractRestTest {

  private static final String APPLICATION_YAML = "application/yaml";

  @Test
  public void malformedInstanceJsonIsRejectedWithoutChangingTheStore() {
    int before = countInstances();

    Response response = request(instancesUrl()).post(Entity.entity("{", MediaType.APPLICATION_JSON_TYPE));

    assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals(before, countInstances(), "malformed JSON must not leave an instance behind");
  }

  @Test
  public void anInvalidInstanceIdentifierIsRejectedWithoutChangingTheStore() {
    int before = countInstances();
    String invalid = """
        type: instance
        name: Bad Identifier
        id: definitely not an IRI
        isBasedOn: https://repo.metadatacenter.org/templates/00000000-0000-0000-0000-000000000000
        """;

    Response response = request(instancesUrl()).post(Entity.entity(invalid, APPLICATION_YAML));
    String body = response.readEntity(String.class);

    assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus(), body);
    assertTrue(body.contains("id") || body.contains("URI") || body.contains("IRI"), body);
    assertEquals(before, countInstances(), "an invalid identifier must not leave an instance behind");
  }

  @Test
  public void aControlledTermValueSurvivesTheTemplateInstanceBoundary() throws IOException {
    String template = """
        type: template
        name: Controlled Term Boundary
        children:
          - key: disease
            type: controlled-term-field
            name: Disease
            datatype: iri
            values:
              - type: ontology
                sourceAcronym: DOID
                sourceName: Human Disease Ontology
        """;
    JsonNode createdTemplate = createYamlArtifact(templatesUrl(), template, CedarResourceType.TEMPLATE);
    String templateId = createdTemplate.path(LinkedData.ID).asText();
    String instance = """
        type: instance
        name: Controlled Term Instance
        isBasedOn: %s
        children:
          disease:
            id: http://purl.obolibrary.org/obo/DOID_4
            label: disease
        """.formatted(templateId);

    JsonNode createdInstance = createYamlArtifact(instancesUrl(), instance, CedarResourceType.INSTANCE);
    String instanceId = createdInstance.path(LinkedData.ID).asText();
    assertEquals("http://purl.obolibrary.org/obo/DOID_4",
        createdInstance.path("disease").path(LinkedData.ID).asText());
    assertEquals("disease", createdInstance.path("disease").path("rdfs:label").asText());

    Response yamlResponse = request(instancesUrl() + "/" + encode(instanceId)).accept(APPLICATION_YAML).get();
    assertEquals(CedarResponseStatus.OK.getStatusCode(), yamlResponse.getStatus());
    String rendered = yamlResponse.readEntity(String.class);
    assertTrue(rendered.contains("id: \"http://purl.obolibrary.org/obo/DOID_4\""), rendered);
    assertTrue(rendered.contains("label: \"disease\""), rendered);
  }

  private JsonNode createYamlArtifact(String url, String yaml, CedarResourceType type) throws IOException {
    Response response = request(url).post(Entity.entity(yaml, APPLICATION_YAML));
    String body = response.readEntity(String.class);
    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus(), body);
    JsonNode created = JsonMapper.MAPPER.readTree(body);
    createdResources.put(created.path(LinkedData.ID).asText(), type);
    return created;
  }

  private int countInstances() {
    Response response = request(instancesUrl()).get();
    try {
      assertEquals(CedarResponseStatus.OK.getStatusCode(), response.getStatus());
      return Integer.parseInt(response.getHeaderString(CustomHttpConstants.HEADER_TOTAL_COUNT));
    } finally {
      response.close();
    }
  }

  private String templatesUrl() {
    return baseTestUrl + "/" + CedarResourceType.TEMPLATE.getPrefix();
  }

  private String instancesUrl() {
    return baseTestUrl + "/" + CedarResourceType.INSTANCE.getPrefix();
  }

  private String encode(String value) {
    return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
  }

  private Invocation.Builder request(String url) {
    return testClient.target(url).request().header(AUTHORIZATION, authHeaderTestUser1);
  }
}
