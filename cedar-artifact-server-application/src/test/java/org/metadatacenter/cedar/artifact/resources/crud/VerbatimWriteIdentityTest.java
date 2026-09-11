package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.http.CedarResponseStatus;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.mongo.MongoUtils;
import org.metadatacenter.util.test.TestAuthUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Named.named;
import static org.metadatacenter.model.ModelNodeNames.SCHEMA_IS_BASED_ON;

/**
 * One property, over every defect a stored artifact is known to carry: a verbatim write stores the
 * document it was given, or stores nothing at all.
 *
 * <p>A verbatim write exists so a defect that lives in the stored representation can be corrected,
 * which only works if the server writes what the caller stated. The write path skips its
 * normalizations one by one for that reason, and each skip was pinned by a test of its own. That is
 * the wrong shape for the promise. A normalization added later somewhere else is covered by none of
 * those tests, which is exactly what happened: instance validation pruned dead context terms on its
 * way to a verdict, so the server answered success and stored a document the caller never sent.
 *
 * <p>Stated once instead, and over a corpus rather than a case: for each defect shape, the artifact
 * is stored carrying it, resubmitted verbatim, and the outcome has to be one of two things. Accepted
 * means the stored document equals the submitted one exactly. Refused means storage did not move.
 * Anything else is the server rewriting a caller's document behind a success.
 */
public class VerbatimWriteIdentityTest extends AbstractResourceCrudTest {

  private static final String FIELD_NAME = "A Field";
  private static final String ATTRIBUTE_VALUE_FIELD_NAME = "Additional Information";
  private static final String PROPERTY_IRI_PREFIX = "https://schema.metadatacenter.org/properties/";

  private static Stream<Arguments> templateDefects() {
    return Stream.of(
        Arguments.of(named("a template that derives from nothing in particular",
            (UnaryOperator<ObjectNode>) template -> {
              template.put("pav:derivedFrom", "");
              return template;
            })),
        Arguments.of(named("a child that derives from nothing in particular",
            (UnaryOperator<ObjectNode>) template -> {
              ((ObjectNode) template.path("properties").path(FIELD_NAME)).put("pav:derivedFrom", "");
              return template;
            })),
        Arguments.of(named("a child declaring no schema of its own",
            (UnaryOperator<ObjectNode>) template -> {
              ((ObjectNode) template.path("properties").path(FIELD_NAME)).remove("$schema");
              return template;
            })),
        Arguments.of(named("a property IRI no instance could use",
            (UnaryOperator<ObjectNode>) template -> {
              ObjectNode mapping = (ObjectNode) template.path("properties").path(LinkedData.CONTEXT)
                  .path("properties").path(FIELD_NAME);
              mapping.putArray("enum").add("not an absolute iri");
              return template;
            })),
        Arguments.of(named("a page layout the model does not allow",
            (UnaryOperator<ObjectNode>) template -> {
              ((ObjectNode) template.path("_ui")).putArray("pages");
              return template;
            })));
  }

  private static Stream<Arguments> instanceDefects() {
    return Stream.of(
        Arguments.of(named("a context term for an attribute nothing names",
            (UnaryOperator<ObjectNode>) instance -> {
              ((ObjectNode) instance.get(LinkedData.CONTEXT))
                  .put("orphan", PROPERTY_IRI_PREFIX + "orphan-term");
              return instance;
            })),
        Arguments.of(named("attribute names the model forbids",
            (UnaryOperator<ObjectNode>) instance -> {
              ((ArrayNode) instance.get(ATTRIBUTE_VALUE_FIELD_NAME)).add(LinkedData.CONTEXT);
              return instance;
            })),
        Arguments.of(named("a duplicated attribute name",
            (UnaryOperator<ObjectNode>) instance -> {
              ArrayNode names = (ArrayNode) instance.get(ATTRIBUTE_VALUE_FIELD_NAME);
              names.add(names.get(0).asText());
              return instance;
            })));
  }

  @ParameterizedTest
  @MethodSource("templateDefects")
  public void aVerbatimTemplateWriteStoresTheSubmittedDocumentOrNothing(UnaryOperator<ObjectNode> defect)
      throws Exception {
    ObjectNode created = createTemplateWithChildren();
    String id = created.get(LinkedData.ID).asText();
    ObjectNode defective = defect.apply(created.deepCopy());
    TestUtil.templateService.updateTemplate(id, defective.deepCopy(),
        TestUtil.templateService.getTemplateRevision(id));

    assertStoresItOrNothing(defective, id, CedarResourceType.TEMPLATE,
        () -> TestUtil.templateService.findTemplate(id));
  }

