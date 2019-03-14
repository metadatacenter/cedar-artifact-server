package org.metadatacenter.cedar.test.util;

import org.metadatacenter.cedar.artifact.resources.rest.IdMatchingSelector;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.server.jsonld.LinkedDataUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class TestValueResourceIdGenerator extends AbstractTestValueGenerator<String> {

  private final IdMatchingSelector idMatchingSelector;
  private final String aliasOfResourceType;

  private String value;

  public TestValueResourceIdGenerator(IdMatchingSelector idMatchingSelector, String aliasOfResourceType) {
    this.idMatchingSelector = idMatchingSelector;
    this.aliasOfResourceType = aliasOfResourceType;
  }

  public static Collection<TestValueResourceIdGenerator> ids(Set<IdMatchingSelector> idMatchingSelector, String
      aliasOfResourceType) {
    List<TestValueResourceIdGenerator> ret = new ArrayList<>();
    for (IdMatchingSelector sel : idMatchingSelector) {
      ret.add(new TestValueResourceIdGenerator(sel, aliasOfResourceType));
    }
    return ret;
  }

  @Override
  public void generateValue(TestDataGenerationContext tdctx, TestParameterArrayGenerator arrayGenerator) {
    TestParameterGenerator generator = arrayGenerator.getGenerator(aliasOfResourceType);
    int position = generator.getPosition();
    TestParameterValueGenerator<CedarNodeType> resourceTypeGenerator = referenceToLine[position];
    CedarNodeType nodeType = resourceTypeGenerator.getValue();
    String baseTestUrl = tdctx.getBaseTestUrl();
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, nodeType);
    LinkedDataUtil linkedDataUtil = tdctx.getLinkedDataUtil();
    if (idMatchingSelector == IdMatchingSelector.NULL_FULL) {
      value = "";
    } else if (idMatchingSelector == IdMatchingSelector.NULL_ID) {
      value = url;
    } else if (idMatchingSelector == IdMatchingSelector.GIBBERISH) {
      value = "gibberish";
    } else if (idMatchingSelector == IdMatchingSelector.RANDOM_ID) {
      value = linkedDataUtil.buildNewLinkedDataId(nodeType);
    }
  }

  public IdMatchingSelector getIdMatchingSelector() {
    return idMatchingSelector;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public TestValueResourceIdGenerator clone() {
    TestValueResourceIdGenerator c = new TestValueResourceIdGenerator(idMatchingSelector, aliasOfResourceType);
    return c;
  }


}
