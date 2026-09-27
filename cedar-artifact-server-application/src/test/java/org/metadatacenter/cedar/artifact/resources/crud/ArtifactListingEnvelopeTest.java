package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.model.CedarResourceType;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The artifact listings answered in CEDAR's body paging envelope: the page's artifacts beside the
 * whole collection's count and links to the neighbouring pages, with nothing in headers.
 */
public class ArtifactListingEnvelopeTest extends AbstractResourceCrudTest {

  private static final int CREATED = 7;
  private final List<String> createdIds = new ArrayList<>();

  @BeforeEach
  public void createTemplates() {
    for (int i = 0; i < CREATED; i++) {
      JsonNode created = createResource(sampleTemplate, CedarResourceType.TEMPLATE);
      String id = created.get(LinkedData.ID).asText();
      createdResources.put(id, CedarResourceType.TEMPLATE);
      createdIds.add(id);
    }
  }

  private static String templates(String query) {
    return TestUtil.getResourceUrlRoute(baseTestUrl, CedarResourceType.TEMPLATE) + query;
  }

  private static JsonNode get(String url) {
    Response response = testClient.target(url).request().header("Authorization", authHeader).get();
    Assertions.assertEquals(200, response.getStatus(), url);
    Assertions.assertNull(response.getHeaderString("Total-Count"), "the count is in the body, not a header");
    Assertions.assertNull(response.getHeaderString("Link"), "the links are in the body, not a header");
    return response.readEntity(JsonNode.class);
  }

  private static int status(String url) {
    return testClient.target(url).request().header("Authorization", authHeader).get().getStatus();
  }

  @Test
  public void aPageCarriesItsArtifactsBesideTheWholeCount() {
    int total = countResources(CedarResourceType.TEMPLATE);

    JsonNode page = get(templates("?limit=3&offset=3"));

    Assertions.assertEquals(3, page.get("artifacts").size());
    Assertions.assertEquals(total, page.get("totalCount").asInt());
    Assertions.assertEquals(3, page.get("currentOffset").asLong());
    Assertions.assertEquals(3, page.get("request").get("limit").asInt());
    Assertions.assertEquals(3, page.get("request").get("offset").asInt());
    Assertions.assertEquals("6", param(page.get("paging").get("next").asText(), "offset"));
    Assertions.assertEquals("0", param(page.get("paging").get("prev").asText(), "offset"));
    Assertions.assertTrue(page.get("paging").has("last"));
  }

  @Test
  public void followingNextVisitsEveryTemplateOnce() {
    int total = countResources(CedarResourceType.TEMPLATE);

    Set<String> seen = new HashSet<>();
    String next = templates("?limit=2&summary=true");
    int pages = 0;
    while (next != null) {
      JsonNode page = get(next);
      page.get("artifacts").forEach(a -> Assertions.assertTrue(seen.add(a.get(LinkedData.ID).asText()),
          "seen twice: " + a.get(LinkedData.ID).asText()));
      next = page.get("paging").has("next") ? page.get("paging").get("next").asText() : null;
      Assertions.assertTrue(++pages < 1_000, "the walk did not end");
    }

    Assertions.assertEquals(total, seen.size());
    Assertions.assertTrue(seen.containsAll(createdIds));
  }

  @Test
  public void theLinksKeepTheRequestsOwnParameters() {
    JsonNode page = get(templates("?limit=2&summary=true"));

    String next = page.get("paging").get("next").asText();
    Assertions.assertEquals("true", param(next, "summary"));
    Assertions.assertEquals("2", param(next, "offset"));
    Assertions.assertEquals("2", param(next, "limit"));
  }

  @Test
  public void anOffsetPastTheEndIsAnEmptyPageRatherThanARefusal() {
    int total = countResources(CedarResourceType.TEMPLATE);

    JsonNode page = get(templates("?limit=5&offset=" + (total + 10)));

    Assertions.assertEquals(0, page.get("artifacts").size());
    Assertions.assertEquals(total, page.get("totalCount").asInt());
    Assertions.assertFalse(page.get("paging").has("next"));
  }

  @Test
  public void anOutOfRangeLimitOrOffsetIsRefused() {
    Assertions.assertEquals(400, status(templates("?limit=0")));
    Assertions.assertEquals(400, status(templates("?limit=501")));
    Assertions.assertEquals(400, status(templates("?offset=-1")));
  }

  @Test
  public void everyArtifactTypeAnswersTheEnvelope() {
    for (CedarResourceType type : List.of(CedarResourceType.TEMPLATE, CedarResourceType.ELEMENT,
        CedarResourceType.FIELD, CedarResourceType.INSTANCE)) {
      JsonNode page = get(TestUtil.getResourceUrlRoute(baseTestUrl, type) + "?limit=1");
      Assertions.assertTrue(page.get("artifacts").isArray(), type + " answered no artifacts array");
      Assertions.assertTrue(page.has("totalCount"), type + " answered no count");
      Assertions.assertTrue(page.has("paging"), type + " answered no links");
    }
  }

  private static String param(String link, String name) {
    String raw = URI.create(link).getRawQuery();
    if (raw == null) {
      return null;
    }
    for (String pair : raw.split("&")) {
      String[] kv = pair.split("=", 2);
      if (kv[0].equals(name)) {
        return URLDecoder.decode(kv.length > 1 ? kv[1] : "", StandardCharsets.UTF_8);
      }
    }
    return null;
  }
}
