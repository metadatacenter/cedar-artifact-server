package org.metadatacenter.templates.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.metadatacenter.templates.TemplatesService;
import org.metadatacenter.templates.TemplatesServiceMongoDB;
import org.metadatacenter.templates.utils.PropertiesManager;

import javax.management.InstanceNotFoundException;
import java.io.IOException;

public class MainServiceTest
{

  private static TemplatesService<String, JsonNode> mainService;
  private static JsonNode templateElement1;
  private static JsonNode templateElement2;

  /**
   * One-time initialization code.
   * (Called once before any of the test methods in the class).
   */
//  @BeforeClass public static void oneTimeSetUp()
//  {
//    mainService = new TemplatesServiceMongoDB(PropertiesManager.getProperty("mongodb.db_test"),
//      PropertiesManager.getProperty("mongodb.collections.template_elements"),
//      PropertiesManager.getProperty("mongodb.collections.templates"));
//  }

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
    templateElement1 = JsonNodeFactory.instance.objectNode().put("name", "element1 name").put("value", "element1 value");
    templateElement2 = JsonNodeFactory.instance.objectNode().put("name", "element2 name").put("value", "element2 value");
    deleteAllTemplateElements();
  }

  /**
   * Tears down the test fixture.
   * (Called after every test case method.)
   */
  @After public void tearDown()
  {
    deleteAllTemplateElements();
  }

  /**
   * TEST METHODS
   */
//  @Test public void createTElementTest()
//  {
//    try {
//      // Create an element
//      JsonNode e = mainService.createTemplateElement(templateElement1);
//      // Retrieve the element created
//      JsonNode actual = mainService.findTemplateElement(e.get("_id").get("$oid").asText());
//      JsonNode expected = templateElement1;
//      // Check fields
//      Assert.assertNotNull(actual.get("name"));
//      Assert.assertEquals(expected.get("name"), actual.get("name"));
//      Assert.assertNotNull(actual.get("value"));
//      Assert.assertEquals(expected.get("value"), actual.get("value"));
//    } catch (IOException e) {
//      e.printStackTrace();
//    } catch (InstanceNotFoundException e) {
//      e.printStackTrace();
//    }
//  }

  // TODO: Implement more tests

  //  @Test public void findAllTElementsTest()
  //  {
  //
  //  }
  //
  //  @Test public void findTElementByIdTest()
  //  {
  //
  //  }
  //
  //  @Test public void updateTElementTest()
  //  {
  //
  //  }
  //
  //  @Test public void deleteTElementTest()
  //  {
  //
  //  }
  //
  //  @Test public void existsTElementTest()
  //  {
  //
  //  }
  //
  //  @Test public void deleteAllTElementsTest()
  //  {
  //
  //  }

  /**
   * HELPERS
   */

  // Helper method to remove all elements from the DB
  public void deleteAllTemplateElements()
  {
    mainService.deleteAllTemplateElements();
  }

}
