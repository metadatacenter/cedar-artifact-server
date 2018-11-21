package org.metadatacenter.cedar.template;

import com.mongodb.MongoClient;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.template.health.TemplateServerHealthCheck;
import org.metadatacenter.cedar.template.resources.*;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplicationWithMongo;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.MongoConfig;
import org.metadatacenter.model.ServerName;

public class TemplateServerApplication extends CedarMicroserviceApplicationWithMongo<TemplateServerConfiguration> {

  public static void main(String[] args) throws Exception {
    new TemplateServerApplication().run(args);
  }

  @Override
  protected ServerName getServerName() {
    return ServerName.TEMPLATE;
  }

  @Override
  protected void initializeWithBootstrap(Bootstrap<TemplateServerConfiguration> bootstrap, CedarConfig cedarConfig) {
  }

  @Override
  public void initializeApp() {
    MongoConfig templateServerConfig = cedarConfig.getTemplateServerConfig();
    CedarDataServices.initializeMongoClientFactoryForDocuments(templateServerConfig.getMongoConnection());

    MongoClient mongoClientForDocuments = CedarDataServices.getMongoClientFactoryForDocuments().getClient();

    initMongoServices(mongoClientForDocuments, templateServerConfig);
  }

  @Override
  public void runApp(TemplateServerConfiguration configuration, Environment environment) {

    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    final TemplateFieldsResource fields = new TemplateFieldsResource(cedarConfig, templateFieldService);
    environment.jersey().register(fields);

    final TemplateElementsResource elements = new TemplateElementsResource(cedarConfig, templateElementService);
    environment.jersey().register(elements);

    final TemplatesResource templates = new TemplatesResource(cedarConfig, templateService, templateInstanceService);
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
