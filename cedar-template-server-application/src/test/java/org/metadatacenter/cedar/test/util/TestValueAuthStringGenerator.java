package org.metadatacenter.cedar.test.util;

import org.metadatacenter.cedar.template.resources.rest.AuthHeaderSelector;

import static org.metadatacenter.cedar.template.resources.rest.AuthHeaderSelector.*;
import static org.metadatacenter.constant.HttpConstants.HTTP_AUTH_HEADER_APIKEY_PREFIX;

public class TestValueAuthStringGenerator extends AbstractTestValueGenerator<String> {

  private final AuthHeaderSelector authSelector;

  private String value;

  public TestValueAuthStringGenerator(AuthHeaderSelector authSelector) {
    this.authSelector = authSelector;
  }

  @Override
  public void generateValue(TestDataGenerationContext tdctx, TestParameterArrayGenerator arrayGenerator) {
    if (authSelector == TEST_USER_1) {
      value = tdctx.getAuthHeaderTestUser1();
    } else if (authSelector == GIBBERISH_FULL) {
      value = "gibberish";
    } else if (authSelector == GIBBERISH_KEY) {
      value = HTTP_AUTH_HEADER_APIKEY_PREFIX + "gibberish";
    }
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public TestValueAuthStringGenerator clone() {
    TestValueAuthStringGenerator c = new TestValueAuthStringGenerator(this.authSelector);
    return c;
  }

  public AuthHeaderSelector getAuthSelector() {
    return authSelector;
  }
}
