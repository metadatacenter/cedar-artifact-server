package org.metadatacenter.cedar.artifact.resources.utils;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarApiKeyAuthRequest;
import org.metadatacenter.server.security.CedarUserRolePermissionUtil;
import org.metadatacenter.server.security.model.user.CedarUser;
import org.metadatacenter.server.security.model.user.CedarUserApiKey;
import org.metadatacenter.server.security.model.user.CedarUserRole;

import java.time.LocalDateTime;

/**
 * Provides the authenticated identity for integration tests without any live auth backend. The
 * test user is built in memory with the roles the artifact server endpoints require, registered
 * with the Authorization holder through InMemoryUserService, and its API key is used in the
 * Authorization header of every test request.
 *
 * Call installInMemoryUserService once per test class, after the DropwizardAppRule has started:
 * the application's own startup wires the Neo4j-backed user service, and this call replaces it
 * for the lifetime of the test JVM.
 */
public final class TestAuthUtil {

  private static final String TEST_USER_1_API_KEY = "11111111-2222-3333-4444-555555555555";

  private static CedarUser testUser1;

  private TestAuthUtil() {
  }

  public static synchronized CedarUser getTestUser1(CedarConfig cedarConfig) {
    if (testUser1 == null) {
      testUser1 = buildTestUser1(cedarConfig);
    }
    return testUser1;
  }

  public static void installInMemoryUserService(CedarConfig cedarConfig) {
    Authorization.setUserService(new InMemoryUserService(getTestUser1(cedarConfig)));
  }

  public static String getTestUser1AuthHeader(CedarConfig cedarConfig) {
    return new CedarApiKeyAuthRequest(getTestUser1(cedarConfig).getFirstActiveApiKey()).getAuthHeader();
  }

  private static CedarUser buildTestUser1(CedarConfig cedarConfig) {
    CedarUser user = new CedarUser();
    user.setId(cedarConfig.getTestUsers().getTestUser1().getId());
    user.setFirstName("Test1");
    user.setLastName("User");
    user.setEmail("test1@test.com");

    CedarUserApiKey apiKey = new CedarUserApiKey();
    apiKey.setKey(TEST_USER_1_API_KEY);
    apiKey.setServiceName("CEDAR");
    apiKey.setDescription("apiKey for the integration test user");
    apiKey.setCreationDate(LocalDateTime.now());
    apiKey.setEnabled(true);
    user.getApiKeys().add(apiKey);

    user.getRoles().add(CedarUserRole.DEFAULT_USER);
    user.getRoles().add(CedarUserRole.TEMPLATE_CREATOR);
    user.getRoles().add(CedarUserRole.METADATA_CREATOR);
    CedarUserRolePermissionUtil.expandRolesIntoPermissions(user);
    return user;
  }

}
