import com.fasterxml.jackson.databind.JsonNode;
import controllers.CrudController;
import org.junit.*;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static play.test.Helpers.*;

/*
 * Integration Tests. They are done using a test server.
 */
public class CrudHttpTest
{
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
  @BeforeClass public static void oneTimeSetUp()
  {
  }

  /**
   * (Called once after all the test methods in the class).
   */
  @AfterClass public static void oneTimeTearDown()
  {
  }

  /**
   * Sets up the test fixture.
   * (Called before every test case method.)
   */
  @Before public void setUp()
  {
    templateElement1 = Json.newObject().put("name", "template element 1 name").put("value", "template element 2 value");
    templateElement2 = Json.newObject().put("name", "template element 2 name").put("value", "template element 2 value");

    running(testServer(TEST_SERVER_PORT), new Runnable()
    {
      public void run()
      {
        deleteAllTemplateElements();
      }
    });
  }

  /**
   * Tears down the test fixture.
   * (Called after every test case method.)
   */
  @After public void tearDown()
  {
    running(testServer(TEST_SERVER_PORT), new Runnable()
    {
      public void run()
      {
        // Remove the elements created
        deleteAllTemplateElements();
      }
    });
  }

  @Test public void createTemplateElementTest()
  {
    running(testServer(TEST_SERVER_PORT), new Runnable()
    {
      public void run()
      {
        // Service invocation - Create
        WSResponse wsResponse = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE).post(templateElement1).get(TIMEOUT_MS);
        // Check HTTP response
        Assert.assertEquals(OK, wsResponse.getStatus());
        // Check Content-Type
        Assert.assertEquals("application/json; charset=utf-8", wsResponse.getHeader("Content-Type"));
        String actualId = wsResponse.asJson().get("_id").get("$oid").asText();
        // Retrieve the element created
        JsonNode actual = WS.url(SERVER_URL + "/template_elements/" + actualId).get().get(TIMEOUT_MS).asJson();
        JsonNode expected = templateElement1;
        // Check fields
        Assert.assertNotNull(actual.get("name"));
        Assert.assertEquals(expected.get("name"), actual.get("name"));
        Assert.assertNotNull(actual.get("value"));
        Assert.assertEquals(expected.get("value"), actual.get("value"));
      }
    });
  }

  @Test public void findAllTemplateElementsTest()
  {
    running(testServer(TEST_SERVER_PORT), new Runnable()
    {
      public void run()
      {
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

  @Test public void findTemplateElementTest()
  {
    running(testServer(TEST_SERVER_PORT), new Runnable()
    {
      public void run()
      {
        // Create an element
        JsonNode expected = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE).post(templateElement1).get(TIMEOUT_MS).asJson();
        String id = expected.get("_id").get("$oid").asText();
        // Service invocation - Find by Id
        WSResponse wsResponse = WS.url(SERVER_URL + "/template_elements/" + id).get().get(TIMEOUT_MS);
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

  @Test public void updateTemplateElementTest()
  {
    running(testServer(TEST_SERVER_PORT), new Runnable()
    {
      public void run()
      {
        // Create an element
        JsonNode elementCreated = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE).post(templateElement1).get(TIMEOUT_MS).asJson();
        // Update the element created
        String id = elementCreated.get("_id").get("$oid").asText();
        String updatedName = "new name";
        JsonNode changes = Json.newObject().put("name", updatedName);
        // Service invocation - Update
        WSResponse wsResponse = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE + "/" + id).put(changes).get(TIMEOUT_MS);
        // Check response is OK
        Assert.assertEquals(wsResponse.getStatus(), OK);
        // Check Content-Type
        Assert.assertEquals(wsResponse.getHeader("Content-Type"), "application/json; charset=utf-8");
        // Retrieve updated element
        JsonNode actual = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE + "/" + id).get().get(TIMEOUT_MS).asJson();
        // Check if the modifications have been done correctly
        Assert.assertNotNull(actual.get("name"));
        Assert.assertEquals(updatedName, actual.get("name").asText());
        Assert.assertNotNull(actual.get("value"));
        Assert.assertEquals(elementCreated.get("value"), actual.get("value"));
      }
    });
  }

  @Test public void deleteTemplateElementTest()
  {
    running(testServer(TEST_SERVER_PORT), new Runnable()
    {
      public void run()
      {
        // Create an element
        JsonNode elementCreated = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE).post(templateElement1).get(TIMEOUT_MS).asJson();
        String id = elementCreated.get("_id").get("$oid").asText();
        // Service invocation - Delete
        WSResponse wsResponse = WS.url(SERVER_URL + "/template_elements/" + id).delete().get(TIMEOUT_MS);
        // Check response is OK
        Assert.assertEquals(wsResponse.getStatus(), OK);
        // Check that the element has been deleted by trying to find it by id
        WSResponse wsResponse1 = WS.url(SERVER_URL + TEMPLATE_ELEMENTS_ROUTE + "/" + id).get().get(TIMEOUT_MS);
        Assert.assertEquals(NOT_FOUND, wsResponse1.getStatus());
      }
    });
  }

  // Helper method to remove all elements from the DB
  public void deleteAllTemplateElements()
  {
    CrudController.templatesService.deleteAllTemplateElements();
  }
}
