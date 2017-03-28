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
    URL resourceUrl = TestResourcesUtils.class.getClassLoader().getResource(path);
    try {
      byte[] bytes = Files.readAllBytes(Paths.get(resourceUrl.toURI()));
      return new String(bytes, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Internal testing error: Unable to read resource file " + path, e);
    }
  }

  public static List<String> getLineByLineContent(@Nonnull String path) {
    checkNotNull(path);
    URL resourceUrl = TestResourcesUtils.class.getClassLoader().getResource(path);
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
}
