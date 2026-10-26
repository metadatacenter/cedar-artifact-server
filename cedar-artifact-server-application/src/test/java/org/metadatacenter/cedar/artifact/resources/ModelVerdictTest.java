package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.model.validation.ValidationFinding;
import org.metadatacenter.model.validation.report.CedarValidationReport;
import org.metadatacenter.model.validation.report.ErrorItem;
import org.metadatacenter.model.validation.report.ValidationReport;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * What the artifact library makes of an artifact the validator accepts: the server reads it into the
 * model and renders the model in each form it serves, on a copy, and refuses a write that fails either.
 */
class ModelVerdictTest {

  private static ObjectNode fixture(String name) throws Exception {
    return (ObjectNode) TestUtil.readFileAsJson(name);
  }

  @Test
  void aSoundArtifactOfEachKindReadsAndRendersInEveryServedForm() throws Exception {
    for (String name : List.of("crud/SampleTemplate.json", "crud/SampleTemplateElement.json",
        "crud/TemplateWithOneAttributeValueGroup.json", "crud/SampleTemplateInstance.json")) {
      ObjectNode artifact = fixture(name);
      JsonNode before = artifact.deepCopy();
      boolean instance = name.contains("Instance");
      boolean element = name.contains("Element");
      ValidationReport report = AbstractArtifactServerResource.withModelVerdict(
          CedarValidationReport.newEmptyReport(), artifact,
          (reader, body) -> instance ? reader.readTemplateInstanceArtifact(body)
              : element ? reader.readElementSchemaArtifact(body) : reader.readTemplateSchemaArtifact(body),
          AbstractArtifactServerResource.RENDERINGS);
      assertTrue(report.getErrors().isEmpty(), name + ": " + report.getErrors());
      assertEquals(before, artifact, name + " must be stored as received, not as rendered");
    }
  }

  @Test
  void anArtifactTheReaderRefusesIsRefusedWithWhereTheReaderStopped() throws Exception {
    ObjectNode instance = fixture("crud/SampleTemplateInstance.json");
    instance.put("@id", "not an identifier");
    ValidationReport report = AbstractArtifactServerResource.withModelVerdict(
        CedarValidationReport.newInstanceReport(), instance,
        (reader, body) -> reader.readTemplateInstanceArtifact(body), AbstractArtifactServerResource.RENDERINGS);
    assertFalse(report.canSave());
    ErrorItem error = report.getErrors().iterator().next();
    assertFalse(error.getMessage().isBlank(), error.toString());
    assertEquals("not an identifier", instance.get("@id").asText(), "the received body is left as it was");
  }

  @Test
  void aModelNoRendererCanWriteIsRefusedNamingTheForm() throws Exception {
    ValidationReport report = AbstractArtifactServerResource.withModelVerdict(
        CedarValidationReport.newEmptyReport(), fixture("crud/SampleTemplate.json"),
        (reader, body) -> reader.readTemplateSchemaArtifact(body),
        List.of(AbstractArtifactServerResource.RENDERINGS.get(0),
            new AbstractArtifactServerResource.Rendering("YAML", model -> {
              throw new IllegalStateException("no key for this child");
            })));
    assertFalse(report.canSave());
    assertEquals(List.of("The artifact can not be rendered as YAML: no key for this child"),
        report.getErrors().stream().map(ErrorItem::getMessage).toList());
  }

  @Test
  void anArtifactTheValidatorRefusedIsNotReadSoOneDefectIsReportedOnce() throws Exception {
    CedarValidationReport refused = CedarValidationReport.newInstanceReport();
    refused.addFinding(new ValidationFinding("missingProperty", ValidationFinding.Category.STRUCTURAL,
        "/Study Name", "The field slot must contain @value."));
    AtomicBoolean read = new AtomicBoolean();
    ValidationReport report = AbstractArtifactServerResource.withModelVerdict(refused,
        fixture("crud/SampleTemplateInstance.json"), (reader, body) -> {
          read.set(true);
          return reader.readTemplateInstanceArtifact(body);
        }, AbstractArtifactServerResource.RENDERINGS);
    assertFalse(read.get());
    assertEquals(1, report.getErrors().size());
  }
}
