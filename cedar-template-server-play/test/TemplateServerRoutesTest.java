import com.fasterxml.jackson.databind.JsonNode;
import controllers.TemplateServerController;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.mvc.Result;
import play.test.FakeRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static play.test.Helpers.DELETE;
import static play.test.Helpers.GET;
import static play.test.Helpers.NOT_FOUND;
import static play.test.Helpers.OK;
import static play.test.Helpers.POST;
import static play.test.Helpers.PUT;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.route;
import static play.test.Helpers.running;
import static play.test.Helpers.status;

/*
 * Integration Tests for the Application routes. They are done using a "fake application" (FakeApplication class) that
 * provides a running Application as context
 */
public class TemplateServerRoutesTest
{
  private static final String TEMPLATE_ELEMENTS_ROUTE = "/template_elements";

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
    templateElement1 = Json.newObject().
      put("@id", "http://metadatacenter.org/template_elements/682c8141-9a61-4899-9d21-7083e861b0bf").
      put("name", "template element 1 name").put("value", "template element 1 value");
    templateElement2 = Json.newObject().
      put("@id", "http://metadatacenter.org/template_elements/1dd58530-fdba-4c06-8d31-539b18296d8b").
      put("name", "template element 2 name").put("value", "template element 2 value");

    running(fakeApplication(), new Runnable()
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
    running(fakeApplication(), new Runnable()
    {
      public void run()
      {
        deleteAllTemplateElements();
      }
    });
  }

  /**
   * TEST METHODS
   */

  @Test public void createTemplateElementTest()
  {
    running(fakeApplication(), new Runnable()
    {
      public void run()
      {
        // Invoke the "Create" action using the Router
        Result result = route(new FakeRequest(POST, TEMPLATE_ELEMENTS_ROUTE).withJsonBody(templateElement1));
        // Check response is OK
        Assert.assertEquals(OK, status(result));
        // Check Content-Type
        Assert.assertEquals("application/json", contentType(result));
        // Check Charset
        Assert.assertEquals("utf-8", charset(result));
        // Check fields
        JsonNode actual = Json.parse(contentAsString(result));
        JsonNode expected = templateElement1;
        Assert.assertNotNull(actual.get("@id"));
        Assert.assertEquals(expected.get("@id"), actual.get("@id"));
        Assert.assertNotNull(actual.get("name"));
        Assert.assertEquals(expected.get("name"), actual.get("name"));
        Assert.assertNotNull(actual.get("value"));
        Assert.assertEquals(expected.get("value"), actual.get("value"));
      }
    });
  }

  @Test public void findAllTemplateElementsTest()
  {
    running(fakeApplication(), new Runnable()
    {
      public void run()
      {
        // Create two sample elements
        templateElement1 = Json
          .parse(contentAsString(route(new FakeRequest(POST, TEMPLATE_ELEMENTS_ROUTE).withJsonBody(templateElement1))));
        templateElement2 = Json
          .parse(contentAsString(route(new FakeRequest(POST, TEMPLATE_ELEMENTS_ROUTE).withJsonBody(templateElement2))));
        // Invoke the "Find All" action using the Router
        Result result = route(new FakeRequest(GET, TEMPLATE_ELEMENTS_ROUTE));
        // Check response is OK
        Assert.assertEquals(OK, status(result));
        // Check Content-Type
        Assert.assertEquals("application/json", contentType(result));
        // Check Charset
        Assert.assertEquals("utf-8", charset(result));
        // Store actual and expected results into two sets, to compare them
        Set expectedSet = new HashSet<JsonNode>();
        expectedSet.add(templateElement1);
        expectedSet.add(templateElement2);
        Set actualSet = new HashSet<JsonNode>();
        JsonNode jsonResponse = Json.parse((contentAsString(result)));
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

  @Test public void findTemplateElementByIdTest()
  {
    running(fakeApplication(), new Runnable()
    {
      public void run()
      {
        // Create an element
        JsonNode expected = Json
          .parse(contentAsString(route(new FakeRequest(POST, TEMPLATE_ELEMENTS_ROUTE).withJsonBody(templateElement1))));
        String id = expected.get("@id").asText();
        // Invoke the "Find by Id" action using the Router
        Result result = null;
        try {
          result = route(new FakeRequest(GET, TEMPLATE_ELEMENTS_ROUTE + "/" + URLEncoder.encode(id, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
        // Check response is OK
        Assert.assertEquals(OK, status(result));
        // Check Content-Type
        Assert.assertEquals("application/json", contentType(result));
        // Check Charset
        Assert.assertEquals("utf-8", charset(result));
        // Check the element retrieved
        JsonNode actual = Json.parse((contentAsString(result)));
        Assert.assertEquals(expected, actual);
      }
    });
  }

  @Test public void updateTemplateElementTest()
  {
    running(fakeApplication(), new Runnable()
    {
      public void run()
      {
        try {
        // Create an element
        JsonNode elementCreated = Json
          .parse(contentAsString(route(new FakeRequest(POST, TEMPLATE_ELEMENTS_ROUTE).withJsonBody(templateElement1))));
        // Update the element created
        String id = elementCreated.get("@id").asText();
        String updatedName = "new name";
        JsonNode changes = Json.newObject().put("name", updatedName);
        // Invoke the "Update" action using the Router
        Result result = route(new FakeRequest(PUT, TEMPLATE_ELEMENTS_ROUTE + "/" + URLEncoder.encode(id, "UTF-8")).withJsonBody(changes));
        // Check response is OK
        Assert.assertEquals(OK, status(result));
        // Check Content-Type
        Assert.assertEquals("application/json", contentType(result));
        // Check Charset
        Assert.assertEquals("utf-8", charset(result));
        // Retrieve updated element
        JsonNode actual = Json.parse(contentAsString(route(new FakeRequest(GET, TEMPLATE_ELEMENTS_ROUTE + "/" + URLEncoder.encode(id, "UTF-8")))));
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

  @Test public void deleteTemplateElementTest()
  {
    running(fakeApplication(), new Runnable()
    {
      public void run()
      {
        try {
          // Create an element
          JsonNode elementCreated = Json.parse(
            contentAsString(route(new FakeRequest(POST, TEMPLATE_ELEMENTS_ROUTE).withJsonBody(templateElement1))));
          String id = elementCreated.get("@id").asText();
          // Invoke the "Delete" action using the Router
          Result result = null;
          result = route(new FakeRequest(DELETE, TEMPLATE_ELEMENTS_ROUTE + "/" + URLEncoder.encode(id, "UTF-8")));
          // Check response is OK
          Assert.assertEquals(OK, status(result));
          // Check that the element has been deleted by trying to find it by id
          Result result1 = route(new FakeRequest(GET, TEMPLATE_ELEMENTS_ROUTE + "/" + URLEncoder.encode(id, "UTF-8")));
          Assert.assertEquals(NOT_FOUND, status(result1));
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
      }
    });
  }

  /**
   * HELPERS
   */

  // Helper method to remove all elements from the DB
  public void deleteAllTemplateElements()
  {
    TemplateServerController.templatesService.deleteAllTemplateElements();
  }
}
