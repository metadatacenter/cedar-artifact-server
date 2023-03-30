package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.metadatacenter.cedar.artifact.ArtifactServerApplication;
import org.metadatacenter.cedar.artifact.ArtifactServerConfiguration;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.TestUserUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.metadatacenter.constant.HttpConstants.HTTP_HEADER_AUTHORIZATION;

public abstract class BaseServerTest {

  private static String authHeaderValue;

  private static Client testClient;

  @ClassRule
  public static final DropwizardAppRule<ArtifactServerConfiguration> SERVER_APPLICATION =
      new DropwizardAppRule<>(ArtifactServerApplication.class,
          ResourceHelpers.resourceFilePath("test-config.yml"));

  @BeforeClass
  public static void fetchAuthHeader() {
    authHeaderValue = TestUserUtil.getTestUser1AuthHeader(TestUtil.getCedarConfig());
  }

  @BeforeClass
  public static void createTestClient() {
    testClient = ResteasyClientBuilder.newBuilder().build();
    //testClient = new JerseyClientBuilder(SERVER_APPLICATION.getEnvironment()).build("TestClient");
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

  protected String uploadTemplate(String templateDocument) {
    String templateId = extractIdFromDocument(templateDocument);
    String encodedTemplateId = encodeUrl(templateId);
    Response response = sendPutRequest(
        TestRequestUrls.forCreatingTemplate(getPortNumber(), encodedTemplateId),
        templateDocument);
    checkStatusOk(response);
    return encodedTemplateId;
  }

  protected String uploadInstance(String instanceDocument) {
    String instanceId = extractIdFromDocument(instanceDocument);
    String encodedInstanceId = encodeUrl(instanceId);
    Response response = sendPutRequest(
        TestRequestUrls.forCreatingInstances(getPortNumber(), encodedInstanceId),
        instanceDocument);
    checkStatusOk(response);
    return encodedInstanceId;
  }

  protected void removeTemplate(String templateId) {
    Response response = sendDeleteRequest(
        TestRequestUrls.forDeletingTemplate(getPortNumber(), templateId));
    checkStatusOk(response);
  }

  protected void removeInstance(String instanceId) {
    Response response = sendDeleteRequest(
        TestRequestUrls.forDeletingInstance(getPortNumber(), instanceId));
    checkStatusOk(response);
  }

  protected Response sendGetRequest(String requestUrl) {
    Response response = testClient.target(requestUrl)
        .request()
        .header(HTTP_HEADER_AUTHORIZATION, authHeaderValue)
        .get();
    return response;
  }

  protected Response sendPostRequest(String requestUrl, Object payload) {
    Response response = testClient.target(requestUrl)
        .request()
        .header(HTTP_HEADER_AUTHORIZATION, authHeaderValue)
        .post(Entity.json(payload));
    return response;
  }

  protected Response sendPutRequest(String requestUrl, Object payload) {
    Response response = testClient.target(requestUrl)
        .request()
        .header(HTTP_HEADER_AUTHORIZATION, authHeaderValue)
        .put(Entity.json(payload));
    return response;
  }

  protected Response sendDeleteRequest(String requestUrl) {
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

  protected static String extractIdFromDocument(String templateDocument) {
    JsonNode template = null;
    try {
      template = JsonMapper.MAPPER.readTree(templateDocument);
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
    JsonNode idNode = template.get(LinkedData.ID);
    if (idNode != null) {
      return idNode.asText();
    }
    return null;
  }

  protected static String encodeUrl(String url) {
    try {
      return URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  protected static void checkStatusOk(Response response) {
    checkNotNull(response);
    int responseCode = response.getStatus();
    if (Response.Status.Family.familyOf(responseCode) == Response.Status.Family.CLIENT_ERROR) {
      throw new RuntimeException("Request contains bad syntax or cannot be fulfilled:\n" + response.toString());
    } else if (Response.Status.Family.familyOf(responseCode) == Response.Status.Family.SERVER_ERROR) {
      throw new RuntimeException("Server failed to fulfill the request:\n" + response.toString());
    }
  }
}
