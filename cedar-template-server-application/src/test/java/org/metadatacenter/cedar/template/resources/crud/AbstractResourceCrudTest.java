package org.metadatacenter.cedar.template.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.metadatacenter.cedar.template.resources.AbstractResourceTest;
import org.metadatacenter.cedar.template.resources.utils.TestUtil;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.exception.TemplateServerResourceNotFoundException;
import org.metadatacenter.model.CedarNodeType;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.metadatacenter.cedar.template.resources.utils.TestConstants.*;
import static org.metadatacenter.model.ModelNodeNames.SCHEMA_IS_BASED_ON;

public abstract class AbstractResourceCrudTest extends AbstractResourceTest {

  protected static JsonNode sampleTemplate;
  protected static JsonNode sampleElement;
  protected static JsonNode sampleInstance;

  protected static String authHeader;

  protected static Map<String, CedarNodeType> createdResources;

  static {
    log = LoggerFactory.getLogger("Test");
    try {
      sampleTemplate = TestUtil.readFileAsJson(SAMPLE_TEMPLATE_PATH);
      sampleElement = TestUtil.readFileAsJson(SAMPLE_ELEMENT_PATH);
      sampleInstance = TestUtil.readFileAsJson(SAMPLE_INSTANCE_PATH);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @BeforeClass
  public static void oneTimeSetUpAbstract() {
    performOneTimeSetup();
    authHeader = authHeaderTestUser1;
  }

  @AfterClass
  public static void cleanUp() {
    if (testClient != null) {
      testClient.close();
    }
  }

  /**
   * Sets up the test fixture.
   * (Called before every test case method.)
   */
  @Before
  public void setUp() {
    createdResources = new HashMap<>();
  }

  /**
   * Tears down the test fixture.
   * (Called after every test case method.)
   */
  @After
  public void tearDown() {
    // Remove all resources created previously
    try {
      removeResources(createdResources);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (TemplateServerResourceNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * Helpers
   */

  // Count the number of resources of a particular type
  protected static int countResources(CedarNodeType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    Response findAllResponse = testClient.target(url).request().header("Authorization", authHeaderTestUser1).get();
    int totalCount = Integer.parseInt(findAllResponse.getHeaderString(CustomHttpConstants.HEADER_TOTAL_COUNT));
    return totalCount;
  }

  // Creates a template and then creates and instance and sets schema:isBasedOn to the template id
  protected JsonNode setSchemaIsBasedOn(JsonNode template, JsonNode instance, CedarNodeType resourceType) {
    if (resourceType.equals(CedarNodeType.INSTANCE)) {
      try {
        JsonNode createdTemplate = createResource(template, CedarNodeType.TEMPLATE);
        String createdTemplateId = createdTemplate.get(LinkedData.ID).asText();
        createdResources.put(createdTemplateId, CedarNodeType.TEMPLATE);
        ((ObjectNode) instance).put(SCHEMA_IS_BASED_ON, createdTemplateId);
        return instance;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return instance;
  }

  /***
   * Common parameters
   */

  private Object getCommonParams1() {
    return new Object[]{
        new Object[]{sampleTemplate, CedarNodeType.TEMPLATE},
        new Object[]{sampleElement, CedarNodeType.ELEMENT},
        new Object[]{sampleInstance, CedarNodeType.INSTANCE}
    };
  }

  private Object getCommonParams2() {
    return new Object[]{CedarNodeType.TEMPLATE, CedarNodeType.ELEMENT, CedarNodeType.INSTANCE};
  }

}
