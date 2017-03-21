package org.metadatacenter.cedar.template.resources;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class TemplateInstance {

  private final String content;
  private final Map<String, Collection<String>> keywordsMap = Maps.newHashMap();

  public TemplateInstance(@Nonnull String content) {
    this.content = checkNotNull(content);
  }

  public String getContent() {
    return content;
  }

  public void addKeywords(@Nonnull String key, @Nonnull Collection<String> keywords) {
    keywordsMap.put(key, keywords);
  }

  public Set<String> getKeywords(String key) {
    return Sets.newHashSet(keywordsMap.get(key));
  }
}
