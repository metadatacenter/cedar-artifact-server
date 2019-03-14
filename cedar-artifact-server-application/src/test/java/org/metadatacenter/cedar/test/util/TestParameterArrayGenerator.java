package org.metadatacenter.cedar.test.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestParameterArrayGenerator {

  private List<TestParameterGenerator> parameterGenerators;

  public TestParameterArrayGenerator() {
    parameterGenerators = new ArrayList<>();
    addParameterValue(0, new TestParameterArrayGeneratorGenerator(this), "generator");
  }

  public void addParameterValue(int position, Object value, String alias) {
    ensurePosition(position);
    TestParameterGenerator generator = parameterGenerators.get(position);
    generator.alias(alias).value(value);
  }

  public void addParameterValue(int position, TestParameterValueGenerator valueGenerator) {
    ensurePosition(position);
    TestParameterGenerator generator = parameterGenerators.get(position);
    generator.value(valueGenerator);
  }

  public void registerParameter(int position, Collection<? extends Object> values, String alias) {
    ensurePosition(position);
    TestParameterGenerator generator = parameterGenerators.get(position);
    generator.alias(alias);
    for (Object value : values) {
      generator.value(value);
    }
  }

  private void ensurePosition(int position) {
    while (parameterGenerators.size() < position + 1) {
      parameterGenerators.add(new TestParameterGenerator(position));
    }
  }

  public List<TestParameterValueGenerator[]> generateAllCombinations() {
    List<TestParameterValueGenerator[]> combination = new ArrayList<>();
    generateAllCombinations(combination, parameterGenerators, 0, null);
    return combination;
  }

  private void generateAllCombinations(List<TestParameterValueGenerator[]> combination, List<TestParameterGenerator>
      parameterGenerators, int position, TestParameterValueGenerator[] currentLine) {
    if (position == parameterGenerators.size()) {
      TestParameterValueGenerator[] copy = new TestParameterValueGenerator[currentLine.length];
      for (int i = 0; i < copy.length; i++) {
        copy[i] = currentLine[i].clone();
        copy[i].setRef(copy);
      }
      combination.add(copy);
      return;
    }
    if (position == 0) {
      currentLine = new TestParameterValueGenerator[parameterGenerators.size()];
    }
    TestParameterGenerator generator = parameterGenerators.get(position);
    List<TestParameterValueGenerator> values = generator.getAll();
    for (TestParameterValueGenerator value : values) {
      currentLine[position] = value;
      generateAllCombinations(combination, parameterGenerators, position + 1, currentLine);
    }
  }

  TestParameterGenerator getGenerator(String alias) {
    for (TestParameterGenerator gen : parameterGenerators) {
      if (gen.getAlias().equals(alias)) {
        return gen;
      }
    }
    return null;
  }
}