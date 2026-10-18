package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.util.test.TestAuthUtil;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP writes preserve quality violations while forbidding malformed typed values. */
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
  private void updateTemplate(ObjectNode template) {
    try (Response response = testClient.target(templateUrl).request().header("Authorization", authHeaderValue)
        .header("If-Match", currentEtag(templateUrl, authHeaderValue)).put(Entity.json(template))) {
      assertEquals(200, response.getStatus(), response.readEntity(String.class));
    }
  }
  private ObjectNode report(ObjectNode instance) {
    try (Response response = testClient.target("http://localhost:" + getPortNumber() + "/command/validate?resource_type=instance")
        .request().header("Authorization", authHeaderValue).post(Entity.json(instance))) {
      String text = response.readEntity(String.class);
      assertEquals(200, response.getStatus(), text);
      try { return (ObjectNode) org.metadatacenter.util.json.JsonMapper.MAPPER.readTree(text); }
      catch (Exception e) { throw new AssertionError(text, e); }
    }
  }

  @ParameterizedTest @CsvSource({"100,minValue", "200,maxValue"})
  void tightenedNumericRangeAllowsWritesAndReportsQualityAgainstTheCurrentTemplate(String value, String code) throws Exception {
    ObjectNode template = read(templateUrl);
    ((ObjectNode) template.at("/properties/Export Good/properties/Quantity/_valueConstraints"))
        .put("minValue", 150).put("maxValue", 175);
    updateTemplate(template);
    ObjectNode instance = read(instanceUrl);
    ((ObjectNode) instance.at("/Export Good/Quantity")).put("@value", value);
    ObjectNode quality = report(instance);
    assertTrue(quality.path("canSave").asBoolean(), quality.toString());
    assertFalse(quality.path("isValid").asBoolean(), quality.toString());
    assertTrue(quality.path("findings").toString().contains("\"code\":\"" + code + "\""), quality.toString());
    for (String query : new String[]{"", "?skip_validation=true", "?verbatim=true"}) {
      String auth = query.contains("verbatim") ? TestAuthUtil.getAdminUserAuthHeader(TestUtil.getCedarConfig()) : authHeaderValue;
      try (Response response = testClient.target(instanceUrl + query).request().header("Authorization", auth)
          .header("If-Match", currentEtag(instanceUrl, authHeaderValue)).put(Entity.json(instance))) {
        assertEquals(200, response.getStatus(), response.readEntity(String.class));
      }
      assertEquals(value, read(instanceUrl).at("/Export Good/Quantity/@value").asText());
    }
    String yaml;
    try (Response response = sendGetRequest(instanceUrl, "application/yaml")) { yaml = response.readEntity(String.class); }
    try (Response response = testClient.target(instanceUrl).request().header("Authorization", authHeaderValue)
        .header("If-Match", currentEtag(instanceUrl, authHeaderValue)).put(Entity.entity(yaml, "application/yaml"))) {
      assertEquals(200, response.getStatus(), response.readEntity(String.class));
    }
    ObjectNode created = instance.deepCopy(); created.putNull("@id");
    String createdId;
    try (Response response = testClient.target("http://localhost:" + getPortNumber() + "/template-instances")
        .request().header("Authorization", authHeaderValue).post(Entity.json(created))) {
      String body = response.readEntity(String.class);
      assertEquals(201, response.getStatus(), body);
      createdId = encodeUrl(org.metadatacenter.util.json.JsonMapper.MAPPER.readTree(body).path("@id").asText());
    }
    removeInstance(createdId);
  }

  @Test void malformedNumericValuesRemainBlockedAndPreserveContentAndEtag() throws Exception {
    ObjectNode before = read(instanceUrl);
    String etag = currentEtag(instanceUrl, authHeaderValue);
    for (String query : new String[]{"", "?skip_validation=true", "?verbatim=true"}) {
      ObjectNode malformed = before.deepCopy();
      ((ObjectNode) malformed.at("/Export Good/Quantity")).put("@value", "not a number");
      String auth = query.contains("verbatim") ? TestAuthUtil.getAdminUserAuthHeader(TestUtil.getCedarConfig()) : authHeaderValue;
      try (Response response = testClient.target(instanceUrl + query).request().header("Authorization", auth)
          .header("If-Match", etag).put(Entity.json(malformed))) {
        String body = response.readEntity(String.class);
        assertEquals(400, response.getStatus(), body);
        assertTrue(body.contains("numberType"), body);
      }
      assertEquals(before, read(instanceUrl));
      assertEquals(etag, currentEtag(instanceUrl, authHeaderValue));
    }
  }

  @Test void regexMismatchesAreSaveableAndRetainTheAnswer() throws Exception {
    ObjectNode template = read(templateUrl);
    ((ObjectNode) template.at("/properties/Company Name/_valueConstraints")).put("regex", "^[A-Z]+$");
    updateTemplate(template);
    ObjectNode instance = read(instanceUrl);
    ((ObjectNode) instance.get("Company Name")).put("@value", "lowercase");
    ObjectNode quality = report(instance);
    assertTrue(quality.path("canSave").asBoolean());
    assertTrue(quality.path("findings").toString().contains("\"code\":\"regex\""), quality.toString());
    try (Response response = testClient.target(instanceUrl).request().header("Authorization", authHeaderValue)
        .header("If-Match", currentEtag(instanceUrl, authHeaderValue)).put(Entity.json(instance))) {
      assertEquals(200, response.getStatus(), response.readEntity(String.class));
    }
    assertEquals("lowercase", read(instanceUrl).path("Company Name").path("@value").asText());
  }
}
