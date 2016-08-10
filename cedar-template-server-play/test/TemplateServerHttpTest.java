import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.metadatacenter.config.CedarConfig;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import utils.DataServices;
import utils.TestUtils;

import java.io.File;
import java.io.IOException;

import static play.test.Helpers.*;
import static utils.TestConstants.*;

public class TemplateServerHttpTest {

  private static final int TEST_SERVER_PORT = CedarConfig.getInstance().getTestConfig().getPort();
  private static final String SERVER_URL = CedarConfig.getInstance().getTestConfig().getBase() + ":" + TEST_SERVER_PORT;
  private static final String BASE_ROUTE = CedarConfig.getInstance().getTestConfig().getTemplate().getBaseRoute();
  private static final int TIMEOUT_MS = CedarConfig.getInstance().getTestConfig().getTimeout();
  private static final String AUTH_HEADER = TestUtils.getTestAuthHeader();

  private static JsonNode template1;

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
    ObjectMapper mapper = new ObjectMapper();
    try {
      template1 = mapper.readTree(new File(SAMPLE_TEMPLATE_PATH));
    } catch (IOException e) {
      e.printStackTrace();
    }
    deleteAllTemplates();
  }

  /**
   * Tears down the test fixture.
   * (Called after every test case method.)
   */
  @After
  public void tearDown() {
    deleteAllTemplates();
  }

  @Rule
  public TestRule watcher = new TestWatcher() {
    protected void starting(Description description) {
      System.out.println("\n------ Test class: " +
          this.getClass().getName().substring(0, this.getClass().getName().indexOf("$")) +
          ", Test: " + description.getMethodName() + " ------");
    }
  };

  @Test
  public void createTemplateTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        // Service invocation - Create
        WSResponse wsResponse =
            WS.url(SERVER_URL + BASE_ROUTE).setHeader("Authorization", AUTH_HEADER).post(template1).get(TIMEOUT_MS);
        // Check HTTP response
        Assert.assertEquals(CREATED, wsResponse.getStatus());
        // Check Content-Type
        Assert.assertEquals("application/json; charset=utf-8", wsResponse.getHeader("Content-Type"));
        // Read location header
        String location = wsResponse.getHeader(LOCATION);
        // Retrieve the element created
        JsonNode actual = WS.url(location).setHeader("Authorization", AUTH_HEADER).get().get(TIMEOUT_MS).asJson();
        JsonNode expected = template1;
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
    });
  }

  @Test
  public void missingAuthorizationHeaderTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        // Service invocation - Create
        WSResponse wsResponse =
            WS.url(SERVER_URL + BASE_ROUTE).post(template1).get(TIMEOUT_MS);
        // Check HTTP response
        Assert.assertEquals(UNAUTHORIZED, wsResponse.getStatus());
      }
    });
  }
  
  /*** Helper methods ***/

  public void deleteAllTemplates() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        DataServices.getInstance().getTemplateService().deleteAllTemplates();
      }
    });
  }
}
