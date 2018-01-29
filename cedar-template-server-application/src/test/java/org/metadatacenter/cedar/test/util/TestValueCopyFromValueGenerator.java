package org.metadatacenter.cedar.test.util;

public class TestValueCopyFromValueGenerator<T> extends AbstractTestValueGenerator<T> {

  private final String sourceAlias;

  private T value;

  private TestValueCopyFromValueGenerator(String sourceAlias) {
    this.sourceAlias = sourceAlias;
  }

  public static TestValueCopyFromValueGenerator copyFrom(String sourceAlias) {
    return new TestValueCopyFromValueGenerator(sourceAlias);
  }

  @Override
  public void generateValue(TestDataGenerationContext tdctx, TestParameterArrayGenerator arrayGenerator) {
    TestParameterGenerator generator = arrayGenerator.getGenerator(sourceAlias);
    int position = generator.getPosition();
    TestParameterValueGenerator aliasGenerator = referenceToLine[position];
    Object aliasValue = aliasGenerator.getValue();
    value = (T) aliasValue;
  }

  @Override
  public T getValue() {
    return value;
  }

  @Override
  public TestValueCopyFromValueGenerator<T> clone() {
    TestValueCopyFromValueGenerator c = new TestValueCopyFromValueGenerator(this.sourceAlias);
    return c;
  }

  public String getSourceAlias() {
    return sourceAlias;
  }
}
