package org.metadatacenter.templates;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.metadatacenter.templates.template.TemplateDaoMongoDB;
import org.metadatacenter.templates.templateelement.TemplateElementDaoMongoDB;
import org.metadatacenter.templates.templateinstance.TemplateInstanceDaoMongoDB;
import org.metadatacenter.templates.utils.JsonUtils;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TemplatesServiceMongoDB implements TemplatesService<String, JsonNode>
{

  private TemplateElementDaoMongoDB templateElementDao;
  private TemplateDaoMongoDB templateDao;
  private TemplateInstanceDaoMongoDB templateInstanceDao;

  final String JSON_SCHEMA_URL = "http://json-schema.org/draft-04/schema#";

  public TemplatesServiceMongoDB(String db, String templatesCollection, String templateElementsCollection,
    String templateInstancesCollection)
  {
    templateDao = new TemplateDaoMongoDB(db, templatesCollection);
    templateElementDao = new TemplateElementDaoMongoDB(db, templateElementsCollection);
    templateInstanceDao = new TemplateInstanceDaoMongoDB(db, templateInstancesCollection);
  }

  /* Templates */

  public JsonNode createTemplate(JsonNode template) throws IOException
  {
    return templateDao.create(template);
  }

  public List<JsonNode> findAllTemplates() throws IOException
  {
    return templateDao.findAll();
  }

  public JsonNode findTemplate(String templateId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException
  {
    JsonNode template = templateDao.find(templateId);
    if (expanded == true)
      template = expand(template);
    if (validation == true)
      validate(JsonLoader.fromURL(new URL(JSON_SCHEMA_URL)), template);
    return template;
  }

  public JsonNode findTemplateByLinkedDataId(String templateId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException
  {
    JsonNode template = templateDao.findByLinkedDataId(templateId);
    if (expanded == true)
      template = expand(template);
    if (validation == true)
      validate(JsonLoader.fromURL(new URL(JSON_SCHEMA_URL)), template);
    return template;
  }

  public JsonNode updateTemplate(String templateId, JsonNode modifications)
    throws InstanceNotFoundException, IOException
  {
    return templateDao.update(templateId, modifications);
  }

  public void deleteTemplate(String templateId) throws InstanceNotFoundException, IOException
  {
    templateDao.delete(templateId);
  }

  public boolean existsTemplate(String templateId) throws IOException
  {
    return templateDao.exists(templateId);
  }

  public void deleteAllTemplates()
  {
    templateDao.deleteAll();
  }

  /* Template Elements */

  public JsonNode createTemplateElement(JsonNode templateElement) throws IOException
  {
    JsonUtils utils = new JsonUtils();
    return templateElementDao.create(templateElement);
  }

  public List<JsonNode> findAllTemplateElements() throws IOException
  {
    JsonUtils utils = new JsonUtils();
    List<JsonNode> templateElements = templateElementDao.findAll();
    List<JsonNode> templateElementsAdapted = new ArrayList<JsonNode>();
    for (JsonNode te : templateElements) {
      templateElementsAdapted.add(utils.fixMongoDB(te, 2));
    }
    return templateElementsAdapted;
  }

  public JsonNode findTemplateElement(String templateElementId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException
  {
    JsonNode templateElement = templateElementDao.find(templateElementId);
    if (expanded == true)
      templateElement = expand(templateElement);
    if (validation == true)
      validate(JsonLoader.fromURL(new URL(JSON_SCHEMA_URL)), templateElement);
    return templateElement;
  }

  public JsonNode findTemplateElementByLinkedDataId(String templateElementId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException
  {
    JsonNode templateElement = templateElementDao.findByLinkedDataId(templateElementId);
    if (expanded == true)
      templateElement = expand(templateElement);
    if (validation == true)
      validate(JsonLoader.fromURL(new URL(JSON_SCHEMA_URL)), templateElement);
    return templateElement;
  }

  public JsonNode updateTemplateElement(String templateElementId, JsonNode modifications)
    throws InstanceNotFoundException, IOException
  {
    return templateElementDao.update(templateElementId, modifications);
  }

  public void deleteTemplateElement(String templateElementId) throws InstanceNotFoundException, IOException
  {
    templateElementDao.delete(templateElementId);
  }

  public boolean existsTemplateElement(String templateElementId) throws IOException
  {
    return templateElementDao.exists(templateElementId);
  }

  public void deleteAllTemplateElements()
  {
    templateElementDao.deleteAll();
  }

  /* Template Instances */

  public JsonNode createTemplateInstance(JsonNode templateInstance) throws IOException
  {
    return templateInstanceDao.create(templateInstance);
  }

  public List<JsonNode> findAllTemplateInstances() throws IOException
  {
    return templateInstanceDao.findAll();
  }

  public JsonNode findTemplateInstance(String templateInstanceId) throws IOException, InstanceNotFoundException
  {
    return templateInstanceDao.find(templateInstanceId);
  }

  public JsonNode updateTemplateInstance(String templateInstanceId, JsonNode modifications)
    throws InstanceNotFoundException, IOException
  {
    return templateInstanceDao.update(templateInstanceId, modifications);
  }

  public void deleteTemplateInstance(String templateInstanceId) throws InstanceNotFoundException, IOException
  {
    templateInstanceDao.delete(templateInstanceId);
  }

  /* Other methods */

  // Expands Template or a Template Element (Json Schema). All the template elements referenced using $ref are
  // retrieved and embedded into the Json Schema code.
  public JsonNode expand(JsonNode schema) throws IOException, ProcessingException
  {
    JsonUtils jsonUtils = new JsonUtils();
    return jsonUtils.resolveTemplateElementRefs(schema, this);
  }

  // Validation against Json Schema
  public void validate(JsonNode schema, JsonNode instance) throws IOException, ProcessingException
  {
    JsonUtils jsonUtils = new JsonUtils();
    jsonUtils.validate(schema, instance);
  }

}
