package org.metadatacenter.templates;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.util.List;

public interface TemplatesService<K, T>
{

  /* Templates */

  public T createTemplate(T template) throws IOException;

  public List<T> findAllTemplates() throws IOException;

  public T findTemplate(K templateId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException;

  public T findTemplateByLinkedDataId(String templateId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException;

  public T updateTemplate(K templateId, T modifications) throws InstanceNotFoundException, IOException;

  public void deleteTemplate(K templateId) throws InstanceNotFoundException, IOException;

  public boolean existsTemplate(K templateId) throws IOException;

  public void deleteAllTemplates();

  /* Template Elements */

  public T createTemplateElement(T templateElement) throws IOException;

  public List<T> findAllTemplateElements() throws IOException;

  public T findTemplateElement(K templateElementId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException;

  public T findTemplateElementByLinkedDataId(String templateElementId, boolean expanded, boolean validation)
    throws InstanceNotFoundException, IOException, ProcessingException;

  public T updateTemplateElement(K templateElementId, T modifications) throws InstanceNotFoundException, IOException;

  public void deleteTemplateElement(K templateElementId) throws InstanceNotFoundException, IOException;

  public boolean existsTemplateElement(K templateElementId) throws IOException;

  public void deleteAllTemplateElements();

  /* Template Instances */

  public T createTemplateInstance(T templateInstance) throws IOException;

  public List<T> findAllTemplateInstances() throws IOException;

  public T findTemplateInstance(K templateInstanceId) throws InstanceNotFoundException, IOException;

  public T updateTemplateInstance(K templateInstanceId, T modifications)
    throws InstanceNotFoundException, IOException;

  public void deleteTemplateInstance(K templateInstanceId) throws InstanceNotFoundException, IOException;

}
