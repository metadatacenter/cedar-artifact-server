package com.metadatacenter.server.security;

import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.CedarCapability;

public class AuthorizationNoauthResolver implements IAuthorizationResolver {

  @Override
  public void mustHaveCapability(CedarFrontendToPlayAuthRequest authRequest, CedarCapability capability) throws
      CedarAccessException {
    //System.out.println("AuthorizationNoauthResolver: doNothing");
  }
}
