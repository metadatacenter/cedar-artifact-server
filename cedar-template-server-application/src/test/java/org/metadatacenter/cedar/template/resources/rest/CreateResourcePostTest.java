package org.metadatacenter.cedar.template.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.metadatacenter.cedar.template.resources.utils.TestUtil;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.util.json.JsonMapper;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static org.metadatacenter.cedar.template.resources.utils.TestConstants.PROV_FIELDS;
import static org.metadatacenter.cedar.template.resources.utils.TestConstants.TEST_NAME_PATTERN_METHOD_PARAMS;
import static org.metadatacenter.constant.CedarConstants.SCHEMA_IS_BASED_ON;
import static org.metadatacenter.constant.HttpConstants.CREATED;
import static org.metadatacenter.constant.HttpConstants.HTTP_AUTH_HEADER_APIKEY_PREFIX;

@RunWith(JUnitParamsRunner.class)
public class CreateResourcePostTest extends AbstractRestTest {

  @Test
  @TestCaseName(TEST_NAME_PATTERN_METHOD_PARAMS)
  @Parameters(method = "getParamsCreateResource")
  public void createResourcePostTest(String jsonFileName, CedarNodeType resourceType, AuthHeaderSelector
      authSelector, int status) throws IOException {
    String originalFileContent = getFileContentAsString(jsonFileName);
    String authHeaderValue = null;
    if (authSelector == AuthHeaderSelector.TEST_USER_1) {
      authHeaderValue = authHeaderTestUser1;
    } else if (authSelector == AuthHeaderSelector.GIBBERISH_FULL) {
      authHeaderValue = "gibberish";
    } else if (authSelector == AuthHeaderSelector.GIBBERISH_KEY) {
      authHeaderValue = HTTP_AUTH_HEADER_APIKEY_PREFIX + "gibberish";
    }
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    log.info("Test URL     :" + url);
    log.info("Authorization:" + authHeaderValue);
    Invocation.Builder request = testClient.target(url).request();
    if (authHeaderValue != null) {
      request.header(AUTHORIZATION, authHeaderValue);
    }

    // if we really want to create this instance, we need to set the schema:isBasedOn to something real
    if ("minimal-instance".equals(jsonFileName) && status == CREATED) {
      JsonNode minimalTemplate = getFileContentAsJson("minimal-template");
      JsonNode createdTemplate = createResource(minimalTemplate, CedarNodeType.TEMPLATE);
      JsonNode instance = JsonMapper.MAPPER.readTree(originalFileContent);
      String createdTemplateId = createdTemplate.get(LinkedData.ID).asText();
      createdResources.put(createdTemplateId, CedarNodeType.TEMPLATE);
      ((ObjectNode) instance).put(SCHEMA_IS_BASED_ON, createdTemplateId);
      originalFileContent = JsonMapper.MAPPER.writeValueAsString(instance);
    }

    Response response;
    if (originalFileContent != null) {
      response = request.post(Entity.json(originalFileContent));
    } else {
      response = request.post(null);
    }

    int responseStatus = response.getStatus();
    Assert.assertEquals(status, responseStatus);
    // if it was created, perform other tests as well
    if (responseStatus == Response.Status.CREATED.getStatusCode()) {
      JsonNode responseJson = response.readEntity(JsonNode.class);
      // Store the resource, in order to be removed later
      createdResources.put(responseJson.get(LinkedData.ID).asText(), resourceType);

      // Retrieve the resource created
      String location = response.getHeaderString(LOCATION);
      Response readBackResponse = testClient.target(location).request().header(AUTHORIZATION, authHeaderValue).get();
      JsonNode readBackJson = readBackResponse.readEntity(JsonNode.class);

      // Check that the @id was generated
      Assert.assertNotEquals(readBackJson.get(LinkedData.ID), null);

      // Check that the provenance fields were generated
      for (String provField : PROV_FIELDS) {
        Assert.assertNotEquals(readBackJson.get(provField), null);
      }

      JsonNode originalJson = JsonMapper.MAPPER.readTree(originalFileContent);

      // Check that all the other fields contain the expected values
      ((ObjectNode) readBackJson).remove(LinkedData.ID);
      ((ObjectNode) originalJson).remove(LinkedData.ID);
      for (String provField : PROV_FIELDS) {
        ((ObjectNode) readBackJson).remove(provField);
        ((ObjectNode) originalJson).remove(provField);
      }
      Assert.assertEquals(originalJson, readBackJson);
    } else {
      JsonNode responseJson = response.readEntity(JsonNode.class);
      log.info(responseJson.asText());
    }
  }

  private Object getParamsCreateResource() {
    return new Object[]{
        // Element
        new Object[]{null, CedarNodeType.ELEMENT, AuthHeaderSelector.NULL, HttpConstants.FORBIDDEN},
        new Object[]{null, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_FULL, HttpConstants.FORBIDDEN},
        new Object[]{null, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_KEY, HttpConstants.FORBIDDEN},
        new Object[]{null, CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"non-json", CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"bad-json", CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"empty-json", CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"ui-title", CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"ui-description", CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1, HttpConstants
            .BAD_REQUEST},
        new Object[]{"minimal-element-with-id", CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1,
            HttpConstants.BAD_REQUEST},
        new Object[]{"minimal-element", CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1, HttpConstants.CREATED},
        // Template
        new Object[]{null, CedarNodeType.TEMPLATE, AuthHeaderSelector.NULL, HttpConstants.FORBIDDEN},
        new Object[]{null, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_FULL, HttpConstants.FORBIDDEN},
        new Object[]{null, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_KEY, HttpConstants.FORBIDDEN},
        new Object[]{null, CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"non-json", CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"bad-json", CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"empty-json", CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"ui-title", CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"ui-description", CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1, HttpConstants
            .BAD_REQUEST},
        new Object[]{"minimal-template-with-id", CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1,
            HttpConstants.BAD_REQUEST},
        new Object[]{"minimal-template", CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1, HttpConstants
            .CREATED},
        // Instance
        new Object[]{null, CedarNodeType.INSTANCE, AuthHeaderSelector.NULL, HttpConstants.FORBIDDEN},
        new Object[]{null, CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_FULL, HttpConstants.FORBIDDEN},
        new Object[]{null, CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_KEY, HttpConstants.FORBIDDEN},
        new Object[]{null, CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"non-json", CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"bad-json", CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"empty-json", CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"schema-name", CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1, HttpConstants.BAD_REQUEST},
        new Object[]{"schema-description", CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1, HttpConstants
            .BAD_REQUEST},
        new Object[]{"schema-name-description", CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1, HttpConstants
            .BAD_REQUEST},
        new Object[]{"minimal-instance-with-id", CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1,
            HttpConstants.BAD_REQUEST},
        new Object[]{"minimal-instance-no-template", CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1,
            HttpConstants.BAD_REQUEST},
        new Object[]{"minimal-instance", CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1, HttpConstants.CREATED},

    };
  }


}
