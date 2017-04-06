package org.metadatacenter.cedar.template.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.*;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.request.ResourceType;
import org.metadatacenter.model.request.ResourceTypeDetector;
import org.metadatacenter.model.validation.CEDARModelValidator;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.http.CedarResponse;
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
import java.util.Optional;

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
  public Response validateTemplate(@QueryParam(QP_RESOURCE_TYPES) String resourceType) throws CedarException {
    CedarRequestContext c = CedarRequestContextFactory.fromRequest(request);
    c.must(c.user()).be(LoggedIn);
//    c.must(c.user()).have(CedarPermission.TEMPLATE_INSTANCE_CREATE); // XXX Permission for validation?

    ResourceType type = ResourceTypeDetector.detectType(resourceType);

    JsonNode payload = c.request().getRequestBody().asJson();

    ProcessingReport report = new DevNullProcessingReport();
    if (type == ResourceType.TEMPLATE) {
      report = validateTemplateNode(payload);
    } else if (type == ResourceType.ELEMENT) {
      report = validateTemplateElementNode(payload);
    } else if (type == ResourceType.FIELD) {
      report = validateTemplateFieldNode(payload);
    } else if (type == ResourceType.INSTANCE) {
      report = validateTemplateInstanceNode(payload);
    }
    ValidationReport validationReport = new ProcessingReportWrapper(report);
    return Response.ok().entity(validationReport).build();
  }

  private ProcessingReport validateTemplateNode(JsonNode template) {
    CEDARModelValidator validator = new CEDARModelValidator();
    ProcessingReport processingReport = validator.validateTemplateNode(template);
    return processingReport;
  }

  private ProcessingReport validateTemplateElementNode(JsonNode templateElement) {
    CEDARModelValidator validator = new CEDARModelValidator();
    ProcessingReport processingReport = validator.validateTemplateElementNode(templateElement);
    return processingReport;
  }

  private ProcessingReport validateTemplateFieldNode(JsonNode templateField) throws CedarException {
    try {
      CEDARModelValidator validator = new CEDARModelValidator();
      ProcessingReport processingReport = validator.validateTemplateFieldNode(templateField);
      return processingReport;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw newCedarException(e.getMessage());
    }
  }

  private ProcessingReport validateTemplateInstanceNode(JsonNode templateInstance) throws CedarException {
    try {
      JsonNode instanceSchema = getSchemaSource(templateInstance);
      CEDARModelValidator validator = new CEDARModelValidator();
      ProcessingReport processingReport = validator.validateTemplateInstanceNode(templateInstance, instanceSchema);
      return processingReport;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw newCedarException(e.getMessage());
    }
  }

  private JsonNode getSchemaSource(JsonNode templateInstance) throws IOException, ProcessingException {
    String templateRefId = templateInstance.get("schema:isBasedOn").asText();
    JsonNode template = templateService.findTemplate(templateRefId);
    MongoUtils.removeIdField(template);
    return template;
  }

  private static CedarException newCedarException(String message) {
    return new CedarException(message) {};
  }

}
