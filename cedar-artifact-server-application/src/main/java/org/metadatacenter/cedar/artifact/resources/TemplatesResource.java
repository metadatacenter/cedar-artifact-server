package org.metadatacenter.cedar.artifact.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.error.CedarErrorReasonKey;
import org.metadatacenter.exception.ArtifactServerResourceNotFoundException;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.server.dao.ArtifactWithRevision;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateInstanceService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.util.http.CedarResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.metadatacenter.constant.CedarPathParameters.PP_ID;
import static org.metadatacenter.constant.CedarQueryParameters.*;
import static org.metadatacenter.rest.assertion.GenericAssertions.*;

@Path("/templates")
@Produces(MediaType.APPLICATION_JSON)
public class TemplatesResource extends AbstractArtifactCrudResource {

  private static final Logger logger = LoggerFactory.getLogger(TemplatesResource.class);

  private final TemplateService<String, JsonNode> templateService;
  private final TemplateInstanceService<String, JsonNode> templateInstanceService;

  public TemplatesResource(CedarConfig cedarConfig, TemplateService<String, JsonNode> templateService,
                           TemplateInstanceService<String, JsonNode> templateInstanceService) {
    super(cedarConfig, logger, "artifact", "templates",
        cedarConfig.getArtifactRESTAPI().getSummaries().getTemplate().getFields(), true);
    this.templateService = templateService;
    this.templateInstanceService = templateInstanceService;
  }

  @POST
  @Timed
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  @Consumes({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  public Response createTemplate(
      @QueryParam("compact") Optional<Boolean> compactParam,
      String requestBody) throws CedarException {
    return createArtifact(CedarPermission.TEMPLATE_CREATE, CedarResourceType.TEMPLATE,
        CedarErrorKey.TEMPLATE_NOT_CREATED, requestBody, compactParam);
  }

  @GET
  @Timed
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  public Response findTemplate(@PathParam(PP_ID) String id,
                             @QueryParam("compact") Optional<Boolean> compactParam) throws CedarException {
    return findArtifact(id, CedarPermission.TEMPLATE_READ, CedarErrorKey.TEMPLATE_NOT_FOUND, CedarResourceType.TEMPLATE, compactParam);
  }

  @GET
  @Timed
  public Response findAllTemplates(@QueryParam(QP_LIMIT) Optional<Integer> limitParam,
                                   @QueryParam(QP_OFFSET) Optional<Integer> offsetParam,
                                   @QueryParam(QP_SUMMARY) Optional<Boolean> summaryParam,
                                   @QueryParam(QP_FIELD_NAMES) Optional<String> fieldNamesParam) throws CedarException {
    return findAllArtifacts(limitParam, offsetParam, summaryParam, fieldNamesParam,
        CedarPermission.TEMPLATE_READ, CedarErrorKey.TEMPLATES_NOT_LISTED);
  }

  @PUT
  @Timed
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  @Consumes({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  public Response updateTemplate(@PathParam(PP_ID) String id,
                               @QueryParam("compact") Optional<Boolean> compactParam,
                               @QueryParam(QP_VERBATIM) Optional<Boolean> verbatimParam,
                               String requestBody) throws CedarException {
    return updateArtifact(id, CedarPermission.TEMPLATE_CREATE, CedarPermission.TEMPLATE_UPDATE,
        CedarResourceType.TEMPLATE,
        CedarErrorKey.TEMPLATE_NOT_UPDATED, CedarErrorKey.TEMPLATE_NOT_CREATED, requestBody, compactParam, verbatimParam);
  }

  @DELETE
  @Timed
  @Path("/{id}")
  public Response deleteTemplate(@PathParam(PP_ID) String id) throws CedarException {
    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.TEMPLATE_DELETE);
    c.must(id).be(ValidUrl);

    long referenceCount = templateInstanceService.countReferencingTemplate(id);

    if (referenceCount != 0) {
      return CedarResponse.badRequest()
          .id(id)
          .errorKey(CedarErrorKey.TEMPLATE_NOT_DELETED)
          .errorReasonKey(CedarErrorReasonKey.TEMPLATE_REFERENCED_IN_INSTANCES)
          .errorMessage("The artifact can not be deleted since there are instances using it")
          .parameter("referenceCount", referenceCount)
          .build();
    }

    return deleteArtifactFromDatabase(id, CedarErrorKey.TEMPLATE_NOT_FOUND, CedarErrorKey.TEMPLATE_NOT_DELETED);
  }

  @Override
  protected JsonNode createArtifactInService(JsonNode template) throws IOException {
    return templateService.createTemplate(template);
  }

  @Override
  protected JsonNode findArtifactInService(String id) throws IOException {
    return templateService.findTemplate(id);
  }

  @Override
  protected ArtifactWithRevision<JsonNode> findArtifactWithRevisionInService(String id) throws IOException {
    return templateService.findTemplateWithRevision(id);
  }

  @Override
  protected JsonNode updateArtifactInService(String id, JsonNode content, long expectedRevision) throws IOException,
      ArtifactServerResourceNotFoundException {
    return templateService.updateTemplate(id, content, expectedRevision);
  }

  @Override
  protected void deleteArtifactInService(String id) throws IOException, ArtifactServerResourceNotFoundException {
    templateService.deleteTemplate(id);
  }

  @Override
  protected List<JsonNode> findAllArtifactsInService(Integer limit, Integer offset, List<String> fieldNames,
                                                     FieldNameInEx includeExclude) throws IOException {
    return templateService.findAllTemplates(limit, offset, fieldNames, includeExclude);
  }

  @Override
  protected long countArtifactsInService() {
    return templateService.count();
  }

  @Override
  protected ValidationReport validateArtifact(JsonNode template) throws CedarException {
    return validateTemplate(template);
  }

  @Override
  protected String updateValidationErrorMessage(ValidationReport validationReport) {
    return "There was an error while validating the artifact";
  }
}
