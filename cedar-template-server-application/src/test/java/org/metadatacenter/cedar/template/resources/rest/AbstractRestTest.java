package org.metadatacenter.cedar.template.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.metadatacenter.cedar.template.resources.AbstractResourceTest;
import org.metadatacenter.cedar.template.resources.utils.TestUtil;
import org.metadatacenter.exception.TemplateServerResourceNotFoundException;
import org.metadatacenter.model.CedarNodeType;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.metadatacenter.constant.HttpConstants.HTTP_AUTH_HEADER_APIKEY_PREFIX;

public abstract class AbstractRestTest extends AbstractResourceTest {

  protected static Map<String, CedarNodeType> createdResources;

  private static final String FILE_BASE_PATH = "rest/";

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
    try {
      removeResources(createdResources);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (TemplateServerResourceNotFoundException e) {
      e.printStackTrace();
    }
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


}
