package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.error.CedarErrorKey;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.mongo.MongoUtils;
import org.metadatacenter.util.test.TestAuthUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Named.named;

/**
 * The artifact server refuses each write below, and every refusal leaves storage as it was.
 *
 * <p>A verbatim write stores the document it is sent or nothing. It cannot keep that promise for a YAML
 * body, which is transcoded, for an identifier that holds nothing, which would make it a create, or for a
 * child identifier that is not an absolute IRI, which an ordinary write would mint over. An ordinary write
 * normalizes what it is sent before storing it, and is refused when the normalized document is invalid
 * even though the request was not. The inclusion route replaces a template and never creates one.
 */
public class WriteRefusalTest extends AbstractResourceCrudTest {

  private static final String APPLICATION_YAML = "application/yaml";
  private static final String TEMPORARY_ID = "tmp-1729";

  private static final String TEMPLATE_WITH_NESTED_CHILDREN = """
      type: template
      name: Refusal Template
      children:
        - key: note
          type: text-field
          name: Note
        - key: address
          type: element
          name: Address
          children:
            - key: street
              type: text-field
              name: Street
      """;

  private static final String ELEMENT_WITH_A_CHILD = """
      type: element
      name: Refusal Element
      children:
        - key: street
          type: text-field
          name: Street
      """;

  private static String route(CedarResourceType type) {
    return TestUtil.getResourceUrlRoute(baseTestUrl, type);
  }

  private static String url(CedarResourceType type, String id) {
    return route(type) + "/" + URLEncoder.encode(id, StandardCharsets.UTF_8);
  }

