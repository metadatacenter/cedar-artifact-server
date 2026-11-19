package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.metadatacenter.cedar.artifact.resources.AbstractResourceTest;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.model.CedarResourceType;

import java.io.IOException;

import static org.metadatacenter.cedar.artifact.resources.utils.TestConstants.*;
import static org.metadatacenter.model.ModelNodeNames.SCHEMA_IS_BASED_ON;

public abstract class AbstractResourceCrudTest extends AbstractResourceTest {

  protected JsonNode sampleTemplate;
  protected JsonNode sampleElement;
  protected JsonNode sampleField;
  protected JsonNode sampleInstance;

  protected String authHeader;

  /**
   * Runs after the harness has redirected the environment. Reading the samples loads TestUtil, whose
   * static initializer opens the document store the environment names, so it must not run when the
   * class loads.
   */
  @BeforeAll
  public void oneTimeSetUpAbstract() throws IOException {
    sampleTemplate = TestUtil.readFileAsJson(SAMPLE_TEMPLATE_PATH);
    sampleElement = TestUtil.readFileAsJson(SAMPLE_ELEMENT_PATH);
    sampleField = TestUtil.readFileAsJson(SAMPLE_FIELD_PATH);
    sampleInstance = TestUtil.readFileAsJson(SAMPLE_INSTANCE_PATH);
    performOneTimeSetup();
    authHeader = authHeaderTestUser1;
  }

  /**
   * Helpers
   */

  // Count the number of resources of a particular type
  protected int countResources(CedarResourceType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    Response findAllResponse = testClient.target(url).request().header("Authorization", authHeaderTestUser1).get();
    return findAllResponse.readEntity(JsonNode.class).get("totalCount").asInt();
  }

  // Creates a artifact and then creates and instance and sets schema:isBasedOn to the artifact id
  protected JsonNode setSchemaIsBasedOn(JsonNode template, JsonNode instance, CedarResourceType resourceType) {
    if (resourceType.equals(CedarResourceType.INSTANCE)) {
      JsonNode createdTemplate = createResource(template, CedarResourceType.TEMPLATE);
      String createdTemplateId = createdTemplate.get(LinkedData.ID).asText();
      createdResources.put(createdTemplateId, CedarResourceType.TEMPLATE);
      ((ObjectNode) instance).put(SCHEMA_IS_BASED_ON, createdTemplateId);
      return instance;
    }
    return instance;
  }

  /***
   * Common parameters
   */

  protected Object[] getCommonParams1() {
    return new Object[]{
        new Object[]{sampleTemplate, CedarResourceType.TEMPLATE},
        new Object[]{sampleElement, CedarResourceType.ELEMENT},
        new Object[]{sampleField, CedarResourceType.FIELD},
        new Object[]{sampleInstance, CedarResourceType.INSTANCE}
    };
  }

}
