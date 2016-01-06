package controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Configuration;
import play.Play;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.metadatacenter.server.Constants.*;

public class GenericElementServerController extends Controller {

  protected static List<String> FIELD_NAMES_EXCLUSION_LIST;
  protected static Configuration config;

  static {
    config = Play.application().configuration();
    FIELD_NAMES_EXCLUSION_LIST = new ArrayList<>();
    FIELD_NAMES_EXCLUSION_LIST.addAll(config.getStringList(FIELD_NAMES_LIST_EXCLUSION));
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

  protected static Integer ensureLimit(Integer limit) {
    return limit == null ? config.getInt(PAGINATION_DEFAULT_PAGE_SIZE) : limit;
  }

  protected static void checkPagingParameters(Integer limit, Integer offset) {
    // check offset
    if (offset < 0) {
      throw new IllegalArgumentException("Parameter 'offset' must be positive!");
    }
    // check limit
    if (limit <= 0) {
      throw new IllegalArgumentException("Parameter 'limit' must be greater than zero!");
    }
    int maxPageSize = config.getInt(PAGINATION_MAX_PAGE_SIZE);
    if (limit > maxPageSize) {
      throw new IllegalArgumentException("Parameter 'limit' must be at most " + maxPageSize + "!");
    }
  }

  protected static void checkPagingParametersAgainstTotal(Integer offset, long total) {
    if (offset > total - 1) {
      throw new IllegalArgumentException("Parameter 'offset' must be smaller than the total count of objects, which " +
          "is " + total + "!");
    }
  }


  protected static List<String> getAndCheckFieldNames(String fieldNames, boolean summary) {
    if (fieldNames != null) {
      if (summary == true) {
        throw new IllegalArgumentException("It is no allowed to specify parameter 'fieldNames' and also set 'summary'" +
            " to true!");
      } else if (fieldNames.length() > 0) {
        return Arrays.asList(fieldNames.split(","));
      }
    }
    return null;
  }
}
