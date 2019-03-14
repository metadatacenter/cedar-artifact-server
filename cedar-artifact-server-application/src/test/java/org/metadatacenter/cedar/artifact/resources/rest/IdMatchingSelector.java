package org.metadatacenter.cedar.artifact.resources.rest;

public enum IdMatchingSelector {
  NULL_FULL(null),
  NULL_ID("nullId"),
  GIBBERISH("gibberish"),
  RANDOM_ID("random"),
  FROM_JSON("fromJson"),
  PREVIOUSLY_CREATED("previouslyCreated");

  private final String value;

  IdMatchingSelector(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
