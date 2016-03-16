package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.constant.ConfigConstants;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.http.LinkHeaderUtil;
import org.metadatacenter.util.http.UrlUtil;
import org.metadatacenter.util.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.Result;

import javax.management.InstanceNotFoundException;
import java.rmi.AccessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateServerController extends AbstractTemplateServerController {
  private static Logger log = LoggerFactory.getLogger(TemplateServerController.class);

  private static TemplateService<String, JsonNode> templateService;
  private static TemplateFieldService<String, JsonNode> templateFieldService;
  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  static {
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(config.getStringList(ConfigConstants.FIELD_NAMES_SUMMARY_TEMPLATE));
  }

  public static void injectTemplateService(TemplateService<String, JsonNode> ts) {
    templateService = ts;
  }

  public static void injectTemplateFieldService(TemplateFieldService<String, JsonNode> tfs) {
    templateFieldService = tfs;
  }

  public static Result createTemplate() {
    try {
      Authorization.mustHavePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_CREATE);
      JsonNode template = request().body().asJson();
      templateFieldService.saveNewFieldsAndReplaceIds(template);
      JsonNode createdTemplate = templateService.createTemplate(template);
      // Remove autogenerated _id field to avoid exposing it
      createdTemplate = JsonUtils.removeField(createdTemplate, "_id");
      // Set Location header pointing to the newly created template
      String id = createdTemplate.get("@id").asText();
      String absoluteUrl = routes.TemplateServerController.findTemplate(id).absoluteURL(request());
      response().setHeader(HttpConstants.HTTP_HEADER_LOCATION, absoluteUrl);
      // Return created response
      return created(createdTemplate);
    } catch (IllegalArgumentException e) {
      return badRequestWithError(e);
    } catch (AccessException e) {
      return forbiddenWithError(e);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

  public static Result findTemplate(String templateId) {
    try {
      Authorization.mustHavePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_READ);
      JsonNode template = templateService.findTemplate(templateId);
      if (template != null) {
        // Remove autogenerated _id field to avoid exposing it
        template = JsonUtils.removeField(template, "_id");
        return ok(template);
      }
      return notFound();
    } catch (IllegalArgumentException e) {
      return badRequestWithError(e);
    } catch (AccessException e) {
      return forbiddenWithError(e);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

  public static Result findAllTemplates(Integer limit, Integer offset, boolean summary, String fieldNames) {
    try {
      Authorization.mustHavePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_READ);
      limit = ensureLimit(limit);
      checkPagingParameters(limit, offset);
      List<String> fieldNameList = getAndCheckFieldNames(fieldNames, summary);
      Map<String, Object> r = new HashMap<>();
      List<JsonNode> templates = null;
      if (summary) {
        templates = templateService.findAllTemplates(limit, offset, FIELD_NAMES_SUMMARY_LIST, FieldNameInEx.INCLUDE);
      } else if (fieldNameList != null) {
        templates = templateService.findAllTemplates(limit, offset, fieldNameList, FieldNameInEx.INCLUDE);
      } else {
        templates = templateService.findAllTemplates(limit, offset, FIELD_NAMES_EXCLUSION_LIST, FieldNameInEx.EXCLUDE);
      }
      long total = templateService.count();
      response().setHeader(CustomHttpConstants.HEADER_TOTAL_COUNT, String.valueOf(total));
      checkPagingParametersAgainstTotal(offset, total);
      String absoluteUrl = routes.TemplateServerController.findAllTemplates(0, 0, false, null).absoluteURL(request());
      absoluteUrl = UrlUtil.trimUrlParameters(absoluteUrl);
      String linkHeader = LinkHeaderUtil.getPagingLinkHeader(absoluteUrl, total, limit, offset);
      if (!linkHeader.isEmpty()) {
        response().setHeader(HttpConstants.HTTP_HEADER_LINK, linkHeader);
      }
      return ok(Json.toJson(templates));
    } catch (AccessException e) {
      return forbiddenWithError(e);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

  public static Result updateTemplate(String templateId) {
    try {
      Authorization.mustHavePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_UPDATE);
      JsonNode modifications = request().body().asJson();
      templateFieldService.saveNewFieldsAndReplaceIds(modifications);
      JsonNode updatedTemplate = templateService.updateTemplate(templateId, modifications);
      // Remove autogenerated _id field to avoid exposing it
      updatedTemplate = JsonUtils.removeField(updatedTemplate, "_id");
      return ok(updatedTemplate);
    } catch (IllegalArgumentException e) {
      return badRequestWithError(e);
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (AccessException e) {
      return forbiddenWithError(e);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

  public static Result deleteTemplate(String templateId) {
    try {
      Authorization.mustHavePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_DELETE);
      templateService.deleteTemplate(templateId);
      return noContent();
    } catch (IllegalArgumentException e) {
      return badRequestWithError(e);
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (AccessException e) {
      return forbiddenWithError(e);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

}