  @ParameterizedTest
  @MethodSource("instanceDefects")
  public void aVerbatimInstanceWriteStoresTheSubmittedDocumentOrNothing(UnaryOperator<ObjectNode> defect)
      throws Exception {
    ObjectNode template = createTemplateWithChildren();
    ObjectNode created = createInstanceOf(template);
    String id = created.get(LinkedData.ID).asText();
    ObjectNode defective = defect.apply(created.deepCopy());
    TestUtil.templateInstanceService.updateTemplateInstance(id, defective.deepCopy(),
        TestUtil.templateInstanceService.getTemplateInstanceRevision(id));

    assertStoresItOrNothing(defective, id, CedarResourceType.INSTANCE,
        () -> TestUtil.templateInstanceService.findTemplateInstance(id));
  }

  /** Reads storage rather than the response, since the promise is about what was stored. */
  @FunctionalInterface
  private interface StoredArtifact {
    JsonNode read() throws IOException;
  }

  private void assertStoresItOrNothing(ObjectNode submitted, String id, CedarResourceType resourceType,
                                       StoredArtifact storage) throws Exception {
    JsonNode before = stripped(storage.read());

    Response response = verbatimPut(submitted, id, resourceType);
    int status = response.getStatus();
    response.close();

    JsonNode after = stripped(storage.read());
    if (status == CedarResponseStatus.OK.getStatusCode()) {
      Assertions.assertEquals(submitted, after,
          "a verbatim write reported success while storing a different document");
    } else {
      Assertions.assertEquals(CedarResponseStatus.BAD_REQUEST.getStatusCode(), status,
          "a verbatim write is either honoured or refused, and this was neither");
      Assertions.assertEquals(before, after, "a refused verbatim write moved what was stored");
    }
  }

  /** Mongo's own key is storage bookkeeping and is not part of the document either side stated. */
  private static JsonNode stripped(JsonNode storedArtifact) {
    JsonNode copy = storedArtifact.deepCopy();
    MongoUtils.removeIdField(copy);
    return copy;
  }

  private ObjectNode createTemplateWithChildren() {
    ObjectNode template = sampleTemplate.deepCopy();
    addChild(template, FIELD_NAME, textField(FIELD_NAME));
    addChild(template, ATTRIBUTE_VALUE_FIELD_NAME, attributeValueField(ATTRIBUTE_VALUE_FIELD_NAME));

    // Instances of this template name attributes of their own, so their context carries terms the
    // template does not declare and the schema has to admit them.
    ObjectNode contextSchema = (ObjectNode) template.get("properties").get(LinkedData.CONTEXT);
    ObjectNode contextAdditions = contextSchema.putObject("additionalProperties");
    contextAdditions.put("type", "string");
    contextAdditions.put("format", "uri");

    ObjectNode valueSchema = template.putObject("additionalProperties");
    valueSchema.put("type", "object");
    ObjectNode valueProperties = valueSchema.putObject("properties");
    valueProperties.putObject("@value").putArray("type").add("string").add("null");
    valueProperties.putObject("@type").put("type", "string").put("format", "uri");
    valueSchema.putArray("required").add("@value");
    valueSchema.put("additionalProperties", false);

    String url = TestUtil.getResourceUrlRoute(baseTestUrl, CedarResourceType.TEMPLATE);
    Response created = testClient.target(url)
        .property(ClientProperties.READ_TIMEOUT, 15000)
        .request().header("Authorization", authHeader).post(Entity.json(template));
    Assertions.assertEquals(CedarResponseStatus.CREATED.getStatusCode(), created.getStatus());
    ObjectNode stored = (ObjectNode) created.readEntity(JsonNode.class);
    createdResources.put(stored.get(LinkedData.ID).asText(), CedarResourceType.TEMPLATE);
    return stored;
  }

  private ObjectNode createInstanceOf(ObjectNode template) {
    ObjectNode instance = sampleInstance.deepCopy();
    instance.put(SCHEMA_IS_BASED_ON, template.get(LinkedData.ID).asText());
    ((ObjectNode) instance.get(LinkedData.CONTEXT)).put(FIELD_NAME,
        template.get("properties").get(LinkedData.CONTEXT).get("properties").get(FIELD_NAME)
            .get("enum").get(0).asText());
    instance.putArray(ATTRIBUTE_VALUE_FIELD_NAME).add("named");
    instance.putObject("named").put("@value", "a value");

    ObjectNode stored = (ObjectNode) createResource(instance, CedarResourceType.INSTANCE);
    createdResources.put(stored.get(LinkedData.ID).asText(), CedarResourceType.INSTANCE);
    return stored;
  }

