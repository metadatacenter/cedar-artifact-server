package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;

import java.io.IOException;
import java.net.URLEncoder;


public class DeleteResourceTest extends AbstractResourceCrudTest {

  /**
   * 'DELETE' TESTS
   */

  @ParameterizedTest
  @MethodSource("getCommonParams1")
  public void deleteResourceTest(JsonNode sampleResource, CedarResourceType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
    // Create a artifact
    try {
      JsonNode createdResource = createResource(sampleResource, resourceType);
      createdResources.put(createdResource.get(LinkedData.ID).asText(), resourceType);
      String createdResourceId = createdResource.get("@id").asText();
      // Service invocation - Delete
      Response responseUpdate = testClient.target(url + "/" + URLEncoder.encode(createdResourceId, "UTF-8")).
          request().header("Authorization", authHeader).header("If-Match", "\"1\"").delete();
      // Check HTTP response
      Assertions.assertEquals(CedarResponseStatus.NO_CONTENT.getStatusCode(), responseUpdate.getStatus());
      // Check that the artifact has been deleted
      Response responseFind = testClient.target(url + "/" + URLEncoder.encode(createdResourceId, "UTF-8")).
          request().header("Authorization", authHeader).get();
      Assertions.assertEquals(CedarResponseStatus.NOT_FOUND.getStatusCode(), responseFind.getStatus());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @ParameterizedTest
  @MethodSource("getCommonParams1")
  void staleDeleteCannotRemoveANewerArtifact(JsonNode sampleResource, CedarResourceType resourceType)
      throws Exception {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
    JsonNode created = createResource(sampleResource, resourceType);
    String id = created.get(LinkedData.ID).asText();
    createdResources.put(id, resourceType);
    String resourceUrl = url + "/" + URLEncoder.encode(id, "UTF-8");

    ((com.fasterxml.jackson.databind.node.ObjectNode) created).put("schema:name", "newer revision");
    Response update = testClient.target(resourceUrl).request()
        .header("Authorization", authHeader)
        .header("If-Match", "\"1\"")
        .put(Entity.json(created));
    Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), update.getStatus());
    Assertions.assertEquals("\"2\"", update.getHeaderString("ETag"));

    Response staleDelete = testClient.target(resourceUrl).request()
        .header("Authorization", authHeader)
        .header("If-Match", "\"1\"")
        .delete();
    Assertions.assertEquals(CedarResponseStatus.PRECONDITION_FAILED.getStatusCode(), staleDelete.getStatus());

    Response current = testClient.target(resourceUrl).request()
        .header("Authorization", authHeader).get();
    Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), current.getStatus());
    Assertions.assertEquals("\"2\"", current.getHeaderString("ETag"));

    Response forcedDelete = testClient.target(resourceUrl).request()
        .header("Authorization", authHeader)
        .header("If-Match", "*")
        .delete();
    Assertions.assertEquals(CedarResponseStatus.NO_CONTENT.getStatusCode(), forcedDelete.getStatus());
  }

  @ParameterizedTest
  @MethodSource("getCommonParams1")
  void deleteRequiresIfMatch(JsonNode sampleResource, CedarResourceType resourceType) throws Exception {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
    JsonNode created = createResource(sampleResource, resourceType);
    String id = created.get(LinkedData.ID).asText();
    createdResources.put(id, resourceType);
    String resourceUrl = url + "/" + URLEncoder.encode(id, "UTF-8");

    Response missing = testClient.target(resourceUrl).request()
        .header("Authorization", authHeader).delete();
    Assertions.assertEquals(CedarResponseStatus.PRECONDITION_REQUIRED.getStatusCode(), missing.getStatus());
    Response stillPresent = testClient.target(resourceUrl).request()
        .header("Authorization", authHeader).get();
    Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), stillPresent.getStatus());

    Response cleanup = testClient.target(resourceUrl).request()
        .header("Authorization", authHeader).header("If-Match", "*").delete();
    Assertions.assertEquals(CedarResponseStatus.NO_CONTENT.getStatusCode(), cleanup.getStatus());
  }

}
