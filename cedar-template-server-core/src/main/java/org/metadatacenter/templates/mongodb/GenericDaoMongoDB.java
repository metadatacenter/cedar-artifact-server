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
public class GenericDaoMongoDB implements GenericDao<String, JsonNode>
{
  @NonNull protected final MongoCollection<Document> entityCollection;
  @NonNull private final JsonUtils jsonUtils;

  public GenericDaoMongoDB(@NonNull String dbName, @NonNull String collectionName)
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

  /**
   * Find an element using its ID
   *
   * @param id The ID of the element
   * @return A JSON representation of the element or null if the element was not found
   * @throws IllegalArgumentException  If the ID is not valid
   * @throws IOException               If an error occurs during retrieval
   */
  public JsonNode find(@NonNull String id) throws IOException
  {
    if (!ObjectId.isValid(id)) {
      throw new IllegalArgumentException();
    }
    Document doc = entityCollection.find(eq("_id", new ObjectId(id))).first();
    if (doc == null)
      return null;
    ObjectMapper mapper = new ObjectMapper();
    return jsonUtils.fixMongoDB(mapper.readTree(doc.toJson()), 2);
  }

  /**
   * Find an element using its linked data ID  (@id in JSON-LD)
   *
   * @param id The linked data ID of the element
   * @return A JSON representation of the element or null if the element was not found
   * @throws IllegalArgumentException  If the ID is not valid
   * @throws IOException               If an error occurs during retrieval
   */
  public JsonNode findByLinkedDataId(@NonNull String id) throws IOException
  {
    if ((id == null) || (id.length() == 0)) {
      throw new IllegalArgumentException();
    }
    Document doc = entityCollection.find(eq("@id", id)).first();
    if (doc == null)
      return null;
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
   * Update an element using its linked data ID  (@id in JSON-LD)
   *
   * @param id            The linked data ID of the element to update
   * @param modifications The update
   * @return The updated JSON representation of the element
   * @throws IllegalArgumentException  If the ID is not valid
   * @throws InstanceNotFoundException If the element is not found
   * @throws IOException               If an error occurs during update
   */
  @NonNull public JsonNode updateByLinkedDataId(@NonNull String id, @NonNull JsonNode modifications)
    throws InstanceNotFoundException, IOException
  {
    if ((id == null) || (id.length() == 0)) {
      throw new IllegalArgumentException();
    }
    if (!existsByLinkedDataId(id)) {
      throw new InstanceNotFoundException();
    }
    // Adapts all keys not accepted by MongoDB
    modifications = jsonUtils.fixMongoDB(modifications, 1);
    ObjectMapper mapper = new ObjectMapper();
    Map modificationsMap = mapper.convertValue(modifications, Map.class);
    UpdateResult updateResult = entityCollection
      .updateOne(eq("@id", id), new Document("$set", modificationsMap));
    if (updateResult.getModifiedCount() == 1) {
      return findByLinkedDataId(id);
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
   * Delete an element using its linked data ID  (@id in JSON-LD)
   *
   * @param id The linked data ID of the element to delete
   * @throws IllegalArgumentException  If the ID is not valid
   * @throws InstanceNotFoundException If the element is not found
   * @throws IOException               If an error occurs during deletion
   */
  public void deleteByLinkedDataId(@NonNull String id) throws InstanceNotFoundException, IOException
  {
    if ((id == null) || (id.length() == 0)) {
      throw new IllegalArgumentException();
    }
    if (!existsByLinkedDataId(id)) {
      throw new InstanceNotFoundException();
    }
    DeleteResult deleteResult = entityCollection.deleteOne(eq("@id", id));
    if (deleteResult.getDeletedCount() != 1)
      throw new InternalError();
  }

  /**
   * Check if an element exists using its ID
   *
   * @param id The ID of the element
   * @return True if an element with the supplied ID exists or False otherwise
   * @throws IOException If an error occurs during the existence check
   */
  public boolean exists(@NonNull String id) throws IOException
  {
    if (find(id)!=null)
      return true;
    else
      return false;
  }

  /**
   * Check if an element exists using its linked data ID
   *
   * @param id The linked data ID of the element
   * @return True if an element with the supplied linked data ID exists or False otherwise
   * @throws IOException If an error occurs during the existence check
   */
  public boolean existsByLinkedDataId(@NonNull String id) throws IOException
  {
    if (findByLinkedDataId(id)!=null)
      return true;
    else
      return false;
  }

  /**
   * Delete all elements
   */
  public void deleteAll()
  {
    entityCollection.drop();
  }
}
