package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.AuthorizationTypeNotFoundException;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.exception.CedarUserNotFoundException;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.util.http.LinkHeaderUtil;
import org.metadatacenter.util.http.UrlUtil;
import org.metadatacenter.util.mongo.MongoUtils;
import org.metadatacenter.util.provenance.ProvenanceUtil;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.mvc.Result;

import javax.management.InstanceNotFoundException;
import java.rmi.AccessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateFieldServerController extends AbstractTemplateServerController {

  private static TemplateFieldService<String, JsonNode> templateFieldService;
  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  static {
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(cedarConfig.getTemplateRESTAPISummaries().getField().getFields());
  }

  public static void injectTemplateFieldService(TemplateFieldService<String, JsonNode> tfs) {
    templateFieldService = tfs;
  }


  public static Result createTemplateField(F.Option<Boolean> importMode) {
    try {
      IAuthRequest authRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(authRequest, CedarPermission.TEMPLATE_FIELD_CREATE);

      JsonNode templateField = request().body().asJson();
      if (templateField == null) {
        Logger.error("Expecting Json data");
        return badRequest("Expecting Json data");
      }

      ProvenanceInfo pi = ProvenanceUtil.build(cedarConfig, authRequest);
      checkImportModeSetProvenanceAndId(CedarNodeType.FIELD, templateField, pi, importMode);

      JsonNode createdTemplateField = templateFieldService.createTemplateField(templateField);
      MongoUtils.removeIdField(createdTemplateField);

      // Set Location header pointing to the newly created element
      String id = createdTemplateField.get("@id").asText();
      String absoluteUrl = routes.TemplateFieldServerController.findTemplateField(id).absoluteURL(request());

      response().setHeader(HttpConstants.HTTP_HEADER_LOCATION, absoluteUrl);
      // Return created response
      return created(createdTemplateField);
    } catch (IllegalArgumentException e) {
      Logger.error("Illegal Argument while creating the template field", e);
      return badRequestWithError(e);
    } catch (CedarUserNotFoundException e) {
      Logger.error("User not found", e);
      return unauthorizedWithError(e);
    } catch (AccessException e) {
      Logger.error("Access Error while creating the template field", e);
      return forbiddenWithError(e);
    } catch (AuthorizationTypeNotFoundException e) {
      Logger.error("Authorization header not found", e);
      return badRequestWithError(e);
    } catch (Exception e) {
      Logger.error("Error while creating the template field", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result findAllTemplateFields(Integer limit, Integer offset, boolean summary, String fieldNames) {
    try {
      Authorization.getUserAndEnsurePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
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
      Authorization.getUserAndEnsurePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
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
      Authorization.getUserAndEnsurePermission(authRequest, CedarPermission.TEMPLATE_FIELD_UPDATE);
      JsonNode modifications = request().body().asJson();

      ProvenanceInfo pi = ProvenanceUtil.build(cedarConfig, authRequest);
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
      Authorization.getUserAndEnsurePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
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
