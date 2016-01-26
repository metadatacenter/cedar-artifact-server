package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.server.Constants;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.util.JsonObjectUtils;
import org.metadatacenter.util.LinkHeaderUtil;
import org.metadatacenter.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.Result;

import javax.management.InstanceNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.metadatacenter.server.Constants.FIELD_NAMES_SUMMARY_TEMPLATE_ELEMENT;
import static org.metadatacenter.server.Constants.HTTP_HEADER_LINK;

public class TemplateElementServerController extends GenericElementServerController {
  private static Logger log = LoggerFactory.getLogger(TemplateElementServerController.class);

  private static TemplateElementService<String, JsonNode> templateElementService;
  private static TemplateFieldService<String, JsonNode> templateFieldService;
  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  static {
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(config.getStringList(FIELD_NAMES_SUMMARY_TEMPLATE_ELEMENT));
  }

  public static void injectTemplateElementService(TemplateElementService<String, JsonNode> tes) {
    templateElementService = tes;
  }

  public static void injectTemplateFieldService(TemplateFieldService<String, JsonNode> tfs) {
    templateFieldService = tfs;
  }

  public static Result createTemplateElement() {
    try {
      JsonNode templateElement = request().body().asJson();
      templateFieldService.saveNewFieldsAndReplaceIds(templateElement);
      JsonNode createdTemplateElement = templateElementService.createTemplateElementLinkedData(templateElement);
      // Remove autogenerated _id field to avoid exposing it
      createdTemplateElement = JsonObjectUtils.removeField(createdTemplateElement, "_id");
      // Set Location header pointing to the newly created element
      String id = createdTemplateElement.get("@id").asText();
      String absoluteUrl = routes.TemplateElementServerController.findTemplateElement(id, false, false).absoluteURL
          (request());
      response().setHeader(Constants.HTTP_HEADER_LOCATION, absoluteUrl);
      // Return created response
      return created(createdTemplateElement);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

  public static Result findAllTemplateElements(Integer limit, Integer offset, boolean summary, String fieldNames) {
    try {
      limit = ensureLimit(limit);
      checkPagingParameters(limit, offset);
      List<String> fieldNameList = getAndCheckFieldNames(fieldNames, summary);
      Map<String, Object> r = new HashMap<>();
      List<JsonNode> elements = null;
      if (summary) {
        elements = templateElementService.findAllTemplateElements(limit, offset, FIELD_NAMES_SUMMARY_LIST,
            FieldNameInEx.INCLUDE);
      } else if (fieldNameList != null) {
        elements = templateElementService.findAllTemplateElements(limit, offset, fieldNameList, FieldNameInEx.INCLUDE);
      } else {
        elements = templateElementService.findAllTemplateElements(limit, offset, FIELD_NAMES_EXCLUSION_LIST,
            FieldNameInEx.EXCLUDE);
      }
      long total = templateElementService.count();
      response().setHeader(Constants.HTTP_CUSTOM_HEADER_TOTAL_COUNT, String.valueOf(total));
      checkPagingParametersAgainstTotal(offset, total);
      String absoluteUrl = routes.TemplateElementServerController.findAllTemplateElements(0, 0, false, null).absoluteURL
          (request());
      absoluteUrl = Utils.trimUrlParameters(absoluteUrl);
      String linkHeader = LinkHeaderUtil.getPagingLinkHeader(absoluteUrl, total, limit, offset);
      if (!linkHeader.isEmpty()) {
        response().setHeader(HTTP_HEADER_LINK, linkHeader);
      }
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
        templateElement = JsonObjectUtils.removeField(templateElement, "_id");
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
      updatedTemplateElement = JsonObjectUtils.removeField(updatedTemplateElement, "_id");
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
