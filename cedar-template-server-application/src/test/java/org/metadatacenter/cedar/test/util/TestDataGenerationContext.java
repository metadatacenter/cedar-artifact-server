package org.metadatacenter.cedar.test.util;

import org.metadatacenter.server.jsonld.LinkedDataUtil;

public class TestDataGenerationContext {

  private String authHeaderTestUser1;
  private String baseTestUrl;
  private LinkedDataUtil linkedDataUtil;

  public void setLinkedDataUtil(LinkedDataUtil linkedDataUtil) {
    this.linkedDataUtil = linkedDataUtil;
  }

  public void setAuthHeaderTestUser1(String authHeaderTestUser1) {
    this.authHeaderTestUser1 = authHeaderTestUser1;
  }

  public void setBaseTestUrl(String baseTestUrl) {
    this.baseTestUrl = baseTestUrl;
  }

  public String getAuthHeaderTestUser1() {
    return authHeaderTestUser1;
  }

  public String getBaseTestUrl() {
    return baseTestUrl;
  }

  public LinkedDataUtil getLinkedDataUtil() {
    return linkedDataUtil;
  }
}
