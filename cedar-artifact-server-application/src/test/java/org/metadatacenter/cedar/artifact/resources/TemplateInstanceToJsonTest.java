package org.metadatacenter.cedar.artifact.resources;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metadatacenter.model.request.OutputFormatType;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemplateInstanceToJsonTest extends BaseServerTest {

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
  public void shouldGetJsonOutput() {
    Response response = sendGetRequest(
        TestRequestUrls.forFindingInstance(getPortNumber(), instanceExampleId,
            OutputFormatType.JSON.getValue()));
    checkStatusOk(response);
    // Assert header
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE), is(MediaType.APPLICATION_JSON));
    assertEquals("\"1-json\"", response.getHeaderString(HttpHeaders.ETAG));
    // Assert content
    String responseContent = response.readEntity(String.class);
    System.out.println(responseContent);
  }
}
