import com.fasterxml.jackson.databind.JsonNode;
import controllers.TemplateServerController;
import org.junit.*;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import utils.DataServices;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.net.URLEncoder;

import static play.test.Helpers.*;

/*
 * Integration Tests. They are done using a test server.
 */
public class TemplateServerHttpTest {

  private static final String TEMPLATE_ELEMENTS_ROUTE = "/template_elements";
  private static final int TEST_SERVER_PORT = 3333;
  private static final String SERVER_URL = "http://localhost:" + TEST_SERVER_PORT;
  private static final int TIMEOUT_MS = 10000;

  private static JsonNode templateElement1;
  private static JsonNode templateElement2;

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
    templateElement1 = Json.newObject().
        put("@id", "http://metadatacenter.org/template_elements/682c8141-9a61-4899-9d21-7083e861b0bf").
        put("name", "template element 1 name").put("value", "template element 1 value");
    templateElement2 = Json.newObject().
        put("@id", "http://metadatacenter.org/template_elements/1dd58530-fdba-4c06-8d31-539b18296d8b").
        put("name", "template element 2 name").put("value", "template element 2 value");

    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        deleteAllTemplateElements();
      }
    });
  }

  /**
   * Tears down the test fixture.
   * (Called after every test case method.)
   */
  @After
  public void tearDown() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        // Remove the elements created
        deleteAllTemplateElements();
      }
    });
  }

  @Test
  public void createTemplateElementTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        // Service invocation - Create
        WSResponse wsResponse = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE).post(templateElement1).get(TIMEOUT_MS);
        // Check HTTP response
        Assert.assertEquals(OK, wsResponse.getStatus());
        // Check Content-Type
        Assert.assertEquals("application/json; charset=utf-8", wsResponse.getHeader("Content-Type"));
        // Retrieve the element created
        JsonNode actual = null;
        try {
          actual = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE + "/" +
              URLEncoder.encode(templateElement1.get("@id").asText(), "UTF-8")).get().get(TIMEOUT_MS).asJson();
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
        JsonNode expected = templateElement1;
        // Check fields
        Assert.assertNotNull(actual.get("@id"));
        Assert.assertEquals(expected.get("@id"), actual.get("@id"));
        Assert.assertNotNull(actual.get("name"));
        Assert.assertEquals(expected.get("name"), actual.get("name"));
        Assert.assertNotNull(actual.get("value"));
        Assert.assertEquals(expected.get("value"), actual.get("value"));
      }
    });
  }

  @Test
  public void findAllTemplateElementsTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        // Create two sample elements
        templateElement1 = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE).post(templateElement1).get(TIMEOUT_MS).asJson();
        templateElement2 = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE).post(templateElement2).get(TIMEOUT_MS).asJson();
        // Service invocation - Find all
        WSResponse wsResponse = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE).get().get(TIMEOUT_MS);
        // Check response is OK
        Assert.assertEquals(wsResponse.getStatus(), OK);
        // Check Content-Type
        Assert.assertEquals(wsResponse.getHeader("Content-Type"), "application/json; charset=utf-8");
        // Store actual and expected results into two sets, to compare them
        Set expectedSet = new HashSet<JsonNode>();
        expectedSet.add(templateElement1);
        expectedSet.add(templateElement2);
        Set actualSet = new HashSet<JsonNode>();
        JsonNode jsonResponse = wsResponse.asJson();
        Iterator it = jsonResponse.iterator();
        while (it.hasNext()) {
          actualSet.add(it.next());
        }
        // Check the number of results
        Assert.assertEquals(expectedSet.size(), actualSet.size());
        // Check the results
        Assert.assertEquals(expectedSet, actualSet);
      }
    });
  }

  @Test
  public void findTemplateElementTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        // Create an element
        JsonNode expected = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE).post(templateElement1).get(TIMEOUT_MS).asJson();
        String id = expected.get("@id").asText();
        // Service invocation - Find by Id
        WSResponse wsResponse = null;
        try {
          wsResponse = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE + "/" + URLEncoder.encode(id, "UTF-8")).get().get(TIMEOUT_MS);
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
        // Check response is OK
        Assert.assertEquals(wsResponse.getStatus(), OK);
        // Check Content-Type
        Assert.assertEquals(wsResponse.getHeader("Content-Type"), "application/json; charset=utf-8");
        // Check the element retrieved
        JsonNode actual = wsResponse.asJson();
        Assert.assertEquals(expected, actual);
      }
    });
  }

  @Test
  public void updateTemplateElementTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        try {
          // Create an element
          JsonNode elementCreated =
              WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE).post(templateElement1).get(TIMEOUT_MS).asJson();
          // Update the element created
          String id = elementCreated.get("@id").asText();
          String updatedName = "new name";
          JsonNode changes = Json.newObject().put("name", updatedName);
          // Service invocation - Update
          WSResponse wsResponse =
              WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE + "/" + URLEncoder.encode(id, "UTF-8")).put(changes).get(TIMEOUT_MS);
          // Check response is OK
          Assert.assertEquals(wsResponse.getStatus(), OK);
          // Check Content-Type
          Assert.assertEquals(wsResponse.getHeader("Content-Type"), "application/json; charset=utf-8");
          // Retrieve updated element
          JsonNode actual = null;
          try {
            actual =
                WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE + "/" + URLEncoder.encode(id, "UTF-8")).get().get(TIMEOUT_MS).asJson();
          } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
          }
          // Check if the modifications have been done correctly
          Assert.assertNotNull(actual.get("name"));
          Assert.assertEquals(updatedName, actual.get("name").asText());
          Assert.assertNotNull(actual.get("value"));
          Assert.assertEquals(elementCreated.get("value"), actual.get("value"));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    });
  }

  @Test
  public void deleteTemplateElementTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        try {
          // Create an element
          JsonNode elementCreated = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE).post(templateElement1).get(TIMEOUT_MS).asJson();
          String id = elementCreated.get("@id").asText();
          // Service invocation - Delete
          WSResponse wsResponse = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE + "/" + URLEncoder.encode(id, "UTF-8")).delete()
              .get(TIMEOUT_MS);
          // Check response is OK
          Assert.assertEquals(wsResponse.getStatus(), OK);
          // Check that the element has been deleted by trying to find it by id
          WSResponse wsResponse1 = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE + "/" + URLEncoder.encode(id, "UTF-8")).get().get(TIMEOUT_MS);
          Assert.assertEquals(NOT_FOUND, wsResponse1.getStatus());
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    });
  }

  // Helper method to remove all elements from the DB
  public void deleteAllTemplateElements() {
    DataServices.getInstance().getTemplateElementService().deleteAllTemplateElements();
  }
}
