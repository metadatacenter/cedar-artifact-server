package org.metadatacenter.cedar.template.resources;

import com.google.common.base.MoreObjects;

import javax.annotation.Nonnull;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class TestResource {

  private final String content;
  private final String expectedResult;

  private TestResource(@Nonnull String content, @Nonnull String expectedResult) {
    this.content = checkNotNull(content);
    this.expectedResult = checkNotNull(expectedResult);
  }

  public static TestResource create(@Nonnull String templateDocument, @Nonnull String expectedResult) {
    return new TestResource(templateDocument, expectedResult);
  }

  public String getContent() {
    return content;
  }

  public String getExpected() {
    return expectedResult;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (this == o) {
      return true;
    }
    if (!(o instanceof TestResource)) {
      return false;
    }
    TestResource other = (TestResource) o;
    return Objects.equals(getContent(), other.getContent()) && Objects.equals(getExpected(), other.getExpected());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getContent(), getExpected());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("Content", getContent())
        .add("Expected result", getExpected())
        .toString();
  }
}
