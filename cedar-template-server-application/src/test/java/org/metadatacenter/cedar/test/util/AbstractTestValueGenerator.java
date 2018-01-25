package org.metadatacenter.cedar.test.util;

public abstract class AbstractTestValueGenerator<T extends Object> implements TestParameterValueGenerator<T> {

  TestParameterValueGenerator[] referenceToLine;

  @Override
  public void setRef(TestParameterValueGenerator[] referenceToLine) {
    this.referenceToLine = referenceToLine;
  }

  public abstract AbstractTestValueGenerator<T> clone();
}
