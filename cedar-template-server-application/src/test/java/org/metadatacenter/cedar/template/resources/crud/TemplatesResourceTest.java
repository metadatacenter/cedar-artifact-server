//package org.metadatacenter.cedar.template.resources.crud;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import junitparams.JUnitParamsRunner;
//import junitparams.Parameters;
//import junitparams.naming.TestCaseName;
//import org.apache.http.client.utils.URIBuilder;
//import org.junit.*;
//import org.junit.rules.TestRule;
//import org.junit.rules.TestWatcher;
//import org.junit.runner.Description;
//import org.junit.runner.RunWith;
//import org.metadatacenter.constant.CustomHttpConstants;
//import org.metadatacenter.constant.HttpConstants;
//
//import java.io.IOException;
//import java.io.UnsupportedEncodingException;
//import java.net.URISyntaxException;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//@RunWith(JUnitParamsRunner.class)
//public class TemplatesResourceTest {
//
//  // We could directly use JsonNode for the content sent to the server but then, if the Json is wrong, we would
//  // receive an error before sending the json to the service. By using String, we force the service to do the json
//  // conversion and we can test whether the server returns the appropriate HTTP status codes
//  private static String sampleTemplate;
//  private static String sampleElement;
//  private static String sampleField;
//  private static String sampleInstance;
//
//  static {
//    try {
//      sampleTemplate = TestUtils.readFile(SAMPLE_TEMPLATE_PATH, StandardCharsets.UTF_8);
//      sampleElement = TestUtils.readFile(SAMPLE_ELEMENT_PATH, StandardCharsets.UTF_8);
//      sampleField = TestUtils.readFile(SAMPLE_FIELD_PATH, StandardCharsets.UTF_8);
//      sampleInstance = TestUtils.readFile(SAMPLE_INSTANCE_PATH, StandardCharsets.UTF_8);
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//  }
//
//  /**
//   * One-time initialization code.
//   * (Called once before any of the test methods in the class).
//   */
//  @BeforeClass
//  public static void oneTimeSetUp() {
//  }
//
//  /**
//   * (Called once after all the test methods in the class).
//   */
//  @AfterClass
//  public static void oneTimeTearDown() {
//  }
//
//  /**
//   * Sets up the test fixture.
//   * (Called before every test case method.)
//   */
//  @Before
//  public void setUp() {
//    deleteAllResources();
//  }
//
//  /**
//   * Tears down the test fixture.
//   * (Called after every test case method.)
//   */
//  @After
//  public void tearDown() {
//    deleteAllResources();
//  }
//
//  /**
//   * Prints the class name and test name before running the test
//   */
//  @Rule
//  public TestRule watcher = new TestWatcher() {
//    protected void starting(Description description) {
//      log("------------------------------------------------------------------------");
//      log("TEST: " + description);
//      log("------------------------------------------------------------------------");
//    }
//  };
//
//  /**
//   * 'CREATE' TESTS
//   */
//
//  @Test
//  @TestCaseName(TEST_NAME_PATTERN)
//  @Parameters(method = "getCommonParams1")
//  public void createResourceTest(String resourceUrlRoute, String sampleResource) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        // Service invocation - Create
//        WSResponse wsResponse =
//            WS.url(SERVER_URL + resourceUrlRoute)
//                .setHeader("Content-Type", CONTENT_TYPE_HEADER)
//                .setHeader("Authorization", AUTH_HEADER).post(sampleResource).get(TIMEOUT_MS);
//        // Check HTTP response
//        Assert.assertEquals(CREATED, wsResponse.getStatus());
//        // Check Content-Type
//        Assert.assertEquals("application/json; charset=utf-8", wsResponse.getHeader("Content-Type"));
//        // Read location header
//        String location = wsResponse.getHeader(LOCATION);
//        // Retrieve the element created
//        JsonNode actual = WS.url(location).setHeader("Authorization", AUTH_HEADER).get().get(TIMEOUT_MS).asJson();
//        JsonNode expected = TestUtils.readAsJson(sampleResource);
//        // Check that id and provenance information has been generated
//        Assert.assertNotEquals(actual.get(ID_FIELD), null);
//        for (String provField : PROV_FIELDS) {
//          Assert.assertNotEquals(actual.get(provField), null);
//        }
//        // Check that all the other fields contain the expected values
//        ((ObjectNode) expected).remove(ID_FIELD);
//        ((ObjectNode) actual).remove(ID_FIELD);
//        for (String provField : PROV_FIELDS) {
//          ((ObjectNode) expected).remove(provField);
//          ((ObjectNode) actual).remove(provField);
//        }
//        Assert.assertEquals(expected, actual);
//      }
//    });
//  }
//
//  @Test
//  @TestCaseName(TEST_NAME_PATTERN)
//  @Parameters(method = "getCommonParams2")
//  public void createResourceMalformedBodyTest(String resourceUrlRoute) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        WSResponse wsResponse = null;
//        // Empty json
//        wsResponse = WS.url(SERVER_URL + resourceUrlRoute).setHeader("Authorization", AUTH_HEADER).post("").get(TIMEOUT_MS);
//        Assert.assertEquals(BAD_REQUEST, wsResponse.getStatus());
//        // Invalid json
//        wsResponse = WS.url(SERVER_URL + resourceUrlRoute).setHeader("Authorization", AUTH_HEADER).post(INVALID_JSON).get
//            (TIMEOUT_MS);
//        Assert.assertEquals(BAD_REQUEST, wsResponse.getStatus());
//      }
//    });
//  }
//
//  @Test
//  @TestCaseName(TEST_NAME_PATTERN)
//  @Parameters(method = "getCommonParams1")
//  public void createResourceMissingAuthorizationHeaderTest(String resourceUrlRoute, String sampleResource) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        // Service invocation - Create
//        WSResponse wsResponse =
//            WS.url(SERVER_URL + resourceUrlRoute).post(sampleResource).get(TIMEOUT_MS);
//        // Check HTTP response
//        Assert.assertEquals(BAD_REQUEST, wsResponse.getStatus());
//      }
//    });
//  }
//
//  @Test
//  @TestCaseName(TEST_NAME_PATTERN)
//  @Parameters(method = "getCommonParams1")
//  public void createResourceUnauthorizedKeyTest(String resourceUrlRoute, String sampleResource) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        String authHeader = "apiKey " + NON_EXISTENT_API_KEY;
//        // Service invocation - Create
//        WSResponse wsResponse =
//            WS.url(SERVER_URL + resourceUrlRoute).setHeader("Authorization", authHeader).post(sampleResource).get(TIMEOUT_MS);
//        // Check HTTP response
//        Assert.assertEquals(UNAUTHORIZED, wsResponse.getStatus());
//      }
//    });
//  }
//
//  /**
//   * 'FIND' TESTS
//   */
//
//  @Test
//  @TestCaseName(TEST_NAME_PATTERN)
//  @Parameters(method = "getCommonParams1")
//  public void findResourceTest(String resourceUrlRoute, String sampleResource) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        // Create a resource
//        WSResponse wsResponseCreate =
//            WS.url(SERVER_URL + resourceUrlRoute)
//                .setHeader("Content-Type", CONTENT_TYPE_HEADER)
//                .setHeader("Authorization", AUTH_HEADER).post(sampleResource).get(TIMEOUT_MS);
//        JsonNode expected = wsResponseCreate.asJson();
//        // Use generated id to retrieve the resource
//        String id = expected.get("@id").asText();
//        // Service invocation - Find by Id
//        WSResponse wsResponseFind = null;
//        try {
//          wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
//              .setHeader("Authorization", AUTH_HEADER)
//              .get().get(TIMEOUT_MS);
//        } catch (UnsupportedEncodingException e) {
//          e.printStackTrace();
//        }
//        // Check response is OK
//        Assert.assertEquals(OK, wsResponseFind.getStatus());
//        // Check Content-Type
//        Assert.assertEquals(wsResponseFind.getHeader("Content-Type"), "application/json; charset=utf-8");
//        // Check the element retrieved
//        JsonNode actual = wsResponseFind.asJson();
//        Assert.assertEquals(expected, actual);
//      }
//    });
//  }
//
//  @Test
//  @TestCaseName(TEST_NAME_PATTERN)
//  @Parameters
//  public void findNonExistentResourceTest(String resourceUrlRoute, String nonExistentResourceId) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        // Service invocation - Find by Id
//        WSResponse wsResponseFind = null;
//        try {
//          wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(nonExistentResourceId,
//              "UTF-8"))
//              .setHeader("Authorization", AUTH_HEADER)
//              .get().get(TIMEOUT_MS);
//        } catch (UnsupportedEncodingException e) {
//          e.printStackTrace();
//        }
//        // Check response
//        Assert.assertEquals(NOT_FOUND, wsResponseFind.getStatus());
//      }
//    });
//  }
//  private Object parametersForFindNonExistentResourceTest() {
//    return new Object[]{
//        new Object[]{TEMPLATE_ROUTE, NON_EXISTENT_TEMPLATE_ID},
//        new Object[]{ELEMENT_ROUTE, NON_EXISTENT_ELEMENT_ID},
//        new Object[]{FIELD_ROUTE, NON_EXISTENT_FIELD_ID},
//        new Object[]{INSTANCE_ROUTE, NON_EXISTENT_INSTANCE_ID}
//    };
//  }
//
//  @Test
//  @TestCaseName(TEST_NAME_PATTERN)
//  @Parameters
//  public void findInvalidIdTest(String resourceUrlRoute, String id) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        // Service invocation - Find by Id
//        WSResponse wsResponseFind = null;
//        wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + id)
//            .setHeader("Authorization", AUTH_HEADER)
//            .get().get(TIMEOUT_MS);
//        // Check response
//        Assert.assertEquals(NOT_FOUND, wsResponseFind.getStatus());
//      }
//    });
//  }
//  private Object parametersForFindInvalidIdTest() {
//    return new Object[]{
//        new Object[]{TEMPLATE_ROUTE, INVALID_ID},
//        new Object[]{ELEMENT_ROUTE, INVALID_ID},
//        new Object[]{FIELD_ROUTE, INVALID_ID},
//        new Object[]{INSTANCE_ROUTE, INVALID_ID}
//    };
//  }
//
//  @Test
//  @TestCaseName(TEST_NAME_PATTERN)
//  @Parameters(method = "getCommonParams1")
//  public void findResourceMissingAuthorizationHeaderTest(String resourceUrlRoute, String sampleResource) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        // Create a resource
//        WSResponse wsResponseCreate =
//            WS.url(SERVER_URL + resourceUrlRoute)
//                .setHeader("Content-Type", CONTENT_TYPE_HEADER)
//                .setHeader("Authorization", AUTH_HEADER).post(sampleResource).get(TIMEOUT_MS);
//        JsonNode expected = wsResponseCreate.asJson();
//        // Use generated id to retrieve the resource
//        String id = expected.get("@id").asText();
//        // Service invocation - Find by Id
//        WSResponse wsResponseFind = null;
//        try {
//          wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
//              .get().get(TIMEOUT_MS);
//        } catch (UnsupportedEncodingException e) {
//          e.printStackTrace();
//        }
//        // Check HTTP response
//        Assert.assertEquals(BAD_REQUEST, wsResponseFind.getStatus());
//      }
//    });
//  }
//
//  @Test
//  @TestCaseName(TEST_NAME_PATTERN)
//  @Parameters(method = "getCommonParams1")
//  public void findResourceUnauthorizedKeyTest(String resourceUrlRoute, String sampleResource) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        // Create a resource
//        WSResponse wsResponseCreate =
//            WS.url(SERVER_URL + resourceUrlRoute)
//                .setHeader("Content-Type", CONTENT_TYPE_HEADER)
//                .setHeader("Authorization", AUTH_HEADER).post(sampleResource).get(TIMEOUT_MS);
//        JsonNode expected = wsResponseCreate.asJson();
//        // Use generated id to retrieve the resource
//        String id = expected.get("@id").asText();
//        // Service invocation - Find by Id
//        String authHeader = "apiKey " + NON_EXISTENT_API_KEY;
//        WSResponse wsResponseFind = null;
//        try {
//          wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
//              .setHeader("Authorization", authHeader)
//              .get().get(TIMEOUT_MS);
//        } catch (UnsupportedEncodingException e) {
//          e.printStackTrace();
//        }
//        // Check HTTP response
//        Assert.assertEquals(UNAUTHORIZED, wsResponseFind.getStatus());
//      }
//    });
//  }
//
//  /**
//   * 'FIND ALL RESOURCES' TESTS
//   */
//
//  @Test
//  @TestCaseName(TEST_NAME_PATTERN + " limit={2}, offset={3}, summary={4}")
//  @Parameters
//  public void findAllResourcesTest(String resourceUrlRoute, String sampleResource, String limit, String offset,
//                                   String summary) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        try {
//          // Create several resources
//          List<JsonNode> resources = new ArrayList<>();
//          int num = 3;
//          for (int i = 0; i < num; i++) {
//            JsonNode created = WS.url(SERVER_URL + resourceUrlRoute)
//                .setHeader("Content-Type", CONTENT_TYPE_HEADER)
//                .setHeader("Authorization", AUTH_HEADER).post(sampleResource).get(TIMEOUT_MS).asJson();
//            resources.add(created);
//          }
//          // Find All
//          URIBuilder b = new URIBuilder(SERVER_URL + resourceUrlRoute);
//          if (limit.length() > 0) {
//            b.addParameter("limit", limit);
//          }
//          if (offset.length() > 0) {
//            b.addParameter("offset", offset);
//          }
//          if (summary.length() > 0) {
//            b.addParameter("summary", summary);
//          }
//          String url = b.build().toString();
//          Logger.info("URL: " + url);
//          WSResponse wsResponseFindAll = WS.url(url)
//              .setHeader("Authorization", AUTH_HEADER)
//              .get().get(TIMEOUT_MS);
//          // Check response is OK
//          Assert.assertEquals(OK, wsResponseFindAll.getStatus());
//          // Check headers
//          Assert.assertEquals(wsResponseFindAll.getHeader("Content-Type"), "application/json; charset=utf-8");
//          Assert.assertNotNull(wsResponseFindAll.getHeader(CustomHttpConstants.HEADER_TOTAL_COUNT));
//          Assert.assertNotNull(wsResponseFindAll.getHeader(HttpConstants.HTTP_HEADER_LINK));
//          // Check the number of elements retrieved
//          List<JsonNode> actual = new ArrayList<>();
//          for (JsonNode r : wsResponseFindAll.asJson()) {
//            actual.add(r);
//          }
//          int expectedSize = num;
//          if (limit.length() > 0) {
//            expectedSize = Math.min(num, Integer.parseInt(limit));
//          }
//          Assert.assertEquals(expectedSize, actual.size());
//          // Check the elements retrieved
//          List<JsonNode> expected = resources;
//          if (summary.compareTo("true") != 0) {
//            Assert.assertTrue(expected.containsAll(actual));
//          } else {
//            Assert.assertFalse(expected.containsAll(actual));
//          }
//        } catch (URISyntaxException e) {
//          e.printStackTrace();
//        }
//      }
//    });
//  }
//  private Object parametersForFindAllResourcesTest() {
//    List<Object> p1p2Values = Arrays.asList(
//        Arrays.asList(TEMPLATE_ROUTE, sampleTemplate),
//        Arrays.asList(ELEMENT_ROUTE, sampleElement),
//        Arrays.asList(FIELD_ROUTE, sampleField),
//        Arrays.asList(INSTANCE_ROUTE, sampleInstance));
//    List<Object> limitValues = Arrays.asList(Arrays.asList(""), Arrays.asList("2"), Arrays.asList("50"));
//    List<Object> offsetValues = Arrays.asList(Arrays.asList(""), Arrays.asList("0"));
//    List<Object> summaryValues = Arrays.asList(Arrays.asList(""), Arrays.asList("true"), Arrays.asList("false"));
//    // TODO: add fieldNames
//    return TestParameterUtils.getParameterPermutations(Arrays.asList(p1p2Values, limitValues, offsetValues,
//        summaryValues));
//  }
//
////  @Test
////  public void findAllResourcesTest() {
////
////  }
//
////  @Test
////  public void findResourceDetails() {
////    running(testServer(TEST_SERVER_PORT), new Runnable() {
////      public void run() {
////
////      }
////    });
////  }
//
//  @Test
//  @TestCaseName(TEST_NAME_PATTERN)
//  @Parameters(method = "getCommonParams1")
//  public void updateResourceTest(String resourceUrlRoute, String sampleResource) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        try {
//          // Create a resource
//          WSResponse wsResponseCreate =
//              WS.url(SERVER_URL + resourceUrlRoute)
//                  .setHeader("Content-Type", CONTENT_TYPE_HEADER)
//                  .setHeader("Authorization", AUTH_HEADER).post(sampleResource).get(TIMEOUT_MS);
//          JsonNode original = wsResponseCreate.asJson();
//          String id = original.get("@id").asText();
//          // Update the resource
//          String fieldName = "title";
//          String fieldNewValue = "This is a new title";
//          JsonNode updated = ((ObjectNode) original).put(fieldName, fieldNewValue);
//          // Service invocation - Update
//          WSResponse wsResponseUpdate =
//              WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
//                  .setHeader("Content-Type", CONTENT_TYPE_HEADER)
//                  .setHeader("Authorization", AUTH_HEADER).put(updated).get(TIMEOUT_MS);
//          // Check response
//          Assert.assertEquals(OK, wsResponseUpdate.getStatus());
//          // Check Content-Type
//          Assert.assertEquals("application/json; charset=utf-8", wsResponseUpdate.getHeader("Content-Type"));
//          // Retrieve updated element
//          WSResponse wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
//              .setHeader("Authorization", AUTH_HEADER).get().get(TIMEOUT_MS);
//          JsonNode actual = wsResponseFind.asJson();
//          // Check that the modifications have been done correctly
//          Assert.assertNotNull(actual.get(fieldName));
//          Assert.assertEquals(fieldNewValue, actual.get(fieldName).asText());
//          // Check that all the other fields contain the expected values
//          ((ObjectNode) original).remove(fieldName);
//          ((ObjectNode) actual).remove(fieldName);
//          Assert.assertEquals(original, actual);
//        } catch (UnsupportedEncodingException e) {
//          e.printStackTrace();
//        }
//      }
//    });
//  }
//
//  @Test
//  @TestCaseName(TEST_NAME_PATTERN)
//  @Parameters(method = "getCommonParams1")
//  public void deleteResourceTest(String resourceUrlRoute, String sampleResource) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        try {
//          // Create a resource
//          WSResponse wsResponseCreate =
//              WS.url(SERVER_URL + resourceUrlRoute)
//                  .setHeader("Content-Type", CONTENT_TYPE_HEADER)
//                  .setHeader("Authorization", AUTH_HEADER).post(sampleResource).get(TIMEOUT_MS);
//          JsonNode created = wsResponseCreate.asJson();
//          String id = created.get("@id").asText();
//          // Service invocation - Delete
//          WSResponse wsResponse = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
//              .setHeader("Authorization", AUTH_HEADER)
//              .delete()
//              .get(TIMEOUT_MS);
//          // Check response is OK
//          Assert.assertEquals(NO_CONTENT, wsResponse.getStatus());
//          // Check that the resource has been deleted
//          WSResponse wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
//              .setHeader("Authorization", AUTH_HEADER)
//              .get().get(TIMEOUT_MS);
//
//          Assert.assertEquals(NOT_FOUND, wsResponseFind.getStatus());
//
//        } catch (UnsupportedEncodingException e) {
//          e.printStackTrace();
//        }
//      }
//    });
//  }
//
//  /***
//   * Helper methods
//   ***/
//
//  public void deleteAllResources() {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        DataServices.getInstance().getTemplateService().deleteAllTemplates();
//        DataServices.getInstance().getTemplateElementService().deleteAllTemplateElements();
//        DataServices.getInstance().getTemplateFieldService().deleteAllTemplateFields();
//        DataServices.getInstance().getTemplateInstanceService().deleteAllTemplateInstances();
//      }
//    });
//  }
//
//  public void log(String message) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        Logger.info(message);
//      }
//    });
//  }
//
//  /***
//   * Common parameters
//   */
//
//  // String resourceUrlRoute, String sampleResource
//  private Object getCommonParams1() {
//    return new Object[]{
//        new Object[]{TEMPLATE_ROUTE, sampleTemplate},
//        new Object[]{ELEMENT_ROUTE, sampleElement},
//        new Object[]{FIELD_ROUTE, sampleField},
//        new Object[]{INSTANCE_ROUTE, sampleInstance}
//    };
//  }
//
//  // String sampleResource
//  private Object getCommonParams2() {
//    return new Object[]{TEMPLATE_ROUTE, ELEMENT_ROUTE, FIELD_ROUTE, INSTANCE_ROUTE};
//  }
//
//}