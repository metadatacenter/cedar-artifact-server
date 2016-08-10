package utils;

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

  public static final String TEST_RESOURCES_PATH;
  public static final String SAMPLE_TEMPLATE_PATH = TEST_RESOURCES_PATH + "SampleTemplate.json";
  public static final String[] PROV_FIELDS = {"pav:createdOn", "pav:createdBy", "pav:lastUpdatedOn", "cedar:lastUpdatedBy"};
  public static final String ID_FIELD = "@id";



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


