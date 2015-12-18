package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateElementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.Result;
import utils.JsonUtils;

import javax.management.InstanceNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateElementServerController extends GenericElementServerController {
  private static Logger log = LoggerFactory.getLogger(TemplateElementServerController.class);

  private static TemplateElementService<String, JsonNode> templateElementService;
  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  static {
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<String>();
    FIELD_NAMES_SUMMARY_LIST.add("@id");
    FIELD_NAMES_SUMMARY_LIST.add("title");
    FIELD_NAMES_SUMMARY_LIST.add("properties.info.title");
    FIELD_NAMES_SUMMARY_LIST.add("properties.info.description");
  }

  public static void injectTemplateElementService(TemplateElementService<String, JsonNode> tes) {
    templateElementService = tes;
  }

  public static Result createTemplateElement() {
    try {
      JsonNode templateElement = request().body().asJson();
      JsonNode createdTemplateElement = templateElementService.createTemplateElementLinkedData(templateElement);
      // Remove autogenerated _id field to avoid exposing it
      createdTemplateElement = JsonUtils.removeField(createdTemplateElement, "_id");
      // Set Location header pointing to the newly created element
      String id = createdTemplateElement.get("@id").asText();
      String absoluteUrl = routes.TemplateElementServerController.findTemplateElement(id, false, false).absoluteURL
          (request());
      response().setHeader("Location", absoluteUrl);
      // Return created response
      return created(createdTemplateElement);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

  public static Result findAllTemplateElements(Integer limit, Integer offset, boolean summary) {
    try {
      Map<String, Object> r = new HashMap<>();
      List<JsonNode> elements = null;
      if (summary) {
        elements = templateElementService.findAllTemplateElements(limit, offset, FIELD_NAMES_SUMMARY_LIST,
            FieldNameInEx.INCLUDE);
        response().setHeader("Cedar-Field-Names", Json.toJson(FIELD_NAMES_SUMMARY_LIST).toString());
      } else {
        elements = templateElementService.findAllTemplateElements(limit, offset, FIELD_NAMES_EXCLUSION_LIST,
            FieldNameInEx.EXCLUDE);
      }
      long total = templateElementService.count();
      response().setHeader("Total-Count", String.valueOf(total));
      return ok(Json.toJson(elements));
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

  public static Result findTemplateElement(String templateElementId, boolean expanded, boolean validation) {
    try {
      JsonNode templateElement = templateElementService.findTemplateElementByLinkedDataId(templateElementId, expanded,
          validation);
      if (templateElement != null) {
        // Remove autogenerated _id field to avoid exposing it
        templateElement = JsonUtils.removeField(templateElement, "_id");
        return ok(templateElement);
      }
      return notFound();
    } catch (IllegalArgumentException e) {
      return badRequestWithError(e);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

  public static Result updateTemplateElement(String templateElementId) {
    JsonNode modifications = request().body().asJson();
    try {
      JsonNode updatedTemplateElement = templateElementService.updateTemplateElementByLinkedDataId(templateElementId,
          modifications);
      // Remove autogenerated _id field to avoid exposing it
      updatedTemplateElement = JsonUtils.removeField(updatedTemplateElement, "_id");
      return ok(updatedTemplateElement);
    } catch (IllegalArgumentException e) {
      return badRequestWithError(e);
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

  public static Result deleteTemplateElement(String templateElementId) {
    try {
      templateElementService.deleteTemplateElementByLinkedDataId(templateElementId);
      return noContent();
    } catch (IllegalArgumentException e) {
      return badRequestWithError(e);
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

}
