package org.metadatacenter.cedar.template.resources.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoClient;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.MongoConfig;
import org.metadatacenter.config.MongoConnection;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.server.service.TemplateInstanceService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.server.service.mongodb.TemplateElementServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateFieldServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateInstanceServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateServiceMongoDB;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;

public class TestUtil {

  public static CedarConfig cedarConfig;
  public static TemplateFieldService<String, JsonNode> templateFieldService;
  public static TemplateElementService<String, JsonNode> templateElementService;
  public static TemplateService<String, JsonNode> templateService;
  public static TemplateInstanceService<String, JsonNode> templateInstanceService;

  static {
    cedarConfig = CedarConfig.getInstance();

    //MongoConnection mongoConnectionTest = cedarConfig.getTemplateServerConfig().getMongoConnection();
    //mongoConnectionTest.setDatabaseName(cedarConfig.getTemplateServerConfig().getDatabaseNameTest());

    CedarDataServices.initializeMongoClientFactoryForDocuments(
        cedarConfig.getTemplateServerConfig().getMongoConnection());
    MongoClient mongoClientForDocuments = CedarDataServices.getMongoClientFactoryForDocuments().getClient();

    templateElementService = new TemplateElementServiceMongoDB(
        mongoClientForDocuments,
        cedarConfig.getTemplateServerConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.ELEMENT));

    templateService = new TemplateServiceMongoDB(
        mongoClientForDocuments,
        cedarConfig.getTemplateServerConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.TEMPLATE));

    templateInstanceService = new TemplateInstanceServiceMongoDB(
        mongoClientForDocuments,
        cedarConfig.getTemplateServerConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.INSTANCE));
  }

  public static JsonNode readFileAsJson(String path) throws IOException {
    return JsonMapper.MAPPER.readTree(TestUtil.class.getClassLoader().getResourceAsStream(path));
  }

  public static void deleteAllResources() {
    templateService.deleteAllTemplates();;
    templateElementService.deleteAllTemplateElements();
    templateInstanceService.deleteAllTemplateInstances();
  }

}