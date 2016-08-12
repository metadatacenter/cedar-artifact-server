import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import play.Logger;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import utils.DataServices;
import utils.TestUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static play.test.Helpers.*;
import static utils.TestConstants.*;

@RunWith(Parameterized.class)
public class TemplateResourcesServerHttpTest {

  /*
   * TODO:
   * - Check that error is returned if Authorization header is provided and apiKey is valid but it is inactive
   * - Check that error is returned if Authorization header is provided but the user does not have enough permissions
   * . It requires creating a new user without permissions to perform certain operations (e.g. TEMPLATE_CREATE)
   * - Update non existing resource should create it (http://stackoverflow.com/questions/797834/should-a-restful-put-operation-return-something)
   * - Find all resources
   */

  @Parameters(name="{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {RESOURCE_TYPE_TEMPLATE, TEMPLATE_ROUTE, SAMPLE_TEMPLATE_PATH, NON_EXISTENT_TEMPLATE_ID},
        {RESOURCE_TYPE_ELEMENT, ELEMENT_ROUTE, SAMPLE_ELEMENT_PATH, NON_EXISTENT_ELEMENT_ID},
        {RESOURCE_TYPE_FIELD, FIELD_ROUTE, SAMPLE_FIELD_PATH, NON_EXISTENT_FIELD_ID},
        {RESOURCE_TYPE_INSTANCE, INSTANCE_ROUTE, SAMPLE_INSTANCE_PATH, NON_EXISTENT_INSTANCE_ID}
    });
  }

  private String resourceType;
  private String resourceUrlRoute;
  // We could directly use JsonNode for the content sent to the server but then, if the Json is wrong, we would
  // receive an error before sending the json to the service. By using String, we force the service to do the json
  // conversion and we can test whether the server returns the appropriate HTTP status codes
  private String sampleResource;
  public String nonExistentResourceId;

  public TemplateResourcesServerHttpTest(String resourceType, String resourceUrlRoute,
                                         String sampleResourcePath, String nonExistentResourceId) {
    this.resourceType = resourceType;
    this.resourceUrlRoute = resourceUrlRoute;
    try {
      sampleResource = TestUtils.readFile(sampleResourcePath, StandardCharsets.UTF_8);
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.nonExistentResourceId = nonExistentResourceId;
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
    deleteAllResources();
  }

  /**
   * Tears down the test fixture.
   * (Called after every test case method.)
   */
  @After
  public void tearDown() {
    deleteAllResources();
  }

  /**
   * Prints the class name and test name before running the test
   */
  @Rule
  public TestRule watcher = new TestWatcher() {
    protected void starting(Description description) {
      log("------------------------------------------------------------------------");
      log("TEST: " + description);
      log("------------------------------------------------------------------------");
    }
  };

  /**
   * 'CREATE' TESTS
   */

  @Test
  public void createResourceTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        // Service invocation - Create
        WSResponse wsResponse =
            WS.url(SERVER_URL + resourceUrlRoute)
                .setHeader("Content-Type", CONTENT_TYPE_HEADER)
                .setHeader("Authorization", AUTH_HEADER).post(sampleResource).get(TIMEOUT_MS);
        // Check HTTP response
        Assert.assertEquals(CREATED, wsResponse.getStatus());
        // Check Content-Type
        Assert.assertEquals("application/json; charset=utf-8", wsResponse.getHeader("Content-Type"));
        // Read location header
        String location = wsResponse.getHeader(LOCATION);
        // Retrieve the element created
        JsonNode actual = WS.url(location).setHeader("Authorization", AUTH_HEADER).get().get(TIMEOUT_MS).asJson();
        JsonNode expected = TestUtils.readAsJson(sampleResource);
        // Check that id and provenance information has been generated
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
    });
  }

  @Test
  public void createResourceMalformedBodyTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        WSResponse wsResponse = null;
        // Empty json
        wsResponse = WS.url(SERVER_URL + resourceUrlRoute).setHeader("Authorization", AUTH_HEADER).post("").get(TIMEOUT_MS);
        Assert.assertEquals(BAD_REQUEST, wsResponse.getStatus());
        // Invalid json
        wsResponse = WS.url(SERVER_URL + resourceUrlRoute).setHeader("Authorization", AUTH_HEADER).post(INVALID_JSON).get
            (TIMEOUT_MS);
        Assert.assertEquals(BAD_REQUEST, wsResponse.getStatus());
      }
    });
  }

  @Test
  public void createResourceMissingAuthorizationHeaderTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        // Service invocation - Create
        WSResponse wsResponse =
            WS.url(SERVER_URL + resourceUrlRoute).post(sampleResource).get(TIMEOUT_MS);
        // Check HTTP response
        Assert.assertEquals(BAD_REQUEST, wsResponse.getStatus());
      }
    });
  }

  @Test
  public void createResourceUnauthorizedKeyTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        String authHeader = "apiKey " + NON_EXISTENT_API_KEY;
        // Service invocation - Create
        WSResponse wsResponse =
            WS.url(SERVER_URL + resourceUrlRoute).setHeader("Authorization", authHeader).post(sampleResource).get(TIMEOUT_MS);
        // Check HTTP response
        Assert.assertEquals(UNAUTHORIZED, wsResponse.getStatus());
      }
    });
  }

  /**
   * 'FIND' TESTS
   */

  @Test
  public void findResourceTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        // Create a resource
        WSResponse wsResponseCreate =
            WS.url(SERVER_URL + resourceUrlRoute)
                .setHeader("Content-Type", CONTENT_TYPE_HEADER)
                .setHeader("Authorization", AUTH_HEADER).post(sampleResource).get(TIMEOUT_MS);
        JsonNode expected = wsResponseCreate.asJson();
        // Use generated id to retrieve the resource
        String id = expected.get("@id").asText();
        // Service invocation - Find by Id
        WSResponse wsResponseFind = null;
        try {
          wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
              .setHeader("Authorization", AUTH_HEADER)
              .get().get(TIMEOUT_MS);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
        // Check response is OK
        Assert.assertEquals(OK, wsResponseFind.getStatus());
        // Check Content-Type
        Assert.assertEquals(wsResponseFind.getHeader("Content-Type"), "application/json; charset=utf-8");
        // Check the element retrieved
        JsonNode actual = wsResponseFind.asJson();
        Assert.assertEquals(expected, actual);
      }
    });
  }

  @Test
  public void findNonExistentResourceTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        // Service invocation - Find by Id
        WSResponse wsResponseFind = null;
        try {
          wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(nonExistentResourceId,
              "UTF-8"))
              .setHeader("Authorization", AUTH_HEADER)
              .get().get(TIMEOUT_MS);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
        // Check response
        Assert.assertEquals(NOT_FOUND, wsResponseFind.getStatus());
      }
    });
  }

  @Test
  // How to use multiple invalid ids? "bla", null, unencoded URL
  public void findInvalidIdTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        String id1 = "bla";
        String id2 = null;
        // Service invocation - Find by Id
        WSResponse wsResponseFind1 = null;
        WSResponse wsResponseFind2 = null;
        try {
          wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + id1)
              .setHeader("Authorization", AUTH_HEADER)
              .get().get(TIMEOUT_MS);
          wsResponseFind2 = WS.url(SERVER_URL + resourceUrlRoute + "/" + id2)
              .setHeader("Authorization", AUTH_HEADER)
              .get().get(TIMEOUT_MS);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
        // Check response
        Assert.assertEquals(NOT_FOUND, wsResponseFind.getStatus());
      }
    });
  }

  @Test
  public void findResourceMissingAuthorizationHeaderTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        // Create a resource
        WSResponse wsResponseCreate =
            WS.url(SERVER_URL + resourceUrlRoute)
                .setHeader("Content-Type", CONTENT_TYPE_HEADER)
                .setHeader("Authorization", AUTH_HEADER).post(sampleResource).get(TIMEOUT_MS);
        JsonNode expected = wsResponseCreate.asJson();
        // Use generated id to retrieve the resource
        String id = expected.get("@id").asText();
        // Service invocation - Find by Id
        WSResponse wsResponseFind = null;
        try {
          wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
              .get().get(TIMEOUT_MS);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
        // Check HTTP response
        Assert.assertEquals(BAD_REQUEST, wsResponseFind.getStatus());
      }
    });
  }

  @Test
  public void findResourceUnauthorizedKeyTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        // Create a resource
        WSResponse wsResponseCreate =
            WS.url(SERVER_URL + resourceUrlRoute)
                .setHeader("Content-Type", CONTENT_TYPE_HEADER)
                .setHeader("Authorization", AUTH_HEADER).post(sampleResource).get(TIMEOUT_MS);
        JsonNode expected = wsResponseCreate.asJson();
        // Use generated id to retrieve the resource
        String id = expected.get("@id").asText();
        // Service invocation - Find by Id
        String authHeader = "apiKey " + NON_EXISTENT_API_KEY;
        WSResponse wsResponseFind = null;
        try {
          wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
              .setHeader("Authorization", authHeader)
              .get().get(TIMEOUT_MS);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
        // Check HTTP response
        Assert.assertEquals(UNAUTHORIZED, wsResponseFind.getStatus());
      }
    });
  }

