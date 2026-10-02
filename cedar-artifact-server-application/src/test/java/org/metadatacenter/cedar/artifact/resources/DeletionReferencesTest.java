package org.metadatacenter.cedar.artifact.resources;

import jakarta.ws.rs.client.Entity;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.util.json.JsonMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DeletionReferencesTest extends org.metadatacenter.cedar.artifact.resources.rest.AbstractRestTest {
  @Test void inventoriesContentStoreReferencesWithoutWorkspaceVisibilityFiltering() throws Exception {
    String template = "https://example.org/template/" + UUID.randomUUID();
    String other = "https://example.org/template/" + UUID.randomUUID();
    List<String> ids = List.of("https://example.org/instance/" + UUID.randomUUID(), "https://example.org/instance/" + UUID.randomUUID());
    try {
      for (String id : ids) TestUtil.templateInstanceService.createTemplateInstance(JsonMapper.STRICT_MAPPER.valueToTree(
          Map.of("@id", id, "schema:isBasedOn", template)));
      try (var response = testClient.target(baseTestUrl + "/templates/deletion-references").request()
          .header("Authorization", authHeaderTestUser1).post(Entity.json(List.of(template, other)))) {
        String body = response.readEntity(String.class);
        assertEquals(200, response.getStatus(), body);
        var json = JsonMapper.STRICT_MAPPER.readTree(body);
        assertEquals(2, json.get(template).size());
        assertEquals(0, json.get(other).size());
        assertTrue(json.get(template).toString().contains(ids.get(0)));
        assertTrue(json.get(template).toString().contains(ids.get(1)));
      }
    } finally {
      for (String id : ids) TestUtil.templateInstanceService.deleteTemplateInstance(id);
    }
  }
}
