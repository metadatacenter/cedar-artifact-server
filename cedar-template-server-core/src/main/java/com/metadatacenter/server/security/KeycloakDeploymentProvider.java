package com.metadatacenter.server.security;

import org.keycloak.adapters.KeycloakDeployment;

public class KeycloakDeploymentProvider {

  private static KeycloakDeploymentProvider instance = new KeycloakDeploymentProvider();

  private KeycloakDeployment keycloakDeployment;

  private KeycloakDeploymentProvider() {
    keycloakDeployment = KeycloakUtils.buildDeployment();
  }

  public static KeycloakDeploymentProvider getInstance() {
    return instance;
  }

  public KeycloakDeployment getKeycloakDeployment() {
    return keycloakDeployment;
  }
}
