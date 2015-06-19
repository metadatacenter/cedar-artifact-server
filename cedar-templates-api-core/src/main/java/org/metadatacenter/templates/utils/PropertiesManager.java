package org.metadatacenter.templates.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Properties;

public class PropertiesManager
{
  private static String configFile = "src/main/config/config.properties";
  private static Properties properties;

  static {
    properties = new Properties();
    try {
      System.out.println("------------------ User dir: " + System.getProperty("user.dir"));
      properties.load(new FileInputStream(configFile));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String getProperty(String propertyName)
  {
    return properties.getProperty(propertyName);
  }

  public static double getPropertyDouble(String propertyName)
  {
    NumberFormat nf = NumberFormat.getInstance(Locale.US);
    double result = -1;
    try {
      result = nf.parse(properties.getProperty(propertyName)).doubleValue();
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return result;
  }

  public static int getPropertyInt(String propertyName)
  {
    return Integer.parseInt(properties.getProperty(propertyName));
  }

  /**
   * Test code **
   */
//  public static void main(String[] args)
//  {
//    System.out.println(PropertiesManager.getProperty("mongodb.db"));
//  }
}