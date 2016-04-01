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
import org.metadatacenter.server.service.TemplateInstanceService;
import org.metadatacenter.util.http.LinkHeaderUtil;
import org.metadatacenter.util.http.UrlUtil;
import org.metadatacenter.util.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.mvc.Result;

import javax.management.InstanceNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TemplateInstanceServerController extends AbstractTemplateServerController {
  private static Logger log = LoggerFactory.getLogger(TemplateInstanceServerController.class);

  private static TemplateInstanceService<String, JsonNode> templateInstanceService;
  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  static {
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(config.getStringList(ConfigConstants.FIELD_NAMES_SUMMARY_TEMPLATE_INSTANCE));
  }

  public static void injectTemplateInstanceService(TemplateInstanceService<String, JsonNode> tis) {
    templateInstanceService = tis;
  }

  public static Result createTemplateInstance() {
    try {
      IAuthRequest authRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.mustHavePermission(authRequest, CedarPermission.TEMPLATE_INSTANCE_CREATE);
      JsonNode templateInstance = request().body().asJson();

      ProvenanceInfo pi = buildProvenanceInfo(authRequest);
      ProvenanceUtil.addProvenanceInfo(templateInstance, pi);
      JsonNode createdTemplateInstance = templateInstanceService.createTemplateInstance(templateInstance);
      createdTemplateInstance = JsonUtils.removeField(createdTemplateInstance, "_id");

      // Set Location header pointing to the newly created instance
      String id = createdTemplateInstance.get("@id").asText();
      String absoluteUrl = routes.TemplateInstanceServerController.findTemplateInstance(id).absoluteURL(request());

      response().setHeader(HttpConstants.HTTP_HEADER_LOCATION, absoluteUrl);
      // Return created response
      return created(createdTemplateInstance);
    } catch (CedarAccessException e) {
      return forbiddenWithError(e);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

  public static Result findAllTemplateInstances(Integer limit, Integer offset, boolean summary, String fieldNames) {
    try {
      Authorization.mustHavePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
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
      return forbiddenWithError(e);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

  public static Result findTemplateInstance(String templateInstanceId) {
    try {
      Authorization.mustHavePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_INSTANCE_READ);
      JsonNode templateInstance = templateInstanceService.findTemplateInstance(templateInstanceId);
      if (templateInstance != null) {
        // Remove autogenerated _id field to avoid exposing it
        templateInstance = JsonUtils.removeField(templateInstance, "_id");
        return ok(templateInstance);
      }
      return notFound();
    } catch (IllegalArgumentException e) {
      return badRequestWithError(e);
    } catch (CedarAccessException e) {
      return forbiddenWithError(e);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

  public static Result updateTemplateInstance(String templateInstanceId) {
    try {
      IAuthRequest authRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.mustHavePermission(authRequest, CedarPermission.TEMPLATE_INSTANCE_UPDATE);
      JsonNode modifications = request().body().asJson();
      ProvenanceInfo pi = buildProvenanceInfo(authRequest);
      ProvenanceUtil.patchProvenanceInfo(modifications, pi);
      JsonNode updatedTemplateInstance = templateInstanceService.updateTemplateInstance(templateInstanceId,
          modifications);
      // Remove autogenerated _id field to avoid exposing it
      updatedTemplateInstance = JsonUtils.removeField(updatedTemplateInstance, "_id");
      return ok(updatedTemplateInstance);
    } catch (IllegalArgumentException e) {
      return badRequestWithError(e);
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (CedarAccessException e) {
      return forbiddenWithError(e);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

  public static Result deleteTemplateInstance(String templateInstanceId) {
    try {
      Authorization.mustHavePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_INSTANCE_DELETE);
      templateInstanceService.deleteTemplateInstance(templateInstanceId);
      return noContent();
    } catch (IllegalArgumentException e) {
      return badRequestWithError(e);
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (CedarAccessException e) {
      return forbiddenWithError(e);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }

}
