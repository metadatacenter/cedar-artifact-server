package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.StringWriter;

public final class Utils {

  private Utils() {

  }

  public static String prettyPrint(Object o) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    StringWriter sw = new StringWriter();
    mapper.writeValue(sw, o);
    return sw.toString();
  }

  public static String trimUrlParameters(String url) {
    if (url != null) {
      int p = url.indexOf('?');
      if (p > -1) {
        return url.substring(0, p - 1);
      }
    }
    return url;
  }
}
