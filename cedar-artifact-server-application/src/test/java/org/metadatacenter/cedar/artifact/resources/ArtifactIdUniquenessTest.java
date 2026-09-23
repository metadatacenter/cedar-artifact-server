package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.config.MongoConfig;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.server.dao.ArtifactRevisionConflictException;
import org.metadatacenter.server.service.mongodb.TemplateServiceMongoDB;
import org.metadatacenter.util.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Proves the uniqueness of {@code @id} against the store rather than against a mock.
 *
 * <p>The DAO answers a duplicate-key rejection with the 412 an update gives a stale writer, but
 * only a store carrying the unique index ever produces that rejection. Its own test mocks the
 * rejection, so nothing exercised the path end to end: a collection without the index accepts both
 * inserts, after which a read returns the first document and a conditional delete can strand the
 * other. These tests use the running server's own Mongo client, so what they measure is the store
 * the suite runs against.
 */
public class ArtifactIdUniquenessTest extends BaseServerTest {

  private static final String ID = "https://repo.metadatacenter.orgx/templates/id-uniqueness-test";

  @Test
  public void everyArtifactCollectionCarriesTheUniqueIdIndex() {
    MongoConfig mongoConfig = TestUtil.getCedarConfig().getArtifactServerConfig();
    MongoClient client = CedarDataServices.getInstance().getMongoClientFactoryForDocuments().getClient();

    for (CedarResourceType resourceType : List.of(CedarResourceType.FIELD, CedarResourceType.ELEMENT,
        CedarResourceType.TEMPLATE, CedarResourceType.INSTANCE)) {
      String collectionName = mongoConfig.getMongoCollectionName(resourceType);
      List<Document> indexes = client.getDatabase(mongoConfig.getDatabaseName())
          .getCollection(collectionName).listIndexes().into(new ArrayList<>());

      boolean unique = indexes.stream().anyMatch(index -> {
        Document key = index.get("key", Document.class);
        return key != null && key.containsKey("@id") && index.getBoolean("unique", false);
      });

      Assertions.assertTrue(unique,
          collectionName + " carries no unique @id index, so the suite would accept a repeated identifier: "
              + indexes);
    }
  }

  @Test
  public void theStoreRefusesASecondDocumentWithTheSameId() throws Exception {
    MongoConfig mongoConfig = TestUtil.getCedarConfig().getArtifactServerConfig();
    TemplateServiceMongoDB templateService = new TemplateServiceMongoDB(
        CedarDataServices.getInstance().getMongoClientFactoryForDocuments().getClient(),
        mongoConfig.getDatabaseName(),
        mongoConfig.getMongoCollectionName(CedarResourceType.TEMPLATE));

    templateService.createTemplate(template(ID));

    ArtifactRevisionConflictException refusal = Assertions.assertThrows(
        ArtifactRevisionConflictException.class,
        () -> templateService.createTemplate(template(ID)));
    Assertions.assertTrue(refusal.getMessage().contains(ID), refusal.getMessage());

    templateService.deleteTemplate(ID);
  }

  private static JsonNode template(String id) {
    ObjectNode template = JsonMapper.MAPPER.createObjectNode();
    template.put("@id", id);
    template.put("schema:name", "id uniqueness");
    return template;
  }
}
