package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.model.CedarNodeType;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static org.metadatacenter.cedar.artifact.resources.utils.TestConstants.*;

@RunWith(JUnitParamsRunner.class)
public class CreateResourceTest extends AbstractResourceCrudTest {

  /**
   * 'CREATE' TESTS
   */

  @Test
  @TestCaseName(TEST_NAME_PATTERN_INDEX_METHOD)
  @Parameters(method = "getCommonParams1")
  public void createResourceTest(JsonNode sampleResource, CedarNodeType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    // If the resource is an instance, we need to set the schema:isBasedOn property to the id of an existing artifact.
    // Otherwise we will get a validation error. So, first we create a artifact and then use its id to create the
    // instance
    sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
    // Service invocation
    Response response = testClient.target(url).request().header("Authorization", authHeader).post(Entity.json
        (sampleResource));
    // Check HTTP response
    Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    createdResources.put(response.readEntity(JsonNode.class).get(LinkedData.ID).asText(), resourceType);
    // Retrieve the resource created
    String location = response.getHeaderString(LOCATION);
    Response findResponse = testClient.target(location).request().header("Authorization", authHeader).get();
    JsonNode expected = sampleResource;
    JsonNode actual = findResponse.readEntity(JsonNode.class);
    // Check that id and provenance information have been generated
    Assert.assertNotEquals(actual.get(LinkedData.ID), null);
    for (String provField : PROV_FIELDS) {
      Assert.assertNotEquals(actual.get(provField), null);
    }
    // Check that all the other fields contain the expected values
    ((ObjectNode) expected).remove(LinkedData.ID);
    ((ObjectNode) actual).remove(LinkedData.ID);
    for (String provField : PROV_FIELDS) {
      ((ObjectNode) expected).remove(provField);
      ((ObjectNode) actual).remove(provField);
    }
    Assert.assertEquals(expected, actual);
  }

  // TODO: Fix the following test
//  @Test
//  @TestCaseName(TEST_NAME_PATTERN_INDEX_METHOD)
//  @Parameters(method = "getCommonParams2")
//  public void createResourceMalformedBodyTest(CedarNodeType resourceType) {
//    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
//    // Post empty json
//    Response response1 = testClient.target(url).request().header("Authorization", authHeader).post(Entity.json(""));
//    // Check HTTP response
//    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response1.getStatus());
//    // Post invalid json
//    Response response2 = testClient.target(url).request().header("Authorization", authHeader).post(Entity.json
// (INVALID_JSON));
//    // Check HTTP response
//    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response2.getStatus());
//  }


  @Test
  @TestCaseName(TEST_NAME_PATTERN_INDEX_METHOD)
  @Parameters(method = "getCommonParams1")
  public void createResourceMissingAuthorizationHeaderTest(JsonNode sampleResource, CedarNodeType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    // Service invocation without Authorization header
    Response response = testClient.target(url).request().post(Entity.json(sampleResource));
    // Check HTTP response
    Assert.assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  @Test
  @TestCaseName(TEST_NAME_PATTERN_INDEX_METHOD)
  @Parameters(method = "getCommonParams1")
  public void createResourceUnauthorizedKeyTest(JsonNode sampleResource, CedarNodeType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    String newAuthHeader = "apiKey " + NON_EXISTENT_API_KEY;
    // Service invocation without unauthorized api key
    Response response = testClient.target(url).request().header("Authorization", newAuthHeader).post(Entity.json
        (sampleResource));
    // Check HTTP response
    Assert.assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

}
