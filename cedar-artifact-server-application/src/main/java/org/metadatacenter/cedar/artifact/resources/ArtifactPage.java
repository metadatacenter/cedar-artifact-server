package org.metadatacenter.cedar.artifact.resources;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.metadatacenter.util.artifact.InstanceArtifactDocument;
import org.metadatacenter.util.artifact.SchemaArtifactDocument;
import org.metadatacenter.util.http.PagedListResponse;

import java.util.List;

/**
 * A page of one artifact type's stored documents, in CEDAR's body paging envelope.
 *
 * <p>The listing answered a bare array with the count and links in headers. Nothing outside this
 * server read it, so it moved to the envelope every other CEDAR listing answers rather than being
 * introduced beside the old form.
 */
public class ArtifactPage extends PagedListResponse {

  private final List<JsonNode> artifacts;

  public ArtifactPage(List<JsonNode> artifacts, String requestUrl, long totalCount, int limit, int offset) {
    this.artifacts = artifacts;
    page(requestUrl, totalCount, limit, offset, false);
  }

  public List<JsonNode> getArtifacts() {
    return artifacts;
  }

  /** A page of templates, elements or fields, for the API description. */
  @Schema(name = "SchemaArtifactPage")
  static final class OfSchemaArtifacts extends ArtifactPage {
    @ArraySchema(schema = @Schema(implementation = SchemaArtifactDocument.class))
    private List<JsonNode> artifacts;

    private OfSchemaArtifacts() {
      super(List.of(), null, 0, 1, 0);
    }
  }

  /** A page of template instances, for the API description. */
  @Schema(name = "InstanceArtifactPage")
  static final class OfInstances extends ArtifactPage {
    @ArraySchema(schema = @Schema(implementation = InstanceArtifactDocument.class))
    private List<JsonNode> artifacts;

    private OfInstances() {
      super(List.of(), null, 0, 1, 0);
    }
  }
}
