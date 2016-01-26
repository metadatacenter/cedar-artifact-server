package utils;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.TemplateElementRepoServerController;
import controllers.TemplateFieldRepoServerController;
import controllers.TemplateInstanceRepoServerController;
import controllers.TemplateRepoServerController;
import org.metadatacenter.server.Constants;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.server.service.TemplateInstanceService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.server.service.mongodb.TemplateElementServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateFieldServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateInstanceServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateServiceMongoDB;
import play.Configuration;
import play.Play;

public class DataServices {

  private static DataServices instance = new DataServices();
  public static TemplateElementService<String, JsonNode> templateElementService;
  public static TemplateService<String, JsonNode> templateService;
  public static TemplateFieldService<String, JsonNode> templateFieldService;
  public static TemplateInstanceService<String, JsonNode> templateInstanceService;

  public static DataServices getInstance() {
    return instance;
  }

  private DataServices() {
    Configuration config = Play.application().configuration();
    templateElementService = new TemplateElementServiceMongoDB(
        config.getString(Constants.MONGODB_DATABASE_NAME),
        config.getString(Constants.TEMPLATE_ELEMENTS_COLLECTION_NAME),
        config.getString(Constants.LINKED_DATA_ID_PATH_BASE) + config.getString(Constants
            .LINKED_DATA_ID_PATH_SUFFIX_TEMPLATE_ELEMENTS)
    );
    templateService = new TemplateServiceMongoDB(
        config.getString(Constants.MONGODB_DATABASE_NAME),
        config.getString(Constants.TEMPLATES_COLLECTION_NAME),
        config.getString(Constants.LINKED_DATA_ID_PATH_BASE) + config.getString(Constants
            .LINKED_DATA_ID_PATH_SUFFIX_TEMPLATES),
        templateElementService
    );
    templateInstanceService = new TemplateInstanceServiceMongoDB(
        config.getString(Constants.MONGODB_DATABASE_NAME),
        config.getString(Constants.TEMPLATE_INSTANCES_COLLECTION_NAME),
        config.getString(Constants.LINKED_DATA_ID_PATH_BASE) + config.getString(Constants
            .LINKED_DATA_ID_PATH_SUFFIX_TEMPLATE_INSTANCES)
    );
    templateFieldService = new TemplateFieldServiceMongoDB(
        config.getString(Constants.MONGODB_DATABASE_NAME),
        config.getString(Constants.TEMPLATE_FIELDS_COLLECTION_NAME),
        config.getString(Constants.LINKED_DATA_ID_PATH_BASE) + config.getString(Constants
            .LINKED_DATA_ID_PATH_SUFFIX_TEMPLATE_FIELDS)
    );

    TemplateElementRepoServerController.injectTemplateElementService(templateElementService);
    TemplateRepoServerController.injectTemplateService(templateService);
    TemplateInstanceRepoServerController.injectTemplateInstanceService(templateInstanceService);
    TemplateFieldRepoServerController.injectTemplateFieldService(templateFieldService);

  }

  public static TemplateElementService<String, JsonNode> getTemplateElementService() {
    return templateElementService;
  }
}
