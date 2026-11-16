package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.apache.hc.core5.net.URIBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.metadatacenter.cedar.artifact.resources.utils.TestConstants.*;

public class FindResourceTest extends AbstractResourceCrudTest {

  /**
   * 'FIND' TESTS
   */

  @ParameterizedTest
  @MethodSource("getCommonParams1")
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
    Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), findResponse.getStatus());
    // Check the element retrieved
    JsonNode actual = findResponse.readEntity(JsonNode.class);
    Assertions.assertEquals(expected, actual);
  }

  @ParameterizedTest
  @MethodSource("parametersForFindNonExistentResourceTest")
  public void findNonExistentResourceTest(CedarResourceType resourceType, String nonExistentResourceId) {
    String findUrl = null;
    findUrl = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType) + "/" + URLEncoder.encode(nonExistentResourceId, StandardCharsets.UTF_8);
    // Service invocation - Find by Id
    Response findResponse = testClient.target(findUrl).request().header("Authorization", authHeader).get();
    // Check response
    Assertions.assertEquals(CedarResponseStatus.NOT_FOUND.getStatusCode(), findResponse.getStatus());
  }

  static Object[] parametersForFindNonExistentResourceTest() {
    return new Object[]{
        new Object[]{CedarResourceType.TEMPLATE, NON_EXISTENT_TEMPLATE_ID},
        new Object[]{CedarResourceType.ELEMENT, NON_EXISTENT_ELEMENT_ID},
        new Object[]{CedarResourceType.INSTANCE, NON_EXISTENT_INSTANCE_ID}
    };
  }

  @ParameterizedTest
  @MethodSource("parametersForFindInvalidIdTest")
  public void findInvalidIdTest(CedarResourceType resourceType, String invalidId) {
    String findUrl = null;
    findUrl = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType) + "/" + URLEncoder.encode(invalidId, StandardCharsets.UTF_8);
    // Service invocation - Find by Id
    Response findResponse = testClient.target(findUrl).request().header("Authorization", authHeader).get();
    // Check response
    Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), findResponse.getStatus());
  }

  static Object[] parametersForFindInvalidIdTest() {
    return new Object[]{
        new Object[]{CedarResourceType.TEMPLATE, INVALID_ID},
        new Object[]{CedarResourceType.ELEMENT, INVALID_ID},
        new Object[]{CedarResourceType.INSTANCE, INVALID_ID}
    };
  }

  @ParameterizedTest
  @MethodSource("getCommonParams1")
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
    Assertions.assertEquals(CedarResponseStatus.UNAUTHORIZED.getStatusCode(), findResponse.getStatus());
  }

  @ParameterizedTest
  @MethodSource("getCommonParams1")
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
    Assertions.assertEquals(CedarResponseStatus.UNAUTHORIZED.getStatusCode(), findResponse.getStatus());
  }

  /**
   * 'FIND ALL RESOURCES' TESTS
   */

  @ParameterizedTest
  @MethodSource("parametersForFindAllResourcesTest")
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
      Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), findAllResponse.getStatus());
      // The page, its count and its links are in the body envelope, and no longer in headers.
      JsonNode findAllJsonResponse = findAllResponse.readEntity(JsonNode.class);
      Assertions.assertNull(findAllResponse.getHeaderString(CustomHttpConstants.HEADER_TOTAL_COUNT));
      Assertions.assertNull(findAllResponse.getHeaderString(HttpConstants.HTTP_HEADER_LINK));
      Assertions.assertTrue(findAllJsonResponse.get("paging").has("first"));
      int currentCount = findAllJsonResponse.get("totalCount").asInt();
      // Check the number of resources created
      int expectedCount = initialCount + CREATE_RESOURCES_COUNT;
      Assertions.assertTrue(expectedCount == currentCount, "Expected total count in the envelope is wrong");
      // Check the number of elements retrieved
      List<JsonNode> actual = new ArrayList<>();
      for (JsonNode r : findAllJsonResponse.get("artifacts")) {
        actual.add(r);
      }
      int expectedSize;
      if (!limit.isEmpty()) {
        expectedSize = Math.min(expectedCount, Integer.parseInt(limit));
      } else {
        expectedSize = Math.min(expectedCount,
            TestUtil.cedarConfig.getArtifactRESTAPI().getPagination().getDefaultPageSize());
      }
      Assertions.assertEquals(expectedSize, actual.size());
      // The created artifacts are listed whatever else the store holds: the pages are walked with the
      // same parameters, and each created artifact is found among them, in full or as its summary.
      Map<String, JsonNode> listed = listAll(resourceType, limit, summary.isEmpty() ? Map.of() : Map.of("summary", summary));
      for (JsonNode created : resources) {
        String id = created.get(LinkedData.ID).asText();
        JsonNode item = listed.get(id);
        Assertions.assertNotNull(item, "the listing left out " + id);
        if ("true".equals(summary)) {
          assertSummaryOf(created, item);
        } else {
          Assertions.assertEquals(created, item);
        }
      }
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  /** Every artifact of the kind the listing holds, by identifier, read page by page. */
  private static Map<String, JsonNode> listAll(CedarResourceType resourceType, String limit, Map<String, String> params)
      throws URISyntaxException {
    Map<String, JsonNode> listed = new HashMap<>();
    int pageSize = limit.isEmpty()
        ? TestUtil.cedarConfig.getArtifactRESTAPI().getPagination().getDefaultPageSize() : Integer.parseInt(limit);
    int total = Integer.MAX_VALUE;
    for (int offset = 0; offset < total; offset += pageSize) {
      URIBuilder b = new URIBuilder(TestUtil.getResourceUrlRoute(baseTestUrl, resourceType))
          .addParameter("limit", String.valueOf(pageSize)).addParameter("offset", String.valueOf(offset));
      params.forEach(b::addParameter);
      Response page = testClient.target(b.build().toString()).request().header("Authorization", authHeader).get();
      Assertions.assertEquals(CedarResponseStatus.OK.getStatusCode(), page.getStatus());
      JsonNode body = page.readEntity(JsonNode.class);
      total = body.get("totalCount").asInt();
      for (JsonNode artifact : body.get("artifacts")) {
        listed.put(artifact.get(LinkedData.ID).asText(), artifact);
      }
    }
    return listed;
  }

  /** A summary carries the identifier and the UI title and description, and nothing else. */
  private static void assertSummaryOf(JsonNode created, JsonNode summary) {
    Set<String> fields = new HashSet<>();
    summary.fieldNames().forEachRemaining(fields::add);
    Assertions.assertTrue(Set.of(LinkedData.ID, "_ui").containsAll(fields), "summary fields: " + fields);
    Assertions.assertEquals(created.get(LinkedData.ID), summary.get(LinkedData.ID));
    Set<String> uiFields = new HashSet<>();
    summary.path("_ui").fieldNames().forEachRemaining(uiFields::add);
    Assertions.assertTrue(Set.of("title", "description").containsAll(uiFields), "summary _ui fields: " + uiFields);
    for (String uiField : uiFields) {
      Assertions.assertEquals(created.path("_ui").get(uiField), summary.path("_ui").get(uiField));
    }
  }

  /** {@code field_names} lists exactly the fields it names, for every artifact the listing holds. */
  @ParameterizedTest
  @MethodSource("getCommonParams1")
  public void findAllWithFieldNamesListsOnlyThoseFields(JsonNode sampleResource, CedarResourceType resourceType)
      throws URISyntaxException {
    JsonNode created = createResource(setSchemaIsBasedOn(sampleTemplate, sampleResource.deepCopy(), resourceType),
        resourceType);
    String id = created.get(LinkedData.ID).asText();
    createdResources.put(id, resourceType);

    Map<String, JsonNode> listed = listAll(resourceType, "", Map.of("field_names", "@id,schema:name"));

    Assertions.assertEquals(created.get("schema:name"), listed.get(id).get("schema:name"));
    for (JsonNode artifact : listed.values()) {
      Set<String> fields = new HashSet<>();
      artifact.fieldNames().forEachRemaining(fields::add);
      Assertions.assertTrue(Set.of(LinkedData.ID, "schema:name").containsAll(fields), "listed fields: " + fields);
    }
  }

  /**
   * {@code summary} and {@code field_names} select the listed fields in two ways, so a request naming
   * both is the caller's mistake. It used to answer 500 with no error key, indistinguishable from a
   * Mongo outage.
   */
  @ParameterizedTest
  @MethodSource("parametersForFindAllWithFieldNamesAndSummaryTest")
  public void findAllWithFieldNamesAndSummaryIsABadRequest(CedarResourceType resourceType) throws URISyntaxException {
    String findAllUrl = new URIBuilder(TestUtil.getResourceUrlRoute(baseTestUrl, resourceType))
        .addParameter("summary", "true")
        .addParameter("field_names", "schema:name")
        .build().toString();
    Response findAllResponse = testClient.target(findAllUrl).request().header("Authorization", authHeader).get();
    Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), findAllResponse.getStatus());
    JsonNode error = findAllResponse.readEntity(JsonNode.class);
    Assertions.assertEquals("invalidInput", error.get("errorKey").asText());
    Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), error.get("statusCode").asInt());
    Assertions.assertEquals("schema:name", error.get("parameters").get("field_names").asText());
  }

  static Object[] parametersForFindAllWithFieldNamesAndSummaryTest() {
    return new Object[]{CedarResourceType.TEMPLATE, CedarResourceType.ELEMENT, CedarResourceType.INSTANCE};
  }

  static Object[] parametersForFindAllResourcesTest() {
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
