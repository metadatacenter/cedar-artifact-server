package org.metadatacenter.cedar.artifact.resources.rest.element;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.model.core.CedarModelVocabulary;
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
import static org.metadatacenter.model.CedarResourceType.ELEMENT;

@RunWith(JUnitParamsRunner.class)
public class UpdateElementTest extends AbstractRestTest {

  private static final String TEST_DESCRIPTION_VALUE = "New description";
  private static int index = -1;

  @Test
  @TestCaseName(TEST_NAME_PATTERN_METHOD_PARAMS)
  @Parameters(method = "getParamsUpdateElementPut")
  public void updateElementTest(TestParameterArrayGeneratorGenerator generator,
                                TestParameterValueGenerator<CedarResourceType> rt,
                                TestParameterValueGenerator<String> auth,
                                TestParameterValueGenerator<String> idInUrlGenerator) throws IOException {
    index++;
    TestParameterArrayGenerator arrayGenerator = generator.getValue();
    String jsonFileName = MINIMAL_ELEMENT_NO_ID;
    CedarResourceType resourceType = rt.getValue();

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

    createdResources.put(createdId, CedarResourceType.ELEMENT);

    // Do the actual testing - update

    auth.generateValue(tdctx, arrayGenerator);
    String authHeaderValue = auth.getValue();
    idInUrlGenerator.generateValue(tdctx, arrayGenerator);
    String idInUrl = idInUrlGenerator.getValue();
    if (((TestValueResourceIdGenerator) idInUrlGenerator).getIdMatchingSelector() == PREVIOUSLY_CREATED) {
      idInUrl = createdId;
    }

    String putUrl = getUrlWithId(baseTestUrl, resourceType, idInUrl);

    divider("PUT BLOCK");
    testParam("resourceType", resourceType);
    testParam("deleteAuth", ((TestValueAuthStringGenerator) auth).getAuthSelector());
    testParam("idInUrlPolicy", ((TestValueResourceIdGenerator) idInUrlGenerator).getIdMatchingSelector());
    pair("idInUrl", idInUrl);
    pair("Test PUT URL", putUrl);
    pair("Authorization", authHeaderValue);
    pair("Index", index);
    divider();

    Invocation.Builder putRequest = testClient.target(putUrl).request();
    if (authHeaderValue != null) {
      putRequest.header(AUTHORIZATION, authHeaderValue);
    }

    ((ObjectNode) element).put(CedarModelVocabulary.SCHEMA_DESCRIPTION, TEST_DESCRIPTION_VALUE);
    String modifiedContent = JsonMapper.MAPPER.writeValueAsString(element);
    Response putResponse = putRequest.put(Entity.json(modifiedContent));

    int putResponseStatus = putResponse.getStatus();
    pair("Put response status", putResponseStatus);
    int expectedResponseStatus = getExpectedResponseStatus(generator, rt, auth, idInUrlGenerator);
    Assert.assertEquals(expectedResponseStatus, putResponseStatus);

    // Do the actual testing - verify the changed description field value
    if (expectedResponseStatus == Response.Status.OK.getStatusCode()) {
      String getUrl = getUrlWithId(baseTestUrl, resourceType, createdId);

      divider("GET BLOCK");
      testParam("resourceType", resourceType);
      testParam("createAuth", createAuth);
      pair("createdId", createdId);
      pair("Test GET URL", getUrl);
      pair("Authorization", createAuthHeaderValue);
      pair("Index", index);
      divider();

      Invocation.Builder getRequest = testClient.target(getUrl).request();
      getRequest.header(AUTHORIZATION, createAuthHeaderValue);

      Response getResponse = getRequest.get();

      int getResponseStatus = getResponse.getStatus();
      pair("Get response status", getResponseStatus);
      Assert.assertEquals(Response.Status.OK.getStatusCode(), getResponseStatus);

      String getBody = getResponse.readEntity(String.class);
      JsonNode getElement = null;
      try {
        getElement = JsonMapper.MAPPER.readTree(getBody);
      } catch (JsonParseException e) {
        // do nothing, the json can be invalid intentionally
      }

      JsonNode descriptionNode = getElement.get(CedarModelVocabulary.SCHEMA_DESCRIPTION);
      String description = descriptionNode.asText();
      pair("Updated description", description);
      Assert.assertEquals(TEST_DESCRIPTION_VALUE, description);
    }
  }

  private int getExpectedResponseStatus(TestParameterArrayGeneratorGenerator generator,
                                        TestParameterValueGenerator<CedarResourceType> rt,
                                        TestParameterValueGenerator<String> auth,
                                        TestParameterValueGenerator<String> idInUrlGenerator) {

    if (((TestValueResourceIdGenerator) idInUrlGenerator).getIdMatchingSelector() == NULL_FULL) {
      return Response.Status.METHOD_NOT_ALLOWED.getStatusCode();
    }

    TestValueAuthStringGenerator authGenerator = new TestValueAuthStringGenerator(TEST_USER_1);
    authGenerator.generateValue(tdctx, null);
    if (!authGenerator.getValue().equals(auth.getValue())) {
      return Response.Status.UNAUTHORIZED.getStatusCode();
    }

    if (((TestValueResourceIdGenerator) idInUrlGenerator).getIdMatchingSelector() == NULL_ID) {
      return Response.Status.BAD_REQUEST.getStatusCode();
    }

    if (((TestValueResourceIdGenerator) idInUrlGenerator).getIdMatchingSelector() == GIBBERISH) {
      return Response.Status.BAD_REQUEST.getStatusCode();
    } else if (((TestValueResourceIdGenerator) idInUrlGenerator).getIdMatchingSelector() == RANDOM_ID) {
      return Response.Status.BAD_REQUEST.getStatusCode();
    } else if (((TestValueResourceIdGenerator) idInUrlGenerator).getIdMatchingSelector() == PREVIOUSLY_CREATED) {
      return Response.Status.OK.getStatusCode();
    }

    return 0;
  }

  private Object getParamsUpdateElementPut() {
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
    generatorForElement.addParameterValue(1, ELEMENT, "resourceType");
    generatorForElement.registerParameter(2, authHeader, "authHeader");
    generatorForElement.registerParameter(3, ids(idInUrl, "resourceType"), "idInUrl");

    List<TestParameterValueGenerator[]> testCases = new ArrayList<>();

    testCases.addAll(generatorForElement.generateAllCombinations());

    return testCases.toArray();

  }


}
