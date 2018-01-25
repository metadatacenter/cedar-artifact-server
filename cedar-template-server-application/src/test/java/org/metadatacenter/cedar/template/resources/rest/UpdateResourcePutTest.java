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
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.model.ModelNodeNames;
import org.metadatacenter.model.ModelPaths;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Random;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.metadatacenter.cedar.template.resources.rest.AuthHeaderSelector.*;
import static org.metadatacenter.cedar.template.resources.rest.IdMatchingSelector.*;
import static org.metadatacenter.cedar.template.resources.utils.TestConstants.TEST_NAME_PATTERN_METHOD_PARAMS;
import static org.metadatacenter.constant.CedarConstants.SCHEMA_IS_BASED_ON;
import static org.metadatacenter.constant.HttpConstants.*;
import static org.metadatacenter.model.CedarNodeType.*;

@RunWith(JUnitParamsRunner.class)
public class UpdateResourcePutTest extends AbstractRestTest {

  private final static String UPDATED_DESCRIPTION = "Updated description";

  @Test
  @TestCaseName(TEST_NAME_PATTERN_METHOD_PARAMS)
  @Parameters(method = "getParamsUpdateResource")
  public void updateResourcePutTest(String jsonFileName, CedarNodeType resourceType, AuthHeaderSelector
      authSelector, IdMatchingSelector idInUrlSelector, IdMatchingSelector idInBodySelector, int statusPut) throws
      IOException {

    // Read the document
    JsonNode jsonFromFile = getFileContentAsJson(jsonFileName);
    // if we test an instance, we need to set the schema:isBasedOn to something real
    if (resourceType == INSTANCE) {
      JsonNode minimalTemplate = getFileContentAsJson(MINIMAL_TEMPLATE);
      JsonNode createdTemplate = createResource(minimalTemplate, TEMPLATE);
      String createdTemplateId = createdTemplate.get(LinkedData.ID).asText();
      ((ObjectNode) jsonFromFile).put(SCHEMA_IS_BASED_ON, createdTemplateId);
      createdResources.put(createdTemplateId, CedarNodeType.TEMPLATE);
    }
    // Create the document first
    JsonNode createdResourceJson = createResource(jsonFromFile, resourceType);
    String createdResourceId = createdResourceJson.get(LinkedData.ID).asText();
    createdResources.put(createdResourceId, resourceType);

    String authHeaderValue = getAuthHeader(authSelector);

    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    String updateUrl = getUrlWithId(url, resourceType, idInUrlSelector, createdResourceId);

    log.info("Update URL   :" + updateUrl);
    log.info("Authorization:" + authHeaderValue);
    Invocation.Builder request = testClient.target(updateUrl).request();
    if (authHeaderValue != null) {
      request.header(AUTHORIZATION, authHeaderValue);
    }
    if (idInBodySelector == FROM_JSON) {
      ((ObjectNode) createdResourceJson).put(LinkedData.ID, createdResourceId);
    } else if (idInBodySelector == GIBBERISH) {
      ((ObjectNode) createdResourceJson).put(LinkedData.ID, "gibberish");
    }

    String updatedDescription = UPDATED_DESCRIPTION + " " + new Random().nextInt();
    String pointerToCheck = null;
    ((ObjectNode) createdResourceJson).put(ModelNodeNames.SCHEMA_DESCRIPTION, updatedDescription);
    pointerToCheck = ModelPaths.SCHEMA_DESCRIPTION;
    Response responsePut = request.put(Entity.json(createdResourceJson));
    int responsePutStatus = responsePut.getStatus();
    Assert.assertEquals(statusPut, responsePutStatus);
    if (statusPut == OK) {
      Assert.assertEquals(updatedDescription, responsePut.readEntity(JsonNode.class).at(pointerToCheck).textValue());
    }
  }

