package org.metadatacenter.cedar.template.resources;

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

@Path("/template-instances")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateInstancesResource {

  private
  @Context
  UriInfo uriInfo;

  private
  @Context
  HttpServletRequest request;

  public TemplateInstancesResource() {
  }


  public static Result createTemplateInstance(F.Option<Boolean> importMode) {
    try {
      AuthRequest authRequest = CedarAuthFromRequestFactory.fromRequest(request());
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
    } catch (CedarUserNotFoundException e) {
      Logger.error("User not found", e);
      return unauthorizedWithError(e);
    } catch (AccessException e) {
      Logger.error("Access Error while reading the template instance", e);
      return forbiddenWithError(e);
    } catch (AuthorizationTypeNotFoundException e) {
      Logger.error("Authorization header not found", e);
      return badRequestWithError(e);
    } catch (Exception e) {
      Logger.error("Error while reading the template instance", e);
      return internalServerErrorWithError(e);
    }
  }

  public static Result updateTemplateInstance(String templateInstanceId) {
    try {
      AuthRequest authRequest = CedarAuthFromRequestFactory.fromRequest(request());
      Authorization.getUserAndEnsurePermission(authRequest, CedarPermission.TEMPLATE_INSTANCE_UPDATE);
      JsonNode newInstance = request().body().asJson();
      ProvenanceInfo pi = ProvenanceUtil.build(cedarConfig, authRequest);
      ProvenanceUtil.patchProvenanceInfo(newInstance, pi);
      JsonNode updatedTemplateInstance = templateInstanceService.updateTemplateInstance(templateInstanceId,
          newInstance);
      // Remove autogenerated _id field to avoid exposing it
      MongoUtils.removeIdField(updatedTemplateInstance);
      return ok(updatedTemplateInstance);
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