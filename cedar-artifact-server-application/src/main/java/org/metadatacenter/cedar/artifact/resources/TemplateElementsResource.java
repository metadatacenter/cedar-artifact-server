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
  public Response createTemplateElement(
      @QueryParam("compact") Optional<Boolean> compactParam,
      String requestBody) throws CedarException {
    return createArtifact(CedarPermission.TEMPLATE_ELEMENT_CREATE, CedarResourceType.ELEMENT,
        CedarErrorKey.TEMPLATE_ELEMENT_NOT_CREATED, requestBody, compactParam);
  }

  @GET
  @Timed
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  public Response findTemplateElement(@PathParam(PP_ID) String id,
                             @QueryParam("compact") Optional<Boolean> compactParam) throws CedarException {
    return findArtifact(id, CedarPermission.TEMPLATE_ELEMENT_READ, CedarErrorKey.TEMPLATE_ELEMENT_NOT_FOUND, CedarResourceType.ELEMENT, compactParam);
  }

  @GET
  @Timed
  public Response findAllTemplateElements(@QueryParam(QP_LIMIT) Optional<Integer> limitParam,
                                          @QueryParam(QP_OFFSET) Optional<Integer> offsetParam,
                                          @QueryParam(QP_SUMMARY) Optional<Boolean> summaryParam,
                                          @QueryParam(QP_FIELD_NAMES) Optional<String> fieldNamesParam) throws CedarException {
    return findAllArtifacts(limitParam, offsetParam, summaryParam, fieldNamesParam,
        CedarPermission.TEMPLATE_ELEMENT_READ, CedarErrorKey.TEMPLATE_ELEMENTS_NOT_LISTED);
  }

  @PUT
  @Timed
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  @Consumes({MediaType.APPLICATION_JSON, HttpConstants.CONTENT_TYPE_APPLICATION_YAML, "application/yaml"})
  public Response updateTemplateElement(@PathParam(PP_ID) String id,
                               @QueryParam("compact") Optional<Boolean> compactParam,
                               @QueryParam(QP_VERBATIM) Optional<Boolean> verbatimParam,
                               String requestBody) throws CedarException {
    return updateArtifact(id, CedarPermission.TEMPLATE_ELEMENT_CREATE, CedarPermission.TEMPLATE_ELEMENT_UPDATE,
        CedarResourceType.ELEMENT,
        CedarErrorKey.TEMPLATE_ELEMENT_NOT_UPDATED, CedarErrorKey.TEMPLATE_ELEMENT_NOT_CREATED, requestBody, compactParam, verbatimParam);
  }

  @DELETE
  @Timed
  @Path("/{id}")
  public Response deleteTemplateElement(@PathParam(PP_ID) String id) throws CedarException {
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