  private Object getParamsUpdateResource() {
    return new Object[]{
        // Element

        new Object[]{MINIMAL_ELEMENT_WITH_ID, ELEMENT, NULL_AUTH, NULL_ID, FROM_JSON, METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_ELEMENT_WITH_ID, ELEMENT, NULL_AUTH, GIBBERISH, FROM_JSON, FORBIDDEN},
        new Object[]{MINIMAL_ELEMENT_WITH_ID, ELEMENT, NULL_AUTH, FROM_JSON, FROM_JSON, FORBIDDEN},

        new Object[]{MINIMAL_ELEMENT_WITH_ID, ELEMENT, GIBBERISH_FULL, NULL_ID, FROM_JSON, METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_ELEMENT_WITH_ID, ELEMENT, GIBBERISH_FULL, GIBBERISH, FROM_JSON, FORBIDDEN},
        new Object[]{MINIMAL_ELEMENT_WITH_ID, ELEMENT, GIBBERISH_FULL, FROM_JSON, FROM_JSON, FORBIDDEN},

        new Object[]{MINIMAL_ELEMENT_WITH_ID, ELEMENT, GIBBERISH_KEY, NULL_ID, FROM_JSON, METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_ELEMENT_WITH_ID, ELEMENT, GIBBERISH_KEY, GIBBERISH, FROM_JSON, FORBIDDEN},
        new Object[]{MINIMAL_ELEMENT_WITH_ID, ELEMENT, GIBBERISH_KEY, FROM_JSON, FROM_JSON, FORBIDDEN},

        new Object[]{MINIMAL_ELEMENT_WITH_ID, ELEMENT, TEST_USER_1, NULL_ID, FROM_JSON, METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_ELEMENT_WITH_ID, ELEMENT, TEST_USER_1, GIBBERISH, FROM_JSON, BAD_REQUEST},
        new Object[]{MINIMAL_ELEMENT_WITH_ID, ELEMENT, TEST_USER_1, FROM_JSON, GIBBERISH, BAD_REQUEST},

        new Object[]{MINIMAL_ELEMENT_WITH_ID, ELEMENT, TEST_USER_1, FROM_JSON, FROM_JSON, OK},

        // Template
        new Object[]{MINIMAL_TEMPLATE, TEMPLATE, NULL_AUTH, NULL_ID, FROM_JSON, METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_TEMPLATE, TEMPLATE, NULL_AUTH, GIBBERISH, FROM_JSON, FORBIDDEN},
        new Object[]{MINIMAL_TEMPLATE, TEMPLATE, NULL_AUTH, FROM_JSON, FROM_JSON, FORBIDDEN},

        new Object[]{MINIMAL_TEMPLATE, TEMPLATE, GIBBERISH_FULL, NULL_ID, FROM_JSON, METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_TEMPLATE, TEMPLATE, GIBBERISH_FULL, GIBBERISH, FROM_JSON, FORBIDDEN},
        new Object[]{MINIMAL_TEMPLATE, TEMPLATE, GIBBERISH_FULL, FROM_JSON, FROM_JSON, FORBIDDEN},

        new Object[]{MINIMAL_TEMPLATE, TEMPLATE, GIBBERISH_KEY, NULL_ID, FROM_JSON, METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_TEMPLATE, TEMPLATE, GIBBERISH_KEY, GIBBERISH, FROM_JSON, FORBIDDEN},
        new Object[]{MINIMAL_TEMPLATE, TEMPLATE, GIBBERISH_KEY, FROM_JSON, FROM_JSON, FORBIDDEN},

        new Object[]{MINIMAL_TEMPLATE, TEMPLATE, TEST_USER_1, NULL_ID, FROM_JSON, METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_TEMPLATE, TEMPLATE, TEST_USER_1, GIBBERISH, FROM_JSON, BAD_REQUEST},
        new Object[]{MINIMAL_TEMPLATE, TEMPLATE, TEST_USER_1, FROM_JSON, GIBBERISH, BAD_REQUEST},

        new Object[]{MINIMAL_TEMPLATE, TEMPLATE, TEST_USER_1, FROM_JSON, FROM_JSON, OK},

        // Instance
        new Object[]{MINIMAL_INSTANCE, INSTANCE, NULL_AUTH, NULL_ID, FROM_JSON, METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_INSTANCE, INSTANCE, NULL_AUTH, GIBBERISH, FROM_JSON, FORBIDDEN},
        new Object[]{MINIMAL_INSTANCE, INSTANCE, NULL_AUTH, FROM_JSON, FROM_JSON, FORBIDDEN},

        new Object[]{MINIMAL_INSTANCE, INSTANCE, GIBBERISH_FULL, NULL_ID, FROM_JSON, METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_INSTANCE, INSTANCE, GIBBERISH_FULL, GIBBERISH, FROM_JSON, FORBIDDEN,},
        new Object[]{MINIMAL_INSTANCE, INSTANCE, GIBBERISH_FULL, FROM_JSON, FROM_JSON, FORBIDDEN},

        new Object[]{MINIMAL_INSTANCE, INSTANCE, GIBBERISH_KEY, NULL_ID, FROM_JSON, METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_INSTANCE, INSTANCE, GIBBERISH_KEY, GIBBERISH, FROM_JSON, FORBIDDEN},
        new Object[]{MINIMAL_INSTANCE, INSTANCE, GIBBERISH_KEY, FROM_JSON, FROM_JSON, FORBIDDEN},

        new Object[]{MINIMAL_INSTANCE, INSTANCE, TEST_USER_1, NULL_ID, FROM_JSON, METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_INSTANCE, INSTANCE, TEST_USER_1, GIBBERISH, FROM_JSON, BAD_REQUEST},
        new Object[]{MINIMAL_INSTANCE, INSTANCE, TEST_USER_1, FROM_JSON, GIBBERISH, BAD_REQUEST},

        new Object[]{MINIMAL_INSTANCE, INSTANCE, TEST_USER_1, FROM_JSON, FROM_JSON, OK},

    };
  }


}
