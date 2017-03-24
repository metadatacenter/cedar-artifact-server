package org.metadatacenter.cedar.template.resources;

import com.google.common.base.MoreObjects;

import javax.annotation.Nonnull;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class TemplateResource implements TestResource {

  private final String templateDocument;
  private final String expectedResult;

  private TemplateResource(@Nonnull String templateDocument, @Nonnull String expectedResult) {
    this.templateDocument = checkNotNull(templateDocument);
    this.expectedResult = checkNotNull(expectedResult);
  }

  public static TemplateResource create(@Nonnull String templateDocument, @Nonnull String expectedResult) {
    return new TemplateResource(templateDocument, expectedResult);
  }

  @Override
  public String getContent() {
    return templateDocument;
  }

  @Override
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
    if (!(o instanceof TemplateResource)) {
      return false;
    }
    TemplateResource other = (TemplateResource) o;
    return Objects.equals(getContent(), other.getContent()) && Objects.equals(getExpected(), other.getExpected());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getContent(), getExpected());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("Input object", getContent())
        .add("Expected result", getExpected())
        .toString();
  }
}
