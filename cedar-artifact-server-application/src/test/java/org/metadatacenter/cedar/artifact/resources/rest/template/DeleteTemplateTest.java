package org.metadatacenter.cedar.artifact.resources.rest.template;

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
import org.metadatacenter.model.CedarResourceType;
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
import static org.metadatacenter.model.CedarResourceType.TEMPLATE;

@RunWith(JUnitParamsRunner.class)
public class DeleteTemplateTest extends AbstractRestTest {

  private static int index = -1;

  @Test
  @TestCaseName(TEST_NAME_PATTERN_METHOD_PARAMS)
  @Parameters(method = "getParamsDeleteTemplate")
  public void deleteTemplateTest(TestParameterArrayGeneratorGenerator generator,
                                 TestParameterValueGenerator<CedarResourceType> rt,
                                 TestParameterValueGenerator<String> auth,
                                 TestParameterValueGenerator<String> idInUrlGenerator) throws IOException {
    index++;
    TestParameterArrayGenerator arrayGenerator = generator.getValue();
    String jsonFileName = MINIMAL_TEMPLATE_NO_ID;
    CedarResourceType resourceType = rt.getValue();

    // Create the artifact first

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
    JsonNode template = null;
    try {
      template = JsonMapper.MAPPER.readTree(createdBody);
    } catch (JsonParseException e) {
      // do nothing, the json can be invalid intentionally
    }
    JsonNode idNode = template.get(LinkedData.ID);
    createdId = idNode.asText();
    pair("Created id", createdId);
    divider();

    createdResources.put(createdId, CedarResourceType.TEMPLATE);

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
      return Response.Status.NOT_FOUND.getStatusCode();
    } else if (((TestValueResourceIdGenerator) idInUrlGenerator).getIdMatchingSelector() == PREVIOUSLY_CREATED) {
      return Response.Status.NO_CONTENT.getStatusCode();
    }

    return 0;
  }

  private Object getParamsDeleteTemplate() {
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

    TestParameterArrayGenerator generatorForTemplate = new TestParameterArrayGenerator();
    generatorForTemplate.addParameterValue(1, TEMPLATE, "resourceType");
    generatorForTemplate.registerParameter(2, authHeader, "authHeader");
    generatorForTemplate.registerParameter(3, ids(idInUrl, "resourceType"), "idInUrl");

    List<TestParameterValueGenerator[]> testCases = new ArrayList<>();

    testCases.addAll(generatorForTemplate.generateAllCombinations());

    return testCases.toArray();

  }


}
