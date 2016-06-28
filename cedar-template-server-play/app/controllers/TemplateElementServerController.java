package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.constant.CustomHttpConstants;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.model.CedarNodeType;
import org.metadatacenter.server.model.provenance.ProvenanceInfo;
import org.metadatacenter.server.security.Authorization;
import org.metadatacenter.server.security.CedarAuthFromRequestFactory;
import org.metadatacenter.server.security.exception.CedarAccessException;
import org.metadatacenter.server.security.model.IAuthRequest;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateElementService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateElementServerController extends AbstractTemplateServerController {

  private static TemplateElementService<String, JsonNode> templateElementService;
  private static TemplateFieldService<String, JsonNode> templateFieldService;
  protected static List<String> FIELD_NAMES_SUMMARY_LIST;

  static {
    FIELD_NAMES_SUMMARY_LIST = new ArrayList<>();
    FIELD_NAMES_SUMMARY_LIST.addAll(cedarConfig.getTemplateRESTAPISummaries().getElement().getFields());
  }

  public static void injectTemplateElementService(TemplateElementService<String, JsonNode> tes) {
    templateElementService = tes;
  }

  public static void injectTemplateFieldService(TemplateFieldService<String, JsonNode> tfs) {
    templateFieldService = tfs;
  }

  public static Result createTemplateElement(F.Option<Boolean> importMode) {
    try {
      IAuthRequest authRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(authRequest, CedarPermission.TEMPLATE_ELEMENT_CREATE);
      JsonNode templateElement = request().body().asJson();

      ProvenanceInfo pi = ProvenanceUtil.build(cedarConfig, authRequest);
      checkImportModeSetProvenanceAndId(CedarNodeType.ELEMENT, templateElement, pi, importMode);

      templateFieldService.saveNewFieldsAndReplaceIds(templateElement, pi,
          cedarConfig.getLinkedDataPrefix(CedarNodeType.FIELD));
      JsonNode createdTemplateElement = templateElementService.createTemplateElement(templateElement);
      MongoUtils.removeIdField(createdTemplateElement);

      // Set Location header pointing to the newly created element
      String id = createdTemplateElement.get("@id").asText();
      String absoluteUrl = routes.TemplateElementServerController.findTemplateElement(id).absoluteURL(request());

      response().setHeader(HttpConstants.HTTP_HEADER_LOCATION, absoluteUrl);
      // Return created response
      return created(createdTemplateElement);
    } catch (CedarAccessException e) {
      Logger.error("Access Error while creating the template element", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while creating the template element", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result findAllTemplateElements(Integer limit, Integer offset, boolean summary, String fieldNames) {
    try {
      Authorization.getUserAndEnsurePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_ELEMENT_READ);
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
      response().setHeader(CustomHttpConstants.HEADER_TOTAL_COUNT, String.valueOf(total));
      checkPagingParametersAgainstTotal(offset, total);
      String absoluteUrl = routes.TemplateElementServerController.findAllTemplateElements(0, 0, false, null).absoluteURL
          (request());
      absoluteUrl = UrlUtil.trimUrlParameters(absoluteUrl);
      String linkHeader = LinkHeaderUtil.getPagingLinkHeader(absoluteUrl, total, limit, offset);
      if (!linkHeader.isEmpty()) {
        response().setHeader(HttpConstants.HTTP_HEADER_LINK, linkHeader);
      }
      return ok(Json.toJson(elements));
    } catch (CedarAccessException e) {
      Logger.error("Access Error while reading the template elements", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while reading the template elements", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result findTemplateElement(String templateElementId) {
    try {
      Authorization.getUserAndEnsurePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_ELEMENT_READ);
      JsonNode templateElement = templateElementService.findTemplateElement(templateElementId);
      if (templateElement != null) {
        // Remove autogenerated _id field to avoid exposing it
        MongoUtils.removeIdField(templateElement);
        return ok(templateElement);
      }
      Logger.error("Template element not found:(" + templateElementId + ")");
      return notFound();
    } catch (IllegalArgumentException e) {
      Logger.error("Illegal Argument while reading the template element", e);
      return badRequestWithError(e);
    } catch (CedarAccessException e) {
      Logger.error("Access Error while reading the template element", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while reading the template elements", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result updateTemplateElement(String templateElementId) {
    try {
      IAuthRequest authRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(authRequest, CedarPermission.TEMPLATE_ELEMENT_UPDATE);
      JsonNode modifications = request().body().asJson();

      ProvenanceInfo pi = ProvenanceUtil.build(cedarConfig, authRequest);
      ProvenanceUtil.patchProvenanceInfo(modifications, pi);
      templateFieldService.saveNewFieldsAndReplaceIds(modifications, pi,
          cedarConfig.getLinkedDataPrefix(CedarNodeType.FIELD));
      JsonNode updatedTemplateElement = templateElementService.updateTemplateElement(templateElementId, modifications);
      // Remove autogenerated _id field to avoid exposing it
      MongoUtils.removeIdField(updatedTemplateElement);
      return ok(updatedTemplateElement);
    } catch (IllegalArgumentException e) {
      Logger.error("Illegal Argument while reading the template element", e);
      return badRequestWithError(e);
    } catch (InstanceNotFoundException e) {
      Logger.error("Template element not found for update:(" + templateElementId + ")");
      return notFound();
    } catch (CedarAccessException e) {
      Logger.error("Access Error while reading the template element", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while updating the template element", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result deleteTemplateElement(String templateElementId) {
    try {
      Authorization.getUserAndEnsurePermission(CedarAuthFromRequestFactory.fromRequest(request()), CedarPermission
          .TEMPLATE_ELEMENT_DELETE);
      templateElementService.deleteTemplateElement(templateElementId);
      return noContent();
    } catch (IllegalArgumentException e) {
      Logger.error("Illegal Argument while deleting the template element", e);
      return badRequestWithError(e);
    } catch (InstanceNotFoundException e) {
      Logger.error("Template element not found while deleting:(" + templateElementId + ")");
      return notFound();
    } catch (CedarAccessException e) {
      Logger.error("Access Error while deleting the template element", e);
      return forbiddenWithError(e);
    } catch (Exception e) {
      Logger.error("Error while deleting the template element", e);
      return internalServerErrorWithError(e);
    }
  }

}
