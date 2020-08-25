package org.metadatacenter.cedar.artifact.resources;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.metadatacenter.model.request.OutputFormatType;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TemplateInstanceToRdfTest extends BaseServerTest {

  private String templateExampleId;
  private String instanceExampleId;

  private static String templateExample;
  private static String instanceExample;

  @BeforeClass
  public static void loadTestPayload() {
    templateExample = TestResourcesUtils.getStringContent("instances/usecase-template.json");
    instanceExample = TestResourcesUtils.getStringContent("instances/usecase-instance.jsonld");
  }

  @Before
  public void uploadResources() {
    templateExampleId = uploadTemplate(templateExample);
    instanceExampleId = uploadInstance(instanceExample);
  }

  @After
  public void removeResources() {
    removeInstance(instanceExampleId);
    removeTemplate(templateExampleId);
  }

  @Test
  public void shouldGetRdfOutput() {
    Response response = sendGetRequest(TestRequestUrls.forFindingInstance(getPortNumber(), instanceExampleId, OutputFormatType.RDF_NQUAD.getValue()));
    checkStatusOk(response);
    // Assert header
    assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE), is("application/n-quads"));
    // Assert content
    String responseContent = response.readEntity(String.class);
    System.out.println(responseContent);
  }
}
