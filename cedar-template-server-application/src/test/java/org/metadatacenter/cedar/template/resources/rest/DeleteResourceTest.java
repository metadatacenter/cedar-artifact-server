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

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLEncoder;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.metadatacenter.cedar.template.resources.utils.TestConstants.TEST_NAME_PATTERN_METHOD_PARAMS;
import static org.metadatacenter.constant.CedarConstants.SCHEMA_IS_BASED_ON;

@RunWith(JUnitParamsRunner.class)
public class DeleteResourceTest extends AbstractRestTest {

  @Test
  @TestCaseName(TEST_NAME_PATTERN_METHOD_PARAMS)
  @Parameters(method = "getParamsDeleteResource")
  public void createResourcePostTest(String jsonFileName, CedarNodeType resourceType, AuthHeaderSelector
      authSelector, IdMatchingSelector idSelector, int statusDelete, int statusFind) throws IOException {

    String createdTemplateId = null;

    // if we test the deletion of an instance, we need a template that this instance is based on
    if (resourceType == CedarNodeType.INSTANCE) {
      JsonNode minimalTemplate = getFileContentAsJson("minimal-template");
      JsonNode createdTemplate = createResource(minimalTemplate, CedarNodeType.TEMPLATE);
      createdTemplateId = createdTemplate.get(LinkedData.ID).asText();
      createdResources.put(createdTemplateId, CedarNodeType.TEMPLATE);
    }

    // Create the document first
    JsonNode jsonFromFile = getFileContentAsJson(jsonFileName);

    // if we test the deletion of an instance, we set the is based on
    if (resourceType == CedarNodeType.INSTANCE) {
      ((ObjectNode) jsonFromFile).put(SCHEMA_IS_BASED_ON, createdTemplateId);
    }

    JsonNode createdResourceJson = createResource(jsonFromFile, resourceType);
    String createdResourceId = createdResourceJson.get(LinkedData.ID).asText();
    createdResources.put(createdResourceId, resourceType);

    String authHeaderValue = getAuthHeader(authSelector);

    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    String deleteUrl = null;
    if (idSelector == IdMatchingSelector.NULL) {
      deleteUrl = url;
    } else if (idSelector == IdMatchingSelector.GIBBERISH) {
      deleteUrl = url + "/" + URLEncoder.encode("gibberish", "UTF-8");
    } else if (idSelector == IdMatchingSelector.FROM_JSON) {
      deleteUrl = url + "/" + URLEncoder.encode(createdResourceId, "UTF-8");
    }
    log.info("Delete URL   :" + deleteUrl);
    log.info("Authorization:" + authHeaderValue);
    Invocation.Builder request = testClient.target(deleteUrl).request();
    if (authHeaderValue != null) {
      request.header(AUTHORIZATION, authHeaderValue);
    }
    Response responseDelete = request.delete();
    int responseDeleteStatus = responseDelete.getStatus();
    Assert.assertEquals(statusDelete, responseDeleteStatus);

    String findUrl = url + "/" + URLEncoder.encode(createdResourceId, "UTF-8");
    authHeaderValue = authHeaderTestUser1;
    log.info("Find   URL   :" + deleteUrl);
    log.info("Authorization:" + authHeaderValue);
    Response responseFind = testClient.target(findUrl).request().header(AUTHORIZATION, authHeaderValue).get();
    int responseFindStatus = responseFind.getStatus();
    Assert.assertEquals(statusFind, responseFindStatus);
  }

  private Object getParamsDeleteResource() {
    return new Object[]{
        // Element
        new Object[]{"minimal-element", CedarNodeType.ELEMENT, AuthHeaderSelector.NULL,
            IdMatchingSelector.NULL, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{"minimal-element", CedarNodeType.ELEMENT, AuthHeaderSelector.NULL,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{"minimal-element", CedarNodeType.ELEMENT, AuthHeaderSelector.NULL,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{"minimal-element", CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.NULL, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{"minimal-element", CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{"minimal-element", CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{"minimal-element", CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.NULL, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{"minimal-element", CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{"minimal-element", CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{"minimal-element", CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.NULL, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{"minimal-element", CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.GIBBERISH, HttpConstants.NOT_FOUND, HttpConstants.OK},
        new Object[]{"minimal-element", CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.FROM_JSON, HttpConstants.NO_CONTENT, HttpConstants.NOT_FOUND},

        // Template
        new Object[]{"minimal-template", CedarNodeType.TEMPLATE, AuthHeaderSelector.NULL,
            IdMatchingSelector.NULL, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{"minimal-template", CedarNodeType.TEMPLATE, AuthHeaderSelector.NULL,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{"minimal-template", CedarNodeType.TEMPLATE, AuthHeaderSelector.NULL,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{"minimal-template", CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.NULL, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{"minimal-template", CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{"minimal-template", CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{"minimal-template", CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.NULL, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{"minimal-template", CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{"minimal-template", CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{"minimal-template", CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.NULL, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{"minimal-template", CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.GIBBERISH, HttpConstants.NOT_FOUND, HttpConstants.OK},
        new Object[]{"minimal-template", CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.FROM_JSON, HttpConstants.NO_CONTENT, HttpConstants.NOT_FOUND},

        // Instance
        new Object[]{"minimal-instance", CedarNodeType.INSTANCE, AuthHeaderSelector.NULL,
            IdMatchingSelector.NULL, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{"minimal-instance", CedarNodeType.INSTANCE, AuthHeaderSelector.NULL,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{"minimal-instance", CedarNodeType.INSTANCE, AuthHeaderSelector.NULL,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{"minimal-instance", CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.NULL, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{"minimal-instance", CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{"minimal-instance", CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{"minimal-instance", CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.NULL, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{"minimal-instance", CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{"minimal-instance", CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{"minimal-instance", CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.NULL, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{"minimal-instance", CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.GIBBERISH, HttpConstants.NOT_FOUND, HttpConstants.OK},
        new Object[]{"minimal-instance", CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.FROM_JSON, HttpConstants.NO_CONTENT, HttpConstants.NOT_FOUND},

    };
  }


}
