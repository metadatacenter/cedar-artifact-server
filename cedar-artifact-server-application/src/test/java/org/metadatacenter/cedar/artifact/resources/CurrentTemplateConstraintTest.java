package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.util.test.TestAuthUtil;

import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP writes validate the latest stored template, even if the client loaded an older one. */
class CurrentTemplateConstraintTest extends BaseServerTest {
  private String templateId, instanceId, templateUrl, instanceUrl;

  @BeforeEach void create() {
    templateId = uploadTemplate(TestResourcesUtils.getStringContent("instances/usecase-template.json"));
    instanceId = uploadInstance(TestResourcesUtils.getStringContent("instances/usecase-instance.jsonld"));
    templateUrl = TestRequestUrls.forCreatingTemplate(getPortNumber(), templateId);
    instanceUrl = TestRequestUrls.forCreatingInstances(getPortNumber(), instanceId);
  }

  @AfterEach void clean() {
    if (instanceId != null) removeInstance(instanceId);
    if (templateId != null) removeTemplate(templateId);
  }

  private ObjectNode read(String url) {
    try (Response response = sendGetRequest(url)) {
      assertEquals(200, response.getStatus());
      return response.readEntity(ObjectNode.class);
    }
  }

  @Test void tighteningRegexRefusesCreateAndUpdateButAllowsCorrectionWithTheOriginalInstanceEtag() throws Exception {
    ObjectNode before = read(instanceUrl);
    String etag = currentEtag(instanceUrl, authHeaderValue);
    String staleYaml;
    try (Response response = sendGetRequest(instanceUrl, "application/yaml")) {
      assertEquals(200, response.getStatus());
      String yaml = response.readEntity(String.class);
      assertTrue(yaml.contains("Cedar Inc."), yaml);
      staleYaml = yaml.replace("Cedar Inc.", "lowercase");
    }
    ObjectNode staleClient = before.deepCopy();
    ((ObjectNode) staleClient.get("Company Name")).put("@value", "lowercase");

    ObjectNode template = read(templateUrl);
    ((ObjectNode) template.at("/properties/Company Name/_valueConstraints")).put("regex", "^[A-Z]+$");
    try (Response response = testClient.target(templateUrl).request()
        .header("Authorization", authHeaderValue).header("If-Match", currentEtag(templateUrl, authHeaderValue))
        .put(Entity.json(template))) {
      assertEquals(200, response.getStatus(), response.readEntity(String.class));
    }

    // Neither the compatibility skip flag nor verbatim updates bypass the current constraint.
    for (String query : new String[]{"", "?skip_validation=true", "?verbatim=true"}) {
      String auth = query.contains("verbatim")
          ? TestAuthUtil.getAdminUserAuthHeader(TestUtil.getCedarConfig()) : authHeaderValue;
      try (Response response = testClient.target(instanceUrl + query).request()
          .header("Authorization", auth).header("If-Match", etag).put(Entity.json(staleClient))) {
        assertRejected(response);
      }
      assertEquals(before, read(instanceUrl), "refusal must not alter content or provenance");
      assertEquals(etag, currentEtag(instanceUrl, authHeaderValue), "refusal must not consume the validator");
    }
    try (Response response = testClient.target(instanceUrl).request()
        .header("Authorization", authHeaderValue).header("If-Match", etag)
        .put(Entity.entity(staleYaml, "application/yaml"))) {
      assertRejected(response);
    }
    assertEquals(before, read(instanceUrl));
    assertEquals(etag, currentEtag(instanceUrl, authHeaderValue));

    ObjectNode newInstance = staleClient.deepCopy();
    newInstance.putNull("@id");
    try (Response response = testClient.target("http://localhost:" + getPortNumber() + "/template-instances")
        .request().header("Authorization", authHeaderValue).post(Entity.json(newInstance))) {
      assertRejected(response);
    }

    ((ObjectNode) staleClient.get("Company Name")).put("@value", "FIXED");
    try (Response response = testClient.target(instanceUrl).request()
        .header("Authorization", authHeaderValue).header("If-Match", etag).put(Entity.json(staleClient))) {
      assertEquals(200, response.getStatus(), response.readEntity(String.class));
    }
    ObjectNode stored = read(instanceUrl);
    assertEquals("FIXED", stored.at("/Company Name/@value").asText());
    for (ObjectNode value : new ObjectNode[]{before, stored}) {
      value.remove(java.util.List.of("Company Name", "pav:lastUpdatedOn", "oslc:modifiedBy"));
    }
    assertEquals(before, stored, "correction preserves every unrelated answer and identity");
  }

  private void assertRejected(Response response) {
    String body = response.readEntity(String.class);
    assertEquals(400, response.getStatus(), body);
    assertTrue(body.contains("/Company Name/@value"), body);
    assertTrue(body.contains("Does not match ^[A-Z]+$."), body);
  }
}
