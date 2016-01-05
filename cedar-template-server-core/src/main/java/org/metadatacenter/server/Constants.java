package org.metadatacenter.server;

public interface Constants {
  String JSON_SCHEMA_URL = "http://json-schema.org/draft-04/schema#";

  // application.conf keys
  String MONGODB_DATABASE_NAME = "mongodb.db";
  String TEMPLATES_COLLECTION_NAME = "mongodb.collections.templates";
  String TEMPLATE_ELEMENTS_COLLECTION_NAME = "mongodb.collections.template-elements";
  String TEMPLATE_INSTANCES_COLLECTION_NAME = "mongodb.collections.template-instances";

  String LINKED_DATA_ID_PATH_BASE = "linkedData.idPath.base";
  String LINKED_DATA_ID_PATH_SUFFIX_TEMPLATES = "linkedData.idPath.suffix.templates";
  String LINKED_DATA_ID_PATH_SUFFIX_TEMPLATE_ELEMENTS = "linkedData.idPath.suffix.template-elements";
  String LINKED_DATA_ID_PATH_SUFFIX_TEMPLATE_INSTANCES = "linkedData.idPath.suffix.template-instances";

  String PAGINATION_DEFAULT_PAGE_SIZE = "pagination.defaultPageSize";
  String PAGINATION_MAX_PAGE_SIZE = "pagination.maxPageSize";

  String FIELD_NAMES_SUMMARY_TEMPLATE_ELEMENT = "summary.templateElement.fields";
  String FIELD_NAMES_SUMMARY_TEMPLATE = "summary.template.fields";
  String FIELD_NAMES_SUMMARY_TEMPLATE_INSTANCE = "summary.templateInstance.fields";

  String FIELD_NAMES_LIST_EXCLUSION = "list.excludedFields";

  // HTTP headers
  String HTTP_HEADER_LOCATION = "Location";
  String HTTP_HEADER_LINK = "Link";
  String HTTP_CUSTOM_HEADER_TOTAL_COUNT = "Total-Count";

  // HTTP Link header types
  String HEADER_LINK_TYPE_FIRST = "first";
  String HEADER_LINK_TYPE_LAST = "last";
  String HEADER_LINK_TYPE_PREV = "prev";
  String HEADER_LINK_TYPE_NEXT = "next";

  // Query String parameter names
  String PARAM_OFFSET = "offset";
  String PARAM_LIMIT = "limit";

}
