package org.metadatacenter.cedar.template.resources.rest;

public enum AuthHeaderSelector {
  NULL(null),
  GIBBERISH_FULL("gibberishFull"),
  GIBBERISH_KEY("gibberishKey"),
  TEST_USER_1("testUser1");

  private final String value;

  AuthHeaderSelector(String value) {
    this.value = value;
  }
}
