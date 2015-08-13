package org.metadatacenter.templates.mongodb;

import checkers.nullness.quals.NonNull;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.metadatacenter.templates.TemplateServerNames;
import org.metadatacenter.templates.service.TemplatesService;
import org.metadatacenter.templates.utils.JsonUtils;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TemplatesServiceMongoDB implements TemplatesService<String, JsonNode>
{
  @NonNull private final TemplateElementDaoMongoDB templateElementDao;
  @NonNull private final TemplateDaoMongoDB templateDao;
  @NonNull private final TemplateInstanceDaoMongoDB templateInstanceDao;

  public TemplatesServiceMongoDB(@NonNull String db, @NonNull String templatesCollection,
    @NonNull String templateElementsCollection, @NonNull String templateInstancesCollection)
  {
    templateDao = new TemplateDaoMongoDB(db, templatesCollection);
    templateElementDao = new TemplateElementDaoMongoDB(db, templateElementsCollection);
    templateInstanceDao = new TemplateInstanceDaoMongoDB(db, templateInstancesCollection);
  }

  /* Templates */

  @NonNull public JsonNode createTemplate(@NonNull JsonNode template) throws IOException
  {
    return templateDao.create(template);
  }

  @NonNull public List<JsonNode> findAllTemplates() throws IOException
  {
    return templateDao.findAll();
  }

  @NonNull public JsonNode findTemplate(@NonNull String templateId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException
  {
    JsonNode template = templateDao.find(templateId);
    if (expanded)
      template = expand(template);
    if (validation)
      validate(JsonLoader.fromURL(new URL(TemplateServerNames.JSON_SCHEMA_URL)), template);
    return template;
  }

  @NonNull public JsonNode findTemplateByLinkedDataId(@NonNull String templateId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException
  {
    JsonNode template = templateDao.findByLinkedDataId(templateId);
    if (expanded)
      template = expand(template);
    if (validation)
      validate(JsonLoader.fromURL(new URL(TemplateServerNames.JSON_SCHEMA_URL)), template);
    return template;
  }

  @NonNull public JsonNode updateTemplate(@NonNull String templateId, @NonNull JsonNode modifications)
    throws InstanceNotFoundException, IOException
  {
    return templateDao.update(templateId, modifications);
  }

  @NonNull public JsonNode updateTemplateByLinkedDataId(@NonNull String templateId, @NonNull JsonNode modifications)
    throws InstanceNotFoundException, IOException
  {
    return templateDao.updateByLinkedDataId(templateId, modifications);
  }

  public void deleteTemplate(@NonNull String templateId) throws InstanceNotFoundException, IOException
  {
    templateDao.delete(templateId);
  }

  public void deleteTemplateByLinkedDataId(@NonNull String templateId) throws InstanceNotFoundException, IOException
  {
    templateDao.deleteByLinkedDataId(templateId);
  }

  public boolean existsTemplate(@NonNull String templateId) throws IOException
  {
    return templateDao.exists(templateId);
  }

  public boolean existsTemplateByLinkedDataId(@NonNull String templateId) throws IOException
  {
    return templateDao.existsByLinkedDataId(templateId);
  }

  public void deleteAllTemplates()
  {
    templateDao.deleteAll();
  }

  /* Template Elements */

  @NonNull public JsonNode createTemplateElement(@NonNull JsonNode templateElement) throws IOException
  {
    JsonUtils utils = new JsonUtils();
    return templateElementDao.create(templateElement);
  }

  @NonNull public List<JsonNode> findAllTemplateElements() throws IOException
  {
    JsonUtils utils = new JsonUtils();
    List<JsonNode> templateElements = templateElementDao.findAll();
    List<JsonNode> templateElementsAdapted = new ArrayList<JsonNode>();
    for (JsonNode te : templateElements) {
      templateElementsAdapted.add(utils.fixMongoDB(te, 2));
    }
    return templateElementsAdapted;
  }

  @NonNull public JsonNode findTemplateElement(@NonNull String templateElementId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException
  {
    JsonNode templateElement = templateElementDao.find(templateElementId);
    if (expanded)
      templateElement = expand(templateElement);
    if (validation)
      validate(JsonLoader.fromURL(new URL(TemplateServerNames.JSON_SCHEMA_URL)), templateElement);
    return templateElement;
  }

  @NonNull public JsonNode findTemplateElementByLinkedDataId(@NonNull String templateElementId, boolean expanded,
    boolean validation) throws InstanceNotFoundException, IOException, ProcessingException
  {
    JsonNode templateElement = templateElementDao.findByLinkedDataId(templateElementId);
    if (expanded)
      templateElement = expand(templateElement);
    if (validation)
      validate(JsonLoader.fromURL(new URL(TemplateServerNames.JSON_SCHEMA_URL)), templateElement);
    return templateElement;
  }

  @NonNull public JsonNode updateTemplateElement(@NonNull String templateElementId, @NonNull JsonNode modifications)
    throws InstanceNotFoundException, IOException
  {
    return templateElementDao.update(templateElementId, modifications);
  }

  @NonNull public JsonNode updateTemplateElementByLinkedDataId(@NonNull String templateElementId, @NonNull JsonNode modifications)
    throws InstanceNotFoundException, IOException
  {
    return templateElementDao.updateByLinkedDataId(templateElementId, modifications);
  }

  public void deleteTemplateElement(@NonNull String templateElementId) throws InstanceNotFoundException, IOException
  {
    templateElementDao.delete(templateElementId);
  }

  public void deleteTemplateElementByLinkedDataId(@NonNull String templateElementId) throws InstanceNotFoundException, IOException
  {
    templateElementDao.deleteByLinkedDataId(templateElementId);
  }

  public boolean existsTemplateElement(@NonNull String templateElementId) throws IOException
  {
    return templateElementDao.exists(templateElementId);
  }

  public boolean existsTemplateElementByLinkedDataId(@NonNull String templateElementId) throws IOException
  {
    return templateElementDao.existsByLinkedDataId(templateElementId);
  }

  public void deleteAllTemplateElements()
  {
    templateElementDao.deleteAll();
  }

  /* Template Instances */

  @NonNull public JsonNode createTemplateInstance(@NonNull JsonNode templateInstance) throws IOException
  {
    return templateInstanceDao.create(templateInstance);
  }

  @NonNull public List<JsonNode> findAllTemplateInstances() throws IOException
  {
    return templateInstanceDao.findAll();
  }

  @NonNull public JsonNode findTemplateInstance(@NonNull String templateInstanceId)
    throws IOException, InstanceNotFoundException
  {
    return templateInstanceDao.find(templateInstanceId);
  }

  @NonNull public JsonNode findTemplateInstanceByLinkedDataId(@NonNull String templateInstanceId)
    throws IOException, InstanceNotFoundException
  {
    return templateInstanceDao.findByLinkedDataId(templateInstanceId);
  }

  @NonNull public JsonNode updateTemplateInstance(@NonNull String templateInstanceId, @NonNull JsonNode modifications)
    throws InstanceNotFoundException, IOException
  {
    return templateInstanceDao.update(templateInstanceId, modifications);
  }

  @NonNull public JsonNode updateTemplateInstanceByLinkedDataId(@NonNull String templateInstanceId, @NonNull JsonNode modifications)
    throws InstanceNotFoundException, IOException
  {
    return templateInstanceDao.updateByLinkedDataId(templateInstanceId, modifications);
  }

  public void deleteTemplateInstance(@NonNull String templateInstanceId) throws InstanceNotFoundException, IOException
  {
    templateInstanceDao.delete(templateInstanceId);
  }

  public void deleteTemplateInstanceByLinkedDataId(@NonNull String templateInstanceId) throws InstanceNotFoundException, IOException
  {
    templateInstanceDao.deleteByLinkedDataId(templateInstanceId);
  }

  /* Other methods */

  // Expands a Template or a Template Element (JSON Schema). All the template elements referenced using $ref are
  // retrieved and embedded into the JSON Schema
  @NonNull public JsonNode expand(@NonNull JsonNode schema) throws IOException, ProcessingException
  {
    JsonUtils jsonUtils = new JsonUtils();
    return jsonUtils.resolveTemplateElementRefs(schema, this);
  }

  // Validation against JSON schema
  public void validate(@NonNull JsonNode schema, @NonNull JsonNode instance) throws ProcessingException
  {
    JsonUtils jsonUtils = new JsonUtils();
    jsonUtils.validate(schema, instance);
  }

}
