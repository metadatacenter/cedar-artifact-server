package org.metadatacenter.cedar.test.util;

public class TestParameterArrayGeneratorGenerator extends AbstractTestValueGenerator<TestParameterArrayGenerator> {

  private final TestParameterArrayGenerator value;

  public TestParameterArrayGeneratorGenerator(TestParameterArrayGenerator value) {
    this.value = value;
  }

  @Override
  public void generateValue(TestDataGenerationContext tdctx, TestParameterArrayGenerator arrayGenerator) {
  }

  @Override
  public TestParameterArrayGenerator getValue() {
    return value;
  }

  @Override
  public TestParameterArrayGeneratorGenerator clone() {
    TestParameterArrayGeneratorGenerator c = new TestParameterArrayGeneratorGenerator(this.value);
    return c;
  }
}
