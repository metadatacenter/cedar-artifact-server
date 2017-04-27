package org.metadatacenter.cedar.template.resources.crud;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.metadatacenter.cedar.template.TemplateServerApplication;
import org.metadatacenter.cedar.template.TemplateServerConfiguration;
import org.metadatacenter.cedar.template.resources.utils.TestConstants;
import org.metadatacenter.cedar.template.resources.utils.TestUtil;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.util.test.TestUserUtil;

import javax.ws.rs.client.Client;
import java.util.Map;

import static org.metadatacenter.cedar.template.resources.utils.TestConstants.DEFAULT_TIMEOUT;
import static org.metadatacenter.cedar.template.resources.utils.TestConstants.TEST_CLIENT_NAME;
import static org.metadatacenter.cedar.template.resources.utils.TestConstants.TEST_CONFIG_FILE;

public class AbstractResourceCrudTest {

  protected static String baseTestUrl;
  protected static String authHeader;
  protected static Client testClient;

  protected static Map<String, CedarNodeType> createdResources;

  @ClassRule
  public static final DropwizardAppRule<TemplateServerConfiguration> SERVER_APPLICATION =
      new DropwizardAppRule<>(TemplateServerApplication.class,
          ResourceHelpers.resourceFilePath(TEST_CONFIG_FILE));

  @BeforeClass
  public static void oneTimeSetUpAbstract() {
    // Get authorization header for TestUser1
    authHeader = TestUserUtil.getTestUser1AuthHeader(TestUtil.getCedarConfig());

    // Test server url
    baseTestUrl = TestConstants.BASE_URL + ":" + SERVER_APPLICATION.getLocalPort();

    // Set up test client
    testClient = new JerseyClientBuilder(SERVER_APPLICATION.getEnvironment()).build(TEST_CLIENT_NAME);
    testClient.property(ClientProperties.READ_TIMEOUT, DEFAULT_TIMEOUT);
    testClient.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_TIMEOUT);


    //****
    String host = TestUtil.getCedarConfig().getHost();
    String base = TestUtil.getCedarConfig().getServers().getTemplate().getBase();
    String port = TestUtil.getCedarConfig().getServers().getTemplate().getBase();
    String a = "2";



  }

  @AfterClass
  public static void cleanUp() {
    if (testClient != null) {
      testClient.close();
    }
  }

}
