package org.metadatacenter.cedar.template.resources.utils;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;

public class TestConstants {

  /* General constants */
  public static int DEFAULT_TIMEOUT = 3000;
  public static final String TEST_CONFIG_FILE = "test-config.yml";
  public static final String TEST_CLIENT_NAME = "TestClient";
  public static final String BASE_URL = "http://localhost";
  public static final String[] PROV_FIELDS = {"pav:createdOn", "pav:createdBy", "pav:lastUpdatedOn", "oslc:modifiedBy"};
  public static final String ID_FIELD = "@id";
  public static final String NON_EXISTENT_API_KEY = "11111111-2222-3333-4444-555555555555";
  public static final String CONTENT_TYPE_HEADER = "application/json; charset=utf-8";
  public static final String INVALID_JSON = "{sometext}";
  //public static final int TEST_SERVER_PORT = CedarConfig.getInstance().getTestConfig().getPort();
  //public static final String SERVER_URL = CedarConfig.getInstance().getTestConfig().getBase() + ":" + TEST_SERVER_PORT;
  //public static final int TIMEOUT_MS = CedarConfig.getInstance().getTestConfig().getTimeout();
  //public static final String AUTH_HEADER = TestUtils.getTestAuthHeader();
  public static final String INVALID_ID = "invalid-id";
  public static final String TEST_NAME_PATTERN = "[{index}] {method}";

  /* Templates */
  public static final String RESOURCE_TYPE_TEMPLATE = CedarNodeType.TEMPLATE.name();
  //public static final String TEMPLATE_ROUTE = CedarConfig.getInstance().getTestConfig().getTemplate().getBaseRoute();
  public static final String SAMPLE_TEMPLATE_PATH = "crud/SampleTemplate.json";
  public static final String NON_EXISTENT_TEMPLATE_ID = "https://repo.metadatacenter.net/templates/11111111-2222-3333-4444-555555555555";

  /* Template Elements */
  public static final String RESOURCE_TYPE_ELEMENT = CedarNodeType.ELEMENT.name();
  //public static final String ELEMENT_ROUTE = CedarConfig.getInstance().getTestConfig().getElement().getBaseRoute();
  public static final String SAMPLE_ELEMENT_PATH = "crud/SampleTemplateElement.json";
  public static final String NON_EXISTENT_ELEMENT_ID = "https://repo.metadatacenter.net/template-elements/11111111-2222-3333-4444-555555555555";

  /* Template Instances */
  public static final String RESOURCE_TYPE_INSTANCE = CedarNodeType.INSTANCE.name();
  //public static final String INSTANCE_ROUTE = CedarConfig.getInstance().getTestConfig().getInstance().getBaseRoute();
  public static final String SAMPLE_INSTANCE_PATH = "crud/SampleTemplateInstance.json";
  public static final String NON_EXISTENT_INSTANCE_ID = "https://repo.metadatacenter.net/template-instances/11111111-2222-3333-4444-555555555555";

  // PRIVATE //

  /**
   * The caller references the constants using Constants.EMPTY_STRING,
   * and so on. Thus, the caller should be prevented from constructing objects of
   * this class, by declaring this private constructor.
   */
  private TestConstants() {
    // This restricts instantiation
    throw new AssertionError();
  }

}
