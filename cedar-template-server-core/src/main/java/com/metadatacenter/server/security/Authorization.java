package com.metadatacenter.server.security;

import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.CedarCapability;
import play.mvc.Http;

public final class Authorization {

  private static final int connectTimeout = 1000;
  private static final int socketTimeout = 10000;

  private Authorization() {
  }

  public static void mustHaveCapability(Http.Request request, CedarCapability capability) throws CedarAccessException {
    String cn = capability.getCapabilityName();
    System.out.println("Must have capability:" + cn);
    KeycloakUtils.enforceRealmRoleOnOfflineToken(new CedarFrontendToPlayAuthRequest(request), cn);
    System.out.println("Token must be active");
    KeycloakUtils.checkIfTokenIsStillActiveByUserInfo(new CedarFrontendToPlayAuthRequest(request));
  }

}