package org.metadatacenter.cedar.artifact.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.constant.HttpConstants;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.exception.ArtifactServerResourceNotFoundException;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.server.dao.ArtifactWithRevision;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.server.service.FieldNameInEx;
import org.metadatacenter.server.service.TemplateElementService;
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

@Path("/template-elements")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Template elements")
@SecurityRequirement(name = "api_key")
public class TemplateElementsResource extends AbstractArtifactCrudResource {

  private static final Logger logger = LoggerFactory.getLogger(TemplateInstancesResource.class);

  private static TemplateElementService<String, JsonNode> templateElementService;

  public TemplateElementsResource(CedarConfig cedarConfig, TemplateElementService<String, JsonNode> templateElementService) {
    super(cedarConfig, logger, "artifact element", "artifact elements",
        cedarConfig.getArtifactRESTAPI().getSummaries().getElement().getFields(), true);
    TemplateElementsResource.templateElementService = templateElementService;
  }

  @POST
  @Timed
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  @Consumes({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  @Operation(summary = "Create a template element",
      description = "Create a template element. " + ArtifactApiDocs.BODY_FORMAT + " The artifact is validated against the CEDAR "
          + "model before it is stored; an invalid one is refused rather than stored. The server mints "
          + "the identifier, so the body must carry none, and must carry a name.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "The stored template element",
          headers = {
              @Header(name = "Location", description = "URL of the created artifact.",
                  schema = @Schema(type = "string")),
              @Header(name = "ETag", description = ArtifactApiDocs.ETAG, schema = @Schema(type = "string")),
              @Header(name = "CEDAR-Validation-Status", description = ArtifactApiDocs.VALIDATION_STATUS,
                  schema = @Schema(type = "string"))
          }),
      @ApiResponse(responseCode = "400",
          description = "The body is empty, carries an identifier, has no name, or failed validation"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "406", description = ArtifactApiDocs.NOT_ACCEPTABLE),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response createTemplateElement(
      @Parameter(description = ArtifactApiDocs.COMPACT_ON_WRITE)
      @QueryParam("compact") Optional<Boolean> compactParam,
      String requestBody) throws CedarException {
    return createArtifact(CedarPermission.TEMPLATE_ELEMENT_CREATE, CedarResourceType.ELEMENT,
        CedarErrorKey.TEMPLATE_ELEMENT_NOT_CREATED, requestBody, compactParam);
  }

  @GET
  @Timed
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  @Operation(summary = "Get a template element",
      description = "Get a template element by identifier. " + ArtifactApiDocs.READ_FORMAT)
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The stored template element",
          headers = {
              @Header(name = "ETag", description = ArtifactApiDocs.ETAG, schema = @Schema(type = "string")),
              @Header(name = "Vary", description = "Accept, since the representation is negotiated.",
                  schema = @Schema(type = "string"))
          }),
      @ApiResponse(responseCode = "400", description = "The identifier is not a valid URL"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "No such artifact"),
      @ApiResponse(responseCode = "406", description = ArtifactApiDocs.NOT_ACCEPTABLE),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response findTemplateElement(
      @Parameter(description = "Artifact identifier, as an absolute IRI.", required = true)
      @PathParam(PP_ID) String id,
      @Parameter(description = ArtifactApiDocs.COMPACT_ON_READ)
      @QueryParam("compact") Optional<Boolean> compactParam) throws CedarException {
    return findArtifact(id, CedarPermission.TEMPLATE_ELEMENT_READ, CedarErrorKey.TEMPLATE_ELEMENT_NOT_FOUND, CedarResourceType.ELEMENT, compactParam);
  }

  @GET
  @Timed
  @Operation(summary = "List template elements",
      description = "List template elements, one page at a time. The response carries the size of the whole "
          + "collection and paging links, not just the returned page.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "A page of template elements",
          headers = {
              @Header(name = "Total-Count", description = ArtifactApiDocs.TOTAL_COUNT, schema = @Schema(type = "integer")),
              @Header(name = "Link", description = ArtifactApiDocs.LINK, schema = @Schema(type = "string"))
          }),
      @ApiResponse(responseCode = "400", description = "A paging parameter is out of range"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response findAllTemplateElements(
      @Parameter(description = ArtifactApiDocs.LIMIT)
      @QueryParam(QP_LIMIT) Optional<Integer> limitParam,
      @Parameter(description = ArtifactApiDocs.OFFSET)
      @QueryParam(QP_OFFSET) Optional<Integer> offsetParam,
      @Parameter(description = ArtifactApiDocs.SUMMARY)
      @QueryParam(QP_SUMMARY) Optional<Boolean> summaryParam,
      @Parameter(description = ArtifactApiDocs.FIELD_NAMES)
      @QueryParam(QP_FIELD_NAMES) Optional<String> fieldNamesParam) throws CedarException {
    return findAllArtifacts(limitParam, offsetParam, summaryParam, fieldNamesParam,
        CedarPermission.TEMPLATE_ELEMENT_READ, CedarErrorKey.TEMPLATE_ELEMENTS_NOT_LISTED);
  }

  @PUT
  @Timed
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  @Consumes({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  @Operation(summary = "Replace a template element",
      description = "Replace a template element, or create one at a client-supplied identifier that does not yet "
          + "exist. " + ArtifactApiDocs.BODY_FORMAT + " Replacing an artifact that exists is conditional: the "
          + "current ETag must be supplied in If-Match, so a write can not silently overwrite one that "
          + "landed since the artifact was read.",
      parameters = @Parameter(in = ParameterIn.HEADER, name = "If-Match",
          description = ArtifactApiDocs.IF_MATCH_FOR_CREATE_OR_REPLACE, schema = @Schema(type = "string")))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The replaced a template element",
          headers = {
              @Header(name = "ETag", description = ArtifactApiDocs.ETAG, schema = @Schema(type = "string")),
              @Header(name = "CEDAR-Validation-Status", description = ArtifactApiDocs.VALIDATION_STATUS,
                  schema = @Schema(type = "string"))
          }),
      @ApiResponse(responseCode = "201", description = "A template element created at the supplied identifier",
          headers = @Header(name = "ETag", description = ArtifactApiDocs.ETAG, schema = @Schema(type = "string"))),
      @ApiResponse(responseCode = "400", description = "The body is empty, has no name, or failed validation"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "406", description = ArtifactApiDocs.NOT_ACCEPTABLE),
      @ApiResponse(responseCode = "412", description = ArtifactApiDocs.PRECONDITION_FAILED),
      @ApiResponse(responseCode = "428", description = ArtifactApiDocs.PRECONDITION_REQUIRED),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response updateTemplateElement(
      @Parameter(description = "Artifact identifier, as an absolute IRI.", required = true)
      @PathParam(PP_ID) String id,
      @Parameter(description = ArtifactApiDocs.COMPACT_ON_WRITE)
      @QueryParam("compact") Optional<Boolean> compactParam,
      @Parameter(description = ArtifactApiDocs.VERBATIM)
      @QueryParam(QP_VERBATIM) Optional<Boolean> verbatimParam,
      String requestBody) throws CedarException {
    return updateArtifact(id, CedarPermission.TEMPLATE_ELEMENT_CREATE, CedarPermission.TEMPLATE_ELEMENT_UPDATE,
        CedarResourceType.ELEMENT,
        CedarErrorKey.TEMPLATE_ELEMENT_NOT_UPDATED, CedarErrorKey.TEMPLATE_ELEMENT_NOT_CREATED, requestBody, compactParam, verbatimParam);
  }

  @DELETE
  @Timed
  @Path("/{id}")
  @Operation(summary = "Delete a template element",
      description = "Delete a template element. Conditional on the current ETag, so a delete can not discard a "
          + "revision written since the artifact was read.",
      parameters = @Parameter(in = ParameterIn.HEADER, name = "If-Match", required = true,
          description = ArtifactApiDocs.IF_MATCH_REQUIRED, schema = @Schema(type = "string")))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Deleted"),
      @ApiResponse(responseCode = "400", description = "The identifier is not a valid URL"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden"),
      @ApiResponse(responseCode = "404", description = "No such artifact"),
      @ApiResponse(responseCode = "412", description = ArtifactApiDocs.PRECONDITION_FAILED),
      @ApiResponse(responseCode = "428", description = ArtifactApiDocs.PRECONDITION_REQUIRED),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  public Response deleteTemplateElement(
      @Parameter(description = "Artifact identifier, as an absolute IRI.", required = true)
      @PathParam(PP_ID) String id) throws CedarException {
    return deleteArtifact(id, CedarPermission.TEMPLATE_ELEMENT_DELETE, CedarErrorKey.TEMPLATE_ELEMENT_NOT_FOUND,
        CedarErrorKey.TEMPLATE_ELEMENT_NOT_DELETED);
  }

  @Override
  protected JsonNode createArtifactInService(JsonNode templateElement) throws IOException {
    return templateElementService.createTemplateElement(templateElement);
  }

  @Override
  protected JsonNode findArtifactInService(String id) throws IOException {
    return templateElementService.findTemplateElement(id);
  }

  @Override
  protected ArtifactWithRevision<JsonNode> findArtifactWithRevisionInService(String id) throws IOException {
    return templateElementService.findTemplateElementWithRevision(id);
  }

  @Override
  protected JsonNode updateArtifactInService(String id, JsonNode content, long expectedRevision) throws IOException,
      ArtifactServerResourceNotFoundException {
    return templateElementService.updateTemplateElement(id, content, expectedRevision);
  }

  @Override
  protected void deleteArtifactInService(String id) throws IOException, ArtifactServerResourceNotFoundException {
    templateElementService.deleteTemplateElement(id);
  }

  @Override
  protected void deleteArtifactInService(String id, long expectedRevision)
      throws IOException, ArtifactServerResourceNotFoundException {
    templateElementService.deleteTemplateElement(id, expectedRevision);
  }

  @Override
  protected List<JsonNode> findAllArtifactsInService(Integer limit, Integer offset, List<String> fieldNames,
                                                     FieldNameInEx includeExclude) throws IOException {
    return templateElementService.findAllTemplateElements(limit, offset, fieldNames, includeExclude);
  }

  @Override
  protected long countArtifactsInService() {
    return templateElementService.count();
  }

  @Override
  protected ValidationReport validateArtifact(JsonNode templateElement) throws CedarException {
    return validateTemplateElement(templateElement);
  }
}
