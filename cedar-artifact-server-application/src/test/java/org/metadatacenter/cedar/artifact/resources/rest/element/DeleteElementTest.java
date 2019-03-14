package org.metadatacenter.cedar.artifact.resources.rest.element;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.util.json.JsonMapper;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static org.metadatacenter.cedar.artifact.resources.rest.AuthHeaderSelector.*;
import static org.metadatacenter.cedar.artifact.resources.rest.IdMatchingSelector.*;
import static org.metadatacenter.cedar.artifact.resources.utils.TestConstants.TEST_NAME_PATTERN_METHOD_PARAMS;
import static org.metadatacenter.cedar.test.util.TestValueResourceIdGenerator.ids;
import static org.metadatacenter.model.CedarNodeType.ELEMENT;

@RunWith(JUnitParamsRunner.class)
public class DeleteElementTest extends AbstractRestTest {

  private static int index = -1;

  @Test
  @TestCaseName(TEST_NAME_PATTERN_METHOD_PARAMS)
  @Parameters(method = "getParamsDeleteElement")
  public void deleteElementTest(TestParameterArrayGeneratorGenerator generator,
                                TestParameterValueGenerator<CedarNodeType> rt,
                                TestParameterValueGenerator<String> auth,
                                TestParameterValueGenerator<String> idInUrlGenerator) throws IOException {
    index++;
    TestParameterArrayGenerator arrayGenerator = generator.getValue();
    String jsonFileName = MINIMAL_ELEMENT_NO_ID;
    CedarNodeType resourceType = rt.getValue();

    // Create the element first

    AuthHeaderSelector createAuth = TEST_USER_1;
    TestParameterValueGenerator<String> createAuthGenerator = new TestValueAuthStringGenerator(createAuth);
    createAuthGenerator.generateValue(tdctx, arrayGenerator);
    String createAuthHeaderValue = createAuthGenerator.getValue();
    String postUrl = getUrlWithId(baseTestUrl, resourceType, (String) null);
    divider("CREATE BLOCK");
    testParam("jsonFileName", jsonFileName);
    testParam("resourceType", resourceType);
    testParam("createAuth", createAuth);
    pair("POST URL", postUrl);
    pair("Authorization", createAuthHeaderValue);
    pair("Index", index);
    divider();

    String originalFileContent = getFileContentAsString(jsonFileName);
    Invocation.Builder request = testClient.target(postUrl).request();
    request.header(AUTHORIZATION, createAuthHeaderValue);

    Response createResponse = request.post(Entity.json(originalFileContent));
    int createResponseStatus = createResponse.getStatus();
    String createdLocation = createResponse.getHeaderString(LOCATION);
    pair("createResponseStatus", createResponseStatus);
    pair("createdLocation", createdLocation);
    divider();

    // Extract the Id, mark it for deletion
    String createdId = null;
    String createdBody = createResponse.readEntity(String.class);
    JsonNode element = null;
    try {
      element = JsonMapper.MAPPER.readTree(createdBody);
    } catch (JsonParseException e) {
      // do nothing, the json can be invalid intentionally
    }
    JsonNode idNode = element.get(LinkedData.ID);
    createdId = idNode.asText();
    pair("Created id", createdId);
    divider();

    createdResources.put(createdId, CedarNodeType.ELEMENT);

    // Do the actual testing

    auth.generateValue(tdctx, arrayGenerator);
    String authHeaderValue = auth.getValue();
    idInUrlGenerator.generateValue(tdctx, arrayGenerator);
    String idInUrl = idInUrlGenerator.getValue();
    if (((TestValueResourceIdGenerator) idInUrlGenerator).getIdMatchingSelector() == PREVIOUSLY_CREATED) {
      idInUrl = createdId;
    }

    String deleteUrl = getUrlWithId(baseTestUrl, resourceType, idInUrl);

    divider("DELETE BLOCK");
    testParam("resourceType", resourceType);
    testParam("deleteAuth", ((TestValueAuthStringGenerator) auth).getAuthSelector());
    testParam("idInUrlPolicy", ((TestValueResourceIdGenerator) idInUrlGenerator).getIdMatchingSelector());
    pair("idInUrl", idInUrl);
    pair("Test DELETE URL", deleteUrl);
    pair("Authorization", authHeaderValue);
    pair("Index", index);
    divider();

    Invocation.Builder deleteRequest = testClient.target(deleteUrl).request();
    if (authHeaderValue != null) {
      deleteRequest.header(AUTHORIZATION, authHeaderValue);
    }

    Response deleteResponse = deleteRequest.delete();

    int deleteResponseStatus = deleteResponse.getStatus();
    pair("Delete response status", deleteResponseStatus);
    int expectedResponseStatus = getExpectedResponseStatus(generator, rt, auth, idInUrlGenerator);
    Assert.assertEquals(expectedResponseStatus, deleteResponseStatus);
  }

  private int getExpectedResponseStatus(TestParameterArrayGeneratorGenerator generator,
                                        TestParameterValueGenerator<CedarNodeType> rt,
                                        TestParameterValueGenerator<String> auth,
                                        TestParameterValueGenerator<String> idInUrlGenerator) {

    if (((TestValueResourceIdGenerator)idInUrlGenerator).getIdMatchingSelector() == NULL_FULL) {
      return Response.Status.METHOD_NOT_ALLOWED.getStatusCode();
    }

    TestValueAuthStringGenerator authGenerator = new TestValueAuthStringGenerator(TEST_USER_1);
    authGenerator.generateValue(tdctx, null);
    if (!authGenerator.getValue().equals(auth.getValue())) {
      return Response.Status.UNAUTHORIZED.getStatusCode();
    }

    if (((TestValueResourceIdGenerator)idInUrlGenerator).getIdMatchingSelector() == NULL_ID) {
      return Response.Status.BAD_REQUEST.getStatusCode();
    }

    if (((TestValueResourceIdGenerator) idInUrlGenerator).getIdMatchingSelector() == GIBBERISH) {
      return Response.Status.BAD_REQUEST.getStatusCode();
    } else if (((TestValueResourceIdGenerator) idInUrlGenerator).getIdMatchingSelector() == RANDOM_ID) {
      return Response.Status.NOT_FOUND.getStatusCode();
    } else if (((TestValueResourceIdGenerator) idInUrlGenerator).getIdMatchingSelector() == PREVIOUSLY_CREATED) {
      return Response.Status.NO_CONTENT.getStatusCode();
    }

    return 0;
  }

  private Object getParamsDeleteElement() {
    Set<AuthHeaderSelector> authHeader = new LinkedHashSet<>();
    authHeader.add(NULL_AUTH);
    authHeader.add(GIBBERISH_FULL);
    authHeader.add(GIBBERISH_KEY);
    authHeader.add(TEST_USER_1);

    Set<IdMatchingSelector> idInUrl = new LinkedHashSet<>();
    idInUrl.add(NULL_FULL);
    idInUrl.add(NULL_ID);
    idInUrl.add(GIBBERISH);
    idInUrl.add(RANDOM_ID);
    idInUrl.add(PREVIOUSLY_CREATED);

    TestParameterArrayGenerator generatorForElement = new TestParameterArrayGenerator();
    generatorForElement.addParameterValue(1, ELEMENT, "nodeType");
    generatorForElement.registerParameter(2, authHeader, "authHeader");
    generatorForElement.registerParameter(3, ids(idInUrl, "nodeType"), "idInUrl");

    List<TestParameterValueGenerator[]> testCases = new ArrayList<>();

    testCases.addAll(generatorForElement.generateAllCombinations());

    return testCases.toArray();

  }


}
