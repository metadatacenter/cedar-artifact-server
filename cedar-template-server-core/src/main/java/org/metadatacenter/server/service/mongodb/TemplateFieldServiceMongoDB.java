package org.metadatacenter.server.service.mongodb;

import checkers.nullness.quals.NonNull;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.metadatacenter.server.Constants;
import org.metadatacenter.server.dao.mongodb.TemplateFieldDaoMongoDB;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateFieldService;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TemplateFieldServiceMongoDB extends GenericTemplateServiceMongoDB<String, JsonNode> implements
    TemplateFieldService<String, JsonNode> {

  private final @NonNull TemplateFieldDaoMongoDB templateFieldDao;

  public TemplateFieldServiceMongoDB(@NonNull String db, @NonNull String templateFieldsCollection, String
      linkedDataIdBasePath) {
    templateFieldDao = new TemplateFieldDaoMongoDB(db, templateFieldsCollection, linkedDataIdBasePath);
  }

  @Override
  @NonNull
  public List<JsonNode> findAllTemplateFields(Integer limit, Integer offset, List<String> fieldNames, FieldNameInEx
      includeExclude) throws IOException {
    return templateFieldDao.findAll(limit, offset, fieldNames, includeExclude);
  }

  public JsonNode findTemplateFieldByLinkedDataId(@NonNull String templateFieldId,
                                                  boolean validation) throws IOException, ProcessingException {
    JsonNode templateField = templateFieldDao.findByLinkedDataId(templateFieldId);
    if (templateField == null) {
      return null;
    }
    if (validation) {
      validate(JsonLoader.fromURL(new URL(Constants.JSON_SCHEMA_URL)), templateField);
    }
    return templateField;
  }

  @Override
  public long count() {
    return templateFieldDao.count();
  }

  @Override
  public void saveNewFieldsAndReplaceIds(JsonNode genericInstance) throws IOException {

    JsonNode properties = genericInstance.get("properties");
    Iterator<Map.Entry<String, JsonNode>> it = properties.fields();
    while (it.hasNext()) {
      Map.Entry<String, JsonNode> entry = it.next();
      JsonNode fieldCandidate = entry.getValue();
      // If the entry is an object
      if (fieldCandidate.isObject()) {
        if (fieldCandidate.get("@id") != null) {
          String id = fieldCandidate.get("@id").asText();
          if (id != null && id.indexOf("@tmp-") == 0) {
            JsonNode removeId = ((ObjectNode) fieldCandidate).remove("@id");
            templateFieldDao.createLinkedData(fieldCandidate);
          }
        }
      }
    }
  }

}
