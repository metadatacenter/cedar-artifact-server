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
    templateService = new TemplateServiceMongoDB(
        dbName, cedarConfig.getMongoCollectionName(CedarNodeType.TEMPLATE),
        templateElementService);

    templateElementService = new TemplateElementServiceMongoDB(
        dbName, cedarConfig.getMongoCollectionName(CedarNodeType.ELEMENT));

    templateFieldService = new TemplateFieldServiceMongoDB(
        dbName, cedarConfig.getMongoCollectionName(CedarNodeType.FIELD));

    templateInstanceService = new TemplateInstanceServiceMongoDB(
        dbName, cedarConfig.getMongoCollectionName(CedarNodeType.INSTANCE));

    TemplateServerController.injectTemplateService(templateService);
    TemplateServerController.injectTemplateFieldService(templateFieldService);

    TemplateElementServerController.injectTemplateElementService(templateElementService);
    TemplateElementServerController.injectTemplateFieldService(templateFieldService);

    TemplateFieldServerController.injectTemplateFieldService(templateFieldService);

    TemplateInstanceServerController.injectTemplateInstanceService(templateInstanceService);
  }

  private static void initializeOtherServices(String dbName) {
    diagnosticsService = new DiagnosticsServiceMongoDB(dbName);
    userService = new UserServiceMongoDB(dbName, cedarConfig.getMongoCollectionName(CedarNodeType.USER));

    DiagnosticsController.injectDiagnosticsService(diagnosticsService);
  }

  public TemplateService<String, JsonNode> getTemplateService() {
    return templateService;
  }

  public TemplateElementService<String, JsonNode> getTemplateElementService() {
    return templateElementService;
  }

  public TemplateFieldService<String, JsonNode> getTemplateFieldService() {
    return templateFieldService;
  }

  public TemplateInstanceService<String, JsonNode> getTemplateInstanceService() {
    return templateInstanceService;
  }

  public UserService getUserService() {
    return userService;
  }

  public CedarConfig getCedarConfig() {
    return cedarConfig;
  }
}