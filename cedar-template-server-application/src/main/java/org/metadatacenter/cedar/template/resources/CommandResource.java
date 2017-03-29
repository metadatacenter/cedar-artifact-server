package org.metadatacenter.cedar.template.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.DevNullProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.request.ResourceType;
import org.metadatacenter.model.request.ResourceTypeDetector;
import org.metadatacenter.model.validation.CEDARModelValidator;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.rest.context.CedarRequestContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.metadatacenter.constant.CedarQueryParameters.QP_RESOURCE_TYPES;
import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/command")
@Produces(MediaType.APPLICATION_JSON)
public class CommandResource extends AbstractTemplateServerResource {

  private static final Logger log = LoggerFactory.getLogger(CommandResource.class);

  public CommandResource(@Nonnull CedarConfig cedarConfig) {
    super(cedarConfig);
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
      // NO-OP: Not implemented yet
    }
    ValidationReport validationReport = new ProcessingReportWrapper(report);
    return Response.ok().entity(validationReport).build();
  }

  private static ProcessingReport validateTemplateNode(JsonNode template) {
    CEDARModelValidator validator = new CEDARModelValidator();
    Optional<ProcessingReport> processingReport = validator.validateTemplateNode(template);
    return processingReport.orElse(new DevNullProcessingReport());
  }

  private static ProcessingReport validateTemplateElementNode(JsonNode templateElement) {
    CEDARModelValidator validator = new CEDARModelValidator();
    Optional<ProcessingReport> processingReport = validator.validateTemplateElementNode(templateElement);
    return processingReport.orElse(new DevNullProcessingReport());
  }

  private static ProcessingReport validateTemplateFieldNode(JsonNode templateField) {
    try {
      CEDARModelValidator validator = new CEDARModelValidator();
      Optional<ProcessingReport> processingReport = validator.validateTemplateFieldNode(templateField);
      return processingReport.orElse(new DevNullProcessingReport());
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return new DevNullProcessingReport();
    }
  }
}
