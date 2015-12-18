package org.metadatacenter.server;

public interface Constants {
  String JSON_SCHEMA_URL = "http://json-schema.org/draft-04/schema#";

  String MONGODB_DATABASE_NAME = "mongodb.db";
  String TEMPLATES_COLLECTION_NAME = "mongodb.collections.templates";
  String TEMPLATE_ELEMENTS_COLLECTION_NAME = "mongodb.collections.template-elements";
  String TEMPLATE_INSTANCES_COLLECTION_NAME = "mongodb.collections.template-instances";

  String LINKED_DATA_ID_PATH_BASE = "linkedData.idPath.base";
  String LINKED_DATA_ID_PATH_SUFFIX_TEMPLATES = "linkedData.idPath.suffix.templates";
  String LINKED_DATA_ID_PATH_SUFFIX_TEMPLATE_ELEMENTS = "linkedData.idPath.suffix.template-elements";
  String LINKED_DATA_ID_PATH_SUFFIX_TEMPLATE_INSTANCES = "linkedData.idPath.suffix.template-instances";
}
