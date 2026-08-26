package org.metadatacenter.cedar.artifact;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.server.service.DiagnosticsService;

/** Readiness check for the document store that backs every artifact CRUD route. */
public class ArtifactServerMongoHealthCheck extends HealthCheck {

  private final DiagnosticsService<JsonNode> diagnosticsService;

  public ArtifactServerMongoHealthCheck(DiagnosticsService<JsonNode> diagnosticsService) {
    this.diagnosticsService = diagnosticsService;
  }

  @Override
  protected Result check() {
    JsonNode heartbeat = diagnosticsService.heartbeat();
    if (heartbeat != null && heartbeat.path("storageServerConnection").asBoolean(false)) {
      return Result.healthy();
    }
    String detail = heartbeat == null ? "Mongo heartbeat returned no result" : heartbeat.toString();
    return Result.unhealthy(detail);
  }
}
