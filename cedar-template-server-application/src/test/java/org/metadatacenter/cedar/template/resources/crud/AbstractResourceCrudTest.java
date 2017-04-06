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
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.util.test.TestUserUtil;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static org.metadatacenter.constant.HttpConstants.HTTP_HEADER_AUTHORIZATION;

public class AbstractResourceCrudTest {

  private static String authHeaderValue;

  private static Client testClient;

  @ClassRule
  public static final DropwizardAppRule<TemplateServerConfiguration> SERVER_APPLICATION =
      new DropwizardAppRule<>(TemplateServerApplication.class,
          ResourceHelpers.resourceFilePath("test-config.yml"));

  @BeforeClass
  public static void fetchAuthHeader() {
    authHeaderValue = TestUserUtil.getTestUser1AuthHeader(CedarConfig.getInstance());
  }

  @BeforeClass
  public static void createTestClient() {
    testClient = new JerseyClientBuilder(SERVER_APPLICATION.getEnvironment()).build("TestClient");
    testClient.property(ClientProperties.READ_TIMEOUT, 3000); // 3s
    testClient.property(ClientProperties.CONNECT_TIMEOUT, 3000); // 3s
  }

  @AfterClass
  public static void cleanUp() {
    if (testClient != null) {
      testClient.close();
    }
  }

  protected int getPortNumber() {
    return SERVER_APPLICATION.getLocalPort();
  }

  protected Response sendGetRequest(@Nonnull String requestUrl) {
    Response response = testClient.target(requestUrl)
        .request()
        .header(HTTP_HEADER_AUTHORIZATION, authHeaderValue)
        .get();
    return response;
  }

  protected Response sendPostRequest(@Nonnull String requestUrl, @Nonnull Object payload) {
    Response response = testClient.target(requestUrl)
        .request()
        .header(HTTP_HEADER_AUTHORIZATION, authHeaderValue)
        .post(Entity.json(payload));
    return response;
  }

  protected Response sendDeleteRequest(@Nonnull String requestUrl) {
    Response response = testClient.target(requestUrl)
        .request()
        .header(HTTP_HEADER_AUTHORIZATION, authHeaderValue)
        .delete();
    return response;
  }
}
