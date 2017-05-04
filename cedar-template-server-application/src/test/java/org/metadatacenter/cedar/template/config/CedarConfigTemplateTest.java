package org.metadatacenter.cedar.template.config;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentUtil;
import org.metadatacenter.config.environment.CedarEnvironmentVariable;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.test.TestUtil;

import java.util.HashMap;
import java.util.Map;

public class CedarConfigTemplateTest {

  @Before
  public void setEnvironment() {
    Map<String, String> env = new HashMap<>();

    env.put(CedarEnvironmentVariable.CEDAR_HOST.getName(), "metadatacenter.orgx");

    env.put(CedarEnvironmentVariable.CEDAR_KEYCLOAK_CLIENT_ID.getName(), "cedar-angular-app");

    CedarEnvironmentUtil.copy(CedarEnvironmentVariable.CEDAR_MONGO_USER_NAME, env);
    CedarEnvironmentUtil.copy(CedarEnvironmentVariable.CEDAR_MONGO_USER_PASSWORD, env);

    env.put(CedarEnvironmentVariable.CEDAR_LD_USER_BASE.getName(), "https://metadatacenter.org/users/");

    CedarEnvironmentUtil.copy(CedarEnvironmentVariable.CEDAR_TEST_USER1_ID, env);

    TestUtil.setEnv(env);
  }

  @Test
  public void testGetInstance() throws Exception {
    SystemComponent systemComponent = SystemComponent.SERVER_TEMPLATE;
    Map<String, String> environment = CedarEnvironmentVariableProvider.getFor(systemComponent);
    CedarConfig instance = CedarConfig.getInstance(environment);
    Assert.assertNotNull(instance);
  }


}