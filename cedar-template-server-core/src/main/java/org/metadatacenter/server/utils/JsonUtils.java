package org.metadatacenter.server.utils;

import checkers.nullness.quals.NonNull;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import org.metadatacenter.server.service.TemplateElementService;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class JsonUtils {
  /* JSON Schema Validation */

  public void validate(@NonNull JsonNode schema, @NonNull JsonNode instance) throws ProcessingException {
    JsonValidator validator = JsonSchemaFactory.byDefault().getValidator();
    ProcessingReport report = validator.validate(schema, instance);
    if (!report.isSuccess()) {
      throw new RuntimeException("JSON Schema validation failed");
    }
    // System.out.println(report.isSuccess());
  }

  /* Resolution of Json Schema references ($ref) */

  @NonNull
  public JsonNode resolveTemplateElementRefs(@NonNull JsonNode node,
                                             @NonNull TemplateElementService<String, JsonNode> templateElementService) throws IOException, ProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();

    Iterator<Map.Entry<String, JsonNode>> it = node.fields();
    while (it.hasNext()) {
      Map.Entry<String, JsonNode> entry = it.next();
      // If the entry is an object
      if (entry.getValue().isObject()) {
        // If it contains only one property, which is $ref
        if ((entry.getValue().size() == 1) && (entry.getValue().get("$ref") != null)) {
          String ref = entry.getValue().get("$ref").asText();
          // Load the template element
          if (ref.length() > 0) {
            JsonNode templateElement = templateElementService.findTemplateElementByLinkedDataId(ref, false, false);
            if (templateElement != null) {
              rootNode.set(entry.getKey(), resolveTemplateElementRefs(templateElement, templateElementService));
            } else {
              rootNode.put(entry.getKey(), "unresolved_reference");
            }
          }
          // Empty reference
          else {
            rootNode.put(entry.getKey(), "unresolved_reference");
          }
        }
        // If it contains more properties, or only one but it is not $ref
        else {
          rootNode.set(entry.getKey(), resolveTemplateElementRefs(entry.getValue(), templateElementService));
        }
      }
      // If it is not an object
      else {
        //        if (entry.getKey().compareTo("_$schema") == 0) {
        //          rootNode.set("$schema", entry.getValue());
        //        } else {
        rootNode.set(entry.getKey(), entry.getValue());
        //        }
      }
    }
    return rootNode;
  }

  /* Fix for the keywords not allowed by MongoDB (e.g. $schema) */
  // Rename JSON field to be stored into MongoDB
  // direction: WRITE_TO_MONGO  -> update field names for MongoDB storage (e.g. $schema -> _$schema)
  // direction: READ_FROM_MONGO -> update field names after reading them from MongoDB (e.g. _$schema -> $schema)
  @NonNull
  public JsonNode fixMongoDB(@NonNull JsonNode node, FixMongoDirection direction) {
    boolean reverse = false;
    if (direction == FixMongoDirection.READ_FROM_MONGO) {
      reverse = true;
    }
    updateFieldName(node, "$schema", "_$schema", reverse);
    updateFieldName(node, "$oid", "_$oid", reverse);
    updateFieldName(node, "$numberLong", "_$numberLong", reverse);
    // Now, recursively invoke this method on all properties
    for (JsonNode child : node) {
      fixMongoDB(child, direction);
    }
    return node;
  }

  @NonNull
  private JsonNode updateFieldName(@NonNull JsonNode node, @NonNull String fieldName,
                                   @NonNull String newFieldName, boolean reverse) {
    if (reverse) {
      String swap = fieldName;
      fieldName = newFieldName;
      newFieldName = swap;
    }
    if (node.has(fieldName)) {
      ((ObjectNode) node).set(newFieldName, new TextNode(node.get(fieldName).asText()));
      ((ObjectNode) node).remove(fieldName);
    }
    return node;
  }
}
