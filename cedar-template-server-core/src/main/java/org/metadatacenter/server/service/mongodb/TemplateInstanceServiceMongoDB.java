package org.metadatacenter.server.service.mongodb;

import checkers.nullness.quals.NonNull;
import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.server.dao.mongodb.TemplateInstanceDaoMongoDB;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateInstanceService;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.util.List;

public class TemplateInstanceServiceMongoDB extends GenericTemplateServiceMongoDB<String, JsonNode> implements TemplateInstanceService<String, JsonNode> {

  private final @NonNull TemplateInstanceDaoMongoDB templateInstanceDao;

  public TemplateInstanceServiceMongoDB(@NonNull String db, @NonNull String templateInstancesCollection) {
    templateInstanceDao = new TemplateInstanceDaoMongoDB(db, templateInstancesCollection);
  }

  @NonNull
  public JsonNode createTemplateInstance(@NonNull JsonNode templateInstance) throws IOException {
    return templateInstanceDao.create(templateInstance);
  }

  @NonNull
  public JsonNode createTemplateInstanceLinkedData(@NonNull JsonNode templateInstance) throws IOException {
    return templateInstanceDao.createLinkedData(templateInstance);
  }

  @Override
  @NonNull
  public List<JsonNode> findAllTemplateInstances() throws IOException {
    return templateInstanceDao.findAll();
  }

  @Override
  @NonNull
  public List<JsonNode> findAllTemplateInstances(List<String> fieldNames, FieldNameInEx includeExclude) throws IOException {
    return templateInstanceDao.findAll(fieldNames, includeExclude);
  }

  @Override
  @NonNull
  public List<JsonNode> findAllTemplateInstances(Integer count, Integer page, List<String> fieldNames, FieldNameInEx includeExclude) throws IOException {
    return templateInstanceDao.findAll(count, page, fieldNames, includeExclude);
  }

  public JsonNode findTemplateInstance(@NonNull String templateInstanceId)
      throws IOException {
    return templateInstanceDao.find(templateInstanceId);
  }

  public JsonNode findTemplateInstanceByLinkedDataId(@NonNull String templateInstanceId)
      throws IOException {
    return templateInstanceDao.findByLinkedDataId(templateInstanceId);
  }

  @NonNull
  public JsonNode updateTemplateInstance(@NonNull String templateInstanceId, @NonNull JsonNode modifications)
      throws InstanceNotFoundException, IOException {
    return templateInstanceDao.update(templateInstanceId, modifications);
  }

  @NonNull
  public JsonNode updateTemplateInstanceByLinkedDataId(@NonNull String templateInstanceId, @NonNull JsonNode modifications)
      throws InstanceNotFoundException, IOException {
    return templateInstanceDao.updateByLinkedDataId(templateInstanceId, modifications);
  }

  public void deleteTemplateInstance(@NonNull String templateInstanceId) throws InstanceNotFoundException, IOException {
    templateInstanceDao.delete(templateInstanceId);
  }

  public void deleteTemplateInstanceByLinkedDataId(@NonNull String templateInstanceId) throws InstanceNotFoundException, IOException {
    templateInstanceDao.deleteByLinkedDataId(templateInstanceId);
  }

  @Override
  public long count() {
    return templateInstanceDao.count();
  }

}
