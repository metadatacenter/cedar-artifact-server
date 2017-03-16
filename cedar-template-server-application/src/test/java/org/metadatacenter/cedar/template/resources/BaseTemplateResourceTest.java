package org.metadatacenter.cedar.template.resources;

import javax.servlet.http.HttpServletRequest;

import static org.metadatacenter.constant.HttpConstants.HTTP_HEADER_AUTHORIZATION;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BaseTemplateResourceTest {

  public HttpServletRequest getMockRequest() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(HTTP_HEADER_AUTHORIZATION)).thenReturn("apiKey f843a2b9-a8c1-49a7-b35a-1905ec0817d6");
    return request;
  }
}
