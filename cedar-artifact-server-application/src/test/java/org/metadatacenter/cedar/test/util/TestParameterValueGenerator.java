package org.metadatacenter.cedar.test.util;

public interface TestParameterValueGenerator<T extends Object> extends Cloneable {

  void generateValue(TestDataGenerationContext ctx, TestParameterArrayGenerator arrayGenerator);

  T getValue();

  void setRef(TestParameterValueGenerator[] copy);

  TestParameterValueGenerator<T> clone();
}
