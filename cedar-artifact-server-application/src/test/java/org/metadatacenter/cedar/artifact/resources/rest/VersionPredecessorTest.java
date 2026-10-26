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

  /** Each malformed or unconditional projection is refused, and the template is left as it was. */
  @Test void aProjectionItCannotApplyExactlyIsRefusedAndChangesNothing() throws Exception {
    String id, etag;
    try (var response=testClient.target(baseTestUrl+"/templates").request()
        .header("Authorization",authHeaderTestUser1).post(Entity.json(getFileContentAsString(MINIMAL_TEMPLATE_NULL_ID)))) {
      assertEquals(201,response.getStatus());
      id=JsonMapper.STRICT_MAPPER.readTree(response.readEntity(String.class)).path("@id").asText();
      etag=response.getHeaderString("ETag");
      createdResources.put(id,CedarResourceType.TEMPLATE);
    }
    String url=getUrlWithId(baseTestUrl,CedarResourceType.TEMPLATE,id)+"/version-predecessor";
    String admin="apiKey "+TestUtil.getCedarConfig().getAdminUserConfig().getApiKey();
    String valid="{\"previousVersion\":\"https://example.org/predecessor\"}";
    var stored=TestUtil.templateService.findTemplate(id);
    for (String patch : new String[]{"", "not json", "null", "[\"https://example.org/predecessor\"]",
        "{\"predecessor\":\"https://example.org/predecessor\"}", "{\"previousVersion\":7}",
        "{\"previousVersion\":\"not an absolute url\"}"}) {
      try (var response=testClient.target(url).request()
          .header("Authorization",admin).header("If-Match",etag).put(Entity.json(patch))) {
        assertEquals(400,response.getStatus(),"patch <"+patch+">: "+response.readEntity(String.class));
      }
    }
    try (var response=testClient.target(url).request()
        .header("Authorization",admin).header("If-Match","*").put(Entity.json(valid))) {
      assertEquals(400,response.getStatus(),"a projection must name the revision it changes");
    }
    assertEquals(stored,TestUtil.templateService.findTemplate(id));
    String absent=getUrlWithId(baseTestUrl,CedarResourceType.TEMPLATE,
        linkedDataUtil.buildNewLinkedDataId(CedarResourceType.TEMPLATE))+"/version-predecessor";
    try (var response=testClient.target(absent).request()
        .header("Authorization",admin).header("If-Match",etag).put(Entity.json(valid))) {
      assertEquals(404,response.getStatus());
    }
  }
}
