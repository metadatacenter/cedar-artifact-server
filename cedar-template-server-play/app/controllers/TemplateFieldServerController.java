package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.constant.ConfigConstants;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.provenance.ProvenanceInfo;
import org.metadatacenter.provenance.ProvenanceUtil;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.util.http.LinkHeaderUtil;
import org.metadatacenter.util.http.UrlUtil;
import org.metadatacenter.util.mongo.MongoUtils;
import play.Logger;
import play.libs.Json;
import play.mvc.Result;

import javax.management.InstanceNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateFieldServerController extends AbstractTemplateServerController {

  private static TemplateFieldService<String, JsonNode> templateFieldService;
  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  static {
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(config.getStringList(ConfigConstants.FIELD_NAMES_SUMMARY_TEMPLATE_FIELD));
  }

  public static void injectTemplateFieldService(TemplateFieldService<String, JsonNode> tfs) {
    templateFieldService = tfs;
  }


  public static Result createTemplateField() {
    try {
      IAuthRequest authRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.mustHavePermission(authRequest, CedarPermission.TEMPLATE_FIELD_CREATE);
      JsonNode templateField = request().body().asJson();

      ProvenanceInfo pi = buildProvenanceInfo(authRequest);
      ProvenanceUtil.addProvenanceInfo(templateField, pi);
      JsonNode createdTemplateField = templateFieldService.createTemplateField(templateField);
      MongoUtils.removeIdField(createdTemplateField);

      // Set Location header pointing to the newly created element
      String id = createdTemplateField.get("@id").asText();
      String absoluteUrl = routes.TemplateFieldServerController.findTemplateField(id).absoluteURL(request());

      response().setHeader(HttpConstants.HTTP_HEADER_LOCATION, absoluteUrl);
      // Return created response
      return created(createdTemplateField);
    } catch (CedarAccessException e) {
      Logger.error("Access Error while creating the template field", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while creating the template field", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result findAllTemplateFields(Integer limit, Integer offset, boolean summary, String fieldNames) {
    try {
      Authorization.mustHavePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_FIELD_READ);
      limit = ensureLimit(limit);
      checkPagingParameters(limit, offset);
      List<String> fieldNameList = getAndCheckFieldNames(fieldNames, summary);
      Map<String, Object> r = new HashMap<>();
      List<JsonNode> elements = null;
      if (summary) {
        elements = templateFieldService.findAllTemplateFields(limit, offset, FIELD_NAMES_SUMMARY_LIST,
            FieldNameInEx.INCLUDE);
      } else if (fieldNameList != null) {
        elements = templateFieldService.findAllTemplateFields(limit, offset, fieldNameList, FieldNameInEx.INCLUDE);
      } else {
        elements = templateFieldService.findAllTemplateFields(limit, offset, FIELD_NAMES_EXCLUSION_LIST,
            FieldNameInEx.EXCLUDE);
      }
      long total = templateFieldService.count();
      response().setHeader(CustomHttpConstants.HEADER_TOTAL_COUNT, String.valueOf(total));
      checkPagingParametersAgainstTotal(offset, total);
      String absoluteUrl = routes.TemplateFieldServerController.findAllTemplateFields(0, 0, false, null).absoluteURL
          (request());
      absoluteUrl = UrlUtil.trimUrlParameters(absoluteUrl);
      String linkHeader = LinkHeaderUtil.getPagingLinkHeader(absoluteUrl, total, limit, offset);
      if (!linkHeader.isEmpty()) {
        response().setHeader(HttpConstants.HTTP_HEADER_LINK, linkHeader);
      }
      return ok(Json.toJson(elements));
    } catch (CedarAccessException e) {
      Logger.error("Access Error while reading the template fields", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while reading the template fields", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result findTemplateField(String templateFieldId) {
    try {
      Authorization.mustHavePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_FIELD_READ);
      JsonNode templateField = templateFieldService.findTemplateField(templateFieldId);
      if (templateField != null) {
        // Remove autogenerated _id field to avoid exposing it
        MongoUtils.removeIdField(templateField);
        return ok(templateField);
      }
      Logger.error("Template field not found:(" + templateFieldId + ")");
      return notFound();
    } catch (IllegalArgumentException e) {
      Logger.error("Illegal Argument while reading the template field", e);
      return badRequestWithError(e);
    } catch (CedarAccessException e) {
      Logger.error("Access Error while reading the template field", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while reading the template field", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result updateTemplateField(String templateFieldId) {
    try {
      IAuthRequest authRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.mustHavePermission(authRequest, CedarPermission.TEMPLATE_FIELD_UPDATE);
      JsonNode modifications = request().body().asJson();

      ProvenanceInfo pi = buildProvenanceInfo(authRequest);
      ProvenanceUtil.patchProvenanceInfo(modifications, pi);
      JsonNode updatedTemplateElement = templateFieldService.updateTemplateField(templateFieldId, modifications);
      // Remove autogenerated _id field to avoid exposing it
      MongoUtils.removeIdField(updatedTemplateElement);
      return ok(updatedTemplateElement);
    } catch (IllegalArgumentException e) {
      Logger.error("Illegal Argument while reading the template field", e);
      return badRequestWithError(e);
    } catch (InstanceNotFoundException e) {
      Logger.error("Template field not found for update:(" + templateFieldId + ")");
      return notFound();
    } catch (CedarAccessException e) {
      Logger.error("Access Error while reading the template field", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while updating the template field", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result deleteTemplateField(String templateFieldId) {
    try {
      Authorization.mustHavePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_FIELD_DELETE);
      templateFieldService.deleteTemplateField(templateFieldId);
      return noContent();
    } catch (IllegalArgumentException e) {
      Logger.error("Illegal Argument while deleting the template field", e);
      return badRequestWithError(e);
    } catch (InstanceNotFoundException e) {
      Logger.error("Template field not found while deleting:(" + templateFieldId + ")");
      return notFound();
    } catch (CedarAccessException e) {
      Logger.error("Access Error while deleting the template field", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while deleting the template field", e);
      return internalServerErrorWithError(e);
    }
  }


}
