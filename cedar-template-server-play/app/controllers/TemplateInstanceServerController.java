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
import org.metadatacenter.server.service.TemplateInstanceService;
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


public class TemplateInstanceServerController extends AbstractTemplateServerController {

  private static TemplateInstanceService<String, JsonNode> templateInstanceService;
  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  static {
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(cedarConfig.getTemplateRESTAPISummaries().getInstance().getFields());
  }

  public static void injectTemplateInstanceService(TemplateInstanceService<String, JsonNode> tis) {
    templateInstanceService = tis;
  }

  public static Result createTemplateInstance(F.Option<Boolean> importMode) {
    try {
      IAuthRequest authRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(authRequest, CedarPermission.TEMPLATE_INSTANCE_CREATE);

      JsonNode templateInstance = request().body().asJson();
      if (templateInstance == null) {
        Logger.error("Expecting Json data");
        return badRequest("Expecting Json data");
      }

      ProvenanceInfo pi = ProvenanceUtil.build(cedarConfig, authRequest);
      checkImportModeSetProvenanceAndId(CedarNodeType.INSTANCE, templateInstance, pi, importMode);

      JsonNode createdTemplateInstance = templateInstanceService.createTemplateInstance(templateInstance);
      MongoUtils.removeIdField(createdTemplateInstance);

      // Set Location header pointing to the newly created instance
      String id = createdTemplateInstance.get("@id").asText();
      String absoluteUrl = routes.TemplateInstanceServerController.findTemplateInstance(id).absoluteURL(request());

      response().setHeader(HttpConstants.HTTP_HEADER_LOCATION, absoluteUrl);
      // Return created response
      return created(createdTemplateInstance);
    } catch (IllegalArgumentException e) {
      Logger.error("Illegal Argument while creating the template instance", e);
      return badRequestWithError(e);
    } catch (CedarUserNotFoundException e) {
      Logger.error("User not found", e);
      return unauthorizedWithError(e);
    } catch (AccessException e) {
      Logger.error("Access Error while creating the template instance", e);
      return forbiddenWithError(e);
    } catch (AuthorizationTypeNotFoundException e) {
      Logger.error("Authorization header not found", e);
      return badRequestWithError(e);
    } catch (Exception e) {
      Logger.error("Error while creating the template instance", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result findAllTemplateInstances(Integer limit, Integer offset, boolean summary, String fieldNames) {
    try {
      Authorization.getUserAndEnsurePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_INSTANCE_READ);
      limit = ensureLimit(limit);
      checkPagingParameters(limit, offset);
      List<String> fieldNameList = getAndCheckFieldNames(fieldNames, summary);
      Map<String, Object> r = new HashMap<>();
      List<JsonNode> instances = null;
      if (summary) {
        instances = templateInstanceService.findAllTemplateInstances(limit, offset, FIELD_NAMES_SUMMARY_LIST,
            FieldNameInEx.INCLUDE);
      } else if (fieldNameList != null) {
        instances = templateInstanceService.findAllTemplateInstances(limit, offset, fieldNameList, FieldNameInEx
            .INCLUDE);
      } else {
        instances = templateInstanceService.findAllTemplateInstances(limit, offset, FIELD_NAMES_EXCLUSION_LIST,
            FieldNameInEx.EXCLUDE);
      }
      long total = templateInstanceService.count();
      response().setHeader(CustomHttpConstants.HEADER_TOTAL_COUNT, String.valueOf(total));
      checkPagingParametersAgainstTotal(offset, total);
      String absoluteUrl = routes.TemplateInstanceServerController.findAllTemplateInstances(0, 0, false, null)
          .absoluteURL
              (request());
      absoluteUrl = UrlUtil.trimUrlParameters(absoluteUrl);
      String linkHeader = LinkHeaderUtil.getPagingLinkHeader(absoluteUrl, total, limit, offset);
      if (!linkHeader.isEmpty()) {
        response().setHeader(HttpConstants.HTTP_HEADER_LINK, linkHeader);
      }
      return ok(Json.toJson(instances));
    } catch (CedarAccessException e) {
      Logger.error("Access Error while reading the template instances", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while reading the template instances", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result findTemplateInstance(String templateInstanceId) {
    try {
      Authorization.getUserAndEnsurePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_INSTANCE_READ);
      JsonNode templateInstance = templateInstanceService.findTemplateInstance(templateInstanceId);
      if (templateInstance != null) {
        // Remove autogenerated _id field to avoid exposing it
        MongoUtils.removeIdField(templateInstance);
        return ok(templateInstance);
      }
      Logger.error("Template instance not found:(" + templateInstanceId + ")");
      return notFound();
    } catch (IllegalArgumentException e) {
      Logger.error("Illegal Argument while reading the template instance", e);
      return badRequestWithError(e);
    } catch (CedarAccessException e) {
      Logger.error("Access Error while reading the template instance", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while reading the template instance", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result updateTemplateInstance(String templateInstanceId) {
    try {
      IAuthRequest authRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(authRequest, CedarPermission.TEMPLATE_INSTANCE_UPDATE);
      JsonNode modifications = request().body().asJson();
      ProvenanceInfo pi = ProvenanceUtil.build(cedarConfig, authRequest);
      ProvenanceUtil.patchProvenanceInfo(modifications, pi);
      JsonNode updatedTemplateInstance = templateInstanceService.updateTemplateInstance(templateInstanceId,
          modifications);
      return noContent();
    } catch (IllegalArgumentException e) {
      Logger.error("Illegal Argument while reading the template instance", e);
      return badRequestWithError(e);
    } catch (InstanceNotFoundException e) {
      Logger.error("Template instance not found for update:(" + templateInstanceId + ")");
      return notFound();
    } catch (CedarAccessException e) {
      Logger.error("Access Error while reading the template instance", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while updating the template instance", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result deleteTemplateInstance(String templateInstanceId) {
    try {
      Authorization.getUserAndEnsurePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_INSTANCE_DELETE);
      templateInstanceService.deleteTemplateInstance(templateInstanceId);
      return noContent();
    } catch (IllegalArgumentException e) {
      Logger.error("Illegal Argument while deleting the template instance", e);
      return badRequestWithError(e);
    } catch (InstanceNotFoundException e) {
      Logger.error("Template instance not found while deleting:(" + templateInstanceId + ")");
      return notFound();
    } catch (CedarAccessException e) {
      Logger.error("Access Error while deleting the template instance", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while deleting the template instance", e);
      return internalServerErrorWithError(e);
    }
  }

}
