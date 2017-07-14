package org.metadatacenter.cedar.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoClient;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.template.health.TemplateServerHealthCheck;
import org.metadatacenter.cedar.template.resources.*;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.config.MongoConfig;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.model.ServerName;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.server.service.TemplateInstanceService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.server.service.mongodb.TemplateElementServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateFieldServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateInstanceServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateServiceMongoDB;

public class TemplateServerApplication extends CedarMicroserviceApplication<TemplateServerConfiguration> {

  protected static TemplateFieldService<String, JsonNode> templateFieldService;
  protected static TemplateElementService<String, JsonNode> templateElementService;
  protected static TemplateService<String, JsonNode> templateService;
  protected static TemplateInstanceService<String, JsonNode> templateInstanceService;

  public static void main(String[] args) throws Exception {
    new TemplateServerApplication().run(args);
  }

  @Override
  protected ServerName getServerName() {
    return ServerName.TEMPLATE;
  }

  @Override
  protected void initializeWithBootstrap(Bootstrap<TemplateServerConfiguration> bootstrap) {
  }

  @Override
  public void initializeApp() {

    MongoConfig templateServerConfig = cedarConfig.getTemplateServerConfig();
    CedarDataServices.initializeMongoClientFactoryForDocuments(templateServerConfig.getMongoConnection());

    MongoClient mongoClientForDocuments = CedarDataServices.getMongoClientFactoryForDocuments().getClient();
    templateFieldService = new TemplateFieldServiceMongoDB(mongoClientForDocuments, templateServerConfig.getDatabaseName(),
        templateServerConfig.getMongoCollectionName(CedarNodeType.FIELD));

    templateElementService = new TemplateElementServiceMongoDB(
        mongoClientForDocuments,
        templateServerConfig.getDatabaseName(),
        templateServerConfig.getMongoCollectionName(CedarNodeType.ELEMENT));

    templateService = new TemplateServiceMongoDB(
        mongoClientForDocuments,
        templateServerConfig.getDatabaseName(),
        templateServerConfig.getMongoCollectionName(CedarNodeType.TEMPLATE));

    templateInstanceService = new TemplateInstanceServiceMongoDB(
        mongoClientForDocuments,
        templateServerConfig.getDatabaseName(),
        templateServerConfig.getMongoCollectionName(CedarNodeType.INSTANCE));

  }

  @Override
  public void runApp(TemplateServerConfiguration configuration, Environment environment) {
    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    // TODO: we do not handle field now
    /*final TemplateFieldsResource fields = new TemplateFieldsResource(cedarConfig, templateFieldService);
    environment.jersey().register(fields);*/

    final TemplateElementsResource elements = new TemplateElementsResource(cedarConfig, templateElementService,
        templateFieldService);
    environment.jersey().register(elements);

    final TemplatesResource templates = new TemplatesResource(cedarConfig, templateService, templateFieldService,
        templateInstanceService);
    environment.jersey().register(templates);

    final TemplateInstancesResource instances = new TemplateInstancesResource(cedarConfig, templateInstanceService,
        templateService);
    environment.jersey().register(instances);

    final CommandResource commands = new CommandResource(cedarConfig, templateService);
    environment.jersey().register(commands);

    final TemplateServerHealthCheck healthCheck = new TemplateServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);
  }
}
