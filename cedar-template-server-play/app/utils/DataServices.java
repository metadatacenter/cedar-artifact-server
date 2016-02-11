package utils;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.*;
import org.metadatacenter.constant.ConfigConstants;
import org.metadatacenter.server.service.*;
import org.metadatacenter.server.service.mongodb.*;
import play.Configuration;
import play.Play;

public class DataServices {

  private static DataServices instance = new DataServices();
  public TemplateService<String, JsonNode> templateService;
  public TemplateElementService<String, JsonNode> templateElementService;
  public TemplateFieldService<String, JsonNode> templateFieldService;
  public TemplateInstanceService<String, JsonNode> templateInstanceService;
  public DiagnosticsService<JsonNode> diagnosticsService;

  public static DataServices getInstance() {
    return instance;
  }

  private DataServices() {
    Configuration config = Play.application().configuration();
    templateElementService = new TemplateElementServiceMongoDB(
        config.getString(ConfigConstants.MONGODB_DATABASE_NAME),
        config.getString(ConfigConstants.TEMPLATE_ELEMENTS_COLLECTION_NAME),
        config.getString(ConfigConstants.LINKED_DATA_ID_PATH_BASE) + config.getString(ConfigConstants
            .LINKED_DATA_ID_PATH_SUFFIX_TEMPLATE_ELEMENTS)
    );
    templateService = new TemplateServiceMongoDB(
        config.getString(ConfigConstants.MONGODB_DATABASE_NAME),
        config.getString(ConfigConstants.TEMPLATES_COLLECTION_NAME),
        config.getString(ConfigConstants.LINKED_DATA_ID_PATH_BASE) + config.getString(ConfigConstants
            .LINKED_DATA_ID_PATH_SUFFIX_TEMPLATES),
        templateElementService
    );
    templateInstanceService = new TemplateInstanceServiceMongoDB(
        config.getString(ConfigConstants.MONGODB_DATABASE_NAME),
        config.getString(ConfigConstants.TEMPLATE_INSTANCES_COLLECTION_NAME),
        config.getString(ConfigConstants.LINKED_DATA_ID_PATH_BASE) + config.getString(ConfigConstants
            .LINKED_DATA_ID_PATH_SUFFIX_TEMPLATE_INSTANCES)
    );
    templateFieldService = new TemplateFieldServiceMongoDB(
        config.getString(ConfigConstants.MONGODB_DATABASE_NAME),
        config.getString(ConfigConstants.TEMPLATE_FIELDS_COLLECTION_NAME),
        config.getString(ConfigConstants.LINKED_DATA_ID_PATH_BASE) + config.getString(ConfigConstants
            .LINKED_DATA_ID_PATH_SUFFIX_TEMPLATE_FIELDS)
    );
    diagnosticsService = new DiagnosticsServiceMongoDB(config.getString(ConfigConstants.MONGODB_DATABASE_NAME));

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
}
