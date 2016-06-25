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

  public static DataServices getInstance() {
    return instance;
  }

  private DataServices() {
    cedarConfig = CedarConfig.getInstance();

    templateElementService = new TemplateElementServiceMongoDB(
        cedarConfig.getMongoConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.ELEMENT));

    templateService = new TemplateServiceMongoDB(
        cedarConfig.getMongoConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.TEMPLATE),
        templateElementService);

    templateInstanceService = new TemplateInstanceServiceMongoDB(
        cedarConfig.getMongoConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.INSTANCE));

    templateFieldService = new TemplateFieldServiceMongoDB(
        cedarConfig.getMongoConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.FIELD));

    diagnosticsService = new DiagnosticsServiceMongoDB(
        cedarConfig.getMongoConfig().getDatabaseName());

    userService = new UserServiceMongoDB(
        cedarConfig.getMongoConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.USER));


    TemplateElementServerController.injectTemplateElementService(templateElementService);
    TemplateElementServerController.injectTemplateFieldService(templateFieldService);
    TemplateServerController.injectTemplateService(templateService);
    TemplateServerController.injectTemplateFieldService(templateFieldService);
    TemplateInstanceServerController.injectTemplateInstanceService(templateInstanceService);
    TemplateFieldServerController.injectTemplateFieldService(templateFieldService);
    DiagnosticsController.injectDiagnosticsService(diagnosticsService);
  }

  public TemplateElementService<String, JsonNode> getTemplateElementService() {
    return templateElementService;
  }

  public UserService getUserService() {
    return userService;
  }
}