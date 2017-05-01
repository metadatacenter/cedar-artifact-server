package org.metadatacenter.cedar.template.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.metadatacenter.cedar.template.resources.utils.TestUtil;
import org.metadatacenter.model.CedarNodeType;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLEncoder;

import static org.metadatacenter.cedar.template.resources.utils.TestConstants.*;

@RunWith(JUnitParamsRunner.class)
public class DeleteResourceTest extends AbstractResourceCrudTest {

  /**
   * 'DELETE' TESTS
   */

  @Test
  @TestCaseName(TEST_NAME_PATTERN)
  @Parameters(method = "getCommonParams1")
  public void deleteResourceTest(JsonNode sampleResource, CedarNodeType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
    // Create a resource
    try {
      JsonNode createdResource = createResource(sampleResource, resourceType);
      createdResources.put(createdResource.get(ID_FIELD).asText(), resourceType);
      String createdResourceId = createdResource.get("@id").asText();
      // Service invocation - Delete
      Response responseUpdate = testClient.target(url + "/" + URLEncoder.encode(createdResourceId, "UTF-8")).
          request().header("Authorization", authHeader).delete();
      // Check HTTP response
      Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), responseUpdate.getStatus());
      // Check that the resource has been deleted
      Response responseFind = testClient.target(url + "/" + URLEncoder.encode(createdResourceId, "UTF-8")).
          request().header("Authorization", authHeader).get();
      Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), responseFind.getStatus());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
