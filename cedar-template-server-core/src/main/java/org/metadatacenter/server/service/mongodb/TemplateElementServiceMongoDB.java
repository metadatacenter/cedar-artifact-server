package org.metadatacenter.server.service.mongodb;

import checkers.nullness.quals.NonNull;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.metadatacenter.server.Constants;
import org.metadatacenter.server.dao.mongodb.TemplateElementDaoMongoDB;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.util.JsonUtils;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class TemplateElementServiceMongoDB extends GenericTemplateServiceMongoDB<String, JsonNode> implements
    TemplateElementService<String, JsonNode> {

  private final @NonNull TemplateElementDaoMongoDB templateElementDao;

  public TemplateElementServiceMongoDB(@NonNull String db, @NonNull String templateElementsCollection, String
      linkedDataIdBasePath) {
    templateElementDao = new TemplateElementDaoMongoDB(db, templateElementsCollection, linkedDataIdBasePath);
  }

  @NonNull
  public JsonNode createTemplateElement(@NonNull JsonNode templateElement) throws IOException {
    return templateElementDao.create(templateElement);
  }

  @NonNull
  public JsonNode createTemplateElementLinkedData(@NonNull JsonNode templateElement) throws IOException {
    return templateElementDao.createLinkedData(templateElement);
  }

  @Override
  @NonNull
  public List<JsonNode> findAllTemplateElements() throws IOException {
    return templateElementDao.findAll();
  }

  @Override
  @NonNull
  public List<JsonNode> findAllTemplateElements(List<String> fieldNames, FieldNameInEx includeExclude) throws
      IOException {
    return templateElementDao.findAll(fieldNames, includeExclude);
  }

  @Override
  @NonNull
  public List<JsonNode> findAllTemplateElements(Integer limit, Integer offset, List<String> fieldNames, FieldNameInEx
      includeExclude) throws IOException {
    return templateElementDao.findAll(limit, offset, fieldNames, includeExclude);
  }

  public JsonNode findTemplateElement(@NonNull String templateElementId, boolean expanded, boolean validation)
      throws IOException, ProcessingException {
    JsonNode templateElement = templateElementDao.find(templateElementId);
    if (templateElement == null) {
      return null;
    }
    if (expanded) {
      templateElement = expand(templateElement);
    }
    if (validation) {
      validate(JsonLoader.fromURL(new URL(Constants.JSON_SCHEMA_URL)), templateElement);
    }
    return templateElement;
  }

  public JsonNode findTemplateElementByLinkedDataId(@NonNull String templateElementId, boolean expanded,
                                                    boolean validation) throws IOException, ProcessingException {
    JsonNode templateElement = templateElementDao.findByLinkedDataId(templateElementId);
    if (templateElement == null) {
      return null;
    }
    if (expanded) {
      templateElement = expand(templateElement);
    }
    if (validation) {
      validate(JsonLoader.fromURL(new URL(Constants.JSON_SCHEMA_URL)), templateElement);
    }
    return templateElement;
  }

  @NonNull
  public JsonNode updateTemplateElement(@NonNull String templateElementId, @NonNull JsonNode modifications)
      throws InstanceNotFoundException, IOException {
    return templateElementDao.update(templateElementId, modifications);
  }

  @NonNull
  public JsonNode updateTemplateElementByLinkedDataId(@NonNull String templateElementId, @NonNull JsonNode
      modifications)
      throws InstanceNotFoundException, IOException {
    return templateElementDao.updateByLinkedDataId(templateElementId, modifications);
  }

  public void deleteTemplateElement(@NonNull String templateElementId) throws InstanceNotFoundException, IOException {
    templateElementDao.delete(templateElementId);
  }

  public void deleteTemplateElementByLinkedDataId(@NonNull String templateElementId) throws
      InstanceNotFoundException, IOException {
    templateElementDao.deleteByLinkedDataId(templateElementId);
  }

  public boolean existsTemplateElement(@NonNull String templateElementId) throws IOException {
    return templateElementDao.exists(templateElementId);
  }

  public boolean existsTemplateElementByLinkedDataId(@NonNull String templateElementId) throws IOException {
    return templateElementDao.existsByLinkedDataId(templateElementId);
  }

  public void deleteAllTemplateElements() {
    templateElementDao.deleteAll();
  }

  @Override
  public long count() {
    return templateElementDao.count();
  }

  // Expands a Template or a Template Element (JSON Schema). All the template elements referenced using $ref are
  // retrieved and embedded into the JSON Schema
  @NonNull
  public JsonNode expand(@NonNull JsonNode schema) throws IOException, ProcessingException {
    JsonUtils jsonUtils = new JsonUtils();
    return jsonUtils.resolveTemplateElementRefs(schema, this);
  }
}
