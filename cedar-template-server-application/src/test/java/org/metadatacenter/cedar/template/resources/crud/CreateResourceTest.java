package org.metadatacenter.cedar.template.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.metadatacenter.cedar.template.resources.utils.TestUtil;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static org.metadatacenter.cedar.template.resources.utils.TestConstants.*;

public class CreateResourceTest extends AbstractResourceCrudTest {

  private static JsonNode sampleTemplate;
  private static JsonNode sampleElement;
  private static JsonNode sampleField;
  private static JsonNode sampleInstance;

  static {
    try {
      sampleTemplate = TestUtil.readFileAsJson(SAMPLE_TEMPLATE_PATH, StandardCharsets.UTF_8);
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
//  @Before
//  public void setUp() {
//    deleteAllResources();
//  }

  /**
   * Tears down the test fixture.
   * (Called after every test case method.)
   */
//  @After
//  public void tearDown() {
//    deleteAllResources();
//  }

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
    Response response = testClient.target(url).request().header("Authorization", authHeaderValue).post(Entity.json(sampleResource));
    // Check HTTP response
    Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
    // Retrieve the created resource
    //JsonNode actual =
    JsonNode expected = sampleResource;


//    String url = base baseUrlBpOntologies + "/" + Util.getShortIdentifier(c.getOntology()) + "/" + BP_CLASSES;
//    // Service invocation
//    Response response = client.target(url).request().header("Authorization", authHeader).post(Entity.json(c));
//    // Check HTTP response
//    Assert.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
//    OntologyClass created = response.readEntity(OntologyClass.class);
//    createdClasses.add(created);
//    return created;
//
//
//
//
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

  }

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

  /***
   * Helper methods
   ***/

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

//  public void log(String message) {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//        Logger.info(message);
//      }
//    });
//  }
}
