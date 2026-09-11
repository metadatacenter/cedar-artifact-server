package org.metadatacenter.cedar.artifact;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import org.metadatacenter.cedar.util.dw.CedarHealthCheckResource;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceIndexResource;
import org.metadatacenter.cedar.util.dw.CedarServerInsightReportResource;
import org.metadatacenter.cedar.util.dw.CedarServerReportResource;
import org.metadatacenter.config.ArtifactServiceConfig;
import org.metadatacenter.util.http.CedarResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

/** All business resources require internal authentication before their existing user checks. */
@Priority(Priorities.AUTHENTICATION - 100)
public final class ArtifactServiceAuthenticationFilter implements ContainerRequestFilter {
  private static final Set<Class<?>> OPERATIONAL_RESOURCES = Set.of(CedarMicroserviceIndexResource.class,
      CedarHealthCheckResource.class, CedarServerReportResource.class, CedarServerInsightReportResource.class);
  private final byte[] current;
  private final byte[] previous;
  @Context private ResourceInfo resourceInfo;

  public ArtifactServiceAuthenticationFilter(ArtifactServiceConfig config) {
    current = digest(config.requireApiKey());
    String old = config.getPreviousApiKey();
    if (old != null && !old.isEmpty() && !ArtifactServiceConfig.isValidKey(old)) {
      throw new IllegalStateException("Invalid previous artifact service API key");
    }
    previous = old == null || old.isEmpty() ? current : digest(old);
  }

  @Override
  public void filter(ContainerRequestContext request) {
    if (resourceInfo != null && OPERATIONAL_RESOURCES.contains(resourceInfo.getResourceClass())) return;
    var values = request.getHeaders().get(ArtifactServiceConfig.HEADER);
    String candidate = values != null && values.size() == 1 ? values.get(0) : null;
    boolean accepted = false;
    if (ArtifactServiceConfig.isValidKey(candidate)) {
      byte[] supplied = digest(candidate);
      accepted = MessageDigest.isEqual(current, supplied) | MessageDigest.isEqual(previous, supplied);
    }
    if (!accepted) {
      request.abortWith(CedarResponse.unauthorized().message("Internal service authentication required").build());
    }
  }

  private static byte[] digest(String key) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }
}
