package org.metadatacenter.cedar.template.resources;

import javax.annotation.Nonnull;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
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

  public static TestResource useResource(@Nonnull String path, @Nonnull String pathToExpectedOutput) {
    checkNotNull(path);
    checkNotNull(pathToExpectedOutput);
    String content = getStringContent(path);
    String expected = getStringContent(pathToExpectedOutput);
    return TestResource.create(content, expected);
  }

  public static TestResource useResource(@Nonnull String path) {
    String pathToExpectedResult = findDefaultPathToExpectedOutput(path);
    return useResource(path, pathToExpectedResult);
  }

  public static TestResource useFormattedResource(@Nonnull String path, @Nonnull String[] args, @Nonnull String pathToExpectedOutput) {
    checkNotNull(path);
    checkNotNull(args);
    checkNotNull(pathToExpectedOutput);
    String content = TextFormatter.format(getStringContent(path), args);
    String expected = getStringContent(pathToExpectedOutput);
    return TestResource.create(content, expected);
  }

  public static TestResource useFormattedResource(@Nonnull String path, @Nonnull String[] args) {
    String pathToExpectedResult = findDefaultPathToExpectedOutput(path);
    return useFormattedResource(path, args, pathToExpectedResult);
  }

  private static String getStringExpected(String pathToExpectedResult) {
    String value = "true";
    if (exists(pathToExpectedResult)) {
      value = getStringContent(pathToExpectedResult);
    }
    return value;
  }

  private static String findDefaultPathToExpectedOutput(@Nonnull String path) {
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
