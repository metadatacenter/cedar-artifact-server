package org.metadatacenter.cedar.artifact.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
import junitparams.JUnitParamsRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.metadatacenter.cedar.artifact.resources.AbstractResourceTest;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.model.CedarResourceType;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static org.metadatacenter.constant.HttpConstants.HTTP_AUTH_HEADER_APIKEY_PREFIX;

@RunWith(JUnitParamsRunner.class)
public abstract class AbstractRestTest extends AbstractResourceTest {

  protected static Map<String, CedarResourceType> createdResources;

  private static final String FILE_BASE_PATH = "rest/";

  private static final String SPACES = "                                        ";
  private static final String DIVIDER = "-----------------------------------------------------------------------------";
  private static final int PARAM_NAME_OFFSET = 20;

  protected static final String SCHEMA_DESCRIPTION = "schema-description";
  protected static final String SCHEMA_NAME = "schema-name";
  protected static final String EMPTY_JSON = "empty-json";
  protected static final String BAD_JSON = "bad-json";
  protected static final String NON_JSON = "non-json";

  protected static final String MINIMAL_ELEMENT_WITH_ID = "minimal-element-with-id";
  protected static final String MINIMAL_ELEMENT_NO_ID = "minimal-element-no-id";
  protected static final String FULL_ELEMENT = "full-element";

  protected static final String MINIMAL_TEMPLATE_WITH_ID = "minimal-template-with-id";
  protected static final String MINIMAL_TEMPLATE_NO_ID = "minimal-template-no-id";
  protected static final String FULL_TEMPLATE = "full-template";

  protected boolean showTestDebug = false;

  static {
    log = LoggerFactory.getLogger("REST Test");
  }

  @BeforeClass
  public static void oneTimeSetUpAbstract() {
    performOneTimeSetup();
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
    removeResources(createdResources);
  }

  protected JsonNode getFileContentAsJson(String jsonFileName) throws IOException {
    if (jsonFileName != null) {
      String filePath = FILE_BASE_PATH + jsonFileName + ".json";
      try {
        return TestUtil.readFileAsJson(filePath);
      } catch (IOException e) {
        log.error("Error reading input file:" + filePath);
        throw e;
      }
    }
    return null;
  }

  protected String getFileContentAsString(String jsonFileName) throws IOException {
    if (jsonFileName != null) {
      String filePath = FILE_BASE_PATH + jsonFileName + ".json";
      try {
        return TestUtil.readFileAsString(filePath);
      } catch (IOException e) {
        log.error("Error reading input file:" + filePath);
        throw e;
      }
    }
    return null;
  }

  protected String getAuthHeader(AuthHeaderSelector authSelector) {
    String authHeaderValue = null;
    if (authSelector == AuthHeaderSelector.TEST_USER_1) {
      authHeaderValue = authHeaderTestUser1;
    } else if (authSelector == AuthHeaderSelector.GIBBERISH_FULL) {
      authHeaderValue = "gibberish";
    } else if (authSelector == AuthHeaderSelector.GIBBERISH_KEY) {
      authHeaderValue = HTTP_AUTH_HEADER_APIKEY_PREFIX + "gibberish";
    }
    return authHeaderValue;
  }

  protected String getUrlWithId(String url, CedarResourceType resourceType, IdMatchingSelector idSelector) throws
      UnsupportedEncodingException {
    return getUrlWithId(url, resourceType, idSelector, null);
  }

  protected String getUrlWithId(String url, CedarResourceType resourceType, IdMatchingSelector idSelector, String resourceId)
      throws UnsupportedEncodingException {
    String urlWithId = null;
    if (idSelector == IdMatchingSelector.NULL_ID) {
      urlWithId = url;
    } else if (idSelector == IdMatchingSelector.GIBBERISH) {
      urlWithId = url + "/" + URLEncoder.encode("gibberish", "UTF-8");
    } else if (idSelector == IdMatchingSelector.RANDOM_ID) {
      String uuid = linkedDataUtil.buildNewLinkedDataId(resourceType);
      urlWithId = url + "/" + URLEncoder.encode(uuid, "UTF-8");
    } else if (idSelector == IdMatchingSelector.FROM_JSON) {
      urlWithId = url + "/" + URLEncoder.encode(resourceId, "UTF-8");
    }
    return urlWithId;
  }

  protected String getUrlWithId(String url, CedarResourceType resourceType, String resourceId)
      throws UnsupportedEncodingException {
    url += "/" + resourceType.getPrefix();
    if (resourceId != null) {
      url += "/" + URLEncoder.encode(resourceId, "UTF-8");
    }
    return url;
  }

  protected void divider(String message) {
    if (!showTestDebug) {
      return;
    }
    int l = (DIVIDER.length() - message.length() - 2) / 2;
    System.out.print(DIVIDER.substring(0, l));
    System.out.print(" ");
    System.out.print(message);
    System.out.print(" ");
    System.out.print(DIVIDER.substring(0, l));
    if (l * 2 + message.length() + 2 < DIVIDER.length()) {
      System.out.print(DIVIDER.charAt(0));
    }
    System.out.println();
  }

  protected void divider() {
    if (!showTestDebug) {
      return;
    }
    System.out.println(DIVIDER);
  }

  protected void testParam(String paramName, Object paramValue) {
    pair(paramName, paramValue);
  }

  protected void pair(String name, Object value) {
    if (!showTestDebug) {
      return;
    }
    int len = PARAM_NAME_OFFSET - name.length();
    if (len < 0) {
      len = 0;
    }
    System.out.print(name);
    System.out.print(": ");
    System.out.print(SPACES.substring(0, len));
    System.out.println(value);
  }

}
