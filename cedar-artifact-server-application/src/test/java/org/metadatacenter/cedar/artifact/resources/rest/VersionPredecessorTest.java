package org.metadatacenter.cedar.artifact.resources.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class VersionPredecessorTest extends AbstractRestTest {
  @Test void maintenanceIsNarrowConditionalAndAdministratorOnly() throws Exception {
    String id, etag;
    ObjectNode before;
    try (var response=testClient.target(baseTestUrl+"/templates").request()
        .header("Authorization",authHeaderTestUser1).post(Entity.json(getFileContentAsString(MINIMAL_TEMPLATE_NULL_ID)))) {
      assertEquals(201,response.getStatus());
      before=(ObjectNode)JsonMapper.STRICT_MAPPER.readTree(response.readEntity(String.class));
      id=before.path("@id").asText(); etag=response.getHeaderString("ETag");
      createdResources.put(id,CedarResourceType.TEMPLATE);
    }
    String url=getUrlWithId(baseTestUrl,CedarResourceType.TEMPLATE,id);
    String admin="apiKey "+TestUtil.getCedarConfig().getAdminUserConfig().getApiKey();
    String patch="{\"previousVersion\":\"https://example.org/predecessor\"}";
    try (var response=testClient.target(url+"/version-predecessor").request()
        .header("Authorization",authHeaderTestUser1).header("If-Match",etag).put(Entity.json(patch))) {
      assertEquals(403,response.getStatus());
    }
    try (var response=testClient.target(url+"/version-predecessor").request()
        .header("Authorization",admin).header("If-Match",etag).put(Entity.json("{\"previousVersion\":null,\"bibo:status\":\"published\"}"))) {
      assertEquals(400,response.getStatus());
    }
    try (var response=testClient.target(url+"/version-predecessor").request()
        .header("Authorization",admin).header("If-Match",etag).put(Entity.json(patch))) {
      assertEquals(200,response.getStatus(),response.readEntity(String.class));
    }
    try (var response=testClient.target(url+"/version-predecessor").request()
        .header("Authorization",admin).header("If-Match",etag).put(Entity.json("{\"previousVersion\":null}"))) {
      assertEquals(412,response.getStatus());
    }
    String newEtag;
    try (var response=testClient.target(url).request().header("Authorization",authHeaderTestUser1).get()) {
      var after=(ObjectNode)JsonMapper.STRICT_MAPPER.readTree(response.readEntity(String.class));
      assertEquals("https://example.org/predecessor",after.remove("pav:previousVersion").asText());
      before.remove("pav:previousVersion"); assertEquals(before,after);
      newEtag=response.getHeaderString("ETag");
    }
    try (var response=testClient.target(url+"/version-predecessor").request()
        .header("Authorization",admin).header("If-Match",newEtag).put(Entity.json("{\"previousVersion\":null}"))) {
      assertEquals(200,response.getStatus());
    }
    try (var response=testClient.target(url).request().header("Authorization",authHeaderTestUser1).get()) {
      assertEquals(before,JsonMapper.STRICT_MAPPER.readTree(response.readEntity(String.class)));
    }
  }
}
