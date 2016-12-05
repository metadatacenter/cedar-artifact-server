package org.metadatacenter.cedar.template.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.server.service.TemplateInstanceService;

import javax.management.InstanceNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.rmi.AccessException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/template-fields")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateFieldsResource {

  private
  @Context
  UriInfo uriInfo;

  private
  @Context
  HttpServletRequest request;

  private final CedarConfig cedarConfig;

  private final TemplateFieldService<String, JsonNode> templateFieldService;

  public TemplateFieldsResource(CedarConfig cedarConfig, TemplateFieldService<String, JsonNode> templateFieldService) {
    this.cedarConfig = cedarConfig;
    this.templateFieldService = templateFieldService;
  }

  public static Result createTemplateField(F.Option<Boolean> importMode) {
    try {
      AuthRequest authRequest = CedarAuthFromRequestFactory.fromRequest(request());
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
    } catch (CedarUserNotFoundException e) {
      Logger.error("User not found", e);
      return unauthorizedWithError(e);
    } catch (AccessException e) {
      Logger.error("Access Error while reading the template field", e);
      return forbiddenWithError(e);
    } catch (AuthorizationTypeNotFoundException e) {
      Logger.error("Authorization header not found", e);
      return badRequestWithError(e);
    } catch (Exception e) {
      Logger.error("Error while reading the template field", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result updateTemplateField(String templateFieldId) {
    try {
      AuthRequest authRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(authRequest, CedarPermission.TEMPLATE_FIELD_UPDATE);
      JsonNode newField = request().body().asJson();
      ProvenanceInfo pi = ProvenanceUtil.build(cedarConfig, authRequest);
      ProvenanceUtil.patchProvenanceInfo(newField, pi);
      JsonNode updatedTemplateElement = templateFieldService.updateTemplateField(templateFieldId, newField);
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