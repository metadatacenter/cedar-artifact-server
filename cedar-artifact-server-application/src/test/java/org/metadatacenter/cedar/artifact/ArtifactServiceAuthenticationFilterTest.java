package org.metadatacenter.cedar.artifact;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;
import org.metadatacenter.config.ArtifactServiceConfig;
import org.metadatacenter.util.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArtifactServiceAuthenticationFilterTest {
  private static final String CURRENT = "test-only-artifact-service-key-not-for-prod";
  private static final String PREVIOUS = "old--only-artifact-service-key-not-for-prod";

  private ArtifactServiceConfig config(String current, String previous) {
    return JsonMapper.STRICT_MAPPER.convertValue(Map.of("apiKey", current, "previousApiKey", previous),
        ArtifactServiceConfig.class);
  }

  @Test
  void acceptsAnOverlappingRotationAndRejectsTheRetiredKey() {
    var filter = new ArtifactServiceAuthenticationFilter(config(CURRENT, PREVIOUS));
    for (String key : List.of(CURRENT, PREVIOUS)) {
      var request = request(List.of(key));
      filter.filter(request);
      verify(request, never()).abortWith(any());
    }
    var retired = request(List.of(PREVIOUS));
    new ArtifactServiceAuthenticationFilter(config(CURRENT, "")).filter(retired);
    verify(retired).abortWith(argThat(response -> response.getStatus() == 401));
  }

  @Test
  void rejectsMissingInvalidAndDuplicateKeys() {
    var filter = new ArtifactServiceAuthenticationFilter(config(CURRENT, ""));
    for (var keys : List.of(List.<String>of(), List.of("wrong"), List.of(CURRENT, CURRENT))) {
      var request = request(keys);
      filter.filter(request);
      verify(request).abortWith(argThat(response -> response.getStatus() == 401));
    }
  }

  @Test
  void refusesToStartWithMissingOrMalformedConfiguration() {
    assertThrows(IllegalStateException.class, () -> new ArtifactServiceAuthenticationFilter(config("", "")));
    assertThrows(IllegalStateException.class, () -> new ArtifactServiceAuthenticationFilter(config(CURRENT, "wrong")));
  }

  @Test
  void apiContractRequiresBothCredentialsRatherThanEitherOne() throws Exception {
    try (var input = getClass().getResourceAsStream("/assets/swagger-api/swagger.json")) {
      var spec = JsonMapper.STRICT_MAPPER.readTree(input);
      var paths = spec.path("paths");
      for (String path : List.of("/templates", "/template-elements", "/template-fields", "/template-instances",
          "/command/validate")) {
        var operation = paths.path(path).path(path.startsWith("/command") ? "post" : "get");
        assertFalse(operation.isMissingNode(), path);
        var security = operation.has("security") ? operation.path("security") : spec.path("security");
        assertEquals(1, security.size(), path);
        assertTrue(security.get(0).has("api_key") && security.get(0).has("artifact_service"), path);
      }
      assertFalse(spec.has("security"));
      assertFalse(paths.path("/").path("get").has("security"));
      assertFalse(paths.path("/healthcheck").path("get").path("security").get(0).has("artifact_service"));
    }
  }

  private ContainerRequestContext request(List<String> keys) {
    var request = mock(ContainerRequestContext.class);
    var headers = new MultivaluedHashMap<String, String>();
    headers.put(ArtifactServiceConfig.HEADER, keys);
    when(request.getHeaders()).thenReturn(headers);
    return request;
  }
}
