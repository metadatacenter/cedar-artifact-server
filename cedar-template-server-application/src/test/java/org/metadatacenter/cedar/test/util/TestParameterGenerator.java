package org.metadatacenter.cedar.test.util;

import org.metadatacenter.cedar.template.resources.rest.AuthHeaderSelector;

import java.util.ArrayList;
import java.util.List;

public class TestParameterGenerator {

  private String alias;
  private int position;
  private List<TestParameterValueGenerator> valueGenerators;

  public TestParameterGenerator(int position) {
    this.position = position;
    valueGenerators = new ArrayList<>();
  }

  public TestParameterGenerator alias(String alias) {
    this.alias = alias;
    return this;
  }

  String getAlias() {
    return alias;
  }

  public int getPosition() {
    return position;
  }

  public void value(Object value) {
    if (value instanceof AuthHeaderSelector) {
      valueGenerators.add(new TestValueAuthStringGenerator((AuthHeaderSelector) value));
    } else if (value instanceof TestParameterValueGenerator) {
      valueGenerators.add((TestParameterValueGenerator) value);
    } else {
      valueGenerators.add(new TestValueConstantGenerator(value));
    }
  }

  public List<TestParameterValueGenerator> getAll() {
    List<TestParameterValueGenerator> all = new ArrayList<>();
    for (TestParameterValueGenerator valueGenerator : valueGenerators) {
      all.add(valueGenerator);
    }
    return all;
  }

}
