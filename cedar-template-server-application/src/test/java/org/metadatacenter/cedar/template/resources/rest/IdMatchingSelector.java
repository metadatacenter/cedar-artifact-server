package org.metadatacenter.cedar.template.resources.rest;

public enum IdMatchingSelector {
  NULL_ID(null),
  GIBBERISH("gibberish"),
  RANDOM_ID("random"),
  FROM_JSON("fromJson");

  private final String value;

  IdMatchingSelector(String value) {
    this.value = value;
  }
}
