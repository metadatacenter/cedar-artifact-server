package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.metadatacenter.artifacts.model.core.TemplateSchemaArtifact;
import org.metadatacenter.artifacts.model.renderer.JsonArtifactRenderer;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.server.dao.ArtifactRevisionConflictException;
import org.metadatacenter.server.dao.mongodb.GenericLDDaoMongoDB;
import org.metadatacenter.util.json.JsonMapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** A saved validator must identify an incarnation, including across delete/recreate. */
public class ArtifactRecreationTest extends BaseServerTest {
  private record Artifact(String url, ObjectNode body, String etag) {}

  private Artifact create(String name) {
    String id = TestUtil.getCedarConfig().getLinkedDataUtil().buildNewLinkedDataId(CedarResourceType.TEMPLATE);
    String url = "http://localhost:" + getPortNumber() + "/templates/" + encodeUrl(id);
    ObjectNode body = new JsonArtifactRenderer().renderTemplateSchemaArtifact(
        TemplateSchemaArtifact.builder().withName(name).withJsonLdId(URI.create(id)).build());
    try (Response created = testClient.target(url).request().header("Authorization", authHeaderValue)
        .put(Entity.json(body))) {
      assertEquals(201, created.getStatus(), created.readEntity(String.class));
      return new Artifact(url, body, currentEtag(url, authHeaderValue));
    }
  }

  private Artifact recreate(Artifact original) {
    try (Response deleted = testClient.target(original.url()).request().header("Authorization", authHeaderValue)
        .header("If-Match", original.etag()).delete()) {
      assertEquals(204, deleted.getStatus());
    }
    ObjectNode replacement = original.body().deepCopy();
    replacement.put("schema:name", "Replacement incarnation");
    try (Response created = testClient.target(original.url()).request().header("Authorization", authHeaderValue)
        .put(Entity.json(replacement))) {
      assertEquals(201, created.getStatus(), created.readEntity(String.class));
    }
    return new Artifact(original.url(), replacement, currentEtag(original.url(), authHeaderValue));
  }

  @Test
  void staleDeleteCannotDeleteARecreatedArtifact() {
    Artifact original = create("Old delete target");
    Artifact replacement = recreate(original);
    try (Response stale = testClient.target(original.url()).request().header("Authorization", authHeaderValue)
        .header("If-Match", original.etag()).delete()) {
      assertEquals(412, stale.getStatus(), "a validator from the deleted incarnation must not delete its replacement; old="
          + original.etag() + ", replacement=" + replacement.etag());
    }
  }

  @Test
  void staleUpdateCannotOverwriteARecreatedArtifact() {
    Artifact original = create("Old editor contents");
    Artifact replacement = recreate(original);
    try (Response stale = testClient.target(original.url()).request().header("Authorization", authHeaderValue)
        .header("If-Match", original.etag()).put(Entity.json(original.body()))) {
      assertEquals(412, stale.getStatus(), "an old editor must not overwrite a replacement; old="
          + original.etag() + ", replacement=" + replacement.etag());
    }
  }

  @Test
  void bulkDeletionPreservesIndexesAndRevisionChecksForCompetingRecreations() throws Exception {
    // Reuse this fixture's embedded Mongo process, but isolate maintenance from its HTTP artifacts.
    String database = "bulk-recreation-regression";
    try (var client = MongoClients.create("mongodb://"
        + CedarEnvironmentSource.get("CEDAR_MONGO_HOST") + ":"
        + CedarEnvironmentSource.get("CEDAR_MONGO_PORT"))) {
      var db = client.getDatabase(database);
      try {
        var collection = db.getCollection("artifacts");
        collection.createIndex(Indexes.ascending("@id"),
            new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending("schema:name"));
        var indexes = collection.listIndexes().into(new ArrayList<Document>());
        var dao = new GenericLDDaoMongoDB(client, database, "artifacts");
        ObjectNode original = JsonMapper.STRICT_MAPPER.createObjectNode()
            .put("@id", "same-id").put("schema:name", "original");
        var receipt = dao.createWithRevision(original);
        dao.update("same-id", original.deepCopy().put("schema:name", "edited"), receipt.revision());
        long oldRevision = dao.findWithRevision("same-id").revision();
        dao.deleteAll();
        assertEquals(0, dao.count());
        assertEquals(indexes, collection.listIndexes().into(new ArrayList<>()),
            "bulk deletion must preserve uniqueness and secondary indexes");

        var executor = Executors.newFixedThreadPool(2);
        var ready = new CyclicBarrier(2);
        try {
          var results = new ArrayList<Future<Boolean>>();
          for (int writer = 0; writer < 2; writer++) {
            ObjectNode body = original.deepCopy().put("schema:name", "replacement-" + writer);
            results.add(executor.submit(() -> {
              ready.await(10, TimeUnit.SECONDS);
              var contender = new GenericLDDaoMongoDB(client, database, "artifacts");
              try {
                contender.createWithRevision(body);
                return true;
              } catch (ArtifactRevisionConflictException expected) {
                return false;
              }
            }));
          }
          int successes = 0;
          for (var result : results) if (result.get(15, TimeUnit.SECONDS)) successes++;
          assertEquals(1, successes, "only one concurrent recreation may win");
        } finally {
          executor.shutdownNow();
          assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals(1, dao.count());
        var replacement = dao.findWithRevision("same-id");
        assertTrue(replacement.revision() > oldRevision);
        assertThrows(ArtifactRevisionConflictException.class,
            () -> dao.update("same-id", original, oldRevision));
        assertThrows(ArtifactRevisionConflictException.class,
            () -> dao.delete("same-id", oldRevision));
        assertEquals(replacement, dao.findWithRevision("same-id"));
      } finally {
        db.drop();
      }
    }
  }

  @Test
  void ordinaryUpdateStillRejectsThePreviousValidator() {
    Artifact original = create("Ordinary revision control");
    ObjectNode edited = original.body().deepCopy();
    edited.put("schema:name", "Edited once");
    try (Response updated = testClient.target(original.url()).request().header("Authorization", authHeaderValue)
        .header("If-Match", original.etag()).put(Entity.json(edited))) {
      assertEquals(200, updated.getStatus(), updated.readEntity(String.class));
    }
    try (Response stale = testClient.target(original.url()).request().header("Authorization", authHeaderValue)
        .header("If-Match", original.etag()).put(Entity.json(original.body()))) {
      assertEquals(412, stale.getStatus());
    }
  }
}
