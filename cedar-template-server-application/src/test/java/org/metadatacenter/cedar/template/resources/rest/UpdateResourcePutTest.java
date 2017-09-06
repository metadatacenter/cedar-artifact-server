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

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.metadatacenter.cedar.template.resources.utils.TestConstants.TEST_NAME_PATTERN_METHOD_PARAMS;

@RunWith(JUnitParamsRunner.class)
public class UpdateResourcePutTest extends AbstractRestTest {

  @Test
  @TestCaseName(TEST_NAME_PATTERN_METHOD_PARAMS)
  @Parameters(method = "getParamsUpdateResource")
  public void updateResourcePutTest(String jsonFileName, CedarNodeType resourceType, AuthHeaderSelector
      authSelector, IdMatchingSelector idInUrlSelector, IdMatchingSelector idInBodySelector, int statusPut) throws
      IOException {

    // Create the document first
    JsonNode jsonFromFile = getFileContentAsJson(jsonFileName);
    JsonNode createdResourceJson = createResource(jsonFromFile, resourceType);
    String createdResourceId = createdResourceJson.get(LinkedData.ID).asText();
    //--createdResources.put(createdResourceId, resourceType);

    String authHeaderValue = getAuthHeader(authSelector);

    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    String updateUrl = getUrlWithId(url, idInUrlSelector, createdResourceId);

    log.info("Update URL   :" + updateUrl);
    log.info("Authorization:" + authHeaderValue);
    Invocation.Builder request = testClient.target(updateUrl).request();
    if (authHeaderValue != null) {
      request.header(AUTHORIZATION, authHeaderValue);
    }
    if (idInBodySelector == IdMatchingSelector.FROM_JSON) {
      ((ObjectNode) createdResourceJson).put(LinkedData.ID, createdResourceId);
    } else if (idInBodySelector == IdMatchingSelector.GIBBERISH) {
      ((ObjectNode) createdResourceJson).put(LinkedData.ID, "gibberish");
    }
    Response responsePut = request.put(Entity.json(createdResourceJson));
    int responsePutStatus = responsePut.getStatus();
    Assert.assertEquals(statusPut, responsePutStatus);
  }

  private Object getParamsUpdateResource() {
    return new Object[]{
        // Element

        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.NULL,
            IdMatchingSelector.NULL, IdMatchingSelector.FROM_JSON, HttpConstants.METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.NULL,
            IdMatchingSelector.GIBBERISH, IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.NULL,
            IdMatchingSelector.FROM_JSON, IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN},

        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.NULL, IdMatchingSelector.FROM_JSON, HttpConstants.METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.GIBBERISH, IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.FROM_JSON, IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN},

        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.NULL, IdMatchingSelector.FROM_JSON, HttpConstants.METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.GIBBERISH, IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.FROM_JSON, IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN},

        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.NULL, IdMatchingSelector.FROM_JSON, HttpConstants.METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.GIBBERISH, IdMatchingSelector.FROM_JSON, HttpConstants.BAD_REQUEST},
        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.FROM_JSON, IdMatchingSelector.GIBBERISH, HttpConstants.BAD_REQUEST},

        new Object[]{MINIMAL_ELEMENT, CedarNodeType.ELEMENT, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.FROM_JSON, IdMatchingSelector.FROM_JSON, HttpConstants.OK},

        // Template
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.NULL,
            IdMatchingSelector.NULL, IdMatchingSelector.FROM_JSON, HttpConstants.METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.NULL,
            IdMatchingSelector.GIBBERISH, IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.NULL,
            IdMatchingSelector.FROM_JSON, IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN},

        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.NULL, IdMatchingSelector.FROM_JSON, HttpConstants.METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.GIBBERISH, IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_FULL,
            IdMatchingSelector.FROM_JSON, IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN},

        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.NULL, IdMatchingSelector.FROM_JSON, HttpConstants.METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.GIBBERISH, IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.GIBBERISH_KEY,
            IdMatchingSelector.FROM_JSON, IdMatchingSelector.FROM_JSON, HttpConstants.FORBIDDEN},

        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.NULL, IdMatchingSelector.FROM_JSON, HttpConstants.METHOD_NOT_ALLOWED},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.GIBBERISH, IdMatchingSelector.FROM_JSON, HttpConstants.BAD_REQUEST},
        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.FROM_JSON, IdMatchingSelector.GIBBERISH, HttpConstants.BAD_REQUEST},

        new Object[]{MINIMAL_TEMPLATE, CedarNodeType.TEMPLATE, AuthHeaderSelector.TEST_USER_1,
            IdMatchingSelector.FROM_JSON, IdMatchingSelector.FROM_JSON, HttpConstants.OK},

        /*
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
*/
    };
  }


}
