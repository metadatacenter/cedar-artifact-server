package org.metadatacenter.cedar.template.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import static org.metadatacenter.constant.HttpConstants.HTTP_HEADER_AUTHORIZATION;

@RunWith(MockitoJUnitRunner.class)
public class TemplateInstancesResourceTest extends BaseTemplateResourceTest {

  @Mock
  TemplateInstancesResource instance;

  @Rule
  public final ResourceTestRule resources = ResourceTestRule.builder()
      .addResource(instance)
      .build();

  @Test
  public void shouldFindTemplateInstance() {
    Response response = resources.client().target("https://resource.metadatacenter.orgx/template-instances/123")
        .request()
        .header(HTTP_HEADER_AUTHORIZATION, "apiKey f843a2b9-a8c1-49a7-b35a-1905ec0817d6")
        .get();
    assertThat(response.getStatus(), is(202));
  }
}
