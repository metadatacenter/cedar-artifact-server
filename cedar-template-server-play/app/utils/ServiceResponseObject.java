package utils;

import java.util.HashMap;
import java.util.Map;

public class ServiceResponseObject {

  private Map<String, Object> m;
  private Map<String, Object> request;
  private Map<String, Object> response;
  private Map<String, Object> extra;

  private ServiceResponseObject() {
    m = new HashMap<>();
    request = new HashMap<>();
    response = new HashMap<>();
    extra = new HashMap<>();
    m.put("request", request);
    m.put("response", response);
    m.put("extra", extra);
  }

  public static ServiceResponseObject build() {
    return new ServiceResponseObject();
  }

  public ServiceResponseObject request(String key, Object value) {
    request.put(key, value);
    return this;
  }

  public ServiceResponseObject response(String key, Object value) {
    response.put(key, value);
    return this;
  }

  public ServiceResponseObject data(Object data) {
    m.put("data", data);
    return this;
  }

  public Map<String, Object> get() {
    return m;
  }

  public ServiceResponseObject extra(String key, Object value) {
    extra.put(key, value);
    return this;
  }
}
