package org.metadatacenter.cedar.template.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Ignore;
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
  @Ignore
  public void deleteResourceTest(String jsonFileName, CedarNodeType resourceType, AuthHeaderSelector authSelector,
                                 IdMatchingSelector idSelector, int statusDelete, int statusFind) throws IOException {

    String createdTemplateId = null;

    // if we test the deletion of an instance, we need a template that this instance is based on
    if (resourceType == CedarNodeType.INSTANCE) {
      JsonNode minimalTemplate = getFileContentAsJson(MINIMAL_TEMPLATE);
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
    String deleteUrl = getUrlWithId(url, resourceType, idSelector, createdResourceId);

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

  @Test
  @TestCaseName(TEST_NAME_PATTERN_METHOD_PARAMS)
  public void deleteTemplateReferencedByInstanceTest() throws IOException {
  }

  private Object getParamsDeleteResource() {
    return new Object[]{
        // Element
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.NULL_AUTH,
            IdMatchingSelector.NULL_ID, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.NULL_AUTH,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.NULL_AUTH,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.NULL_ID, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.NULL_ID, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.NULL_ID, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.GIBBERISH, HttpConstants.NOT_FOUND, HttpConstants.OK},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.FROM_JSON, HttpConstants.NO_CONTENT, HttpConstants.NOT_FOUND},

        // Template
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.NULL_AUTH,
            IdMatchingSelector.NULL_ID, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.NULL_AUTH,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.NULL_AUTH,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.NULL_ID, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.NULL_ID, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.NULL_ID, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.GIBBERISH, HttpConstants.NOT_FOUND, HttpConstants.OK},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.FROM_JSON, HttpConstants.NO_CONTENT, HttpConstants.NOT_FOUND},

        // Instance
        new Object[]{MINIMAL_INSTANCE, CedarNodeType.INSTANCE, AuthHeaderSelector.NULL_AUTH,
            IdMatchingSelector.NULL_ID, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{MINIMAL_INSTANCE, CedarNodeType.INSTANCE, AuthHeaderSelector.NULL_AUTH,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{MINIMAL_INSTANCE, CedarNodeType.INSTANCE, AuthHeaderSelector.NULL_AUTH,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{MINIMAL_INSTANCE, CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.NULL_ID, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{MINIMAL_INSTANCE, CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{MINIMAL_INSTANCE, CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{MINIMAL_INSTANCE, CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.NULL_ID, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{MINIMAL_INSTANCE, CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.GIBBERISH, HttpConstants.FORBIDDEN, HttpConstants.OK},
        new Object[]{MINIMAL_INSTANCE, CedarNodeType.INSTANCE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN, HttpConstants.OK},

        new Object[]{MINIMAL_INSTANCE, CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.NULL_ID, HttpConstants.METHOD_NOT_ALLOWED, HttpConstants.OK},
        new Object[]{MINIMAL_INSTANCE, CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.GIBBERISH, HttpConstants.NOT_FOUND, HttpConstants.OK},
        new Object[]{MINIMAL_INSTANCE, CedarNodeType.INSTANCE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.FROM_JSON, HttpConstants.NO_CONTENT, HttpConstants.NOT_FOUND},

    };
  }


}
