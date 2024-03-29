package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.metadatacenter.cedar.artifact.resources.utils.TestParameterUtil;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;

import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.metadatacenter.cedar.artifact.resources.utils.TestConstants.*;

@RunWith(JUnitParamsRunner.class)
public class FindResourceTest extends AbstractResourceCrudTest {

  /**
   * 'FIND' TESTS
   */

  @Test
  @TestCaseName(TEST_NAME_PATTERN_INDEX_METHOD)
  @Parameters(method = "getCommonParams1")
  public void findResourceTest(JsonNode sampleResource, CedarResourceType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    // If the artifact is an instance, we need to set the schema:isBasedOn property to the id of an existing artifact.
    // Otherwise we will get a validation error. So, first we create a artifact and then use its id to create the
    // instance
    sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
    // Create a artifact
    Response response =
        testClient.target(url).request().header("Authorization", authHeader).post(Entity.json(sampleResource));
    JsonNode expected = response.readEntity(JsonNode.class);
    createdResources.put(expected.get(LinkedData.ID).asText(), resourceType);
    // Use generated id to retrieve the artifact
    String id = expected.get(LinkedData.ID).asText();
    String findUrl = null;
    findUrl = url + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8);
    // Service invocation - Find by Id
    Response findResponse = testClient.target(findUrl).request().header("Authorization", authHeader).get();
    // Check response is OK
    Assert.assertEquals(CedarResponseStatus.OK.getStatusCode(), findResponse.getStatus());
    // Check the element retrieved
    JsonNode actual = findResponse.readEntity(JsonNode.class);
    Assert.assertEquals(expected, actual);
  }

  @Test
  @TestCaseName(TEST_NAME_PATTERN_INDEX_METHOD)
  @Parameters
  public void findNonExistentResourceTest(CedarResourceType resourceType, String nonExistentResourceId) {
    String findUrl = null;
    findUrl = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType) + "/" + URLEncoder.encode(nonExistentResourceId, StandardCharsets.UTF_8);
    // Service invocation - Find by Id
    Response findResponse = testClient.target(findUrl).request().header("Authorization", authHeader).get();
    // Check response
    Assert.assertEquals(CedarResponseStatus.NOT_FOUND.getStatusCode(), findResponse.getStatus());
  }

  private Object parametersForFindNonExistentResourceTest() {
    return new Object[]{
        new Object[]{CedarResourceType.TEMPLATE, NON_EXISTENT_TEMPLATE_ID},
        new Object[]{CedarResourceType.ELEMENT, NON_EXISTENT_ELEMENT_ID},
        new Object[]{CedarResourceType.INSTANCE, NON_EXISTENT_INSTANCE_ID}
    };
  }

  @Test
  @TestCaseName(TEST_NAME_PATTERN_INDEX_METHOD)
  @Parameters
  public void findInvalidIdTest(CedarResourceType resourceType, String invalidId) {
    String findUrl = null;
    findUrl = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType) + "/" + URLEncoder.encode(invalidId, StandardCharsets.UTF_8);
    // Service invocation - Find by Id
    Response findResponse = testClient.target(findUrl).request().header("Authorization", authHeader).get();
    // Check response
    Assert.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
  }

  private Object parametersForFindInvalidIdTest() {
    return new Object[]{
        new Object[]{CedarResourceType.TEMPLATE, INVALID_ID},
        new Object[]{CedarResourceType.ELEMENT, INVALID_ID},
        new Object[]{CedarResourceType.INSTANCE, INVALID_ID}
    };
  }

  @Test
  @TestCaseName(TEST_NAME_PATTERN_INDEX_METHOD)
  @Parameters(method = "getCommonParams1")
  public void findResourceMissingAuthorizationHeaderTest(JsonNode sampleResource, CedarResourceType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    // If the artifact is an instance, we need to set the schema:isBasedOn property to the id of an existing artifact.
    // Otherwise we will get a validation error. So, first we create a artifact and then use its id to create the
    // instance
    sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
    // Create a artifact
    Response response =
        testClient.target(url).request().header("Authorization", authHeader).post(Entity.json(sampleResource));
    JsonNode expected = response.readEntity(JsonNode.class);
    createdResources.put(expected.get(LinkedData.ID).asText(), resourceType);
    // Use generated id to retrieve the artifact
    String id = expected.get(LinkedData.ID).asText();
    String findUrl = null;
    findUrl = url + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8);
    // Service invocation - Find by Id - missing Authorization header
    Response findResponse = testClient.target(findUrl).request().get();
    // Check response
    Assert.assertEquals(CedarResponseStatus.UNAUTHORIZED.getStatusCode(), findResponse.getStatus());
  }

  @Test
  @TestCaseName(TEST_NAME_PATTERN_INDEX_METHOD)
  @Parameters(method = "getCommonParams1")
  public void findResourceUnauthorizedKeyTest(JsonNode sampleResource, CedarResourceType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    // If the artifact is an instance, we need to set the schema:isBasedOn property to the id of an existing artifact.
    // Otherwise we will get a validation error. So, first we create a artifact and then use its id to create the
    // instance
    sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
    // Create a artifact
    Response response =
        testClient.target(url).request().header("Authorization", authHeader).post(Entity.json(sampleResource));
    JsonNode expected = response.readEntity(JsonNode.class);
    createdResources.put(expected.get(LinkedData.ID).asText(), resourceType);
    // Use generated id to retrieve the artifact
    String id = expected.get(LinkedData.ID).asText();
    String findUrl = null;
    findUrl = url + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8);
    // Service invocation - Find by Id - unauthorized user
    String authHeader = "apiKey " + NON_EXISTENT_API_KEY;
    Response findResponse = testClient.target(findUrl).request().header("Authorization", authHeader).get();
    // Check response
    Assert.assertEquals(CedarResponseStatus.UNAUTHORIZED.getStatusCode(), findResponse.getStatus());
  }

  /**
   * 'FIND ALL RESOURCES' TESTS
   */

  @Test
  @TestCaseName(TEST_NAME_PATTERN_INDEX_METHOD + " limit={2}, offset={3}, summary={4}")
  @Parameters
  public void findAllResourcesTest(CedarResourceType resourceType, JsonNode sampleResource, String limit,
                                   String offset, String summary) {
    int initialCount = countResources(resourceType);
    final int CREATE_RESOURCES_COUNT = 3; // number of resources to be created
    // create resources
    List<JsonNode> resources = new ArrayList<>();
    for (int i = 0; i < CREATE_RESOURCES_COUNT; i++) {
      // If the artifact is an instance, we need to set the schema:isBasedOn property to the id of an existing artifact.
      // Otherwise we will get a validation error. So, first we create a artifact and then use its id to create the
      // instance
      sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
      // Create a artifact
      JsonNode createdResource = null;
      createdResource = createResource(sampleResource, resourceType);
      createdResources.put(createdResource.get(LinkedData.ID).asText(), resourceType);
      resources.add(createdResource);
    }
    // find All
    try {
      String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
      URIBuilder b = new URIBuilder(url);
      if (!limit.isEmpty()) {
        b.addParameter("limit", limit);
      }
      if (!offset.isEmpty()) {
        b.addParameter("offset", offset);
      }
      if (!summary.isEmpty()) {
        b.addParameter("summary", summary);
      }
      String findAllUrl = b.build().toString();
      log.info("URL: " + url);
      Response findAllResponse = testClient.target(findAllUrl).request().header("Authorization", authHeader).get();
      // Check response is OK
      Assert.assertEquals(CedarResponseStatus.OK.getStatusCode(), findAllResponse.getStatus());
      // Check headers
      Assert.assertNotNull(findAllResponse.getHeaderString(CustomHttpConstants.HEADER_TOTAL_COUNT));
      int currentCount = Integer.parseInt(findAllResponse.getHeaderString(CustomHttpConstants.HEADER_TOTAL_COUNT));
      Assert.assertNotNull(findAllResponse.getHeaderString(HttpConstants.HTTP_HEADER_LINK));
      // Check the number of resources created
      int expectedCount = initialCount + CREATE_RESOURCES_COUNT;
      Assert.assertTrue("Expected total count specified in header is wrong", expectedCount == currentCount);
      JsonNode findAllJsonResponse = findAllResponse.readEntity(JsonNode.class);
      // Check the number of elements retrieved
      List<JsonNode> actual = new ArrayList<>();
      for (JsonNode r : findAllJsonResponse) {
        actual.add(r);
      }
      int expectedSize;
      if (!limit.isEmpty()) {
        expectedSize = Math.min(expectedCount, Integer.parseInt(limit));
      } else {
        expectedSize = Math.min(expectedCount,
            TestUtil.cedarConfig.getArtifactRESTAPI().getPagination().getDefaultPageSize());
      }
      Assert.assertEquals(expectedSize, actual.size());
      // Check the elements retrieved. This check is currently limited to the first page of results, and it is done
      // only if the number of returned resources is big enough to contain all created resources.
      if (initialCount + CREATE_RESOURCES_COUNT == actual.size()) {
        List<JsonNode> expected = resources;
        if (summary.compareTo("true") != 0) {
          Assert.assertTrue(actual.containsAll(expected));
        } else {
          Assert.assertFalse(actual.containsAll(expected));
        }
      }
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  private Object parametersForFindAllResourcesTest() {
    List<Object> p1p2Values = Arrays.asList(
        Arrays.asList(CedarResourceType.TEMPLATE, sampleTemplate),
        Arrays.asList(CedarResourceType.ELEMENT, sampleElement),
        Arrays.asList(CedarResourceType.INSTANCE, sampleInstance));
    List<Object> limitValues = Arrays.asList(List.of(""), List.of("2"), List.of("50"));
    List<Object> offsetValues = Arrays.asList(List.of(""), List.of("0"));
    List<Object> summaryValues = Arrays.asList(List.of(""), List.of("true"), List.of("false"));
    return TestParameterUtil.getParameterPermutations(Arrays.asList(p1p2Values, limitValues, offsetValues, summaryValues));
  }


}
