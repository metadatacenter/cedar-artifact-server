package org.metadatacenter.cedar.artifact.resources.rest.template;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.metadatacenter.cedar.artifact.resources.rest.AbstractRestTest;
import org.metadatacenter.cedar.artifact.resources.rest.AuthHeaderSelector;
import org.metadatacenter.cedar.artifact.resources.rest.IdMatchingSelector;
import org.metadatacenter.cedar.test.util.*;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.metadatacenter.cedar.artifact.resources.rest.AuthHeaderSelector.*;
import static org.metadatacenter.cedar.artifact.resources.rest.IdMatchingSelector.*;
import static org.metadatacenter.cedar.artifact.resources.utils.TestConstants.TEST_NAME_PATTERN_METHOD_PARAMS;
import static org.metadatacenter.cedar.test.util.TestValueResourceIdGenerator.ids;
import static org.metadatacenter.model.CedarResourceType.TEMPLATE;

@RunWith(JUnitParamsRunner.class)
public class CreateTemplatePostTest extends AbstractRestTest {

  private static int index = -1;

  @Test
  @TestCaseName(TEST_NAME_PATTERN_METHOD_PARAMS)
  @Parameters(method = "getParamsCreateTemplatePost")
  public void createTemplatePostTest(TestParameterArrayGeneratorGenerator generator,
                                     TestParameterValueGenerator<String> js,
                                     TestParameterValueGenerator<CedarResourceType> rt,
                                     TestParameterValueGenerator<String> auth,
                                     TestParameterValueGenerator<String> idInBodyGenerator) throws IOException {
    index++;
    TestParameterArrayGenerator arrayGenerator = generator.getValue();
    String jsonFileName = js.getValue();
    CedarResourceType resourceType = rt.getValue();
    auth.generateValue(tdctx, arrayGenerator);
    String authHeaderValue = auth.getValue();
    String postUrl = getUrlWithId(baseTestUrl, resourceType, (String) null);
    idInBodyGenerator.generateValue(tdctx, arrayGenerator);
    String idInBody = idInBodyGenerator.getValue();
    divider("POST BLOCK");
    testParam("jsonFileName", jsonFileName);
    testParam("resourceType", resourceType);
    testParam("putAuth", ((TestValueAuthStringGenerator) auth).getAuthSelector());
    testParam("idInBodyPolicy", ((TestValueResourceIdGenerator) idInBodyGenerator).getIdMatchingSelector());
    pair("idInBody", idInBody);
    pair("Test POST URL", postUrl);
    pair("Authorization", authHeaderValue);
    pair("Index", index);
    divider();

    String originalFileContent = getFileContentAsString(jsonFileName);
    Invocation.Builder request = testClient.target(postUrl).request();
    if (authHeaderValue != null) {
      request.header(AUTHORIZATION, authHeaderValue);
    }
    // if the in body id policy is copy from URL
    // and the content can be translated into json
    // and the json contains an id
    // then copy from URL
    if (idInBodyGenerator instanceof TestValueCopyFromValueGenerator) {
      if (originalFileContent != null) {
        JsonNode template = null;
        try {
          template = JsonMapper.MAPPER.readTree(originalFileContent);
        } catch (JsonParseException e) {
          // do nothing, the json can be invalid intentionally
        }
        if (template != null) {
          JsonNode idNode = template.get(LinkedData.ID);
          if (idNode != null) {
            String templateId = idNode.asText();
            if (templateId != null) {
              ((ObjectNode) template).put(LinkedData.ID, idInBody);
              originalFileContent = JsonMapper.MAPPER.writeValueAsString(template);
            }
          }
        }
      }
    }

    Response response;
    if (originalFileContent != null) {
      response = request.post(Entity.json(originalFileContent));
    } else {
      response = request.post(null);
    }

    // Extract the Id, mark it for deletion
    String createdId = null;
    String createdBody = response.readEntity(String.class);
    JsonNode template = null;
    try {
      template = JsonMapper.MAPPER.readTree(createdBody);
    } catch (JsonParseException e) {
      // do nothing, the json can be invalid intentionally
    }
    JsonNode idNode = template.get(LinkedData.ID);
    if (idNode != null) {
      createdId = idNode.asText();
      pair("Created id", createdId);
      divider();

      createdResources.put(createdId, CedarResourceType.TEMPLATE);
    }

    int responseStatus = response.getStatus();
    int expectedResponseStatus = getExpectedResponseStatus(generator, js, rt, auth, idInBodyGenerator);
    Assert.assertEquals(expectedResponseStatus, responseStatus);
  }

