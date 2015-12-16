package org.metadatacenter.server.service.mongodb;

import checkers.nullness.quals.NonNull;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.metadatacenter.server.utils.JsonUtils;

public class GenericTemplateServiceMongoDB<K, T> {

  // Validation against JSON schema
  public void validate(@NonNull JsonNode schema, @NonNull JsonNode instance) throws ProcessingException {
    JsonUtils jsonUtils = new JsonUtils();
    jsonUtils.validate(schema, instance);
  }
}
