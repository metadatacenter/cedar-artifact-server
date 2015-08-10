package org.metadatacenter.templates.service;

import checkers.nullness.quals.NonNull;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.util.List;

public interface TemplatesService<K, T>
{
  /* Templates */

  @NonNull public T createTemplate(@NonNull T template) throws IOException;

  @NonNull public List<T> findAllTemplates() throws IOException;

  @NonNull public T findTemplate(@NonNull K templateId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException;

  @NonNull public T findTemplateByLinkedDataId(@NonNull String templateId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException;

  @NonNull public T updateTemplate(@NonNull K templateId, @NonNull T modifications)
    throws InstanceNotFoundException, IOException;

  public void deleteTemplate(@NonNull K templateId) throws InstanceNotFoundException, IOException;

  public boolean existsTemplate(@NonNull K templateId) throws IOException;

  public void deleteAllTemplates();

  /* Template Elements */

  @NonNull public T createTemplateElement(@NonNull T templateElement) throws IOException;

  @NonNull public List<T> findAllTemplateElements() throws IOException;

  @NonNull public T findTemplateElement(@NonNull K templateElementId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException;

  @NonNull public T findTemplateElementByLinkedDataId(@NonNull String templateElementId, boolean expanded,
    boolean validation) throws InstanceNotFoundException, IOException, ProcessingException;

  @NonNull public T updateTemplateElement(@NonNull K templateElementId, @NonNull T modifications)
    throws InstanceNotFoundException, IOException;

  public void deleteTemplateElement(@NonNull K templateElementId) throws InstanceNotFoundException, IOException;

  public boolean existsTemplateElement(@NonNull K templateElementId) throws IOException;

  public void deleteAllTemplateElements();

  /* Template Instances */

  @NonNull public T createTemplateInstance(@NonNull T templateInstance) throws IOException;

  @NonNull public List<T> findAllTemplateInstances() throws IOException;

  @NonNull public T findTemplateInstance(@NonNull K templateInstanceId) throws InstanceNotFoundException, IOException;

  @NonNull public T updateTemplateInstance(@NonNull K templateInstanceId, @NonNull T modifications)
    throws InstanceNotFoundException, IOException;

  public void deleteTemplateInstance(@NonNull K templateInstanceId) throws InstanceNotFoundException, IOException;
}
