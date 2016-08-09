import com.fasterxml.jackson.databind.JsonNode;
import org.junit.*;
import org.metadatacenter.config.CedarConfig;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import utils.DataServices;
import utils.TestUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static play.test.Helpers.*;

public class TemplateServerHttpTest {

  private static final int TEST_SERVER_PORT = CedarConfig.getInstance().getTestConfig().getPort();
  private static final String SERVER_URL = CedarConfig.getInstance().getTestConfig().getBase() + ":" + TEST_SERVER_PORT;
  private static final String BASE_ROUTE = CedarConfig.getInstance().getTestConfig().getTemplate().getBaseRoute();
  private static final int TIMEOUT_MS = CedarConfig.getInstance().getTestConfig().getTimeout();
  private static final String AUTH_HEADER = TestUtils.getTestAuthHeader();

  private static JsonNode template1;
  private static JsonNode template2;

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
    // TODO: create valid template
    template1 = Json.newObject().
        //put("@id", "http://metadatacenter.org/template-elements/682c8141-9a61-4899-9d21-7083e861b0bf").
            put("name", "template element 1 name").put("value", "template element 1 value");
    template2 = Json.newObject().
        //put("@id", "http://metadatacenter.org/template-elements/1dd58530-fdba-4c06-8d31-539b18296d8b").
            put("name", "template element 2 name").put("value", "template element 2 value");
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

  @Test
  public void createTemplateTest() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        // Service invocation - Create
        WSResponse wsResponse =
            WS.url(SERVER_URL + BASE_ROUTE).setHeader("Authorization", AUTH_HEADER).post(template1).get(TIMEOUT_MS);

        String createdId = wsResponse.asJson().get("@id").asText();
        // Check HTTP response
        Assert.assertEquals(CREATED, wsResponse.getStatus());
        // Check Content-Type
        Assert.assertEquals("application/json; charset=utf-8", wsResponse.getHeader("Content-Type"));
        // Retrieve the element created
        JsonNode actual = null;
        try {
          actual = WS.url(SERVER_URL + BASE_ROUTE + "/" + URLEncoder.encode(createdId, "UTF-8"))
              .setHeader("Authorization", AUTH_HEADER).get().get(TIMEOUT_MS).asJson();
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
        }
        JsonNode expected = template1;
        // Check fields
        Assert.assertNotNull(actual.get("name"));
        Assert.assertEquals(expected.get("name"), actual.get("name"));
        Assert.assertNotNull(actual.get("value"));
        Assert.assertEquals(expected.get("value"), actual.get("value"));
      }
    });
  }

  // Helper method to remove all templates from DB
  public void deleteAllTemplates() {
    running(testServer(TEST_SERVER_PORT), new Runnable() {
      public void run() {
        DataServices.getInstance().getTemplateService().deleteAllTemplates();
      }
    });
  }
}
