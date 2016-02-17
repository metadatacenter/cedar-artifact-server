package com.metadatacenter.server.security;

import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.CedarCapability;
import play.mvc.Http;

public interface IAuthorizationResolver {

  void mustHaveCapability(CedarFrontendToPlayAuthRequest request, CedarCapability capability) throws
      CedarAccessException;
}
