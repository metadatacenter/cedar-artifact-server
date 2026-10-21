package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** A conservative equivalence proof. Unknown schema content remains significant. */
final class TemplateValidationShape {
  private static final List<String> METADATA = List.of("@id", "schema:name", "schema:description", "title",
      "description", "pav:version", "bibo:status", "pav:derivedFrom", "pav:previousVersion", "pav:createdOn",
      "pav:createdBy", "pav:lastUpdatedOn", "oslc:modifiedBy", "_annotations", "skos:prefLabel", "skos:altLabel");
  private static final List<String> PRESENTATION = List.of("order", "propertyLabels", "propertyDescriptions",
      "header", "footer", "hidden", "valueRecommendationEnabled", "width", "height", "content");

  private TemplateValidationShape() { }

  static boolean equivalent(JsonNode before, JsonNode after) {
    return shape(before).equals(shape(after));
  }

  /** Pulling changes embedded identities or structure. Same-definition constraint editing retains
   * CEDAR's existing advisory data-quality policy; it is not a version replacement. */
  static boolean replacesChildren(JsonNode before, JsonNode after) {
    return !references(before).equals(references(after));
  }

  private static Map<String, Object> references(JsonNode value) {
    Map<String, Object> result = new TreeMap<>();
    value.path("properties").fields().forEachRemaining(entry -> {
      JsonNode child = entry.getValue().has("items") ? entry.getValue().get("items") : entry.getValue();
      if (schemaType(child.get("@type"))) {
        result.put(entry.getKey(), List.of(child.path("@id").asText(), references(child)));
      }
    });
    return result;
  }

  private static JsonNode shape(JsonNode value) {
    JsonNode copy = value.deepCopy();
    // DAO snapshots retain Mongo's root key; public GET bodies and submitted schemas do not.
    if (copy instanceof ObjectNode object) object.remove("_id");
    visit(copy);
    return copy;
  }

  private static boolean schemaType(JsonNode value) {
    if (value == null) return false;
    if (value.isArray()) {
      for (JsonNode item : value) if (schemaType(item)) return true;
      return false;
    }
    return value.isTextual() && value.asText().matches(
        "https://schema\\.metadatacenter\\.org/core/(Template|TemplateElement|TemplateField|StaticTemplateField)");
  }

  private static void visit(JsonNode value) {
    if (!(value instanceof ObjectNode node)) return;
    if (schemaType(node.get("@type"))) {
      node.remove(METADATA);
      if (node.get("_ui") instanceof ObjectNode ui) ui.remove(PRESENTATION);
    }
    // Never strip a property named after metadata, or an enum object's values.
    if (node.get("properties") instanceof ObjectNode properties) {
      for (JsonNode child : properties) {
        visit(child);
        if (child.has("items")) visit(child.get("items"));
      }
    }
  }
}
