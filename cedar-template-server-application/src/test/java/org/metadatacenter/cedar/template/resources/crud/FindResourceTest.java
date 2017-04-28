package org.metadatacenter.cedar.template.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.metadatacenter.cedar.template.resources.utils.TestConstants.*;

@RunWith(JUnitParamsRunner.class)
public class FindResourceTest extends AbstractResourceCrudTest {

  /**
   * 'FIND' TESTS
   */

  @Test
  @TestCaseName(TEST_NAME_PATTERN)
  @Parameters(method = "getCommonParams1")
  public void findResourceTest(JsonNode sampleResource, CedarNodeType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    // If the resource is an instance, we need to set the schema:isBasedOn property to the id of an existing template.
    // Otherwise we will get a validation error. So, first we create a template and then use its id to create the instance
    sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
    // Create a resource
    Response response = testClient.target(url).request().header("Authorization", authHeader).post(Entity.json(sampleResource));
    JsonNode expected = response.readEntity(JsonNode.class);
    createdResources.put(expected.get(ID_FIELD).asText(), resourceType);
    // Use generated id to retrieve the resource
    String id = expected.get(ID_FIELD).asText();
    String findUrl = null;
    try {
      findUrl = url + "/" + URLEncoder.encode(id, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    };
    // Service invocation - Find by Id
    Response findResponse = testClient.target(findUrl).request().header("Authorization", authHeader).get();
    // Check response is OK
    Assert.assertEquals(Response.Status.OK.getStatusCode(), findResponse.getStatus());
    // Check the element retrieved
    JsonNode actual = findResponse.readEntity(JsonNode.class);
    Assert.assertEquals(expected, actual);
  }

  @Test
  @TestCaseName(TEST_NAME_PATTERN)
  @Parameters
  public void findNonExistentResourceTest(CedarNodeType resourceType, String nonExistentResourceId) {
    String findUrl = null;
    try {
      findUrl = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType) + "/" + URLEncoder.encode(nonExistentResourceId, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    };
    // Service invocation - Find by Id
    Response findResponse = testClient.target(findUrl).request().header("Authorization", authHeader).get();
    // Check response
    Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
  }
  private Object parametersForFindNonExistentResourceTest() {
    return new Object[]{
        new Object[]{CedarNodeType.TEMPLATE, NON_EXISTENT_TEMPLATE_ID},
        new Object[]{CedarNodeType.ELEMENT, NON_EXISTENT_ELEMENT_ID},
        new Object[]{CedarNodeType.INSTANCE, NON_EXISTENT_INSTANCE_ID}
    };
  }

  @Test
  @TestCaseName(TEST_NAME_PATTERN)
  @Parameters
  public void findInvalidIdTest(CedarNodeType resourceType, String invalidId) {
    String findUrl = null;
    try {
      findUrl = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType) + "/" + URLEncoder.encode(invalidId, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    };
    // Service invocation - Find by Id
    Response findResponse = testClient.target(findUrl).request().header("Authorization", authHeader).get();
    // Check response
    Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), findResponse.getStatus());
  }
  private Object parametersForFindInvalidIdTest() {
    return new Object[]{
        new Object[]{CedarNodeType.TEMPLATE, INVALID_ID},
        new Object[]{CedarNodeType.ELEMENT, INVALID_ID},
        new Object[]{CedarNodeType.INSTANCE, INVALID_ID}
    };
  }

  @Test
  @TestCaseName(TEST_NAME_PATTERN)
  @Parameters(method = "getCommonParams1")
  public void findResourceMissingAuthorizationHeaderTest(JsonNode sampleResource, CedarNodeType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    // If the resource is an instance, we need to set the schema:isBasedOn property to the id of an existing template.
    // Otherwise we will get a validation error. So, first we create a template and then use its id to create the instance
    sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
    // Create a resource
    Response response = testClient.target(url).request().header("Authorization", authHeader).post(Entity.json(sampleResource));
    JsonNode expected = response.readEntity(JsonNode.class);
    createdResources.put(expected.get(ID_FIELD).asText(), resourceType);
    // Use generated id to retrieve the resource
    String id = expected.get(ID_FIELD).asText();
    String findUrl = null;
    try {
      findUrl = url + "/" + URLEncoder.encode(id, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    };
    // Service invocation - Find by Id - missing Authorization header
    Response findResponse = testClient.target(findUrl).request().get();
    // Check response
    Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), findResponse.getStatus());
  }

  @Test
  @TestCaseName(TEST_NAME_PATTERN)
  @Parameters(method = "getCommonParams1")
  public void findResourceUnauthorizedKeyTest(JsonNode sampleResource, CedarNodeType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    // If the resource is an instance, we need to set the schema:isBasedOn property to the id of an existing template.
    // Otherwise we will get a validation error. So, first we create a template and then use its id to create the instance
    sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
    // Create a resource
    Response response = testClient.target(url).request().header("Authorization", authHeader).post(Entity.json(sampleResource));
    JsonNode expected = response.readEntity(JsonNode.class);
    createdResources.put(expected.get(ID_FIELD).asText(), resourceType);
    // Use generated id to retrieve the resource
    String id = expected.get(ID_FIELD).asText();
    String findUrl = null;
    try {
      findUrl = url + "/" + URLEncoder.encode(id, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    };
    // Service invocation - Find by Id - unauthorized user
    String authHeader = "apiKey " + NON_EXISTENT_API_KEY;
    Response findResponse = testClient.target(findUrl).request().header("Authorization", authHeader).get();
    // Check response
    Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), findResponse.getStatus());
  }

  /**
   * 'FIND ALL RESOURCES' TESTS
   */






}
