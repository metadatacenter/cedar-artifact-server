package org.metadatacenter.server.service;

import checkers.nullness.quals.NonNull;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.util.List;

public interface TemplateInstanceService<K, T> {

  @NonNull
  public T createTemplateInstance(@NonNull T templateInstance) throws IOException;

  @NonNull
  public T createTemplateInstanceLinkedData(@NonNull T templateInstance) throws IOException;

  @NonNull
  public List<T> findAllTemplateInstances() throws IOException;

  @NonNull
  public List<T> findAllTemplateInstances(List<String> fieldNames, FieldNameInEx includeExclude) throws IOException;

  @NonNull
  public List<T> findAllTemplateInstances(Integer limit, Integer offset, List<String> fieldNames, FieldNameInEx includeExclude) throws IOException;

  public T findTemplateInstance(@NonNull K templateInstanceId) throws IOException;

  public T findTemplateInstanceByLinkedDataId(@NonNull K templateInstanceId) throws IOException;

  @NonNull
  public T updateTemplateInstance(@NonNull K templateInstanceId, @NonNull T modifications) throws InstanceNotFoundException, IOException;

  @NonNull
  public T updateTemplateInstanceByLinkedDataId(@NonNull K templateInstanceId, @NonNull T modifications) throws InstanceNotFoundException, IOException;

  public void deleteTemplateInstance(@NonNull K templateInstanceId) throws InstanceNotFoundException, IOException;

  public void deleteTemplateInstanceByLinkedDataId(@NonNull K templateInstanceId) throws InstanceNotFoundException, IOException;

  public long count();
}
