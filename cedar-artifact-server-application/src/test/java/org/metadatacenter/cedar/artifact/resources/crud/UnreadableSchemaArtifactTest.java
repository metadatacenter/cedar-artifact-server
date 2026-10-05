package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A schema artifact the meta-schema accepts but the artifact library's reader refuses is refused on
 * write. A child stored under a reserved key is the case that made this matter: the validator accepted
 * a template whose child was keyed {@code @foo}, the server stored it, and then no editor or viewer
 * could open it, because each reads it through the Java reader or its TypeScript twin.
 */
public class UnreadableSchemaArtifactTest extends AbstractResourceCrudTest {

  private static final String ONE_TEXT_FIELD = "crud/TemplateWithOneTextField.json";
  private static final String ONE_ATTRIBUTE_VALUE_GROUP = "crud/TemplateWithOneAttributeValueGroup.json";

  /** The template with its one child moved to another key, everywhere a child's key is written. */
  private static ObjectNode rekeyed(String resource, String from, String to) {
    ObjectNode template;
    try {
      template = (ObjectNode) TestUtil.readFileAsJson(resource);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    ObjectNode properties = (ObjectNode) template.get("properties");
    properties.set(to, properties.remove(from));
    ObjectNode ui = (ObjectNode) template.get("_ui");
    ui.set("order", renamed(ui.get("order"), from, to));
    for (String map : List.of("propertyLabels", "propertyDescriptions")) {
      ObjectNode labels = (ObjectNode) ui.get(map);
      labels.set(to, labels.remove(from));
    }
    template.set("required", renamed(template.get("required"), from, to));
    ObjectNode context = (ObjectNode) properties.get(LinkedData.CONTEXT);
    ObjectNode contextProperties = (ObjectNode) context.get("properties");
    contextProperties.set(to, contextProperties.remove(from));
    context.set("required", renamed(context.get("required"), from, to));
    return template;
  }

  private static ArrayNode renamed(JsonNode keys, String from, String to) {
    ArrayNode result = ((ArrayNode) keys).arrayNode();
    keys.forEach(key -> result.add(key.asText().equals(from) ? to : key.asText()));
    return result;
  }

  private static Response post(JsonNode template) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, CedarResourceType.TEMPLATE);
    return testClient.target(url).request().header("Authorization", authHeader).post(Entity.json(template));
  }

  /** The messages of the validation report a refused write answers with. */
  private static List<String> errorMessages(Response response) {
    JsonNode body = response.readEntity(JsonNode.class);
    List<String> messages = new ArrayList<>();
    body.path("objects").path("validationReport").path("errors").forEach(error -> messages.add(error.path("message").asText()));
    return messages;
  }

  private static void assertRefusedNaming(Response response, String key) {
    Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), response.getStatus());
    List<String> messages = errorMessages(response);
    Assertions.assertTrue(messages.stream().anyMatch(message -> message.contains(key)),
        "the refusal should name the key " + key + ": " + messages);
  }

  @Test
  public void createStoresTheTemplateUnderAnOrdinaryKey() {
    Response response = post(rekeyed(ONE_TEXT_FIELD, "_text", "Sample ID"));
    Assertions.assertEquals(CedarResponseStatus.CREATED.getStatusCode(), response.getStatus());
    createdResources.put(response.readEntity(JsonNode.class).get(LinkedData.ID).asText(), CedarResourceType.TEMPLATE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"@foo", "__proto__", "prototype"})
  public void createRefusesAChildStoredUnderAReservedKey(String key) {
    int before = countResources(CedarResourceType.TEMPLATE);
    assertRefusedNaming(post(rekeyed(ONE_TEXT_FIELD, "_text", key)), key);
    Assertions.assertEquals(before, countResources(CedarResourceType.TEMPLATE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"name", "annotations", "children"})
  public void createRefusesAnAttributeValueGroupStoredUnderAKeyItsYamlFormReserves(String key) {
    int before = countResources(CedarResourceType.TEMPLATE);
    assertRefusedNaming(post(rekeyed(ONE_ATTRIBUTE_VALUE_GROUP, "_attribute", key)), key);
    Assertions.assertEquals(before, countResources(CedarResourceType.TEMPLATE));
  }

  @Test
  public void updateRefusesAChildMovedToAReservedKeyAndKeepsTheStoredTemplate() throws Exception {
    JsonNode created = createResource(TestUtil.readFileAsJson(ONE_TEXT_FIELD), CedarResourceType.TEMPLATE);
    String id = created.get(LinkedData.ID).asText();
    createdResources.put(id, CedarResourceType.TEMPLATE);
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, CedarResourceType.TEMPLATE) + "/"
        + URLEncoder.encode(id, StandardCharsets.UTF_8);

    ObjectNode update = rekeyed(ONE_TEXT_FIELD, "_text", "@foo");
    for (String identity : List.of(LinkedData.ID, "pav:createdOn", "pav:createdBy", "pav:lastUpdatedOn", "oslc:modifiedBy")) {
      update.set(identity, created.get(identity));
    }
    Response response = testClient.target(url).request().header("Authorization", authHeader)
        .header("If-Match", currentEtag(url, authHeader)).put(Entity.json(update));
    assertRefusedNaming(response, "@foo");

    JsonNode stored = testClient.target(url).request().header("Authorization", authHeader).get(JsonNode.class);
    Assertions.assertTrue(stored.path("properties").has("_text"));
    Assertions.assertFalse(stored.path("properties").has("@foo"));
  }
}
