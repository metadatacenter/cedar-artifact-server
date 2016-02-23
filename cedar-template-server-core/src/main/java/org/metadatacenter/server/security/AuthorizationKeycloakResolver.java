package org.metadatacenter.server.security;

import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.CedarCapability;

public class AuthorizationKeycloakResolver implements IAuthorizationResolver {

  @Override
  public void mustHaveCapability(CedarFrontendToPlayAuthRequest authRequest, CedarCapability capability) throws
      CedarAccessException {
    String cn = capability.getCapabilityName();
    //System.out.println("AuthorizationKeycloakResolver: Must have capability:" + cn);
    KeycloakUtils.enforceRealmRoleOnOfflineToken(authRequest, cn);
    //System.out.println("AuthorizationKeycloakResolver: Token must be active");
    KeycloakUtils.checkIfTokenIsStillActiveByUserInfo(authRequest);
  }
}