  private int getExpectedResponseStatus(TestParameterArrayGeneratorGenerator generator,
                                        TestParameterValueGenerator<String> js,
                                        TestParameterValueGenerator<CedarResourceType> rt,
                                        TestParameterValueGenerator<String> auth,
                                        TestParameterValueGenerator<String> idInBodyGenerator) {


    TestValueAuthStringGenerator authGenerator = new TestValueAuthStringGenerator(TEST_USER_1);
    authGenerator.generateValue(tdctx, null);
    if (!authGenerator.getValue().equals(auth.getValue())) {
      return CedarResponseStatus.UNAUTHORIZED.getStatusCode();
    }
    if (js.getValue() == null) {
      return CedarResponseStatus.BAD_REQUEST.getStatusCode();
    } else if (NON_JSON.equals(js.getValue())) {
      return CedarResponseStatus.BAD_REQUEST.getStatusCode();
    } else if (BAD_JSON.equals(js.getValue())) {
      return CedarResponseStatus.BAD_REQUEST.getStatusCode();
    } else if (EMPTY_JSON.equals(js.getValue())) {
      return CedarResponseStatus.BAD_REQUEST.getStatusCode();
    } else if (SCHEMA_NAME.equals(js.getValue())) {
      return CedarResponseStatus.CREATED.getStatusCode();
    } else if (SCHEMA_DESCRIPTION.equals(js.getValue())) {
      return CedarResponseStatus.BAD_REQUEST.getStatusCode();
    } else if (MINIMAL_TEMPLATE_WITH_ID.equals(js.getValue())) {
      return CedarResponseStatus.BAD_REQUEST.getStatusCode();
    } else if (FULL_TEMPLATE.equals(js.getValue())) {
      return CedarResponseStatus.BAD_REQUEST.getStatusCode();
    } else if (MINIMAL_TEMPLATE_NO_ID.equals(js.getValue())) {
      return CedarResponseStatus.CREATED.getStatusCode();
    }

    return 0;
  }

  private Object getParamsCreateTemplatePost() {
    Set<String> jsonFileName = new LinkedHashSet<>();
    jsonFileName.add(null);
    jsonFileName.add(NON_JSON);
    jsonFileName.add(BAD_JSON);
    jsonFileName.add(EMPTY_JSON);
    jsonFileName.add(SCHEMA_NAME);
    jsonFileName.add(SCHEMA_DESCRIPTION);
    jsonFileName.add(MINIMAL_TEMPLATE_NO_ID);
    jsonFileName.add(MINIMAL_TEMPLATE_WITH_ID);
    jsonFileName.add(FULL_TEMPLATE);

    Set<AuthHeaderSelector> authHeader = new LinkedHashSet<>();
    authHeader.add(NULL_AUTH);
    authHeader.add(GIBBERISH_FULL);
    authHeader.add(GIBBERISH_KEY);
    authHeader.add(TEST_USER_1);

    Set<IdMatchingSelector> idInBody = new LinkedHashSet<>();
    idInBody.add(NULL_ID);
    idInBody.add(GIBBERISH);
    idInBody.add(RANDOM_ID);

    TestParameterArrayGenerator generatorForTemplate = new TestParameterArrayGenerator();
    generatorForTemplate.registerParameter(1, jsonFileName, "jsonFileName");
    generatorForTemplate.addParameterValue(2, TEMPLATE, "resourceType");
    generatorForTemplate.registerParameter(3, authHeader, "authHeader");
    generatorForTemplate.registerParameter(4, ids(idInBody, "resourceType"), "idInBody");

    List<TestParameterValueGenerator[]> testCases = new ArrayList<>();

    testCases.addAll(generatorForTemplate.generateAllCombinations());

    return testCases.toArray();

  }


}
