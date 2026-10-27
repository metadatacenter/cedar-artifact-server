package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A child that declares no artifact kind is refused with the validator's report, which says where the
 * child is and what it lacks. The server also refuses such a child before minting its identifier, since
 * the identifier takes its prefix from that declaration, and that refusal named the child but not where
 * it was. It now stands behind validation, so an author is told by the report.
 */
public class UndeclaredChildRefusalTest extends AbstractResourceCrudTest {

  private static final String ONE_TEXT_FIELD = "crud/TemplateWithOneTextField.json";

  private static ObjectNode template() throws Exception {
    return (ObjectNode) TestUtil.readFileAsJson(ONE_TEXT_FIELD);
  }

  private static ObjectNode child(ObjectNode template) {
    return (ObjectNode) template.path("properties").path("_text");
  }

  private static void assertReportedAt(Response response, String location, String phrase) {
    Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    List<JsonNode> errors = new java.util.ArrayList<>();
    response.readEntity(JsonNode.class).path("objects").path("validationReport").path("errors").forEach(errors::add);
    Assertions.assertEquals(1, errors.size(), errors::toString);
    Assertions.assertEquals(location, errors.get(0).path("location").asText(), errors::toString);
    Assertions.assertTrue(errors.get(0).path("message").asText().contains(phrase), errors::toString);
  }

  private static Response post(JsonNode template) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, CedarResourceType.TEMPLATE);
    return testClient.target(url).request().header("Authorization", authHeader).post(Entity.json(template));
  }

  @Test
  public void createRefusesAChildWithoutATypeWhereTheChildIs() throws Exception {
    ObjectNode template = template();
    child(template).remove(LinkedData.TYPE);
    int before = countResources(CedarResourceType.TEMPLATE);
    assertReportedAt(post(template), "/properties/_text", "'_text' is not a field or element: it has no @type.");
    Assertions.assertEquals(before, countResources(CedarResourceType.TEMPLATE));
  }

  @Test
  public void createRefusesAListWithoutItsItemWhereTheListIs() throws Exception {
    ObjectNode template = template();
    ((ObjectNode) template.path("properties")).putObject("_text").put("type", "array").put("minItems", 0);
    assertReportedAt(post(template), "/properties/_text", "a list without an items object");
  }

  @Test
  public void updateRefusesAChildWithoutATypeWhereTheChildIsAndKeepsTheStoredTemplate() throws Exception {
    JsonNode created = createResource(template(), CedarResourceType.TEMPLATE);
    String id = created.get(LinkedData.ID).asText();
    createdResources.put(id, CedarResourceType.TEMPLATE);
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, CedarResourceType.TEMPLATE) + "/"
        + URLEncoder.encode(id, StandardCharsets.UTF_8);

    ObjectNode update = (ObjectNode) created.deepCopy();
    child(update).remove(LinkedData.TYPE);
    Response response = testClient.target(url).request().header("Authorization", authHeader)
        .header("If-Match", currentEtag(url, authHeader)).put(Entity.json(update));
    assertReportedAt(response, "/properties/_text", "it has no @type");

    JsonNode stored = testClient.target(url).request().header("Authorization", authHeader).get(JsonNode.class);
    Assertions.assertTrue(stored.path("properties").path("_text").has(LinkedData.TYPE));
  }
}
