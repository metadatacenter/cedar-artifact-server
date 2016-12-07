package org.metadatacenter.cedar.template.core;

import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public abstract class CedarResponse {

  private static CedarResponseBuilder newResponseBuilder() {
    return new CedarResponseBuilder();
  }

  public static class CedarResponseBuilder {

    private Map<String, Object> parameters;
    private CedarErrorKey errorKey;
    private String errorMessage;
    private Exception exception;
    private Response.Status status;
    private Object entity;

    protected CedarResponseBuilder() {
      this.parameters = new HashMap<>();
    }

    public Response build() {
      Response.ResponseBuilder responseBuilder = Response.noContent();
      responseBuilder.status(status);

      if (entity != null) {
        responseBuilder.entity(entity);
      } else {
        Map<String, Object> r = new HashMap<>(parameters);
        r.put("errorKey", errorKey);
        r.put("errorMessage", errorMessage);

        if (exception != null) {
          StackTraceElement[] stackTrace = exception.getStackTrace();
          if (stackTrace != null) {
            r.put("stackTrace", stackTrace);
          }
        }

        if (!r.isEmpty()) {
          responseBuilder.entity(parameters);
        }
      }
      return responseBuilder.build();
    }

    public CedarResponseBuilder status(Response.Status status) {
      this.status = status;
      return this;
    }

    public CedarResponseBuilder entity(Object entity) {
      this.entity = entity;
      return this;
    }

    /*public CedarResponseBuilder header(String name, Object value) {
      return this;
    }*/

    public CedarResponseBuilder id(Object id) {
      return this.parameter("id", id);
    }

    public CedarResponseBuilder parameter(String key, Object value) {
      this.parameters.put(key, value);
      return this;
    }

    public CedarResponseBuilder errorKey(CedarErrorKey errorKey) {
      this.errorKey = errorKey;
      return this;
    }

    public CedarResponseBuilder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public CedarResponseBuilder exception(Exception exception) {
      this.exception = exception;
      return this;
    }
  }

  public static CedarResponseBuilder internalServerError() {
    return newResponseBuilder().status(Response.Status.INTERNAL_SERVER_ERROR);
  }

  public static CedarResponseBuilder noContent() {
    return newResponseBuilder().status(Response.Status.NO_CONTENT);
  }

  public static CedarResponseBuilder notFound() {
    return newResponseBuilder().status(Response.Status.NOT_FOUND);
  }

  protected static CedarResponseBuilder status(Response.Status status) {
    return newResponseBuilder().status(status);
  }


}
