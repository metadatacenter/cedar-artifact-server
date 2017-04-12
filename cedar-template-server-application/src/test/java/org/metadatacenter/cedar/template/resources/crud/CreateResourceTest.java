package org.metadatacenter.cedar.template.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.*;
import org.junit.runner.RunWith;
import org.metadatacenter.cedar.template.resources.utils.TestUtil;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static org.metadatacenter.cedar.template.resources.utils.TestConstants.*;

@RunWith(JUnitParamsRunner.class)
public class CreateResourceTest extends AbstractResourceCrudTest {

  private static JsonNode sampleTemplate;
  private static JsonNode sampleElement;
  private static JsonNode sampleInstance;

  static {
    try {
      sampleTemplate = TestUtil.readFileAsJson(SAMPLE_TEMPLATE_PATH);
      sampleElement = TestUtil.readFileAsJson(SAMPLE_ELEMENT_PATH);
      sampleInstance = TestUtil.readFileAsJson(SAMPLE_INSTANCE_PATH);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * One-time initialization code.
   * (Called once before any of the test methods in the class).
   */
  @BeforeClass
  public static void oneTimeSetUp() {
  }

  /**
   * (Called once after all the test methods in the class).
   */
  @AfterClass
  public static void oneTimeTearDown() {
  }

  /**
   * Sets up the test fixture.
   * (Called before every test case method.)
   */
  @Before
  public void setUp() {
    TestUtil.deleteAllResources();
  }

  /**
   * Tears down the test fixture.
   * (Called after every test case method.)
   */
  @After
  public void tearDown() {
    TestUtil.deleteAllResources();
  }

  /**
   * Prints the class name and test name before running the test
   */
//  @Rule
//  public TestRule watcher = new TestWatcher() {
//    protected void starting(Description description) {
//      log("------------------------------------------------------------------------");
//      log("TEST: " + description);
//      log("------------------------------------------------------------------------");
//    }
//  };

  /**
   * 'CREATE' TESTS
   */

  @Test
  @TestCaseName(TEST_NAME_PATTERN)
  @Parameters(method = "getCommonParams1")
  public void createResourceTest(String resourceUrlRoute, JsonNode sampleResource) {
    String url = baseTestUrl + resourceUrlRoute;
    // Service invocation
    Response response = testClient.target(url).request().header("Authorization", authHeader).post(Entity.json
        (sampleResource));
    // Check HTTP response
    Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    // Retrieve the resource created
    String location = response.getHeaderString(LOCATION);
    Response findResponse = testClient.target(location).request().header("Authorization", authHeader).get();
    JsonNode expected = sampleResource;
    JsonNode actual = findResponse.readEntity(JsonNode.class);
    // Check that id and provenance information have been generated
    Assert.assertNotEquals(actual.get(ID_FIELD), null);
    for (String provField : PROV_FIELDS) {
      Assert.assertNotEquals(actual.get(provField), null);
    }
    // Check that all the other fields contain the expected values
    ((ObjectNode) expected).remove(ID_FIELD);
    ((ObjectNode) actual).remove(ID_FIELD);
    for (String provField : PROV_FIELDS) {
      ((ObjectNode) expected).remove(provField);
      ((ObjectNode) actual).remove(provField);
    }
    Assert.assertEquals(expected, actual);
  }

  @Test
  @TestCaseName(TEST_NAME_PATTERN)
  @Parameters(method = "getCommonParams2")
  public void createResourceMalformedBodyTest(String resourceUrlRoute) {
    String url = baseTestUrl + resourceUrlRoute;
    // Post empty json
    Response response1 = testClient.target(url).request().header("Authorization", authHeader).post(Entity.json(""));
    // Check HTTP response
    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response1.getStatus());
    // Post invalid json
    Response response2 = testClient.target(url).request().header("Authorization", authHeader).post(Entity.json(INVALID_JSON));
    // Check HTTP response
    Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response2.getStatus());
  }

  @Test
  @TestCaseName(TEST_NAME_PATTERN)
  @Parameters(method = "getCommonParams1")
  public void createResourceMissingAuthorizationHeaderTest(String resourceUrlRoute, String sampleResource) {
    String url = baseTestUrl + resourceUrlRoute;
    // Service invocation without Authorization header
    Response response = testClient.target(url).request().post(Entity.json(sampleResource));
    // Check HTTP response
    Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }

  @Test
  @TestCaseName(TEST_NAME_PATTERN)
  @Parameters(method = "getCommonParams1")
  public void createResourceUnauthorizedKeyTest(String resourceUrlRoute, String sampleResource) {
    String url = baseTestUrl + resourceUrlRoute;
    String newAuthHeader = "apiKey " + NON_EXISTENT_API_KEY;
    // Service invocation without unauthorized api key
    Response response = testClient.target(url).request().header("Authorization", newAuthHeader).post(Entity.json(sampleResource));
    // Check HTTP response
    Assert.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
  }

  /***
   * Helper methods
   ***/

//  public void log(String message) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        Logger.info(message);
//      }
//    });
//  }

  /***
   * Common parameters
   */

  // String resourceUrlRoute, String sampleResource
  private Object getCommonParams1() {
    return new Object[]{
        new Object[]{TEMPLATE_ROUTE, sampleTemplate},
        new Object[]{ELEMENT_ROUTE, sampleElement},
        new Object[]{INSTANCE_ROUTE, sampleInstance}
    };
  }

  // String sampleResource
  private Object getCommonParams2() {
    return new Object[]{TEMPLATE_ROUTE, ELEMENT_ROUTE, INSTANCE_ROUTE};
  }
}
