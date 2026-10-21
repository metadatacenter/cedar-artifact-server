package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.metadatacenter.artifacts.model.core.TemplateSchemaArtifact;
import org.metadatacenter.artifacts.model.core.TextField;
import org.metadatacenter.artifacts.model.renderer.JsonArtifactRenderer;
import static org.junit.jupiter.api.Assertions.*;

class TemplateValidationShapeTest {
  private ObjectNode template() {
    return new JsonArtifactRenderer().renderTemplateSchemaArtifact(TemplateSchemaArtifact.builder()
        .withName("Before").withFieldSchema(TextField.builder().withName("title").build()).build());
  }
  @Test void metadataAndPresentationDoNotChangeValidation() {
    ObjectNode before = template(), after = before.deepCopy();
    after.put("schema:name", "After").put("pav:lastUpdatedOn", "later");
    ((ObjectNode) after.path("properties").path("title")).put("@id", "https://example.org/new")
        .put("pav:version", "2.0.0").put("schema:description", "Better help");
    ((ObjectNode) after.path("_ui")).put("header", "New header");
    before.putObject("_id").put("$oid", "mongo-internal-key");
    assertTrue(TemplateValidationShape.equivalent(before, after));
    assertNotEquals(before, after);
  }
  @Test void ConstraintsAndUnknownContentStaySignificant() {
    ObjectNode before = template(), after = before.deepCopy();
    ((ObjectNode) after.path("properties").path("title")).put("minLength", 4);
    assertFalse(TemplateValidationShape.equivalent(before, after));
    assertFalse(TemplateValidationShape.replacesChildren(before, after), "same-definition editing keeps its existing quality policy");
    ((ObjectNode) after.path("properties").path("title")).put("@id", "https://example.org/next-release");
    assertTrue(TemplateValidationShape.replacesChildren(before, after), "a pulled definition must pass the storage guard");
    after = before.deepCopy(); after.put("futureValidationRule", true);
    assertFalse(TemplateValidationShape.equivalent(before, after));
    after = before.deepCopy(); ((ObjectNode) after.path("properties").path("title")).put("_id", "unknown child content");
    assertFalse(TemplateValidationShape.equivalent(before, after), "only Mongo's root key is ignored");
    after = before.deepCopy(); ((ObjectNode) after.path("properties")).remove("title");
    assertFalse(TemplateValidationShape.equivalent(before, after), "metadata-named properties are real schema properties");
  }
}
