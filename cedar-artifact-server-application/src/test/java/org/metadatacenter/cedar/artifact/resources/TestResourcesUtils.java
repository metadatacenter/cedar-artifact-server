package org.metadatacenter.cedar.artifact.resources;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class TestResourcesUtils {

  public static String getStringContent(String path) {
    checkNotNull(path);
    URL resourceUrl = getResourceUrl(path);
    try {
      byte[] bytes = Files.readAllBytes(Paths.get(resourceUrl.toURI()));
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Internal testing error: Unable to read artifact file " + path, e);
    }
  }

  public static List<String> getLineByLineContent(String path) {
    checkNotNull(path);
    URL resourceUrl = getResourceUrl(path);
    try {
      return Files.readAllLines(
          Paths.get(resourceUrl.toURI()),
          StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Internal testing error: Unable to read artifact file " + path, e);
    }
  }

  private static URL getResourceUrl(String path) {
    return TestResourcesUtils.class.getClassLoader().getResource(path);
  }
}
