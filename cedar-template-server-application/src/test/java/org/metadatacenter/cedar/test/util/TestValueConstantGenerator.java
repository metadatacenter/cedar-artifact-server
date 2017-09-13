package org.metadatacenter.cedar.test.util;

public class TestValueConstantGenerator<T> extends AbstractTestValueGenerator<T> {

  private final T value;

  public TestValueConstantGenerator(T value) {
    this.value = value;
  }

  @Override
  public void generateValue(TestDataGenerationContext tdctx, TestParameterArrayGenerator arrayGenerator) {
  }

  @Override
  public T getValue() {
    return value;
  }

  @Override
  public TestValueConstantGenerator<T> clone() {
    TestValueConstantGenerator c = new TestValueConstantGenerator(this.value);
    return c;
  }

}
