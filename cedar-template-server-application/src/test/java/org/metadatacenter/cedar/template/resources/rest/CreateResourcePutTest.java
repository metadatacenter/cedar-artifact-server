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
import org.metadatacenter.cedar.test.util.TestParameterArrayGenerator;
import org.metadatacenter.cedar.test.util.TestParameterArrayGeneratorGenerator;
import org.metadatacenter.cedar.test.util.TestParameterValueGenerator;
import org.metadatacenter.cedar.test.util.TestValueResourceIdGenerator;
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
import static org.metadatacenter.cedar.template.resources.rest.AuthHeaderSelector.*;
import static org.metadatacenter.cedar.template.resources.rest.IdMatchingSelector.*;
import static org.metadatacenter.cedar.template.resources.utils.TestConstants.PROV_FIELDS;
import static org.metadatacenter.cedar.template.resources.utils.TestConstants.TEST_NAME_PATTERN_METHOD_PARAMS;
import static org.metadatacenter.cedar.test.util.TestValueCopyFromValueGenerator.copyFrom;
import static org.metadatacenter.cedar.test.util.TestValueResourceIdGenerator.ids;
import static org.metadatacenter.constant.HttpConstants.CREATED;
import static org.metadatacenter.model.CedarNodeType.ELEMENT;
import static org.metadatacenter.model.CedarNodeType.TEMPLATE;

@RunWith(JUnitParamsRunner.class)
public class CreateResourcePutTest extends AbstractRestTest {

  @Test
  @TestCaseName(TEST_NAME_PATTERN_METHOD_PARAMS)
  @Parameters(method = "getParamsCreatePutResource")
  public void createResourcePutTest(TestParameterArrayGeneratorGenerator generator,
                                    TestParameterValueGenerator<String> js,
                                    TestParameterValueGenerator<CedarNodeType> rt,
                                    TestParameterValueGenerator<String> auth,
                                    TestValueResourceIdGenerator idInURLGenerator,
                                    TestParameterValueGenerator<String> idInBodyGenerator) throws IOException {
    TestParameterArrayGenerator arrayGenerator = generator.getValue();
    String jsonFileName = js.getValue();
    CedarNodeType resourceType = rt.getValue();
    auth.generateValue(tdctx, arrayGenerator);
    String authHeaderValue = auth.getValue();
    idInURLGenerator.generateValue(tdctx, arrayGenerator);
    String idInURL = idInURLGenerator.getValue();
    String putUrl = getUrlWithId(baseTestUrl, resourceType, idInURL);
    idInBodyGenerator.generateValue(tdctx, arrayGenerator);
    String idInBody = idInBodyGenerator.getValue();
    System.out.println("--------------------------------------------------------");
    System.out.println(jsonFileName + "\n" + resourceType + "\n" + authHeaderValue + "\n" + idInURL + "\n" + idInBody);

    String originalFileContent = getFileContentAsString(jsonFileName);
    log.info("Test PUT URL :" + putUrl);
    log.info("Authorization:" + authHeaderValue);
    Invocation.Builder request = testClient.target(putUrl).request();
    if (authHeaderValue != null) {
      request.header(AUTHORIZATION, authHeaderValue);
    }

    // if we really want to create this instance, we need to set the schema:isBasedOn to something real
    /*if (MINIMAL_INSTANCE.equals(jsonFileName) && status == CREATED) {
      JsonNode minimalTemplate = getFileContentAsJson(MINIMAL_TEMPLATE);
      JsonNode createdTemplate = createResource(minimalTemplate, TEMPLATE);
      JsonNode instance = JsonMapper.MAPPER.readTree(originalFileContent);
      String createdTemplateId = createdTemplate.get(LinkedData.ID).asText();
      createdResources.put(createdTemplateId, TEMPLATE);
      ((ObjectNode) instance).put(SCHEMA_IS_BASED_ON, createdTemplateId);
      originalFileContent = JsonMapper.MAPPER.writeValueAsString(instance);
    }*/
/*
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
    }*/
  }

  private Object getParamsCreatePutResource() {
    Set<String> jsonFileName = new LinkedHashSet<>();
    jsonFileName.add(null);
    jsonFileName.add("non-json");
    jsonFileName.add("bad-json");
    jsonFileName.add("empty-json");
    jsonFileName.add("ui-title");
    jsonFileName.add("ui-description");
    jsonFileName.add("minimal-element-with-id");
    jsonFileName.add(MINIMAL_ELEMENT);

    Set<AuthHeaderSelector> authHeader = new LinkedHashSet<>();
    authHeader.add(NULL_AUTH);
    authHeader.add(GIBBERISH_FULL);
    authHeader.add(GIBBERISH_KEY);
    authHeader.add(TEST_USER_1);

    Set<IdMatchingSelector> idInURL = new LinkedHashSet<>();
    idInURL.add(NULL_ID);
    idInURL.add(GIBBERISH);
    idInURL.add(RANDOM_ID);

    Set<IdMatchingSelector> idInBody = new LinkedHashSet<>();
    idInBody.add(NULL_ID);
    idInBody.add(GIBBERISH);
    idInBody.add(RANDOM_ID);

    TestParameterArrayGenerator generatorForElement = new TestParameterArrayGenerator();
    generatorForElement.registerParameter(1, jsonFileName, "jsonFileName");
    generatorForElement.addParameterValue(2, ELEMENT, "nodeType");
    generatorForElement.registerParameter(3, authHeader, "authHeader");
    generatorForElement.registerParameter(4, ids(idInURL, "nodeType"), "idInURL");
    generatorForElement.registerParameter(5, ids(idInBody, "nodeType"), "idInBody");
    generatorForElement.addParameterValue(5, copyFrom("idInURL"));

    List<TestParameterValueGenerator[]> testCases = new ArrayList<>();

    testCases.addAll(generatorForElement.generateAllCombinations());

    return testCases.toArray();

  }


}
