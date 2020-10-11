package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.glassfish.jersey.client.ClientProperties;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.metadatacenter.cedar.artifact.ArtifactServerApplication;
import org.metadatacenter.cedar.artifact.ArtifactServerConfiguration;
import org.metadatacenter.cedar.artifact.resources.utils.TestConstants;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.cedar.test.util.TestDataGenerationContext;
import org.metadatacenter.exception.ArtifactServerResourceNotFoundException;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.server.jsonld.LinkedDataUtil;
import org.metadatacenter.util.test.TestUserUtil;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.metadatacenter.cedar.artifact.resources.utils.TestConstants.*;
import static org.metadatacenter.constant.HttpConstants.CREATED;

public abstract class AbstractResourceTest {

  protected static Logger log;

  protected static String baseTestUrl;
  protected static String authHeaderTestUser1;
  protected static Client testClient;
  protected static LinkedDataUtil linkedDataUtil;
  protected static TestDataGenerationContext tdctx;


  /**
   * Prints the class name and test name before running the test
   */
  @Rule
  public TestRule watcher = new TestWatcher() {
    protected void starting(Description description) {
      //log.info("------------------------------------------------------------------------");
      //log.info(description.toString());
      //log.info("------------------------------------------------------------------------");
    }
  };

  @ClassRule
  public static final DropwizardAppRule<ArtifactServerConfiguration> SERVER_APPLICATION =
      new DropwizardAppRule<>(ArtifactServerApplication.class,
          ResourceHelpers.resourceFilePath(TEST_CONFIG_FILE));


  protected static void performOneTimeSetup() {
    // Get authorization header for TestUser1
    authHeaderTestUser1 = TestUserUtil.getTestUser1AuthHeader(TestUtil.getCedarConfig());

    // Test server url
    baseTestUrl = TestConstants.BASE_URL + ":" + SERVER_APPLICATION.getLocalPort();

    // Set up test client
    testClient =  new ResteasyClientBuilder().build();
    //testClient = new JerseyClientBuilder(SERVER_APPLICATION.getEnvironment()).build(TEST_CLIENT_NAME);
    testClient.property(ClientProperties.READ_TIMEOUT, DEFAULT_TIMEOUT);
    testClient.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_TIMEOUT);
    testClient.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, true);

    linkedDataUtil = new LinkedDataUtil(TestUtil.getCedarConfig().getLinkedDataConfig());

    tdctx = new TestDataGenerationContext();
    tdctx.setAuthHeaderTestUser1(authHeaderTestUser1);
    tdctx.setBaseTestUrl(baseTestUrl);
    tdctx.setLinkedDataUtil(linkedDataUtil);

  }

  // Create a artifact
  protected static JsonNode createResource(JsonNode resource, CedarResourceType resourceType) throws IOException {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    Response response =
        testClient.target(url).request().header(AUTHORIZATION, authHeaderTestUser1).post(Entity.json(resource));
    if (response.getStatus() != CREATED) {
      throw new IllegalStateException("The artifact was not created! The test can not continue this way.");
    }
    return response.readEntity(JsonNode.class);
  }

  /**
   * Remove resources by id
   */
  protected static void removeResources(Map<String, CedarResourceType> resourceMap) throws IOException,
      ArtifactServerResourceNotFoundException {
    Iterator it = resourceMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry) it.next();
      String id = (String) pair.getKey();
      CedarResourceType resourceType = (CedarResourceType) pair.getValue();
      removeResource(id, resourceType);
      System.out.println("Resource: " + id + " has been removed correctly");
    }
  }

  protected static void removeResource(String id, CedarResourceType resourceType) {
    try {
      if (resourceType.equals(CedarResourceType.TEMPLATE)) {
        TestUtil.templateService.deleteTemplate(id);
      } else if (resourceType.equals(CedarResourceType.ELEMENT)) {
        TestUtil.templateElementService.deleteTemplateElement(id);
      } else if (resourceType.equals(CedarResourceType.FIELD)) {
        TestUtil.templateFieldService.deleteTemplateField(id);
      } else { // Template instance
        TestUtil.templateInstanceService.deleteTemplateInstance(id);
      }
    } catch (ArtifactServerResourceNotFoundException e) {
      log.info("Resource not found. Id = " + id);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
