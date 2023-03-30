package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.model.CedarResourceType;

import java.io.IOException;
import java.net.URLEncoder;

import static org.metadatacenter.cedar.artifact.resources.utils.TestConstants.TEST_NAME_PATTERN_INDEX_METHOD;

@RunWith(JUnitParamsRunner.class)
public class DeleteResourceTest extends AbstractResourceCrudTest {

  /**
   * 'DELETE' TESTS
   */

  @Test
  @TestCaseName(TEST_NAME_PATTERN_INDEX_METHOD)
  @Parameters(method = "getCommonParams1")
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
      Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), responseUpdate.getStatus());
      // Check that the artifact has been deleted
      Response responseFind = testClient.target(url + "/" + URLEncoder.encode(createdResourceId, "UTF-8")).
          request().header("Authorization", authHeader).get();
      Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), responseFind.getStatus());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
