package org.metadatacenter.cedar.artifact;

import com.mongodb.client.MongoClient;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.artifact.health.ArtifactServerHealthCheck;
import org.metadatacenter.cedar.artifact.resources.*;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplicationWithMongo;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.MongoConfig;
import org.metadatacenter.model.ServerName;

public class ArtifactServerApplication extends CedarMicroserviceApplicationWithMongo<ArtifactServerConfiguration> {

  public static void main(String[] args) throws Exception {
    new ArtifactServerApplication().run(args);
  }

  @Override
  protected ServerName getServerName() {
    return ServerName.ARTIFACT;
  }

  @Override
  protected void initializeWithBootstrap(Bootstrap<ArtifactServerConfiguration> bootstrap, CedarConfig cedarConfig) {
  }

  @Override
  public void initializeApp() {
    MongoConfig artifactServerConfig = cedarConfig.getArtifactServerConfig();
    CedarDataServices.initializeMongoClientFactoryForDocuments(artifactServerConfig.getMongoConnection());

    MongoClient mongoClientForDocuments = CedarDataServices.getMongoClientFactoryForDocuments().getClient();

    initMongoServices(mongoClientForDocuments, artifactServerConfig);
  }

  @Override
  public void runApp(ArtifactServerConfiguration configuration, Environment environment) {

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

    final ArtifactServerHealthCheck healthCheck = new ArtifactServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);
  }
}
