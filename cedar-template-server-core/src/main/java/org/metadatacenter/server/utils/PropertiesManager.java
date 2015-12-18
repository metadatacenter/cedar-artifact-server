package org.metadatacenter.server.utils;

import checkers.nullness.quals.NonNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

public class PropertiesManager {
  private static final String configFile = "src/main/config/config.properties";

  @NonNull
  private static final Properties properties;

  static {
    properties = new Properties();
    try {
      // TODO LOG ("------------------ User dir: " + System.getProperty("user.dir"));
      properties.load(new FileInputStream(configFile));
    } catch (FileNotFoundException e) {
      // TODO LOG
    } catch (IOException e) {
      // TODO LOG
    }
  }

  public static Optional<String> getProperty(@NonNull String propertyName) {
    if (properties.getProperty(propertyName) != null) {
      return Optional.of(properties.getProperty(propertyName));
    } else {
      return Optional.empty();
    }
  }

  public static Optional<Double> getPropertyDouble(@NonNull String propertyName) {
    NumberFormat nf = NumberFormat.getInstance(Locale.US);
    try {
      return Optional.of(nf.parse(properties.getProperty(propertyName)).doubleValue());
    } catch (ParseException e) {
      // TODO Log
      return Optional.empty();
    }
  }

  public static Optional<Integer> getPropertyInt(@NonNull String propertyName) {
    try {
      return Optional.of(Integer.parseInt(properties.getProperty(propertyName)));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}