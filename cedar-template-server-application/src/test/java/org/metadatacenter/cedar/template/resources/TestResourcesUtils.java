package org.metadatacenter.cedar.template.resources;

import javax.annotation.Nonnull;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class TestResourcesUtils {

  public static String getStringContent(@Nonnull String path) {
    checkNotNull(path);
    URL resourceUrl = getResourceUrl(path);
    try {
      byte[] bytes = Files.readAllBytes(Paths.get(resourceUrl.toURI()));
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Internal testing error: Unable to read resource file " + path, e);
    }
  }

  public static List<String> getLineByLineContent(@Nonnull String path) {
    checkNotNull(path);
    URL resourceUrl = getResourceUrl(path);
    try {
      return Files.readAllLines(
          Paths.get(resourceUrl.toURI()),
          StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Internal testing error: Unable to read resource file " + path, e);
    }
  }

  public static TemplateInstance useExampleInstance001() {
    String content = getStringContent("instances/example-001.jsonld");
    TemplateInstance templateInstance = new TemplateInstance(content);
    templateInstance.addKeywords("json",
        getLineByLineContent("instances/example-001_json-keywords.txt"));
    templateInstance.addKeywords("rdf",
        getLineByLineContent("instances/example-001_rdf-keywords.txt"));
    return templateInstance;
  }

  public static TemplateResource useTemplateResource(@Nonnull String path) {
    checkNotNull(path);
    String content = getStringContent(path);
    String expected = getStringExpected(path);
    return TemplateResource.create(content, expected);
  }

  private static String getStringExpected(String path) {
    String value = "true";
    String pathToExpectedResult = findDefaultExpectedResult(path);
    if (exists(pathToExpectedResult)) {
      value = getStringContent(pathToExpectedResult);
    }
    return value;
  }

  private static String findDefaultExpectedResult(@Nonnull String path) {
    int extensionPeriodPosition = path.lastIndexOf(".");
    return new StringBuilder(path)
        .replace(extensionPeriodPosition, path.length(), ".out")
        .toString();
  }

  private static boolean exists(@Nonnull String path) {
    try {
      URL resourceUrl = getResourceUrl(path);
      return Files.exists(Paths.get(resourceUrl.toURI()));
    } catch (Exception e) {
      return false;
    }
  }

  private static URL getResourceUrl(@Nonnull String path) {
    return TestResourcesUtils.class.getClassLoader().getResource(path);
  }
}
