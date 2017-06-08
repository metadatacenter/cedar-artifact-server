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

    CedarEnvironmentUtil.copy(CedarEnvironmentVariable.CEDAR_MONGO_APP_USER_NAME, env);
    CedarEnvironmentUtil.copy(CedarEnvironmentVariable.CEDAR_MONGO_APP_USER_PASSWORD, env);
    env.put(CedarEnvironmentVariable.CEDAR_MONGO_HOST.getName(), "localhost");
    env.put(CedarEnvironmentVariable.CEDAR_MONGO_PORT.getName(), "27017");

    env.put(CedarEnvironmentVariable.CEDAR_TEMPLATE_HTTP_PORT.getName(), "9001");
    env.put(CedarEnvironmentVariable.CEDAR_TEMPLATE_ADMIN_PORT.getName(), "9101");
    env.put(CedarEnvironmentVariable.CEDAR_TEMPLATE_STOP_PORT.getName(), "9201");

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