package org.metadatacenter.cedar.artifact;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactServerMongoHealthCheckTest {

  @Test
  void healthyOnlyWhenMongoHeartbeatIsConnected() {
    ObjectNode connected = JsonNodeFactory.instance.objectNode().put("storageServerConnection", true);
    HealthCheck.Result result = new ArtifactServerMongoHealthCheck(() -> connected).execute();
    assertTrue(result.isHealthy());
  }

  @Test
  void unhealthyWhenMongoHeartbeatReportsFailure() {
    ObjectNode disconnected = JsonNodeFactory.instance.objectNode()
        .put("storageServerConnection", false)
        .put("storageServerException", "connection refused");
    HealthCheck.Result result = new ArtifactServerMongoHealthCheck(() -> disconnected).execute();
    assertFalse(result.isHealthy());
  }
}
