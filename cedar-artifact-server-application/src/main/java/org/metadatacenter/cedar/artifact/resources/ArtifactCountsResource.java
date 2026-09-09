package org.metadatacenter.cedar.artifact.resources;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.server.security.model.auth.CedarPermission;
import org.metadatacenter.util.http.ArtifactCounts;
import org.metadatacenter.util.http.CedarError;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.server.service.TemplateFieldService;
import org.metadatacenter.server.service.TemplateElementService;
import org.metadatacenter.server.service.TemplateService;
import org.metadatacenter.server.service.TemplateInstanceService;

@Path("/" + ArtifactCounts.PATH)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Monitoring")
@SecurityRequirement(name = "api_key")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Actual artifact document-store counts",
        content = @Content(schema = @Schema(implementation = ArtifactCounts.class))),
    @ApiResponse(responseCode = "401", description = "Authentication required",
        content = @Content(schema = @Schema(implementation = CedarError.class))),
    @ApiResponse(responseCode = "403", description = "The caller lacks monitor read permission",
        content = @Content(schema = @Schema(implementation = CedarError.class))),
    @ApiResponse(responseCode = "503", description = "Artifact counts are unavailable",
        content = @Content(schema = @Schema(implementation = CedarError.class)))
})
public class ArtifactCountsResource extends CedarMicroserviceResource {
  private final TemplateFieldService<String, JsonNode> fields;
  private final TemplateElementService<String, JsonNode> elements;
  private final TemplateService<String, JsonNode> templates;
  private final TemplateInstanceService<String, JsonNode> instances;

  public ArtifactCountsResource(CedarConfig config, TemplateFieldService<String, JsonNode> fields,
      TemplateElementService<String, JsonNode> elements, TemplateService<String, JsonNode> templates,
      TemplateInstanceService<String, JsonNode> instances) {
    super(config);
    this.fields = fields;
    this.elements = elements;
    this.templates = templates;
    this.instances = instances;
  }

  @GET @Timed
  @Operation(summary = "Count artifact documents", description = "Requires MONITOR_READ. Counts stored documents, including any absent from the workspace graph.")
  public Response counts() throws CedarException {
    var c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);
    c.must(c.user()).have(CedarPermission.MONITOR_READ);
    return Response.ok(new ArtifactCounts(fields.count(), elements.count(), templates.count(), instances.count()))
        .header("Cache-Control", "no-store").build();
  }
}
