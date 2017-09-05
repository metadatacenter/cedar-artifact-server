package org.metadatacenter.cedar.template.resources.rest;

public enum IdMatchingSelector {
  NULL(null),
  GIBBERISH("gibberish"),
  FROM_JSON("fromJson");

  private final String value;

  IdMatchingSelector(String value) {
    this.value = value;
  }
}
