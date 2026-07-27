package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
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
          request().header("Authorization", authHeader).delete();
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

}
