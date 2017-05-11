package org.metadatacenter.cedar.template.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.metadatacenter.cedar.template.TemplateServerApplication;
import org.metadatacenter.cedar.template.TemplateServerConfiguration;
import org.metadatacenter.cedar.template.resources.utils.TestUtil;
import org.metadatacenter.util.test.TestUserUtil;

import javax.annotation.Nonnull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Iterator;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.metadatacenter.constant.HttpConstants.HTTP_HEADER_AUTHORIZATION;

public abstract class BaseServerTest {

  private static String authHeaderValue;

  private static Client testClient;

  @ClassRule
  public static final DropwizardAppRule<TemplateServerConfiguration> SERVER_APPLICATION =
      new DropwizardAppRule<>(TemplateServerApplication.class,
          ResourceHelpers.resourceFilePath("test-config.yml"));

  @BeforeClass
  public static void fetchAuthHeader() {
    authHeaderValue = TestUserUtil.getTestUser1AuthHeader(TestUtil.getCedarConfig());
  }

  @BeforeClass
  public static void createTestClient() {
    testClient = new JerseyClientBuilder(SERVER_APPLICATION.getEnvironment()).build("TestClient");
    testClient.property(ClientProperties.READ_TIMEOUT, 3000); // 3s
    testClient.property(ClientProperties.CONNECT_TIMEOUT, 3000);
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

  protected void assertValidationMessage(JsonNode responseMessage, String expectedValue) {
    Set<String> errorMessages = Sets.newHashSet();
    Iterator<JsonNode> iter = responseMessage.get("errors").elements();
    while (iter.hasNext()) {
      JsonNode errorItem = iter.next();
      String errorMessage = errorItem.get("message").asText();
      errorMessages.add(errorMessage);
    }
    assertThat(printReason(responseMessage), errorMessages.contains(expectedValue));
  }

  protected void assertValidationStatus(JsonNode responseMessage, String expectedValue) {
    String statusMessage = responseMessage.get("validates").asText();
    assertThat(printReason(responseMessage), statusMessage, is(expectedValue));
  }

  protected String printReason(JsonNode responseMessage) {
    return "The server is returning a different validation report.\n(application/json): " + responseMessage.toString();
  }
}
