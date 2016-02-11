package com.metadatacenter.server.security;

import org.keycloak.adapters.KeycloakDeployment;

public class UserAuthService {

  private static UserAuthService instance = new UserAuthService();

  public KeycloakDeployment keycloakDeployment;

  public static UserAuthService getInstance() {
    return instance;
  }

  private UserAuthService() {
    keycloakDeployment = KeycloakUtils.buildDeployment();
  }

  public KeycloakDeployment getKeycloakDeployment() {
    return keycloakDeployment;
  }
}
