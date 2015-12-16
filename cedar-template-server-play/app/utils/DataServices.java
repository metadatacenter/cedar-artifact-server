package utils;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.TemplateElementServerController;
import controllers.TemplateInstanceServerController;
import controllers.TemplateServerController;
import org.metadatacenter.server.TemplateServerNames;
import org.metadatacenter.server.service.mongodb.TemplateElementServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateInstanceServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateServiceMongoDB;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.service.TemplateInstanceService;
import org.metadatacenter.server.service.TemplateService;
import play.Configuration;
import play.Play;

public class DataServices {

  private static DataServices instance = new DataServices();
  public static TemplateElementService<String, JsonNode> templateElementService;
  public static TemplateService<String, JsonNode> templateService;
  public static TemplateInstanceService<String, JsonNode> templateInstanceService;

  public static DataServices getInstance() {
    return instance;
  }

  private DataServices() {
    Configuration config = Play.application().configuration();
    templateElementService = new TemplateElementServiceMongoDB(
        config.getString(TemplateServerNames.MONGODB_DATABASE_NAME),
        config.getString(TemplateServerNames.TEMPLATE_ELEMENTS_COLLECTION_NAME)
    );
    templateService = new TemplateServiceMongoDB(
        config.getString(TemplateServerNames.MONGODB_DATABASE_NAME),
        config.getString(TemplateServerNames.TEMPLATES_COLLECTION_NAME),
        templateElementService
    );
    templateInstanceService = new TemplateInstanceServiceMongoDB(
        config.getString(TemplateServerNames.MONGODB_DATABASE_NAME),
        config.getString(TemplateServerNames.TEMPLATE_INSTANCES_COLLECTION_NAME)
    );

    TemplateElementServerController.injectTemplateElementService(templateElementService);
    TemplateServerController.injectTemplateService(templateService);
    TemplateInstanceServerController.injectTemplateInstanceService(templateInstanceService);
  }

  public static TemplateElementService<String, JsonNode> getTemplateElementService() {
    return templateElementService;
  }
}
