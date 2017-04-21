package org.metadatacenter.cedar.template.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.error.CedarErrorPack;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.request.ResourceType;
import org.metadatacenter.model.request.ResourceTypeDetector;
import org.metadatacenter.model.validation.CEDARModelValidator;
import org.metadatacenter.model.validation.ModelValidator;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.mongo.MongoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.metadatacenter.constant.CedarQueryParameters.QP_RESOURCE_TYPES;
import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/command")
@Produces(MediaType.APPLICATION_JSON)
public class CommandResource extends AbstractTemplateServerResource {

  private static final Logger log = LoggerFactory.getLogger(CommandResource.class);

  private final TemplateService<String, JsonNode> templateService;

  public CommandResource(@Nonnull CedarConfig cedarConfig, @Nonnull TemplateService<String, JsonNode> templateService) {
    super(cedarConfig);
    this.templateService = checkNotNull(templateService);
  }

  @POST
  @Timed
  @Path("/validate")
  public Response validateResource(@QueryParam(QP_RESOURCE_TYPES) String type) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
//    c.must(c.user()).have(CedarPermission.TEMPLATE_INSTANCE_CREATE); // XXX Permission for validation?

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
      throw new CedarException(errorPack){};
    }
    return validationReport;
  }

  private ValidationReport validateTemplateInstance(JsonNode templateInstance) throws CedarException {
    try {
      JsonNode instanceSchema = getSchemaSource(templateInstance);
      return validateTemplateInstance(templateInstance, instanceSchema);
    } catch (IOException | ProcessingException e) {
      throw newCedarException(e.getMessage());
    }
  }

  private JsonNode getSchemaSource(JsonNode templateInstance) throws IOException, ProcessingException {
    String templateRefId = templateInstance.get("schema:isBasedOn").asText();
    JsonNode template = templateService.findTemplate(templateRefId);
    MongoUtils.removeIdField(template);
    return template;
  }
}
