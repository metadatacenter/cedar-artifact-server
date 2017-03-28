package org.metadatacenter.cedar.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoClient;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.template.health.TemplateServerHealthCheck;
import org.metadatacenter.cedar.template.resources.IndexResource;
import org.metadatacenter.cedar.template.resources.TemplateElementsResource;
import org.metadatacenter.cedar.template.resources.TemplateInstancesResource;
import org.metadatacenter.cedar.template.resources.TemplatesResource;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.model.CedarNodeType;
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
  public String getName() {
    return "cedar-template-server";
  }

  @Override
  public void initializeApp(Bootstrap<TemplateServerConfiguration> bootstrap) {
    CedarDataServices.initializeMongoClientFactoryForDocuments(
        cedarConfig.getTemplateServerConfig().getMongoConnection());
    MongoClient mongoClientForDocuments = CedarDataServices.getMongoClientFactoryForDocuments().getClient();
    templateFieldService = new TemplateFieldServiceMongoDB(
        mongoClientForDocuments,
        cedarConfig.getTemplateServerConfig().getDatabaseName(),
        cedarConfig.getMongoCollectionName(CedarNodeType.FIELD));

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

    final TemplateInstancesResource instances = new TemplateInstancesResource(cedarConfig, templateInstanceService);
    environment.jersey().register(instances);

    final TemplateServerHealthCheck healthCheck = new TemplateServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);
  }
}
