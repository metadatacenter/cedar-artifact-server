package org.metadatacenter.server.security;

import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.CedarCapability;
import play.mvc.Http;

public final class Authorization {

  private static IAuthorizationResolver resolver;

  private Authorization() {
  }

  public static void setAuthorizationResolver(IAuthorizationResolver r) {
    resolver = r;
  }

  public static void mustHaveCapability(Http.Request request, CedarCapability capability) throws CedarAccessException {
    resolver.mustHaveCapability(new CedarFrontendToPlayAuthRequest(request), capability);
  }

}