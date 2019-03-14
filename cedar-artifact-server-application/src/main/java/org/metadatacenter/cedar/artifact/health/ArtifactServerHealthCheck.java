package org.metadatacenter.cedar.artifact.health;

import com.codahale.metrics.health.HealthCheck;

public class ArtifactServerHealthCheck extends HealthCheck {

  public ArtifactServerHealthCheck() {
  }

  @Override
  protected Result check() throws Exception {
    if (2 * 2 == 5) {
      return Result.unhealthy("Unhealthy, because 2 * 2 == 5");
    }
    return Result.healthy();
  }
}
