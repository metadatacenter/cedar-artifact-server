package org.metadatacenter.cedar.template.resources;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jersey.repackaged.com.google.common.base.Preconditions.checkNotNull;

public class TextFormatter {

  private static final Pattern pattern = Pattern.compile("\\{(.+?)\\}");

  public static String format(@Nonnull String formatString, @Nonnull String[] args) {
    checkNotNull(formatString);
    checkNotNull(args);
    Matcher matcher = pattern.matcher(formatString);
    StringBuffer buffer = new StringBuffer();
    try {
      matcher.reset();
      while (matcher.find()) {
        String token = matcher.group(1);
        String value = getProperty(args, token);
        matcher.appendReplacement(buffer, value);
      }
      matcher.appendTail(buffer);
    } catch (Exception e) {
      throw new RuntimeException("Error formatting text using the given args array", e);
    }
    return buffer.toString();
  }

  private static String getProperty(String[] args, String token) {
    int pos = Integer.parseInt(token);
    return args[pos];
  }
}
