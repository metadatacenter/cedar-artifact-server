package controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.List;

public class GenericElementServerController extends Controller {

  protected static List<String> FIELD_NAMES_EXCLUSION_LIST;

  static {
    FIELD_NAMES_EXCLUSION_LIST = new ArrayList<>();
    FIELD_NAMES_EXCLUSION_LIST.add("_id");
  }


  protected static ObjectNode generateErrorDescription(Throwable t) {
    ObjectNode errorDescription = JsonNodeFactory.instance.objectNode();
    errorDescription.put("errorType", "exception");
    errorDescription.put("message", t.getMessage());
    errorDescription.put("localizedMessage", t.getLocalizedMessage());
    errorDescription.put("string", t.toString());
    ArrayNode jsonST = errorDescription.putArray("stackTrace");
    for (StackTraceElement ste : t.getStackTrace()) {
      jsonST.add(ste.toString());
    }
    return errorDescription;
  }

  protected static Result internalServerErrorWithError(Throwable t) {
    return internalServerError(generateErrorDescription(t));
  }

  protected static Result badRequestWithError(Throwable t) {
    return badRequest(generateErrorDescription(t));
  }


}
