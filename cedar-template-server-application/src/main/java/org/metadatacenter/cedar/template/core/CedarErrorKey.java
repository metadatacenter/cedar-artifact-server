package org.metadatacenter.cedar.template.core;

public enum CedarErrorKey {

  TEMPLATE_ELEMENT_NOT_CREATED("templateElementNotCreated"),
  TEMPLATE_ELEMENT_NOT_FOUND("templateElementNotFound"),
  TEMPLATE_ELEMENT_NOT_DELETED("templateElementNotDeleted"),
  TEMPLATE_ELEMENT_NOT_UPDATED("templateElementNotUpdated"),
  TEMPLATE_ELEMENTS_NOT_LISTED("templateElementsNotListed"),

  TEMPLATE_FIELD_NOT_CREATED("templateFieldNotCreated"),
  TEMPLATE_FIELD_NOT_FOUND("templateFieldNotFound"),
  TEMPLATE_FIELD_NOT_DELETED("templateFieldNotDeleted"),
  TEMPLATE_FIELD_NOT_UPDATED("templateFieldNotUpdated"),
  TEMPLATE_FIELDS_NOT_LISTED("templateFieldsNotListed"),

  TEMPLATE_NOT_CREATED("templateNotCreated"),
  TEMPLATE_NOT_FOUND("templateNotFound"),
  TEMPLATE_NOT_DELETED("templateNotDeleted"),
  TEMPLATE_NOT_UPDATED("templateNotUpdated"),
  TEMPLATES_NOT_LISTED("templatesNotListed"),

  TEMPLATE_INSTANCE_NOT_CREATED("templateInstanceNotCreated"),
  TEMPLATE_INSTANCE_NOT_FOUND("templateInstanceNotFound"),
  TEMPLATE_INSTANCE_NOT_DELETED("templateInstanceNotDeleted"),
  TEMPLATE_INSTANCE_NOT_UPDATED("templateInstanceNotUpdated"),
  TEMPLATE_INSTANCES_NOT_LISTED("templateInstancesNotListed");

  private final String errorKey;

  CedarErrorKey(String errorKey) {
    this.errorKey = errorKey;
  }
}
