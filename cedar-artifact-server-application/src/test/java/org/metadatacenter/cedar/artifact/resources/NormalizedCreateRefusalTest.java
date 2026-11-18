package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.model.Filters;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.artifact.resources.rest.AbstractRestTest;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.config.MongoConfig;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.error.CedarErrorReasonKey;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.model.validation.report.CedarValidationReport;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.util.ModelUtil;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A create validates the document it receives, then mints the identifiers of the children that arrived
 * without one, then validates again before storing. No valid request fails the second validation while
 * the minting is correct, so it is reached here by substituting a minting step that leaves the document
 * invalid. The create must then be refused as invalid data, saying the server's own normalization was at
 * fault, and nothing may be stored.
 */
public class NormalizedCreateRefusalTest extends AbstractRestTest {

  @ParameterizedTest(name = "a {0} create")
  @ValueSource(strings = {"template", "element"})
  public void aCreateWhoseMintingLeavesTheDocumentInvalidIsRefusedAndNothingIsStored(String kindName)
      throws IOException {
    CedarResourceType kind = CedarResourceType.forValue(kindName);
    AbstractArtifactCrudResource resource = resource(kind);
    BiFunction<JsonNode, ProvenanceInfo, List<ModelUtil.MintedChildId>> minter = resource.createdChildIdMinter;

    String control = "Normalized Create Control " + UUID.randomUUID();
    Response stored = post(kind, control);
    String storedBody = stored.readEntity(String.class);
    assertEquals(201, stored.getStatus(), "the request is valid with the real minting step: " + storedBody);
    createdResources.put(JsonMapper.STRICT_MAPPER.readTree(storedBody).path(LinkedData.ID).asText(), kind);

    String name = "Normalized Create Refusal " + UUID.randomUUID();
    Response response;
    resource.createdChildIdMinter = (artifact, pi) -> {
      List<ModelUtil.MintedChildId> minted = minter.apply(artifact, pi);
      ((ObjectNode) artifact).remove("@context");
      return minted;
    };
    try {
      response = post(kind, name);
    } finally {
      resource.createdChildIdMinter = minter;
    }
    String body = response.readEntity(String.class);

    assertEquals(400, response.getStatus(), body);
    assertEquals(CedarValidationReport.IS_INVALID,
        response.getHeaderString(CustomHttpConstants.HEADER_CEDAR_VALIDATION_STATUS));
    JsonNode error = JsonMapper.STRICT_MAPPER.readTree(body);
    assertEquals(CedarErrorKey.INVALID_DATA.getValue(), error.path("errorKey").asText(), body);
    assertEquals(CedarErrorReasonKey.VALIDATION_ERROR.getValue(), error.path("errorReasonKey").asText(), body);
    assertTrue(error.path("message").asText().startsWith("Server normalization produced an invalid "), body);
    assertNotNull(storedIdByName(kind, control), "the control create was not stored");
    assertNull(storedIdByName(kind, name), "a create refused after normalization stored an artifact");
  }

  private static Response post(CedarResourceType kind, String name) {
    return testClient.target(baseTestUrl + "/" + kind.getPrefix()).request()
        .header(AUTHORIZATION, authHeaderTestUser1)
        .post(Entity.entity("type: " + kind.getValue() + "\nname: " + name + "\n", "application/yaml"));
  }

  /** The application's own resource for the kind, as Jersey serves it. */
  private static AbstractArtifactCrudResource resource(CedarResourceType kind) {
    Class<? extends AbstractArtifactCrudResource> type =
        kind == CedarResourceType.TEMPLATE ? TemplatesResource.class : TemplateElementsResource.class;
    ServletContainer jersey = (ServletContainer) SERVER_APPLICATION.getEnvironment().getJerseyServletContainer();
    return jersey.getApplicationHandler().getInjectionManager().getInstance(type);
  }

  /** The identifier of the stored artifact with this name, or null when none is stored. */
  private static String storedIdByName(CedarResourceType kind, String name) {
    MongoConfig store = TestUtil.getCedarConfig().getArtifactServerConfig();
    org.bson.Document found = CedarDataServices.getInstance().getMongoClientFactoryForDocuments().getClient()
        .getDatabase(store.getDatabaseName())
        .getCollection(store.getMongoCollectionName(kind))
        .find(Filters.eq("schema:name", name))
        .first();
    return found == null ? null : found.getString(LinkedData.ID);
  }
}
