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

  private final String serverUrl = "http://localhost:3333";
  private final int timeout = 10000;
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
    running(testServer(3333), new Runnable()
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
    running(testServer(3333), new Runnable()
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
    running(testServer(3333), new Runnable()
    {
      public void run()
      {
        // Service invocation - Create
        WSResponse wsResponse = WS.url(serverUrl + "/template_elements").post(templateElement1).get(timeout);
        // Check HTTP response
        Assert.assertEquals(OK, wsResponse.getStatus());
        // Check Content-Type
        Assert.assertEquals("application/json; charset=utf-8", wsResponse.getHeader("Content-Type"));
        String actualId = wsResponse.asJson().get("_id").get("$oid").asText();
        // Retrieve the element created
        JsonNode actual = WS.url(serverUrl + "/template_elements/" + actualId).get().get(timeout).asJson();
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
    running(testServer(3333), new Runnable()
    {
      public void run()
      {
        // Create two sample elements
        templateElement1 = WS.url(serverUrl + "/template_elements").post(templateElement1).get(timeout).asJson();
        templateElement2 = WS.url(serverUrl + "/template_elements").post(templateElement2).get(timeout).asJson();
        // Service invocation - Find all
        WSResponse wsResponse = WS.url(serverUrl + "/template_elements").get().get(timeout);
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
    running(testServer(3333), new Runnable()
    {
      public void run()
      {
        // Create an element
        JsonNode expected = WS.url(serverUrl + "/template_elements").post(templateElement1).get(timeout).asJson();
        String id = expected.get("_id").get("$oid").asText();
        // Service invocation - Find by Id
        WSResponse wsResponse = WS.url(serverUrl + "/template_elements/" + id).get().get(timeout);
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
    running(testServer(3333), new Runnable()
    {
      public void run()
      {
        // Create an element
        JsonNode elementCreated = WS.url(serverUrl + "/template_elements").post(templateElement1).get(timeout).asJson();
        // Update the element created
        String id = elementCreated.get("_id").get("$oid").asText();
        String updatedName = "new name";
        JsonNode changes = Json.newObject().put("name", updatedName);
        // Service invocation - Update
        WSResponse wsResponse = WS.url(serverUrl + "/template_elements/" + id).put(changes).get(timeout);
        // Check response is OK
        Assert.assertEquals(wsResponse.getStatus(), OK);
        // Check Content-Type
        Assert.assertEquals(wsResponse.getHeader("Content-Type"), "application/json; charset=utf-8");
        // Retrieve updated element
        JsonNode actual = WS.url(serverUrl + "/template_elements/" + id).get().get(timeout).asJson();
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
    running(testServer(3333), new Runnable()
    {
      public void run()
      {
        // Create an element
        JsonNode elementCreated = WS.url(serverUrl + "/template_elements").post(templateElement1).get(timeout).asJson();
        String id = elementCreated.get("_id").get("$oid").asText();
        // Service invocation - Delete
        WSResponse wsResponse = WS.url(serverUrl + "/template_elements/" + id).delete().get(timeout);
        // Check response is OK
        Assert.assertEquals(wsResponse.getStatus(), OK);
        // Check that the element has been deleted by trying to find it by id
        WSResponse wsResponse1 = WS.url(serverUrl + "/template_elements/" + id).get().get(timeout);
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
