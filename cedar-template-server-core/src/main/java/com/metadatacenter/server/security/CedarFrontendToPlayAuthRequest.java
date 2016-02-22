package com.metadatacenter.server.security;


import org.metadatacenter.server.security.model.play.IAuthRequest;
import play.mvc.Http;

public class CedarFrontendToPlayAuthRequest implements IAuthRequest {

  private final static String AUTH_TOKEN_PREFIX = "bearer ";

  private String token;

  public CedarFrontendToPlayAuthRequest(Http.Request request) {
    if (request != null) {
      String auth = request.getHeader(Http.HeaderNames.AUTHORIZATION);
      if (auth != null) {
        if (auth.startsWith(AUTH_TOKEN_PREFIX)) {
          token = auth.substring(AUTH_TOKEN_PREFIX.length());
        }
      }
    }
  }

  public String getToken() {
    return token;
  }
}
