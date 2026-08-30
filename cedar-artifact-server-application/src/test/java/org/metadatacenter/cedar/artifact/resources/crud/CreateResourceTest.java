package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;

import static jakarta.ws.rs.core.HttpHeaders.LOCATION;
import static org.metadatacenter.cedar.artifact.resources.utils.TestConstants.*;

public class CreateResourceTest extends AbstractResourceCrudTest {

  /**
   * 'CREATE' TESTS
   */

  @Test
  public void legacySkipValidationFlagStillCreatesAValidInstance() {
    JsonNode instance = setSchemaIsBasedOn(sampleTemplate.deepCopy(), sampleInstance.deepCopy(),
        CedarResourceType.INSTANCE);
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, CedarResourceType.INSTANCE);

    Response response = testClient.target(url).queryParam("skip_validation", true).request()
        .header("Authorization", authHeader).post(Entity.json(instance));

    Assertions.assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus());
    JsonNode created = response.readEntity(JsonNode.class);
    createdResources.put(created.get(LinkedData.ID).asText(), CedarResourceType.INSTANCE);
  }

  @Test
  public void legacySkipValidationFlagCannotStoreAnInvalidInstance() {
    JsonNode instance = setSchemaIsBasedOn(sampleTemplate.deepCopy(), sampleInstance.deepCopy(),
        CedarResourceType.INSTANCE);
    ((ObjectNode) instance).remove(LinkedData.CONTEXT);
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, CedarResourceType.INSTANCE);

    Response response = testClient.target(url).queryParam("skip_validation", true).request()
        .header("Authorization", authHeader).post(Entity.json(instance));

    Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    response.close();
  }

  @Test
  public void createCannotStoreAnEmptyDerivedFromEvenWithLegacySkipValidationFlag() {
    ObjectNode template = sampleTemplate.deepCopy();
    template.put("pav:derivedFrom", "");
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, CedarResourceType.TEMPLATE);

    Response response = testClient.target(url).queryParam("skip_validation", true).request()
        .header("Authorization", authHeader).post(Entity.json(template));

    Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    response.close();
  }

  @ParameterizedTest
  @MethodSource("getCommonParams1")
  public void createResourceTest(JsonNode sampleResource, CedarResourceType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    // If the artifact is an instance, we need to set the schema:isBasedOn property to the id of an existing artifact.
    // Otherwise we will get a validation error. So, first we create a artifact and then use its id to create the
    // instance
    sampleResource = setSchemaIsBasedOn(sampleTemplate, sampleResource, resourceType);
    // Service invocation
    Response response = testClient.target(url).request().header("Authorization", authHeader).post(Entity.json
        (sampleResource));
    // Check HTTP response
    Assertions.assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus());
    createdResources.put(response.readEntity(JsonNode.class).get(LinkedData.ID).asText(), resourceType);
    // Retrieve the artifact created
    String location = response.getHeaderString(LOCATION);
    Response findResponse = testClient.target(location).request().header("Authorization", authHeader).get();
    // A copy: `sampleResource` is the fixture every test in this class shares, and the comparison
    // below strips keys from what it is given. Stripping them from the fixture itself left the next
    // create posting a body with no `@id` key at all, which the server refuses — the identifier is how
    // a client asks for one, so it has to be there.
    JsonNode expected = sampleResource.deepCopy();
    JsonNode actual = findResponse.readEntity(JsonNode.class);
    // Check that id and provenance information have been generated
    Assertions.assertNotEquals(actual.get(LinkedData.ID), null);
    for (String provField : PROV_FIELDS) {
      Assertions.assertNotEquals(actual.get(provField), null);
    }
    // Check that all the other fields contain the expected values
    ((ObjectNode) expected).remove(LinkedData.ID);
    ((ObjectNode) actual).remove(LinkedData.ID);
    for (String provField : PROV_FIELDS) {
      ((ObjectNode) expected).remove(provField);
      ((ObjectNode) actual).remove(provField);
    }
    Assertions.assertEquals(expected, actual);
  }

  @ParameterizedTest
  @MethodSource("getCommonParams1")
  public void createResourceMissingAuthorizationHeaderTest(JsonNode sampleResource, CedarResourceType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    // Service invocation without Authorization header
    Response response = testClient.target(url).request().post(Entity.json(sampleResource));
    // Check HTTP response
    Assertions.assertEquals(CedarResponseStatus.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  @ParameterizedTest
  @MethodSource("getCommonParams1")
  public void createResourceUnauthorizedKeyTest(JsonNode sampleResource, CedarResourceType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    String newAuthHeader = "apiKey " + NON_EXISTENT_API_KEY;
    // Service invocation without unauthorized api key
    Response response = testClient.target(url).request().header("Authorization", newAuthHeader).post(Entity.json
        (sampleResource));
    // Check HTTP response
    Assertions.assertEquals(CedarResponseStatus.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

}
