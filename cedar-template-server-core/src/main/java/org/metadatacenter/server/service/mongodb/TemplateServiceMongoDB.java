package org.metadatacenter.server.service.mongodb;

import checkers.nullness.quals.NonNull;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.metadatacenter.server.TemplateServerNames;
import org.metadatacenter.server.dao.mongodb.TemplateDaoMongoDB;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.server.utils.JsonUtils;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class TemplateServiceMongoDB extends GenericTemplateServiceMongoDB<String, JsonNode> implements TemplateService<String, JsonNode> {

  private final @NonNull TemplateDaoMongoDB templateDao;

  @NonNull
  private final TemplateElementService templateElementService;


  public TemplateServiceMongoDB(@NonNull String db, @NonNull String templatesCollection, TemplateElementService templateElementService) {
    templateDao = new TemplateDaoMongoDB(db, templatesCollection);
    this.templateElementService = templateElementService;
  }

  @NonNull
  public JsonNode createTemplate(@NonNull JsonNode template) throws IOException {
    return templateDao.create(template);
  }

  @NonNull
  public JsonNode createTemplateLinkedData(@NonNull JsonNode template) throws IOException {
    return templateDao.createLinkedData(template);
  }

  @Override
  @NonNull
  public List<JsonNode> findAllTemplates() throws IOException {
    return templateDao.findAll();
  }

  @Override
  @NonNull
  public List<JsonNode> findAllTemplates(List<String> fieldNames, FieldNameInEx includeExclude) throws IOException {
    return templateDao.findAll(fieldNames, includeExclude);
  }

  @Override
  @NonNull
  public List<JsonNode> findAllTemplates(Integer count, Integer page, List<String> fieldNames, FieldNameInEx includeExclude) throws IOException {
    return templateDao.findAll(count, page, fieldNames, includeExclude);
  }

  public JsonNode findTemplate(@NonNull String templateId, boolean expanded, boolean validation)
      throws IOException, ProcessingException {
    JsonNode template = templateDao.find(templateId);
    if (template == null) {
      return null;
    }
    if (expanded) {
      template = expand(template);
    }
    if (validation) {
      validate(JsonLoader.fromURL(new URL(TemplateServerNames.JSON_SCHEMA_URL)), template);
    }
    return template;
  }

  public JsonNode findTemplateByLinkedDataId(@NonNull String templateId, boolean expanded, boolean validation)
      throws IOException, ProcessingException {
    JsonNode template = templateDao.findByLinkedDataId(templateId);
    if (template == null) {
      return null;
    }
    if (expanded) {
      template = expand(template);
    }
    if (validation) {
      validate(JsonLoader.fromURL(new URL(TemplateServerNames.JSON_SCHEMA_URL)), template);
    }
    return template;
  }

  @NonNull
  public JsonNode updateTemplate(@NonNull String templateId, @NonNull JsonNode modifications)
      throws InstanceNotFoundException, IOException {
    return templateDao.update(templateId, modifications);
  }

  @NonNull
  public JsonNode updateTemplateByLinkedDataId(@NonNull String templateId, @NonNull JsonNode modifications)
      throws InstanceNotFoundException, IOException {
    return templateDao.updateByLinkedDataId(templateId, modifications);
  }

  public void deleteTemplate(@NonNull String templateId) throws InstanceNotFoundException, IOException {
    templateDao.delete(templateId);
  }

  public void deleteTemplateByLinkedDataId(@NonNull String templateId) throws InstanceNotFoundException, IOException {
    templateDao.deleteByLinkedDataId(templateId);
  }

  public boolean existsTemplate(@NonNull String templateId) throws IOException {
    return templateDao.exists(templateId);
  }

  public boolean existsTemplateByLinkedDataId(@NonNull String templateId) throws IOException {
    return templateDao.existsByLinkedDataId(templateId);
  }

  public void deleteAllTemplates() {
    templateDao.deleteAll();
  }

  @Override
  public long count() {
    return templateDao.count();
  }

  // Expands a Template or a Template Element (JSON Schema). All the template elements referenced using $ref are
  // retrieved and embedded into the JSON Schema
  @NonNull
  public JsonNode expand(@NonNull JsonNode schema) throws IOException, ProcessingException {
    JsonUtils jsonUtils = new JsonUtils();
    return jsonUtils.resolveTemplateElementRefs(schema, templateElementService);
  }

}
