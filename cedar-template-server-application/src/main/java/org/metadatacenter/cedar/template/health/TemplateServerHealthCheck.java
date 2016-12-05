package org.metadatacenter.cedar.template.health;

import com.codahale.metrics.health.HealthCheck;

public class TemplateServerHealthCheck extends HealthCheck {

  public TemplateServerHealthCheck() {
  }

  @Override
  protected Result check() throws Exception {
    if (2 * 2 == 5) {
      return Result.unhealthy("Unhealthy, because 2 * 2 == 5");
    }
    return Result.healthy();
  }
}