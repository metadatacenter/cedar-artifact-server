package org.metadatacenter.templates.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

// Service to query a MongoDB database
public class GenericDaoMongoDB implements GenericDao<String, JsonNode>
{

  private MongoClient mongoClient;
  // Accessible from subclasses
  protected MongoCollection<Document> entityCollection;
  private JsonUtils jsonUtils;

  public GenericDaoMongoDB(String dbName, String collectionName)
  {
    jsonUtils = new JsonUtils();
    // TODO: close mongoClient after using it
    mongoClient = MongoFactory.getClient();
    entityCollection = mongoClient.getDatabase(dbName).getCollection(collectionName);
  }

  /* CRUD operations */

  // Create
  public JsonNode create(JsonNode element) throws IOException
  {
    // Adapts all keys non accepted by MongoDB
    element = jsonUtils.fixMongoDB(element, 1);
    ObjectMapper mapper = new ObjectMapper();
    Map elementMap = mapper.convertValue(element, Map.class);
    Document elementDoc = new Document(elementMap);
    entityCollection.insertOne(elementDoc);
    // Returns the document created (all keys adapted for MongoDB are restored)
    return jsonUtils.fixMongoDB(mapper.readTree(elementDoc.toJson()), 2);
  }

  // Find All
  public List<JsonNode> findAll() throws IOException
  {
    ObjectMapper mapper = new ObjectMapper();
    MongoCursor<Document> cursor = entityCollection.find().iterator();
    List<JsonNode> docs = new ArrayList<JsonNode>();
    try {
      while (cursor.hasNext()) {
        JsonNode node = jsonUtils.fixMongoDB(mapper.readTree(cursor.next().toJson()), 2);
        docs.add(node);
      }
    } finally {
      cursor.close();
    }
    return docs;
  }

  // Find by Id
  public JsonNode find(String id) throws InstanceNotFoundException, IOException
  {
    if (!ObjectId.isValid(id)) {
      throw new IllegalArgumentException();
    }
    Document doc = entityCollection.find(eq("_id", new ObjectId(id))).first();
    if (doc == null)
      throw new InstanceNotFoundException();
    JsonNode node = null;
    ObjectMapper mapper = new ObjectMapper();
    return jsonUtils.fixMongoDB(mapper.readTree(doc.toJson()), 2);

  }

  // Find by Linked Data Id (@id in JSON-LD)
  public JsonNode findByLinkedDataId(String id) throws InstanceNotFoundException, IOException
  {
    if ((id == null) && (id.length() == 0)) {
      throw new IllegalArgumentException();
    }
    Document doc = entityCollection.find(eq("@id", id)).first();
    if (doc == null)
      throw new InstanceNotFoundException();
    JsonNode node = null;
    ObjectMapper mapper = new ObjectMapper();
    return jsonUtils.fixMongoDB(mapper.readTree(doc.toJson()), 2);
  }

  // Update
  public JsonNode update(String id, JsonNode modifications) throws InstanceNotFoundException, IOException
  {
    if (!ObjectId.isValid(id)) {
      throw new IllegalArgumentException();
    }
    if (!exists(id)) {
      throw new InstanceNotFoundException();
    }
    ObjectMapper mapper = new ObjectMapper();
    Map modificationsMap = mapper.convertValue(modifications, Map.class);
    UpdateResult updateResult = entityCollection
      .updateOne(eq("_id", new ObjectId(id)), new Document("$set", modificationsMap));
    if (updateResult.getModifiedCount() == 1) {
      return find(id);
    } else
      throw new InternalError();
  }

  // Delete
  public void delete(String id) throws InstanceNotFoundException, IOException
  {
    if (!ObjectId.isValid(id)) {
      throw new IllegalArgumentException();
    }
    if (!exists(id)) {
      throw new InstanceNotFoundException();
    }
    DeleteResult deleteResult = entityCollection.deleteOne(eq("_id", new ObjectId(id)));
    if (deleteResult.getDeletedCount() != 1)
      throw new InternalError();
  }

  // Exists
  public boolean exists(String id) throws IOException
  {
    try {
      find(id);
    } catch (InstanceNotFoundException e) {
      return false;
    }
    return true;
  }

  // Delete all
  public void deleteAll()
  {
    entityCollection.drop();
  }
}
