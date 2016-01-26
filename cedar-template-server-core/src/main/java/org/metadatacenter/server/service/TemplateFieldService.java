package org.metadatacenter.server.service;

import checkers.nullness.quals.NonNull;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import java.io.IOException;
import java.util.List;

public interface TemplateFieldService<K, T> {

  @NonNull
  public List<T> findAllTemplateFields(Integer limit, Integer offset, List<String> fieldName, FieldNameInEx
      includeExclude) throws IOException;

  public T findTemplateFieldByLinkedDataId(@NonNull String templateFieldId, boolean validation)
      throws IOException, ProcessingException;

  public long count();

  public void saveNewFieldsAndReplaceIds(T genericInstance) throws IOException;
}