  /** A text field the model accepts, built the way the update tests build one. */
  private ObjectNode textField(String fieldName) {
    ObjectNode field = sampleElement.deepCopy();
    field.put("$schema", "http://json-schema.org/draft-04/schema#");
    field.put("@type", "https://schema.metadatacenter.org/core/TemplateField");
    field.put("schema:name", fieldName);
    field.put("schema:schemaVersion", "1.5.0");
    field.remove("pav:version");
    field.remove("bibo:status");
    ObjectNode fieldContext = (ObjectNode) field.get(LinkedData.CONTEXT);
    fieldContext.put("skos", "http://www.w3.org/2004/02/skos/core#");
    fieldContext.putObject("skos:prefLabel").put("@type", "xsd:string");
    fieldContext.putObject("skos:altLabel").put("@type", "xsd:string");
    ObjectNode fieldUi = field.putObject("_ui");
    fieldUi.put("inputType", "textfield");
    field.putObject("_valueConstraints").put("requiredValue", false);
    ObjectNode fieldProperties = field.putObject("properties");
    ObjectNode typeProperty = fieldProperties.putObject("@type");
    ArrayNode typeAlternatives = typeProperty.putArray("oneOf");
    typeAlternatives.addObject().put("type", "string").put("format", "uri");
    ObjectNode typeArray = typeAlternatives.addObject();
    typeArray.put("type", "array");
    typeArray.put("minItems", 1);
    typeArray.putObject("items").put("type", "string").put("format", "uri");
    typeArray.put("uniqueItems", true);
    fieldProperties.putObject("rdfs:label").putArray("type").add("string").add("null");
    fieldProperties.putObject("@value").putArray("type").add("string").add("null");
    ObjectNode language = fieldProperties.putObject("@language");
    language.putArray("type").add("string").add("null");
    language.put("minLength", 1);
    field.putArray("required").add("@value");
    return field;
  }

  /** The array-of-attribute-names field, whose instances name attributes of their own. */
  private ObjectNode attributeValueField(String fieldName) {
    ObjectNode field = sampleElement.deepCopy();
    field.put("@type", "https://schema.metadatacenter.org/core/TemplateField");
    field.put("type", "string");
    field.put("schema:name", fieldName);
    field.put("schema:schemaVersion", "1.5.0");
    field.remove("properties");
    field.remove("required");
    field.remove("pav:version");
    field.remove("bibo:status");
    ObjectNode fieldContext = (ObjectNode) field.get(LinkedData.CONTEXT);
    fieldContext.put("skos", "http://www.w3.org/2004/02/skos/core#");
    fieldContext.putObject("skos:prefLabel").put("@type", "xsd:string");
    fieldContext.putObject("skos:altLabel").put("@type", "xsd:string");
    ObjectNode fieldUi = field.putObject("_ui");
    fieldUi.put("inputType", "attribute-value");

    ObjectNode wrapper = ((ObjectNode) sampleTemplate).objectNode();
    wrapper.put("type", "array");
    wrapper.put("minItems", 0);
    wrapper.set("items", field);
    return wrapper;
  }

  private void addChild(ObjectNode template, String childName, ObjectNode child) {
    ObjectNode ui = (ObjectNode) template.get("_ui");
    ((ArrayNode) ui.get("order")).add(childName);
    ((ObjectNode) ui.get("propertyLabels")).put(childName, childName);
    ((ObjectNode) ui.get("propertyDescriptions")).put(childName, "");
    ((ObjectNode) template.get("properties")).set(childName, child);
  }

  private Response verbatimPut(JsonNode artifact, String id, CedarResourceType resourceType) throws IOException {
    String url = TestUtil.getResourceUrlRoute(baseTestUrl, resourceType);
    String adminAuthHeader = TestAuthUtil.getAdminUserAuthHeader(TestUtil.getCedarConfig());
    return testClient.target(url + "/" + URLEncoder.encode(id, "UTF-8"))
        .queryParam("verbatim", true)
        .request().header("Authorization", adminAuthHeader)
        .header("If-Match", currentEtag(url + "/" + URLEncoder.encode(id, "UTF-8"), adminAuthHeader))
        .put(Entity.json(artifact));
  }
}