  private static ObjectNode createFromYaml(CedarResourceType type, String yaml) {
    try (Response created = testClient.target(route(type)).property(ClientProperties.READ_TIMEOUT, 15000)
        .request("application/json").header("Authorization", authHeader)
        .post(Entity.entity(yaml, APPLICATION_YAML))) {
      String body = created.readEntity(String.class);
      Assertions.assertEquals(201, created.getStatus(), body);
      ObjectNode stored = (ObjectNode) JsonMapper.STRICT_MAPPER.readTree(body);
      createdResources.put(stored.get(LinkedData.ID).asText(), type);
      return stored;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static ObjectNode createTemplate() {
    return createFromYaml(CedarResourceType.TEMPLATE, TEMPLATE_WITH_NESTED_CHILDREN);
  }

  private static ObjectNode create(CedarResourceType type) {
    return switch (type) {
      case TEMPLATE -> createTemplate();
      case ELEMENT -> createFromYaml(type, ELEMENT_WITH_A_CHILD);
      case FIELD -> createFromYaml(type, "type: text-field\nname: Refusal Field\n");
      case INSTANCE -> createFromYaml(type, "type: instance\nname: Refusal Instance\nisBasedOn: "
          + createTemplate().get(LinkedData.ID).asText() + "\n");
      default -> throw new IllegalArgumentException(type.toString());
    };
  }

  /** Reads the artifact as storage holds it, or null when there is none, leaving out Mongo's own key. */
  private static JsonNode stored(CedarResourceType type, String id) throws IOException {
    JsonNode document = switch (type) {
      case TEMPLATE -> TestUtil.templateService.findTemplate(id);
      case ELEMENT -> TestUtil.templateElementService.findTemplateElement(id);
      case FIELD -> TestUtil.templateFieldService.findTemplateField(id);
      case INSTANCE -> TestUtil.templateInstanceService.findTemplateInstance(id);
      default -> throw new IllegalArgumentException(type.toString());
    };
    if (document != null) {
      MongoUtils.removeIdField(document);
    }
    return document;
  }

  /** Replaces what storage holds, past every check a write makes, as a legacy write once did. */
  private static void storeUnchecked(CedarResourceType type, String id, JsonNode document) throws Exception {
    switch (type) {
      case TEMPLATE -> TestUtil.templateService.updateTemplate(id, document.deepCopy(),
          TestUtil.templateService.getTemplateRevision(id));
      case ELEMENT -> TestUtil.templateElementService.updateTemplateElement(id, document.deepCopy(),
          TestUtil.templateElementService.getTemplateElementRevision(id));
      case FIELD -> TestUtil.templateFieldService.updateTemplateField(id, document.deepCopy(),
          TestUtil.templateFieldService.getTemplateFieldRevision(id));
      case INSTANCE -> TestUtil.templateInstanceService.updateTemplateInstance(id, document.deepCopy(),
          TestUtil.templateInstanceService.getTemplateInstanceRevision(id));
      default -> throw new IllegalArgumentException(type.toString());
    }
  }

  private record Reply(int status, JsonNode body) {
    String errorKey() {
      return body.path("errorKey").asText();
    }
  }

  private static Reply reply(Response response) throws IOException {
    try (response) {
      String entity = response.readEntity(String.class);
      return new Reply(response.getStatus(), entity == null || entity.isBlank()
          ? JsonMapper.STRICT_MAPPER.createObjectNode() : JsonMapper.STRICT_MAPPER.readTree(entity));
    }
  }

  private static Invocation.Builder verbatim(CedarResourceType type, String id, boolean withEtag) {
    String admin = TestAuthUtil.getAdminUserAuthHeader(TestUtil.getCedarConfig());
    Invocation.Builder request = testClient.target(url(type, id)).queryParam("verbatim", true)
        .property(ClientProperties.READ_TIMEOUT, 15000)
        .request("application/json").header("Authorization", admin);
    return withEtag ? request.header("If-Match", currentEtag(url(type, id), admin)) : request;
  }

  private static Reply verbatimPut(CedarResourceType type, String id, JsonNode body) throws IOException {
    return reply(verbatim(type, id, true).put(Entity.json(body.toString())));
  }

  private static void assertVerbatimRefused(Reply reply) {
    Assertions.assertEquals(400, reply.status(), reply.body().toString());
    Assertions.assertEquals(CedarErrorKey.VERBATIM_WRITE_REFUSED.getValue(), reply.errorKey(),
        reply.body().toString());
  }

  // Verbatim writes

  @Test
  public void aVerbatimWriteWithAYamlBodyIsRefused() throws Exception {
    String id = createTemplate().get(LinkedData.ID).asText();
    JsonNode before = stored(CedarResourceType.TEMPLATE, id);

    Reply written = reply(verbatim(CedarResourceType.TEMPLATE, id, true)
        .put(Entity.entity(TEMPLATE_WITH_NESTED_CHILDREN, APPLICATION_YAML)));

    assertVerbatimRefused(written);
    Assertions.assertEquals(before, stored(CedarResourceType.TEMPLATE, id));
  }

  @Test
  public void aVerbatimWriteToAnIdentifierThatHoldsNothingIsRefusedAndCreatesNothing() throws Exception {
    ObjectNode source = create(CedarResourceType.ELEMENT);
    String id = linkedDataUtil.buildNewLinkedDataId(CedarResourceType.ELEMENT);
    ObjectNode submitted = source.deepCopy().put(LinkedData.ID, id);

    Reply written = reply(verbatim(CedarResourceType.ELEMENT, id, false).put(Entity.json(submitted.toString())));

    Assertions.assertEquals(404, written.status(), written.body().toString());
    Assertions.assertEquals(CedarErrorKey.ARTIFACT_NOT_FOUND.getValue(), written.errorKey(), written.body().toString());
    Assertions.assertNull(stored(CedarResourceType.ELEMENT, id), "a verbatim write created an artifact");
  }

  private static Stream<Arguments> childIdentifierPlacements() {
    return Stream.of(
        Arguments.of(named("a field of a template", CedarResourceType.TEMPLATE), "/properties/note", "note"),
        Arguments.of(named("an element of a template", CedarResourceType.TEMPLATE), "/properties/address",
            "address"),
        Arguments.of(named("a field of an element of a template", CedarResourceType.TEMPLATE),
            "/properties/address/properties/street", "address/street"),
        Arguments.of(named("a field of an element", CedarResourceType.ELEMENT), "/properties/street", "street"));
  }

  /**
   * An ordinary write mints an identifier over a child's that is not an absolute IRI, at every depth. A
   * verbatim write alters nothing, so it refuses instead, at every depth: a temporary identifier stored
   * anywhere resolves to nothing and no later write replaces it.
   */
  @ParameterizedTest
  @MethodSource("childIdentifierPlacements")
  public void aVerbatimWriteRefusesAChildIdentifierThatIsNotAnAbsoluteIri(CedarResourceType type, String child,
                                                                          String reported) throws Exception {
    String id = create(type).get(LinkedData.ID).asText();
    JsonNode before = stored(type, id);
    ObjectNode submitted = before.deepCopy();
    ((ObjectNode) submitted.at(child)).put(LinkedData.ID, TEMPORARY_ID);

    Reply written = verbatimPut(type, id, submitted);

    assertVerbatimRefused(written);
    Assertions.assertEquals(reported, written.body().path("parameters").path("children").asText(),
        written.body().toString());
    Assertions.assertEquals(before, stored(type, id), "a refused verbatim write moved what was stored");
  }

  @Test
  public void aVerbatimInstanceWriteRefusesAnElementOccurrenceIdentifierThatIsNotAnAbsoluteIri()
      throws Exception {
    String id = create(CedarResourceType.INSTANCE).get(LinkedData.ID).asText();
    JsonNode before = stored(CedarResourceType.INSTANCE, id);
    ObjectNode submitted = before.deepCopy();
    ((ObjectNode) submitted.get("address")).put(LinkedData.ID, TEMPORARY_ID);

    Reply written = verbatimPut(CedarResourceType.INSTANCE, id, submitted);

    assertVerbatimRefused(written);
    Assertions.assertEquals("/address", written.body().path("parameters").path("occurrences").asText(),
        written.body().toString());
    Assertions.assertEquals(before, stored(CedarResourceType.INSTANCE, id));
  }

  /** A field has no children, so nothing in it can hold a child identifier to refuse. */
  @Test
  public void aVerbatimFieldWriteStoresTheFieldAsSent() throws Exception {
    String id = create(CedarResourceType.FIELD).get(LinkedData.ID).asText();
    ObjectNode submitted = stored(CedarResourceType.FIELD, id).deepCopy();
    submitted.put("schema:description", "Stated by a repair");

    Reply written = verbatimPut(CedarResourceType.FIELD, id, submitted);

    Assertions.assertEquals(200, written.status(), written.body().toString());
    Assertions.assertEquals(submitted, stored(CedarResourceType.FIELD, id));
  }

  // Normalization

  /**
   * An update takes its creation provenance from what is stored, whatever the request claims. When the
   * stored value is one the model refuses, the request is valid and the document the write would store
   * is not, so the write is refused and storage keeps what it held. Every kind of artifact answers so.
   */
  @ParameterizedTest
  @EnumSource(value = CedarResourceType.class, names = {"TEMPLATE", "ELEMENT", "FIELD", "INSTANCE"})
  public void anUpdateWhoseNormalizedDocumentIsInvalidIsRefused(CedarResourceType type) throws Exception {
    ObjectNode created = create(type);
    String id = created.get(LinkedData.ID).asText();
    ObjectNode legacy = stored(type, id).deepCopy();
    legacy.put("pav:createdOn", 1729);
    storeUnchecked(type, id, legacy);
    JsonNode before = stored(type, id);
    ObjectNode submitted = created.deepCopy();
    submitted.put("schema:name", "Renamed over a legacy creation date");

    Reply written = reply(testClient.target(url(type, id)).property(ClientProperties.READ_TIMEOUT, 15000)
        .request("application/json").header("Authorization", authHeader)
        .header("If-Match", currentEtag(url(type, id), authHeader)).put(Entity.json(submitted.toString())));

    Assertions.assertEquals(400, written.status(), written.body().toString());
    Assertions.assertEquals(CedarErrorKey.INVALID_DATA.getValue(), written.errorKey(), written.body().toString());
    Assertions.assertEquals(before, stored(type, id), "a refused write moved what was stored");
  }

  // The inclusion route

  @Test
  public void anInclusionUpdateNeverCreatesATemplate() throws Exception {
    ObjectNode source = createTemplate();
    String id = linkedDataUtil.buildNewLinkedDataId(CedarResourceType.TEMPLATE);
    ObjectNode submitted = source.deepCopy().put(LinkedData.ID, id);

    Reply written = reply(testClient.target(url(CedarResourceType.TEMPLATE, id) + "/inclusion")
        .property(ClientProperties.READ_TIMEOUT, 15000)
        .request("application/json").header("Authorization", authHeader).put(Entity.json(submitted.toString())));

    Assertions.assertEquals(412, written.status(), written.body().toString());
    Assertions.assertEquals(CedarErrorKey.ARTIFACT_HAS_MOVED_ON.getValue(), written.errorKey(),
        written.body().toString());
    Assertions.assertNull(stored(CedarResourceType.TEMPLATE, id), "an inclusion update created a template");
  }
}
