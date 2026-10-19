package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.model.validation.FieldValuePolicy;
import org.metadatacenter.model.validation.ValidationFinding;
import org.metadatacenter.model.validation.ValidationPolicy;
import org.metadatacenter.model.validation.report.CedarValidationReport;
import org.metadatacenter.util.json.JsonMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Request-scoped exact-identifier checks. A 25-second call limit accommodates the terminology
 * service's 20-second lookup budget; all checks share 30 seconds and at most 256 distinct requests.
 * Unavailability never becomes a negative membership answer. Requests are not retried.
 */
final class SemanticValueVerification {
  @FunctionalInterface interface Sender { HttpResponse<String> send(HttpRequest request) throws Exception; }
  private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  private static final Map<String, String> AUTHORITY_ROUTES = Map.of(
      "ext-orcid", "orcid", "ext-ror", "ror", "ext-rrid", "rrid", "ext-pubmed", "pmid",
      "ext-doi", "doi", "ext-pfas", "comp-tox", "ext-nih-grant-id", "nih-grant");
  private final String terminologyBase, bridgeBase, authorization;
  private final Sender sender;
  private final Map<String, JsonNode> checked = new HashMap<>();
  private final long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();

  SemanticValueVerification(String terminologyBase, String bridgeBase, String authorization) {
    this(terminologyBase, bridgeBase, authorization, request -> HTTP.send(request, HttpResponse.BodyHandlers.ofString()));
  }
  SemanticValueVerification(String terminologyBase, String bridgeBase, String authorization, Sender sender) {
    this.terminologyBase = terminologyBase;
    this.bridgeBase = bridgeBase;
    this.authorization = authorization;
    this.sender = sender;
  }

  void defaults(JsonNode schema, String path, CedarValidationReport report) {
    if (schema == null || !schema.isObject()) return;
    if (schema.path("items").isObject()) { defaults(schema.get("items"), path + "/items", report); return; }
    JsonNode value = schema.path("_valueConstraints").path("defaultValue");
    if (value.isObject() && value.path("termUri").isTextual())
      verify(schema, value.get("termUri").textValue(), path + "/_valueConstraints/defaultValue/termUri", report);
    else if (value.isTextual() && !value.textValue().isEmpty())
      verify(schema, value.textValue(), path + "/_valueConstraints/defaultValue", report);
    schema.path("properties").fields().forEachRemaining(child -> {
      if (isChild(child.getValue())) defaults(child.getValue(), path + "/properties/" + escape(child.getKey()), report);
    });
  }

  void instance(JsonNode instance, JsonNode schema, String path, CedarValidationReport report) {
    if (instance == null || schema == null || !schema.isObject()) return;
    if (schema.path("items").isObject()) {
      if (instance.isArray()) for (int i = 0; i < instance.size(); i++)
        instance(instance.get(i), schema.get("items"), path + "/" + i, report);
      return;
    }
    if (!instance.isObject()) return;
    JsonNode iri = instance.path("@id");
    if (iri.isTextual() && !iri.textValue().isEmpty()) verify(schema, iri.textValue(), path + "/@id", report);
    schema.path("properties").fields().forEachRemaining(child -> {
      if (isChild(child.getValue())) instance(instance.get(child.getKey()), child.getValue(), path + "/" + escape(child.getKey()), report);
    });
  }

  private void verify(JsonNode field, String iri, String path, CedarValidationReport report) {
    ObjectNode body = JsonMapper.MAPPER.createObjectNode().put("iri", iri);
    String endpoint;
    boolean controlled = FieldValuePolicy.isControlled(field);
    if (controlled) {
      endpoint = endpoint(terminologyBase, "bioportal/validate-term");
      body.set("valueConstraints", field.path("_valueConstraints"));
    } else {
      String input = field.path("_ui").path("inputType").asText();
      String authority = AUTHORITY_ROUTES.get(input);
      if (authority == null) return;
      endpoint = endpoint(bridgeBase, "ext-auth/" + authority + "/verify");
    }
    String key = endpoint + "\n" + body;
    JsonNode result = checked.get(key);
    if (result == null) {
      result = request(endpoint, body, controlled);
      checked.put(key, result);
    }
    ValidationFinding.VerificationStatus status = switch (result.path("status").asText()) {
      case "passed" -> ValidationFinding.VerificationStatus.PASSED;
      case "failed" -> ValidationFinding.VerificationStatus.FAILED;
      case "notChecked" -> ValidationFinding.VerificationStatus.NOT_CHECKED;
      default -> ValidationFinding.VerificationStatus.UNAVAILABLE;
    };
    String code = result.path("code").asText("semanticUnavailable");
    report.resolveVerification(new ValidationFinding(code, ValidationPolicy.category(code), path,
        result.path("message").asText("Semantic verification is unavailable."), status));
  }

  private JsonNode request(String endpoint, JsonNode body, boolean controlled) {
    if (endpoint == null || checked.size() >= 256 || System.nanoTime() >= deadline) return unavailable();
    try {
      HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(endpoint))
          .timeout(Duration.ofNanos(Math.min(Duration.ofSeconds(25).toNanos(), deadline - System.nanoTime())))
          .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString()));
      if (authorization != null) request.header("Authorization", authorization);
      var response = sender.send(request.build());
      if (response.statusCode() != 200) return unavailable();
      JsonNode answer = JsonMapper.MAPPER.readTree(response.body());
      if (!answer.path("code").isTextual() || !answer.path("message").isTextual()
          || !ValidationPolicy.acceptsSemanticResult(controlled, answer.path("status").asText(), answer.path("code").asText())
          || !body.path("iri").equals(answer.path("iri"))) return unavailable();
      return answer;
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return unavailable();
    } catch (Exception failure) { return unavailable(); }
  }
  private static JsonNode unavailable() {
    return JsonMapper.MAPPER.createObjectNode().put("status", "unavailable").put("code", "semanticUnavailable")
        .put("message", "Semantic verification is unavailable.");
  }
  private static String endpoint(String base, String path) {
    return base == null || base.isEmpty() ? null : base + (base.endsWith("/") ? "" : "/") + path;
  }
  private static boolean isChild(JsonNode field) { return field.path("_ui").isObject() || field.path("items").path("_ui").isObject(); }
  private static String escape(String key) { return key.replace("~", "~0").replace("/", "~1"); }
}
