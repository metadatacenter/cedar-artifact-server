package org.metadatacenter.cedar.template.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.metadatacenter.cedar.template.TemplateServerApplication;
import org.metadatacenter.cedar.template.TemplateServerConfiguration;
import org.metadatacenter.cedar.template.resources.utils.TestConstants;
import org.metadatacenter.cedar.template.resources.utils.TestUtil;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.util.test.TestUserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.metadatacenter.cedar.template.resources.utils.TestConstants.*;
import static org.metadatacenter.cedar.template.resources.utils.TestConstants.BASED_ON_FIELD;

public abstract class AbstractResourceCrudTest {

  protected static JsonNode sampleTemplate;
  protected static JsonNode sampleElement;
  protected static JsonNode sampleInstance;

  protected static String baseTestUrl;
  protected static String authHeader;
  protected static Client testClient;

  protected static Map<String, CedarNodeType> createdResources;
  protected final Logger logger = LoggerFactory.getLogger("Test");

  static {
    try {
      sampleTemplate = TestUtil.readFileAsJson(SAMPLE_TEMPLATE_PATH);
      sampleElement = TestUtil.readFileAsJson(SAMPLE_ELEMENT_PATH);
      sampleInstance = TestUtil.readFileAsJson(SAMPLE_INSTANCE_PATH);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @ClassRule
  public static final DropwizardAppRule<TemplateServerConfiguration> SERVER_APPLICATION =
      new DropwizardAppRule<>(TemplateServerApplication.class,
          ResourceHelpers.resourceFilePath(TEST_CONFIG_FILE));

  @BeforeClass
  public static void oneTimeSetUpAbstract() {
    // Get authorization header for TestUser1
    authHeader = TestUserUtil.getTestUser1AuthHeader(TestUtil.getCedarConfig());

    // Test server url
    baseTestUrl = TestConstants.BASE_URL + ":" + SERVER_APPLICATION.getLocalPort();

    // Set up test client
    testClient = new JerseyClientBuilder(SERVER_APPLICATION.getEnvironment()).build(TEST_CLIENT_NAME);
    testClient.property(ClientProperties.READ_TIMEOUT, DEFAULT_TIMEOUT);
    testClient.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_TIMEOUT);
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
      TestUtil.removeResources(createdResources);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InstanceNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * Prints the class name and test name before running the test
   */
  @Rule
  public TestRule watcher = new TestWatcher() {
    protected void starting(Description description) {
      logger.info("------------------------------------------------------------------------");
      logger.info(description.toString());
      logger.info("------------------------------------------------------------------------");
    }
  };

  /**
   * Helpers
   */

  // Create a resource
  protected static JsonNode createResource(JsonNode resource, CedarNodeType resourceType) throws IOException {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    Response response =
        testClient.target(url).request().header("Authorization", authHeader).post(Entity.json(resource));;
    return response.readEntity(JsonNode.class);
  }

  // Creates a template and then creates and instance and sets schema:isBasedOn to the template id
  protected JsonNode setSchemaIsBasedOn(JsonNode template, JsonNode instance, CedarNodeType resourceType) {
    if (resourceType.equals(CedarNodeType.INSTANCE)) {
      try {
        JsonNode createdTemplate = createResource(template, CedarNodeType.TEMPLATE);
        String createdTemplateId = createdTemplate.get(ID_FIELD).asText();
        createdResources.put(createdTemplateId, CedarNodeType.TEMPLATE);
        ((ObjectNode)instance).put(BASED_ON_FIELD, createdTemplateId);
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
