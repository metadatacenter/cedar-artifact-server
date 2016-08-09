package utils;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.*;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.server.service.*;
import org.metadatacenter.server.service.mongodb.*;

public class DataServices {

  private static DataServices instance = new DataServices();
  private static TemplateService<String, JsonNode> templateService;
  private static TemplateElementService<String, JsonNode> templateElementService;
  private static TemplateFieldService<String, JsonNode> templateFieldService;
  private static TemplateInstanceService<String, JsonNode> templateInstanceService;
  private static DiagnosticsService<JsonNode> diagnosticsService;
  private static UserService userService;
  private static CedarConfig cedarConfig;
  private static boolean initialized = false;

  public static DataServices getInstance() {
    if (!initialized) {
      // Default initializer
      init();
    }
    return instance;
  }

  // Regular initialization
  public static void init() {
    cedarConfig = CedarConfig.getInstance();
    initializeTemplateServices(cedarConfig.getMongoConfig().getDatabaseName());
    initializeOtherServices(cedarConfig.getMongoConfig().getDatabaseName());
    initialized = true;
  }

  // Initialization for testing
  public static void initForTest() {
    cedarConfig = CedarConfig.getInstance();
    initializeTemplateServices(cedarConfig.getMongoConfig().getDatabaseNameTest());
    // All the rest information, such as users, will be read from the regular DB
    initializeOtherServices(cedarConfig.getMongoConfig().getDatabaseName());
    initialized = true;
  }

  private static void initializeTemplateServices(String dbName) {
    templateElementService = new TemplateElementServiceMongoDB(
        dbName, cedarConfig.getMongoCollectionName(CedarNodeType.ELEMENT));

    templateService = new TemplateServiceMongoDB(
        dbName, cedarConfig.getMongoCollectionName(CedarNodeType.TEMPLATE),
        templateElementService);

    templateInstanceService = new TemplateInstanceServiceMongoDB(
        dbName, cedarConfig.getMongoCollectionName(CedarNodeType.INSTANCE));

    templateFieldService = new TemplateFieldServiceMongoDB(
        dbName, cedarConfig.getMongoCollectionName(CedarNodeType.FIELD));

    TemplateElementServerController.injectTemplateElementService(templateElementService);
    TemplateElementServerController.injectTemplateFieldService(templateFieldService);
    TemplateServerController.injectTemplateService(templateService);
    TemplateServerController.injectTemplateFieldService(templateFieldService);
    TemplateInstanceServerController.injectTemplateInstanceService(templateInstanceService);
    TemplateFieldServerController.injectTemplateFieldService(templateFieldService);
  }

  private static void initializeOtherServices(String dbName) {
    diagnosticsService = new DiagnosticsServiceMongoDB(dbName);
    userService = new UserServiceMongoDB(dbName, cedarConfig.getMongoCollectionName(CedarNodeType.USER));

    DiagnosticsController.injectDiagnosticsService(diagnosticsService);
  }

  public TemplateElementService<String, JsonNode> getTemplateElementService() {
    return templateElementService;
  }

  public TemplateService<String, JsonNode> getTemplateService() {
    return templateService;
  }

  public UserService getUserService() {
    return userService;
  }

  public CedarConfig getCedarConfig() {
    return cedarConfig;
  }
}