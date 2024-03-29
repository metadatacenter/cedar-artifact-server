package org.metadatacenter.cedar.artifact.resources.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoClient;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.server.service.TemplateInstanceService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.server.service.mongodb.TemplateElementServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateInstanceServiceMongoDB;
import org.metadatacenter.server.service.mongodb.TemplateServiceMongoDB;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

import static org.metadatacenter.cedar.artifact.resources.utils.TestConstants.*;

public class TestUtil {

  public static CedarConfig cedarConfig;
  public static TemplateFieldService<String, JsonNode> templateFieldService;
  public static TemplateElementService<String, JsonNode> templateElementService;
  public static TemplateService<String, JsonNode> templateService;
  public static TemplateInstanceService<String, JsonNode> templateInstanceService;

  static {
    cedarConfig = getCedarConfig();

    CedarDataServices.initializeMongoClientFactoryForDocuments(
        cedarConfig.getArtifactServerConfig().getMongoConnection());
    MongoClient mongoClientForDocuments = CedarDataServices.getMongoClientFactoryForDocuments().getClient();

    templateElementService = new TemplateElementServiceMongoDB(
        mongoClientForDocuments,
        cedarConfig.getArtifactServerConfig().getDatabaseName(),
        cedarConfig.getArtifactServerConfig().getMongoCollectionName(CedarResourceType.ELEMENT));

    templateService = new TemplateServiceMongoDB(
        mongoClientForDocuments,
        cedarConfig.getArtifactServerConfig().getDatabaseName(),
        cedarConfig.getArtifactServerConfig().getMongoCollectionName(CedarResourceType.TEMPLATE));

    templateInstanceService = new TemplateInstanceServiceMongoDB(
        mongoClientForDocuments,
        cedarConfig.getArtifactServerConfig().getDatabaseName(),
        cedarConfig.getArtifactServerConfig().getMongoCollectionName(CedarResourceType.INSTANCE));
  }

  public static CedarConfig getCedarConfig() {
    SystemComponent systemComponent = SystemComponent.SERVER_ARTIFACT;
    Map<String, String> environment = CedarEnvironmentVariableProvider.getFor(systemComponent);
    CedarConfig cedarConfig = CedarConfig.getInstance(environment);
    return cedarConfig;
  }

  public static JsonNode readFileAsJson(String path) throws IOException {
    return JsonMapper.MAPPER.readTree(TestUtil.class.getClassLoader().getResourceAsStream(path));
  }

  public static String readFileAsString(String path) throws IOException {
    InputStream is = TestUtil.class.getClassLoader().getResourceAsStream(path);
    StringWriter writer = new StringWriter();
    IOUtils.copy(is, writer, Charsets.UTF_8);
    return writer.toString();
  }

  public static String getResourceUrlRoute(String baseTestUrl, CedarResourceType resourceType) {
    String url = baseTestUrl + "/";
    if (resourceType.equals(CedarResourceType.TEMPLATE)) {
      url += TEMPLATE_ROUTE;
    } else if (resourceType.equals(CedarResourceType.ELEMENT)) {
      url += ELEMENT_ROUTE;
    } else if (resourceType.equals(CedarResourceType.INSTANCE)) {
      url += INSTANCE_ROUTE;
    }
    else {
      throw new InternalError("Wrong artifact type: " + resourceType.name());
    }
    return url;
  }

}
