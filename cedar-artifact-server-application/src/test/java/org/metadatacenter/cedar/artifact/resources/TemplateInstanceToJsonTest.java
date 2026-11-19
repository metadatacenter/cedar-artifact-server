package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.metadatacenter.model.request.OutputFormatType;
import org.metadatacenter.util.json.JsonMapper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemplateInstanceToJsonTest extends BaseServerTest {

  private String templateExampleId;
  private String instanceExampleId;
  private String instanceEtag;

  private String templateExample;
  private String instanceExample;

  @BeforeAll
  public void loadTestPayload() {
    templateExample = TestResourcesUtils.getStringContent("instances/usecase-template.json");
    instanceExample = TestResourcesUtils.getStringContent("instances/usecase-instance.jsonld");
  }

  @BeforeEach
  public void uploadResources() {
    templateExampleId = uploadTemplate(templateExample);
    instanceExampleId = uploadInstance(instanceExample);
    instanceEtag = currentEtag(TestRequestUrls.forCreatingInstances(getPortNumber(), instanceExampleId), authHeaderValue);
  }

  @AfterEach
  public void removeResources() {
    removeInstance(instanceExampleId);
    removeTemplate(templateExampleId);
  }

  // Reuse the fixture identifier after deletion: the exported validator must follow the new revision.
  @RepeatedTest(2)
  public void shouldGetJsonOutput() throws Exception {
    Response response = sendGetRequest(
        TestRequestUrls.forFindingInstance(getPortNumber(), instanceExampleId,
            OutputFormatType.JSON.getValue()));
    checkStatusOk(response);
    // Assert header
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE), is(MediaType.APPLICATION_JSON));
    assertEquals(instanceEtag.substring(0, instanceEtag.length() - 1) + "-json\"", response.getHeaderString(HttpHeaders.ETAG));
    // The JSON export is the instance with its JSON-LD keywords removed, which is the whole of it
    JsonNode exported = response.readEntity(JsonNode.class);
    assertEquals(JsonMapper.STRICT_MAPPER.readTree(
        exportFixture("instances/usecase-instance-export.json", instanceExampleId)), exported);
  }
}
