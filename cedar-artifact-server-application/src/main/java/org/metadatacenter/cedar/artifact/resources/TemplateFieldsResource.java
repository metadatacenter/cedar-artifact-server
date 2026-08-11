package org.metadatacenter.cedar.artifact.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.exception.ArtifactServerResourceNotFoundException;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateFieldService;
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

@Path("/template-fields")
@Produces(MediaType.APPLICATION_JSON)
public class TemplateFieldsResource extends AbstractArtifactCrudResource {

  private static final Logger logger = LoggerFactory.getLogger(TemplateFieldsResource.class);

  private static TemplateFieldService<String, JsonNode> templateFieldService;

  public TemplateFieldsResource(CedarConfig cedarConfig, TemplateFieldService<String, JsonNode> templateFieldService) {
    super(cedarConfig, logger, "artifact field", "artifact fields",
        cedarConfig.getArtifactRESTAPI().getSummaries().getField().getFields(), false);
    TemplateFieldsResource.templateFieldService = templateFieldService;
  }

  @POST
  @Timed
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  @Consumes({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  public Response createTemplateField(
      @QueryParam("compact") Optional<Boolean> compactParam,
      String requestBody) throws CedarException {
    return createArtifact(CedarPermission.TEMPLATE_FIELD_CREATE, CedarResourceType.FIELD,
        CedarErrorKey.TEMPLATE_FIELD_NOT_CREATED, requestBody, compactParam);
  }

  @GET
  @Timed
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  public Response findTemplateField(@PathParam(PP_ID) String id,
                             @QueryParam("compact") Optional<Boolean> compactParam) throws CedarException {
    return findArtifact(id, CedarPermission.TEMPLATE_FIELD_READ, CedarErrorKey.TEMPLATE_FIELD_NOT_FOUND, CedarResourceType.FIELD, compactParam);
  }

  @GET
  @Timed
  public Response findAllTemplateFields(@QueryParam(QP_LIMIT) Optional<Integer> limitParam,
                                        @QueryParam(QP_OFFSET) Optional<Integer> offsetParam,
                                        @QueryParam(QP_SUMMARY) Optional<Boolean> summaryParam,
                                        @QueryParam(QP_FIELD_NAMES) Optional<String> fieldNamesParam) throws CedarException {
    return findAllArtifacts(limitParam, offsetParam, summaryParam, fieldNamesParam,
        CedarPermission.TEMPLATE_FIELD_READ, CedarErrorKey.TEMPLATE_FIELDS_NOT_LISTED);
  }

  @PUT
  @Timed
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  @Consumes({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  public Response updateTemplateField(@PathParam(PP_ID) String id,
                               @QueryParam("compact") Optional<Boolean> compactParam,
                               @QueryParam(QP_EXPECTED_LAST_UPDATED_ON) Optional<String> expectedLastUpdatedOn,
                               String requestBody) throws CedarException {
    return updateArtifact(id, CedarPermission.TEMPLATE_FIELD_UPDATE, CedarResourceType.FIELD,
        CedarErrorKey.TEMPLATE_FIELD_NOT_UPDATED, CedarErrorKey.TEMPLATE_FIELD_NOT_CREATED, requestBody, compactParam, expectedLastUpdatedOn);
  }

  @DELETE
  @Timed
  @Path("/{id}")
  public Response deleteTemplateField(@PathParam(PP_ID) String id) throws CedarException {
    return deleteArtifact(id, CedarPermission.TEMPLATE_FIELD_DELETE, CedarErrorKey.TEMPLATE_FIELD_NOT_FOUND,
        CedarErrorKey.TEMPLATE_FIELD_NOT_DELETED);
  }

  @Override
  protected JsonNode createArtifactInService(JsonNode templateField) throws IOException {
    return templateFieldService.createTemplateField(templateField);
  }

  @Override
  protected JsonNode findArtifactInService(String id) throws IOException {
    return templateFieldService.findTemplateField(id);
  }

  @Override
  protected JsonNode updateArtifactInService(String id, JsonNode content) throws IOException,
      ArtifactServerResourceNotFoundException {
    return templateFieldService.updateTemplateField(id, content);
  }

  @Override
  protected void deleteArtifactInService(String id) throws IOException, ArtifactServerResourceNotFoundException {
    templateFieldService.deleteTemplateField(id);
  }

  @Override
  protected List<JsonNode> findAllArtifactsInService(Integer limit, Integer offset, List<String> fieldNames,
                                                     FieldNameInEx includeExclude) throws IOException {
    return templateFieldService.findAllTemplateFields(limit, offset, fieldNames, includeExclude);
  }

  @Override
  protected long countArtifactsInService() {
    return templateFieldService.count();
  }

  @Override
  protected ValidationReport validateArtifact(JsonNode templateField) throws CedarException {
    return validateTemplateField(templateField);
  }
}
