package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.model.request.OutputFormatType;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.metadatacenter.constant.CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS;

public class TemplateInstanceToRdfTest extends BaseServerTest {

  private String templateExampleId;
  private String instanceExampleId;

  private static String templateExample;
  private static String instanceExample;

  @BeforeAll
  public static void loadTestPayload() {
    templateExample = TestResourcesUtils.getStringContent("instances/usecase-template.json");
    instanceExample = TestResourcesUtils.getStringContent("instances/usecase-instance.jsonld");
  }

  @BeforeEach
  public void uploadResources() {
    templateExampleId = uploadTemplate(templateExample);
    instanceExampleId = uploadInstance(instanceExample);
  }

  @AfterEach
  public void removeResources() {
    removeInstance(instanceExampleId);
    removeTemplate(templateExampleId);
  }

  @Test
  public void shouldGetRdfOutput() {
    Response response = sendGetRequest(TestRequestUrls.forFindingInstance(getPortNumber(), instanceExampleId,
        OutputFormatType.RDF_NQUAD.getValue()));
    checkStatusOk(response);
    // Assert header
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE), is("application/n-quads"));
    // Assert content
    String responseContent = response.readEntity(String.class);
    System.out.println(responseContent);
  }

  @Test
  public void shouldGetRdfOutputWhenNQuadsIsAccepted() {
    Response response = sendGetRequest(TestRequestUrls.forFindingInstance(getPortNumber(), instanceExampleId,
        OutputFormatType.RDF_NQUAD.getValue()), "application/n-quads");

    checkStatusOk(response);
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE), is("application/n-quads"));
  }

  /**
   * An update that breaks the instance is refused, and the stored artifact is left as it was.
   *
   * <p>This used to switch validation on by reflection, because it was switchable —
   * {@code CEDAR_VALIDATION_ENABLED} — and the test could not assume the environment it ran in had it
   * on. Validation is unconditional now, so the test says what it always meant to: an artifact the
   * model rejects does not reach storage.
   */
  @Test
  public void shouldRejectAnUpdateThatBreaksTheInstance() {
    String instanceUrl = TestRequestUrls.forCreatingInstances(getPortNumber(), instanceExampleId);
    JsonNode storedInstance = sendGetRequest(instanceUrl).readEntity(JsonNode.class);
    ((ObjectNode) storedInstance).remove("Company Name");

    Response updateResponse = testClient.target(instanceUrl)
        .request()
        .header(HttpHeaders.AUTHORIZATION, authHeaderValue)
        .put(Entity.json(storedInstance));

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateResponse.getStatus());
    assertEquals("false", updateResponse.getHeaderString(HEADER_CEDAR_VALIDATION_STATUS));
    JsonNode unchangedInstance = sendGetRequest(instanceUrl).readEntity(JsonNode.class);
    assertNotNull(unchangedInstance.get("Company Name"));
  }
}
