package org.metadatacenter.cedar.artifact.resources.utils;

import org.metadatacenter.id.CedarUserId;
import org.metadatacenter.server.security.IUserService;
import org.metadatacenter.server.security.model.user.CedarUser;
import org.metadatacenter.server.security.model.user.CedarUserApiKey;

import java.util.List;

/**
 * A user service backed by a fixed set of users, replacing the Neo4j-backed lookup during
 * integration tests. Installed via Authorization.setUserService after the Dropwizard test
 * application has started, it lets API-key authentication work with no live Neo4j. Keycloak is
 * not involved either: the API-key path of AuthorizationKeycloakAndApiKeyResolver never
 * contacts it.
 */
public class InMemoryUserService implements IUserService {

  private final List<CedarUser> users;

  public InMemoryUserService(CedarUser... users) {
    this.users = List.of(users);
  }

  @Override
  public CedarUser findUserByApiKey(String apiKey) {
    if (apiKey == null) {
      return null;
    }
    for (CedarUser user : users) {
      for (CedarUserApiKey key : user.getApiKeys()) {
        if (key.isEnabled() && apiKey.equals(key.getKey())) {
          return user;
        }
      }
    }
    return null;
  }

  @Override
  public CedarUser findUser(CedarUserId userId) {
    if (userId == null) {
      return null;
    }
    for (CedarUser user : users) {
      if (user.getId().equals(userId.getId())) {
        return user;
      }
    }
    return null;
  }

}
