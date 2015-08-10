package org.metadatacenter.templates.mongodb;

import checkers.nullness.quals.NonNull;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.metadatacenter.templates.dao.GenericDao;
import org.metadatacenter.templates.utils.JsonUtils;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

/**
 * Service to manage elements in a MongoDB database
 */
public class MongoDBDao implements GenericDao<String, JsonNode>
{
  @NonNull protected final MongoCollection<Document> entityCollection;
  @NonNull private final JsonUtils jsonUtils;

  public MongoDBDao(@NonNull String dbName, @NonNull String collectionName)
  {
    MongoClient mongoClient = MongoFactory.getClient();
    entityCollection = mongoClient.getDatabase(dbName).getCollection(collectionName);
    jsonUtils = new JsonUtils();
    // TODO: close mongoClient after using it
  }

  /* CRUD operations */

  /**
   * Create an element
   *
   * @param element An element
   * @return The created element
   * @throws IOException If an occurs during creation
   */
  @NonNull public JsonNode create(@NonNull JsonNode element) throws IOException
  {
    // Adapts all keys not accepted by MongoDB
    JsonNode fixedElement = jsonUtils.fixMongoDB(element, 1);
    ObjectMapper mapper = new ObjectMapper();
    Map elementMap = mapper.convertValue(fixedElement, Map.class);
    Document elementDoc = new Document(elementMap);
    entityCollection.insertOne(elementDoc);
    // Returns the document created (all keys adapted for MongoDB are restored)
    return jsonUtils.fixMongoDB(mapper.readTree(elementDoc.toJson()), 2);
  }

  /**
   * Find all elements
   *
   * @return A list of elements
   * @throws IOException If an error occurs during retrieval
   */
  @NonNull public List<JsonNode> findAll() throws IOException
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

  /**
   * Find an element using its ID
   *
   * @param id The ID of the element
   * @return A JSON representation of the element
   * @throws IllegalArgumentException  If the ID is not valid
   * @throws InstanceNotFoundException If the element is not found
   * @throws IOException               If an error occurs during retrieval
   */
  @NonNull public JsonNode find(@NonNull String id) throws InstanceNotFoundException, IOException
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

  /**
   * Find an element using its liked data ID  (@id in JSON-LD)
   *
   * @param id The linked data ID of the element
   * @return A JSON representation of the element
   * @throws IllegalArgumentException  If the ID is not valid
   * @throws InstanceNotFoundException If the element is not found
   * @throws IOException               If an error occurs during retrieval
   */
  @NonNull public JsonNode findByLinkedDataId(@NonNull String id) throws InstanceNotFoundException, IOException
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

  /**
   * Update an element
   *
   * @param id            The ID of the element to update
   * @param modifications The update
   * @return The updated JSON representation of the element
   * @throws IllegalArgumentException  If the ID is not valid
   * @throws InstanceNotFoundException If the element is not found
   * @throws IOException               If an error occurs during update
   */
  @NonNull public JsonNode update(@NonNull String id, @NonNull JsonNode modifications)
    throws InstanceNotFoundException, IOException
  {
    if (!ObjectId.isValid(id)) {
      throw new IllegalArgumentException();
    }
    if (!exists(id)) {
      throw new InstanceNotFoundException();
    }
    // Adapts all keys not accepted by MongoDB
    modifications = jsonUtils.fixMongoDB(modifications, 1);
    ObjectMapper mapper = new ObjectMapper();
    Map modificationsMap = mapper.convertValue(modifications, Map.class);
    UpdateResult updateResult = entityCollection
      .updateOne(eq("_id", new ObjectId(id)), new Document("$set", modificationsMap));
    if (updateResult.getModifiedCount() == 1) {
      return find(id);
    } else
      throw new InternalError();
  }

  /**
   * Delete an element
   *
   * @param id The ID of the element to delete
   * @throws IllegalArgumentException  If the ID is not valid
   * @throws InstanceNotFoundException If the element is not found
   * @throws IOException               If an error occurs during deletion
   */
  public void delete(@NonNull String id) throws InstanceNotFoundException, IOException
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

  /**
   * Does an element exist
   *
   * @param id The ID of the element
   * @return True if an element with the supplied ID exists
   * @throws IOException If an error occurs during the existence check
   */
  public boolean exists(@NonNull String id) throws IOException
  {
    try {
      find(id);
    } catch (InstanceNotFoundException e) {
      return false;
    }
    return true;
  }

  /**
   * Delete all elements
   */
  public void deleteAll()
  {
    entityCollection.drop();
  }
}
