package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.TestAuthUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Creation provenance is server-owned on both implementations of create-by-PUT. */
public class CreateByPutProvenanceTest extends AbstractResourceCrudTest {

  private static final String FORGED_CREATED_ON = "2001-01-01T00:00:00-08:00";
  private static final String FORGED_CREATED_BY = "https://metadatacenter.org/users/forged-creator";

  @ParameterizedTest
  @MethodSource("createByPutArtifacts")
  public void createByPutStampsTheAuthenticatedCreator(JsonNode sampleResource,
                                                        CedarResourceType resourceType) throws Exception {
    ObjectNode artifact = (ObjectNode) setSchemaIsBasedOn(
        sampleTemplate.deepCopy(), sampleResource.deepCopy(), resourceType);
    String id = linkedDataUtil.buildNewLinkedDataId(resourceType);
    artifact.put(LinkedData.ID, id);
    artifact.put("pav:createdOn", FORGED_CREATED_ON);
    artifact.put("pav:createdBy", FORGED_CREATED_BY);

    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType) + "/"
        + URLEncoder.encode(id, StandardCharsets.UTF_8);
    Response response = testClient.target(url).request()
        .header(HttpHeaders.AUTHORIZATION, authHeader)
        .put(Entity.json(artifact));
    String responseBody = response.readEntity(String.class);

    assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus(), responseBody);
    createdResources.put(id, resourceType);
    JsonNode created = JsonMapper.MAPPER.readTree(responseBody);
    assertServerCreationProvenance(created);

    JsonNode stored = testClient.target(url).request()
        .header(HttpHeaders.AUTHORIZATION, authHeader)
        .get().readEntity(JsonNode.class);
    assertServerCreationProvenance(stored);
  }

  private static Object[] createByPutArtifacts() {
    return getCommonParams1();
  }

  private static void assertServerCreationProvenance(JsonNode artifact) {
    String expectedUser = TestAuthUtil.getTestUser1(TestUtil.getCedarConfig()).getId();
    assertEquals(expectedUser, artifact.path("pav:createdBy").asText());
    assertEquals(expectedUser, artifact.path("oslc:modifiedBy").asText());
    assertNotEquals(FORGED_CREATED_BY, artifact.path("pav:createdBy").asText());
    assertNotEquals(FORGED_CREATED_ON, artifact.path("pav:createdOn").asText());
    assertEquals(artifact.path("pav:createdOn").asText(),
        artifact.path("pav:lastUpdatedOn").asText());
  }
}
