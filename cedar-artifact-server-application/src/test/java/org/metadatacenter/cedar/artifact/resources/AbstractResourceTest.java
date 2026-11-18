package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.metadatacenter.cedar.artifact.ArtifactServerApplication;
import org.metadatacenter.cedar.artifact.ArtifactServerConfiguration;
import org.metadatacenter.cedar.artifact.resources.utils.TestConstants;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.cedar.test.util.TestDataGenerationContext;
import org.metadatacenter.exception.ArtifactServerResourceNotFoundException;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.server.jsonld.LinkedDataUtil;
import org.metadatacenter.util.test.EmbeddedCedarMongo;
import org.metadatacenter.util.test.TestAuthUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.metadatacenter.cedar.artifact.resources.utils.TestConstants.DEFAULT_TIMEOUT;
import static org.metadatacenter.cedar.artifact.resources.utils.TestConstants.TEST_CONFIG_FILE;
import static org.metadatacenter.constant.HttpConstants.CREATED;

/**
 * Starts one artifact server for each concrete test class and holds what its tests share: the server's
 * address, a client, the test user's credentials and the artifacts the current test created. All of it
 * belongs to the test instance, which JUnit keeps for the whole class, so no two classes share any of it.
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractResourceTest {

  private static void redirectEnvironment() {
    // Must run before anything builds the CEDAR configuration: the document store comes from an
    // in-process MongoDB, and Redis goes to a dead port, since queue writes are best-effort -
    // the suite needs no live backend at all. OS-assigned server ports, so the test instance
    // never collides with a running dev server.
    EmbeddedCedarMongo.startAndRedirectEnvironment(java.util.Map.of(
        "CEDAR_ARTIFACT_HTTP_PORT", "0",
        "CEDAR_ARTIFACT_ADMIN_PORT", "0",
        "CEDAR_ARTIFACT_STOP_PORT", "0",
        "CEDAR_REDIS_PERSISTENT_PORT", "1",
        "CEDAR_TERMINOLOGY_SERVER_HOST", "127.0.0.1",
        "CEDAR_TERMINOLOGY_HTTP_PORT", "1",
        "CEDAR_BRIDGE_SERVER_HOST", "127.0.0.1",
        "CEDAR_BRIDGE_HTTP_PORT", "1"));
  }

  protected static final Logger log = LoggerFactory.getLogger(AbstractResourceTest.class);

  protected final DropwizardTestSupport<ArtifactServerConfiguration> serverApplication =
      new DropwizardTestSupport<>(ArtifactServerApplication.class,
          ResourceHelpers.resourceFilePath(TEST_CONFIG_FILE));

  protected String baseTestUrl;
  protected String authHeaderTestUser1;
  protected Client testClient;
  protected LinkedDataUtil linkedDataUtil;
  protected TestDataGenerationContext tdctx;

  /** The artifacts the current test created, removed after it. */
  protected Map<String, CedarResourceType> createdResources = new HashMap<>();

  @BeforeAll
  public void startServerApplication() throws Exception {
    // The class-isolation extension restores the process environment after each class, so each
    // class redirects it again before its server starts.
    redirectEnvironment();
    serverApplication.before();
  }

  @AfterAll
  public void stopServerApplication() {
    if (testClient != null) {
      testClient.close();
    }
    serverApplication.after();
  }

  @BeforeEach
  public void startCleanupBookkeeping() {
    createdResources = new HashMap<>();
  }

  @AfterEach
  public void removeCreatedResources() {
    removeResources(createdResources);
  }

  protected void performOneTimeSetup() {
    // Replace the Neo4j-backed user service wired at application startup with an in-memory one,
    // so API-key authentication needs no live Neo4j (and no Keycloak)
    TestAuthUtil.installInMemoryUserService(TestUtil.getCedarConfig());

    // Get authorization header for TestUser1
    authHeaderTestUser1 = TestAuthUtil.getTestUser1AuthHeader(TestUtil.getCedarConfig());

    // Test server url
    baseTestUrl = TestConstants.BASE_URL + ":" + serverApplication.getLocalPort();

    // Set up test client. Many tests check only the response status and never read the entity,
    // which keeps the pooled connection leased until the response is garbage collected; the pool
    // must outsize the largest parameterized test class (432 runs), or the client deadlocks
    // waiting for a free connection.
    // The former RESTEasy client (ResteasyClientBuilder.connectionPoolSize) no longer works: under
    // jakarta ws.rs 3.0 the runtime resolves to Jersey, so build the client through Dropwizard's
    // JerseyClientBuilder instead. A large per-route pool is required because many tests never read
    // the response entity (the pooled connection stays leased until GC), so it must outsize the
    // largest parameterized test class. reuseForks shares one JVM, so the client name must be unique
    // per build to avoid a metrics-registry collision.
    JerseyClientConfiguration clientConfig = new JerseyClientConfiguration();
    clientConfig.setTimeout(Duration.milliseconds(DEFAULT_TIMEOUT));
    clientConfig.setConnectionTimeout(Duration.milliseconds(DEFAULT_TIMEOUT));
    clientConfig.setConnectionRequestTimeout(Duration.milliseconds(DEFAULT_TIMEOUT));
    clientConfig.setMaxConnections(1024);
    clientConfig.setMaxConnectionsPerRoute(1024);
    // Dropwizard's JerseyClientConfiguration enables gzip on the client by default. Against this server
    // that leaves the client trying to gunzip a response body that is not gzip-encoded, so readEntity()
    // fails with "ZipException: Not in GZIP format" on every response the test actually reads (get,
    // update, delete, find). The tests do not need compression; disable it so responses are read as-is.
    clientConfig.setGzipEnabled(false);
    clientConfig.setGzipEnabledForRequests(false);
    testClient = new JerseyClientBuilder(serverApplication.getEnvironment())
        .using(clientConfig)
        .build("artifact-test-client-" + System.nanoTime());
    // The service key is read once: a configuration lookup in the filter ran, and logged, on every request.
    String serviceKey = TestUtil.getCedarConfig().getArtifactService().requireApiKey();
    testClient.register((jakarta.ws.rs.client.ClientRequestFilter) request ->
        request.getHeaders().putSingle(org.metadatacenter.config.ArtifactServiceConfig.HEADER, serviceKey));
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
  protected JsonNode createResource(JsonNode resource, CedarResourceType resourceType) {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    Response response = testClient.target(url).request().header(AUTHORIZATION, authHeaderTestUser1).post(Entity.json(resource));
    if (response.getStatus() != CREATED) {
      throw new IllegalStateException("The artifact was not created! The test can not continue this way.");
    }
    return response.readEntity(JsonNode.class);
  }

  protected String currentEtag(String url, String authorization) {
    Response response = testClient.target(url).request().header(AUTHORIZATION, authorization).get();
    String etag = response.getHeaderString("ETag");
    response.close();
    return etag;
  }

  /**
   * Remove resources by id
   */
  protected static void removeResources(Map<String, CedarResourceType> resourceMap) {
    for (Map.Entry<String, CedarResourceType> pair : resourceMap.entrySet()) {
      String id = pair.getKey();
      CedarResourceType resourceType = pair.getValue();
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
