package org.metadatacenter.cedar.artifact.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.error.CedarErrorPack;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.request.ResourceType;
import org.metadatacenter.model.request.ResourceTypeDetector;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.service.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.metadatacenter.constant.CedarQueryParameters.QP_RESOURCE_TYPE;
import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/command")
@Produces(MediaType.APPLICATION_JSON)
public class CommandResource extends AbstractArtifactServerResource {

  private static final Logger log = LoggerFactory.getLogger(CommandResource.class);

  private final TemplateService<String, JsonNode> templateService;

  public CommandResource(CedarConfig cedarConfig, TemplateService<String, JsonNode> templateService) {
    super(cedarConfig);
    this.templateService = checkNotNull(templateService);
  }

  @POST
  @Timed
  @Path("/validate")
  public Response validateResource(@QueryParam(QP_RESOURCE_TYPE) String type) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    ResourceType resourceType = ResourceTypeDetector.detectType(type);
    JsonNode resourceNode = c.request().getRequestBody().asJson();
    ValidationReport validationReport = validateResource(resourceNode, resourceType);
    return Response.ok().entity(validationReport).build();
  }

  private ValidationReport validateResource(JsonNode resource, ResourceType type) throws CedarException {
    ValidationReport validationReport = null;
    if (type == ResourceType.TEMPLATE) {
      validationReport = validateTemplate(resource);
    } else if (type == ResourceType.ELEMENT) {
      validationReport = validateTemplateElement(resource);
    } else if (type == ResourceType.FIELD) {
      validationReport = validateTemplateField(resource);
    } else if (type == ResourceType.INSTANCE) {
      validationReport = validateTemplateInstance(resource);
    } else {
      CedarErrorPack errorPack = new CedarErrorPack()
          .errorKey(CedarErrorKey.METHOD_NOT_IMPLEMENTED)
          .message("Validation method for type " + type + " is not implemented yet");
      throw new CedarException(errorPack) {
      };
    }
    return validationReport;
  }

  private ValidationReport validateTemplateInstance(JsonNode payload) throws CedarException {
    try {
      ValidationReport validationReport = null;
      JsonNode instanceObject = getInstanceObject(payload);
      if (hasUserSpecifiedSchema(payload)) {
        validationReport = validateUsingUserSpecifiedSchema(payload, instanceObject);
      } else {
        validationReport = validateUsingInstanceSpecifiedSchema(instanceObject);
      }
      return validationReport;
    } catch (IOException e) {
      throw newCedarException(e.getMessage());
    }
  }

  private boolean hasUserSpecifiedSchema(JsonNode payload) {
    return !payload.path("schema").isMissingNode() && !payload.path("schema").isNull();
  }

  private ValidationReport validateUsingUserSpecifiedSchema(JsonNode payload, JsonNode instanceObject)
      throws CedarException {
    JsonNode instanceSchema = payload.get("schema");
    ValidationReport schemaValidationReport = validateTemplate(instanceSchema); // validate the input schema
    if (schemaValidationReport.getValidationStatus().equals("false")) {
      return schemaValidationReport; // return schema validation report instead
    }
    return validateTemplateInstance(instanceObject, instanceSchema);
  }

  private ValidationReport validateUsingInstanceSpecifiedSchema(JsonNode instanceObject)
      throws IOException, CedarException {
    JsonNode instanceSchema = getSchemaSource(templateService, instanceObject);
    return validateTemplateInstance(instanceObject, instanceSchema);
  }

  private static JsonNode getInstanceObject(JsonNode payload) throws CedarException {
    JsonNode instanceObject = payload;
    if (!payload.path("instance").isMissingNode()) {
      instanceObject = payload.get("instance");
    }
    return instanceObject;
  }

}
