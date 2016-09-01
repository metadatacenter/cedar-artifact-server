package utils;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;

public class TestConstants {

  static {
    String cedarPath = System.getenv("CEDAR_HOME");
    // E.g., if running it from intelliJ
    if (cedarPath == null) {
      String workingFolder = System.getProperty("user.dir");
      String projectFolderName = "cedar-template-server/";
      cedarPath = workingFolder.substring(0, workingFolder.indexOf(projectFolderName));
    }
    TEST_RESOURCES_PATH = cedarPath + "cedar-template-server/cedar-template-server-play/test/resources/";
  }

  /* General constants */
  public static final String TEST_RESOURCES_PATH;
  public static final String[] PROV_FIELDS = {"pav:createdOn", "pav:createdBy", "pav:lastUpdatedOn", "oslc:modifiedBy"};
  public static final String ID_FIELD = "@id";
  public static final String NON_EXISTENT_API_KEY = "11111111-2222-3333-4444-555555555555";
  public static final String CONTENT_TYPE_HEADER = "application/json; charset=utf-8";
  public static final String INVALID_JSON = "{sometext}";
  public static final int TEST_SERVER_PORT = CedarConfig.getInstance().getTestConfig().getPort();
  public static final String SERVER_URL = CedarConfig.getInstance().getTestConfig().getBase() + ":" + TEST_SERVER_PORT;
  public static final int TIMEOUT_MS = CedarConfig.getInstance().getTestConfig().getTimeout();
  public static final String AUTH_HEADER = TestUtils.getTestAuthHeader();
  public static final String INVALID_ID = "invalid-id";
  public static final String TEST_NAME_PATTERN = "[{index}] {method}";

  /* Templates */
  public static final String RESOURCE_TYPE_TEMPLATE = CedarNodeType.TEMPLATE.name();
  public static final String TEMPLATE_ROUTE = CedarConfig.getInstance().getTestConfig().getTemplate().getBaseRoute();
  public static final String SAMPLE_TEMPLATE_PATH = TEST_RESOURCES_PATH + "SampleTemplate.json";
  public static final String NON_EXISTENT_TEMPLATE_ID = "https://repo.metadatacenter.net/templates/11111111-2222-3333-4444-555555555555";

  /* Template Elements */
  public static final String RESOURCE_TYPE_ELEMENT = CedarNodeType.ELEMENT.name();
  public static final String ELEMENT_ROUTE = CedarConfig.getInstance().getTestConfig().getElement().getBaseRoute();
  public static final String SAMPLE_ELEMENT_PATH = TEST_RESOURCES_PATH + "SampleElement.json";
  public static final String NON_EXISTENT_ELEMENT_ID = "https://repo.metadatacenter.net/template-elements/11111111-2222-3333-4444-555555555555";

  /* Template Fields */
  public static final String RESOURCE_TYPE_FIELD = CedarNodeType.FIELD.name();
  public static final String FIELD_ROUTE = CedarConfig.getInstance().getTestConfig().getField().getBaseRoute();
  public static final String SAMPLE_FIELD_PATH = TEST_RESOURCES_PATH + "SampleField.json";
  public static final String NON_EXISTENT_FIELD_ID = "https://repo.metadatacenter.net/template-fields/11111111-2222-3333-4444-555555555555";

  /* Template Instances */
  public static final String RESOURCE_TYPE_INSTANCE = CedarNodeType.INSTANCE.name();
  public static final String INSTANCE_ROUTE = CedarConfig.getInstance().getTestConfig().getInstance().getBaseRoute();
  public static final String SAMPLE_INSTANCE_PATH = TEST_RESOURCES_PATH + "SampleInstance.json";
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


