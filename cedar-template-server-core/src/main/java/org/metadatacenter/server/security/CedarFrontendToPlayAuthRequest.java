package org.metadatacenter.server.security;

import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.server.security.model.play.IAuthRequest;
import play.mvc.Http;

public class CedarFrontendToPlayAuthRequest implements IAuthRequest {

  private String token;

  public CedarFrontendToPlayAuthRequest(Http.Request request) {
    if (request != null) {
      String auth = request.getHeader(Http.HeaderNames.AUTHORIZATION);
      if (auth != null) {
        if (auth.startsWith(HttpConstants.HTTP_AUTH_HEADER_BEARER_PREFIX)) {
          token = auth.substring(HttpConstants.HTTP_AUTH_HEADER_BEARER_PREFIX.length());
        }
      }
    }
  }

  public String getToken() {
    return token;
  }
}
