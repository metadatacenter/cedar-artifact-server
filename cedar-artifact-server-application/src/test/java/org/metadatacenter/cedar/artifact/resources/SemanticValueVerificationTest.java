package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.metadatacenter.model.validation.ValidationFinding;
import org.metadatacenter.model.validation.report.CedarValidationReport;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SemanticValueVerificationTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static ObjectNode controlled() throws Exception {
    return (ObjectNode) MAPPER.readTree("""
        {"_ui":{"inputType":"textfield"},"properties":{"@id":{"type":"string"}},
         "_valueConstraints":{"branches":[{"uri":"https://example.org/root","source":"TEST","maxDepth":2,
         "version":{"id":"sha256:one"}}],"actions":[{"action":"delete","termUri":"https://example.org/gone"}]}}
        """);
  }
  private static HttpResponse<String> response(String status, String iri) {
    String code = switch (status) {
      case "passed" -> "controlledTermVerified";
      case "failed" -> "controlledTermMembership";
      case "notChecked" -> "semanticNotChecked";
      default -> "semanticUnavailable";
    };
    return response(status, code, iri);
  }
  @SuppressWarnings("unchecked")
  private static HttpResponse<String> response(String status, String code, String iri) {
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("{\"status\":\"" + status + "\",\"code\":\"" + code + "\","
        + "\"message\":\"Checked term\",\"iri\":\"" + iri + "\"}");
    return response;
  }

  private static String body(java.net.http.HttpRequest request) {
    var result = new java.util.concurrent.CompletableFuture<String>();
    var bytes = new java.io.ByteArrayOutputStream();
    request.bodyPublisher().orElseThrow().subscribe(new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
      public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
      public void onNext(java.nio.ByteBuffer buffer) {
        byte[] part = new byte[buffer.remaining()]; buffer.get(part); bytes.writeBytes(part);
      }
      public void onError(Throwable error) { result.completeExceptionally(error); }
      public void onComplete() { result.complete(bytes.toString(java.nio.charset.StandardCharsets.UTF_8)); }
    });
    return result.join();
  }

  @Test void exactIdentifiersAndCompleteConstraintsArePostedAndDuplicateChecksShareOneRequest() throws Exception {
    var field = controlled();
    var schema = MAPPER.createObjectNode();
    schema.putObject("properties").putObject("a/b~c").set("items", field);
    var instance = MAPPER.readTree("""
        {"a/b~c":[{"@id":"https://example.org/term","rdfs:label":"stale label"},
                    {"@id":"https://example.org/term","rdfs:label":"another label"}]}
        """);
    AtomicInteger calls = new AtomicInteger();
    var service = new SemanticValueVerification("http://terminology/", "http://bridge/", "Bearer example", request -> {
      calls.incrementAndGet();
      assertEquals("http://terminology/bioportal/validate-term", request.uri().toString());
      assertEquals("Bearer example", request.headers().firstValue("Authorization").orElseThrow());
      assertEquals("POST", request.method());
      var posted = MAPPER.readTree(body(request));
      assertEquals("https://example.org/term", posted.path("iri").asText());
      assertEquals(field.path("_valueConstraints"), posted.path("valueConstraints"));
      assertFalse(posted.has("inputText"), "Labels are not evidence of membership");
      return response("failed", "https://example.org/term");
    });
    var report = CedarValidationReport.newInstanceReport();
    service.instance(instance, schema, "", report);
    assertEquals(1, calls.get());
    assertEquals(2, report.getFindings().size());
    assertTrue(report.getFindings().stream().allMatch(f -> f.path().startsWith("/a~1b~0c/")
        && f.verificationStatus() == ValidationFinding.VerificationStatus.FAILED
        && f.category() == ValidationFinding.Category.CONSTRAINT));
    assertTrue(report.canSave());
    assertFalse(report.isValid());
  }

  @Test void failedDefaultsBlockButOutagesAndMismatchedResponsesNeverBecomeFailedMembership() throws Exception {
    var field = controlled();
    field.withObject("/_valueConstraints").putObject("defaultValue")
        .put("termUri", "https://example.org/term").put("rdfs:label", "Term");
    for (String status : new String[]{"passed", "failed", "notChecked", "unavailable"}) {
      var report = CedarValidationReport.newEmptyReport();
      new SemanticValueVerification("http://terms", "http://bridge", null,
          request -> response(status, "https://example.org/term")).defaults(field, "", report);
      assertEquals(!status.equals("failed"), report.canSave(), status);
      assertEquals(status.equals("passed"), report.isValid(), status);
      assertEquals(status.equals("failed") ? ValidationFinding.Category.CONSTRAINT : ValidationFinding.Category.VERIFICATION,
          report.getFindings().iterator().next().category(), status);
    }
    var report = CedarValidationReport.newEmptyReport();
    new SemanticValueVerification("http://terms", "http://bridge", null,
        request -> { throw new java.io.IOException("unavailable"); }).defaults(field, "", report);
    assertTrue(report.canSave());
    assertEquals(ValidationFinding.VerificationStatus.UNAVAILABLE,
        report.getFindings().iterator().next().verificationStatus());
    var wrong = CedarValidationReport.newEmptyReport();
    new SemanticValueVerification("http://terms", "http://bridge", null,
        request -> response("passed", "https://example.org/different")).defaults(field, "", wrong);
    assertFalse(wrong.isValid());
    assertEquals(ValidationFinding.VerificationStatus.UNAVAILABLE,
        wrong.getFindings().iterator().next().verificationStatus());
  }

  @ParameterizedTest
  @CsvSource({"ext-orcid,orcid", "ext-ror,ror", "ext-rrid,rrid", "ext-pubmed,pmid", "ext-doi,doi",
      "ext-pfas,comp-tox", "ext-nih-grant-id,nih-grant"})
  void anAuthorityCheckUsesItsOwnResolverAndReplacesTheOfflineNotCheckedFact(String inputType, String route) throws Exception {
    var field = MAPPER.createObjectNode();
    field.putObject("_ui").put("inputType", inputType);
    var instance = MAPPER.readTree("{\"@id\":\"https://orcid.org/0000-0002-1825-0097\"}");
    var report = CedarValidationReport.newInstanceReport();
    report.addFinding(new ValidationFinding("semanticNotChecked", ValidationFinding.Category.VERIFICATION,
        "/@id", "Not checked", ValidationFinding.VerificationStatus.NOT_CHECKED));
    new SemanticValueVerification("http://terms", "http://bridge", null, request -> {
      assertEquals("http://bridge/ext-auth/" + route + "/verify", request.uri().toString());
      assertEquals(instance.path("@id"), MAPPER.readTree(body(request)).path("iri"));
      return response("passed", "authorityVerified", instance.path("@id").asText());
    }).instance(instance, field, "", report);
    assertTrue(report.isValid());
    assertEquals(1, report.getFindings().size());
    assertTrue(report.getWarnings().isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"true,failed,unknownRule", "true,failed,authorityMembership", "false,failed,controlledTermMembership",
      "true,passed,authorityVerified", "false,passed,controlledTermVerified", "true,notChecked,controlledTermVerified",
      "false,unavailable,authorityMembership", "true,failed,''", "false,unexpected,authorityVerified"})
  void invalidServiceStatusCodePairsStayUnavailableAndNeverBlockAnInstance(boolean controlled, String status, String code) throws Exception {
    var field = controlled ? controlled() : MAPPER.createObjectNode();
    if (!controlled) field.putObject("_ui").put("inputType", "ext-orcid");
    var instance = MAPPER.readTree("{\"@id\":\"https://example.org/term\"}");
    var report = CedarValidationReport.newInstanceReport();
    new SemanticValueVerification("http://terms", "http://bridge", null,
        request -> response(status, code, "https://example.org/term")).instance(instance, field, "", report);
    var finding = report.getFindings().iterator().next();
    assertEquals("semanticUnavailable", finding.code());
    assertEquals(ValidationFinding.Category.VERIFICATION, finding.category());
    assertEquals(ValidationFinding.VerificationStatus.UNAVAILABLE, finding.verificationStatus());
    assertTrue(report.canSave());
    assertFalse(report.isValid());
    assertEquals("true", report.getValidationStatus());
  }

  @ParameterizedTest
  @CsvSource({"authorityIdentifier", "authorityMembership"})
  void authorityFailuresUseConstraintFactsAndTheTemplateSavePolicy(String code) throws Exception {
    var field = MAPPER.createObjectNode();
    field.putObject("_ui").put("inputType", "ext-orcid");
    field.putObject("_valueConstraints").put("defaultValue", "https://example.org/term");
    var report = CedarValidationReport.newEmptyReport();
    new SemanticValueVerification("http://terms", "http://bridge", null,
        request -> response("failed", code, "https://example.org/term")).defaults(field, "", report);
    assertEquals(ValidationFinding.Category.CONSTRAINT, report.getFindings().iterator().next().category());
    assertFalse(report.canSave());
    assertFalse(report.isValid());
  }
}
