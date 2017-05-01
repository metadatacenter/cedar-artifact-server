package org.metadatacenter.cedar.template.resources.utils;

public class TestConstants {

  /* General constants */
  public static final String BASE_URL = "http://localhost";
  public static int DEFAULT_TIMEOUT = 3000;
  public static final String TEST_CONFIG_FILE = "test-config.yml";
  public static final String TEST_CLIENT_NAME = "TestClient";
  public static final String[] PROV_FIELDS = {"pav:createdOn", "pav:createdBy", "pav:lastUpdatedOn", "oslc:modifiedBy"};
  public static final String LAST_UPDATED_ON_FIELD = "pav:lastUpdatedOn";
  public static final String ID_FIELD = "@id";
  public static final String NON_EXISTENT_API_KEY = "11111111-2222-3333-4444-555555555555";
  public static final String INVALID_JSON = "{sometext}";
  public static final String INVALID_ID = "invalid-id";
  public static final String TEST_NAME_PATTERN = "[{index}] {method}";

  /* Templates */
  public static final String TEMPLATE_ROUTE = "templates";
  public static final String SAMPLE_TEMPLATE_PATH = "crud/SampleTemplate.json";
  public static final String NON_EXISTENT_TEMPLATE_ID = "https://repo.metadatacenter.net/templates/11111111-2222-3333-4444-555555555555";

  /* Template Elements */
  public static final String ELEMENT_ROUTE = "template-elements";
  public static final String SAMPLE_ELEMENT_PATH = "crud/SampleTemplateElement.json";
  public static final String NON_EXISTENT_ELEMENT_ID = "https://repo.metadatacenter.net/template-elements/11111111-2222-3333-4444-555555555555";

  /* Template Instances */
  public static final String INSTANCE_ROUTE = "template-instances";
  public static final String SAMPLE_INSTANCE_PATH = "crud/SampleTemplateInstance.json";
  public static final String NON_EXISTENT_INSTANCE_ID = "https://repo.metadatacenter.net/template-instances/11111111-2222-3333-4444-555555555555";
  public static final String BASED_ON_FIELD = "schema:isBasedOn";

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