//  @Test
//  public void findAllResourcesTest() {
//
//  }

//  @Test
//  public void findResourceDetails() {
//    running(testServer(TEST_SERVER_PORT), new Runnable() {
//      public void run() {
//
//      }
//    });
//  }

  @Test
  public void updateResourceTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        try {
          // Create a resource
          WSResponse wsResponseCreate =
              WS.url(SERVER_URL + resourceUrlRoute)
                  .setHeader("Content-Type", CONTENT_TYPE_HEADER)
                  .setHeader("Authorization", AUTH_HEADER).post(sampleResource).get(TIMEOUT_MS);
          JsonNode original = wsResponseCreate.asJson();
          String id = original.get("@id").asText();
          // Update the resource
          String fieldName = "title";
          String fieldNewValue = "This is a new title";
          JsonNode updated = ((ObjectNode) original).put(fieldName, fieldNewValue);
          // Service invocation - Update
          WSResponse wsResponseUpdate =
              WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
                  .setHeader("Content-Type", CONTENT_TYPE_HEADER)
                  .setHeader("Authorization", AUTH_HEADER).put(updated).get(TIMEOUT_MS);
          // Check response
          Assert.assertEquals(OK, wsResponseUpdate.getStatus());
          // Check Content-Type
          Assert.assertEquals("application/json; charset=utf-8", wsResponseUpdate.getHeader("Content-Type"));
          // Retrieve updated element
          WSResponse wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
              .setHeader("Authorization", AUTH_HEADER).get().get(TIMEOUT_MS);
          JsonNode actual = wsResponseFind.asJson();
          // Check that the modifications have been done correctly
          Assert.assertNotNull(actual.get(fieldName));
          Assert.assertEquals(fieldNewValue, actual.get(fieldName).asText());
          // Check that all the other fields contain the expected values
          ((ObjectNode) original).remove(fieldName);
          ((ObjectNode) actual).remove(fieldName);
          Assert.assertEquals(original, actual);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    });
  }

  @Test
  public void deleteResourceTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        try {
          // Create a resource
          WSResponse wsResponseCreate =
              WS.url(SERVER_URL + resourceUrlRoute)
                  .setHeader("Content-Type", CONTENT_TYPE_HEADER)
                  .setHeader("Authorization", AUTH_HEADER).post(sampleResource).get(TIMEOUT_MS);
          JsonNode created = wsResponseCreate.asJson();
          String id = created.get("@id").asText();
          // Service invocation - Delete
          WSResponse wsResponse = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
              .setHeader("Authorization", AUTH_HEADER)
              .delete()
              .get(TIMEOUT_MS);
          // Check response is OK
          Assert.assertEquals(NO_CONTENT, wsResponse.getStatus());
          // Check that the resource has been deleted
          WSResponse wsResponseFind = WS.url(SERVER_URL + resourceUrlRoute + "/" + URLEncoder.encode(id, "UTF-8"))
              .setHeader("Authorization", AUTH_HEADER)
              .get().get(TIMEOUT_MS);

          Assert.assertEquals(NOT_FOUND, wsResponseFind.getStatus());

        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    });
  }


//  @Test
//  public void findAllTemplatesTest() {}
//  @Test
//  public void findTemplateDetailsTest() {}
//  @Test
//  public void updateTemplateTest() {}
//  @Test
//  public void deleteTemplateTest() {}

  /***
   * Helper methods
   ***/

  public void deleteAllResources() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        if (resourceType.compareTo(RESOURCE_TYPE_TEMPLATE) == 0) {
          DataServices.getInstance().getTemplateService().deleteAllTemplates();
        } else if (resourceType.compareTo(RESOURCE_TYPE_ELEMENT) == 0) {
          DataServices.getInstance().getTemplateElementService().deleteAllTemplateElements();
        } else if (resourceType.compareTo(RESOURCE_TYPE_FIELD) == 0) {
          DataServices.getInstance().getTemplateFieldService().deleteAllTemplateFields();
        } else if (resourceType.compareTo(RESOURCE_TYPE_INSTANCE) == 0) {
          DataServices.getInstance().getTemplateInstanceService().deleteAllTemplateInstances();
        }
      }
    });
  }

  public void log(String message) {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        Logger.info(message);
      }
    });
  }

}
