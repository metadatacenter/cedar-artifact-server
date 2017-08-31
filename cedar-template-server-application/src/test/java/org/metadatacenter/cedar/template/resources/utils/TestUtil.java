package org.metadatacenter.cedar.template.resources.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoClient;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.CedarNodeType;
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

import static org.metadatacenter.cedar.template.resources.utils.TestConstants.*;

public class TestUtil {

  public static CedarConfig cedarConfig;
  public static TemplateFieldService<String, JsonNode> templateFieldService;
  public static TemplateElementService<String, JsonNode> templateElementService;
  public static TemplateService<String, JsonNode> templateService;
  public static TemplateInstanceService<String, JsonNode> templateInstanceService;

  static {
    cedarConfig = getCedarConfig();

    CedarDataServices.initializeMongoClientFactoryForDocuments(
        cedarConfig.getTemplateServerConfig().getMongoConnection());
    MongoClient mongoClientForDocuments = CedarDataServices.getMongoClientFactoryForDocuments().getClient();

    templateElementService = new TemplateElementServiceMongoDB(
        mongoClientForDocuments,
        cedarConfig.getTemplateServerConfig().getDatabaseName(),
        cedarConfig.getTemplateServerConfig().getMongoCollectionName(CedarNodeType.ELEMENT));

    templateService = new TemplateServiceMongoDB(
        mongoClientForDocuments,
        cedarConfig.getTemplateServerConfig().getDatabaseName(),
        cedarConfig.getTemplateServerConfig().getMongoCollectionName(CedarNodeType.TEMPLATE));

    templateInstanceService = new TemplateInstanceServiceMongoDB(
        mongoClientForDocuments,
        cedarConfig.getTemplateServerConfig().getDatabaseName(),
        cedarConfig.getTemplateServerConfig().getMongoCollectionName(CedarNodeType.INSTANCE));
  }

  public static CedarConfig getCedarConfig() {
    SystemComponent systemComponent = SystemComponent.SERVER_TEMPLATE;
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

  public static String getResourceUrlRoute(String baseTestUrl, CedarNodeType resourceType) {
    String url = baseTestUrl + "/";
    if (resourceType.equals(CedarNodeType.TEMPLATE)) {
      url += TEMPLATE_ROUTE;
    } else if (resourceType.equals(CedarNodeType.ELEMENT)) {
      url += ELEMENT_ROUTE;
    } else if (resourceType.equals(CedarNodeType.INSTANCE)) {
      url += INSTANCE_ROUTE;
    }
    else {
      throw new InternalError("Wrong resource type: " + resourceType.name());
    }
    return url;
  }

}