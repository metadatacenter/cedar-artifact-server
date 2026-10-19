package org.metadatacenter.cedar.artifact.resources.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

/**
 * The judgement of a template that will replace one about to be created, held to the PUT it predicts.
 *
 * <p>The resource server publishes a template, creates its successor and then gives the successor the
 * definition its author submitted. The last step can refuse the definition, and by then the first two
 * have committed. The judgement lets it ask first. It is only worth asking if it answers as the PUT
 * will: a definition it refuses and the PUT accepts would stop an author for nothing, and one it
 * accepts and the PUT refuses would leave the publication and the successor behind.
 *
 * <p>Each case therefore asks twice. It asks for a judgement at one new identifier, and it makes the
 * real sequence at another: the create by PUT, then the replacing PUT with the create's ETag. The two
 * answers must agree, and the judgement must store nothing.
 */
public class ReplacementJudgementTest extends AbstractRestTest {

  private static final String TEMPLATE_YAML = """
      type: template
      name: Replacement Judgement Template
      children:
        - key: notes
          type: text-field
          name: Notes
      """;

  /** A change to the definition an author submits, and whether the PUT that applies it accepts it. */
  private record Edit(String name, boolean valid, Consumer<ObjectNode> change) {
    @Override
    public String toString() {
      return name;
    }
  }

  private static final List<Edit> EDITS = List.of(
      new Edit("the definition unchanged", true, definition -> {
      }),
      new Edit("a new name", true, definition -> definition.put("schema:name", "Renamed by its author")),
      new Edit("the derived title and description left out", true, definition -> {
        definition.remove("title");
        definition.remove("description");
      }),
      new Edit("the child identifier left out", false, definition ->
          ((ObjectNode) definition.path("properties").path("notes")).remove(LinkedData.ID)),
      new Edit("a child identifier that is not yet an IRI", true, definition ->
          ((ObjectNode) definition.path("properties").path("notes")).put(LinkedData.ID, "tmp-notes")),
      new Edit("the declaration of oslc:modifiedBy left out", false, definition ->
          ((ObjectNode) definition.path("properties")).remove("oslc:modifiedBy")),
      new Edit("the creation provenance left out", false, definition -> {
        definition.remove("pav:createdOn");
        definition.remove("pav:createdBy");
      }),
      new Edit("an empty name", false, definition -> definition.put("schema:name", "")),
      new Edit("a child that declares no type", false, definition ->
          ((ObjectNode) definition.path("properties").path("notes")).remove(LinkedData.TYPE)));

  static Stream<Arguments> edits() {
    List<Arguments> cases = new ArrayList<>();
    for (Edit edit : EDITS) {
      cases.add(Arguments.of(edit));
    }
    return cases.stream();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("edits")
  public void theJudgementAnswersAsTheReplacingPutDoes(Edit edit) throws IOException {
    ObjectNode source = storedTemplate();

    String judged = linkedDataUtil.buildNewLinkedDataId(CedarResourceType.TEMPLATE);
    ObjectNode body = JsonMapper.STRICT_MAPPER.createObjectNode();
    body.set("created", successorOf(source, judged));
    body.set("replacement", definitionFor(successorOf(source, judged), edit));
    Response judgement = request(url(judged) + "/replacement-judgement").post(Entity.json(body.toString()));
    String judgementBody = judgement.hasEntity() ? judgement.readEntity(String.class) : "";
    Assertions.assertTrue(judgement.getStatus() == 204 || judgement.getStatus() == 400,
        "the judgement accepts or refuses: " + judgement.getStatus() + " " + judgementBody);
    Assertions.assertEquals(404, request(url(judged)).get().getStatus(), "the judgement stored the successor");

    String written = linkedDataUtil.buildNewLinkedDataId(CedarResourceType.TEMPLATE);
    Response created = request(url(written)).put(Entity.json(successorOf(source, written).toString()));
    String createdBody = created.readEntity(String.class);
    Assertions.assertEquals(201, created.getStatus(), "the successor could not be created: " + createdBody);
    createdResources.put(written, CedarResourceType.TEMPLATE);
    Response replaced = request(url(written)).header("If-Match", created.getHeaderString("ETag"))
        .put(Entity.json(definitionFor(successorOf(source, written), edit).toString()));
    String replacedBody = replaced.readEntity(String.class);
    Assertions.assertTrue(replaced.getStatus() == 200 || replaced.getStatus() == 400,
        "the PUT stores the definition or refuses it: " + replaced.getStatus() + " " + replacedBody);

    Assertions.assertEquals(replaced.getStatus() == 200, judgement.getStatus() == 204,
        "the judgement answered " + judgement.getStatus() + " (" + judgementBody + ") and the PUT answered "
            + replaced.getStatus() + " (" + replacedBody + ")");
    Assertions.assertEquals(edit.valid(), judgement.getStatus() == 204,
        "the case no longer exercises what it names: " + judgementBody);
    if (judgement.getStatus() == 400) {
      JsonNode judgedRefusal = JsonMapper.STRICT_MAPPER.readTree(judgementBody);
      JsonNode putRefusal = JsonMapper.STRICT_MAPPER.readTree(replacedBody);
      Assertions.assertEquals(putRefusal.path("errorKey"), judgedRefusal.path("errorKey"), judgementBody);
      Assertions.assertEquals(putRefusal.path("errorReasonKey"), judgedRefusal.path("errorReasonKey"), judgementBody);
      Assertions.assertEquals(messages(putRefusal), messages(judgedRefusal),
          "the judgement refused for a different reason than the PUT did");
    }
  }

  @Test
  public void anIdentifierThatAlreadyNamesATemplateIsRefusedAndLeftAsItWas() throws IOException {
    ObjectNode source = storedTemplate();
    String id = source.path(LinkedData.ID).asText();
    ObjectNode body = JsonMapper.STRICT_MAPPER.createObjectNode();
    body.set("created", source.deepCopy());
    body.set("replacement", source.deepCopy());

    Response judgement = request(url(id) + "/replacement-judgement").post(Entity.json(body.toString()));
    String answer = judgement.readEntity(String.class);

    Assertions.assertEquals(412, judgement.getStatus(), answer);
    Assertions.assertEquals("nodeAlreadyPresent", JsonMapper.STRICT_MAPPER.readTree(answer).path("errorKey").asText(),
        answer);
    Assertions.assertEquals(source, read(id), "the judgement changed the stored template");
  }

  @Test
  public void aBodyThatIsNotTheTwoDocumentsIsRefused() throws IOException {
    ObjectNode source = storedTemplate();
    String judged = linkedDataUtil.buildNewLinkedDataId(CedarResourceType.TEMPLATE);
    ObjectNode onlyOne = JsonMapper.STRICT_MAPPER.createObjectNode();
    onlyOne.set("replacement", successorOf(source, judged));

    Response judgement = request(url(judged) + "/replacement-judgement").post(Entity.json(onlyOne.toString()));

    Assertions.assertEquals(400, judgement.getStatus(), judgement.readEntity(String.class));
    Assertions.assertEquals(404, request(url(judged)).get().getStatus());
  }

  /** A template as the server stores it, created for this case. */
  private ObjectNode storedTemplate() throws IOException {
    Response response = request(baseTestUrl + "/templates").post(Entity.entity(TEMPLATE_YAML, "application/yaml"));
    String created = response.readEntity(String.class);
    Assertions.assertEquals(201, response.getStatus(), "the fixture could not be created: " + created);
    String id = JsonMapper.STRICT_MAPPER.readTree(created).path(LinkedData.ID).asText();
    createdResources.put(id, CedarResourceType.TEMPLATE);
    return read(id);
  }

  private ObjectNode read(String id) throws IOException {
    Response response = request(url(id)).header("Accept", "application/json").get();
    String stored = response.readEntity(String.class);
    Assertions.assertEquals(200, response.getStatus(), stored);
    return (ObjectNode) JsonMapper.STRICT_MAPPER.readTree(stored);
  }

  /**
   * The document the resource server sends to create a template's successor: the stored template, at
   * the successor's identifier and next version, as a draft of the template it succeeds.
   */
  private static ObjectNode successorOf(ObjectNode source, String successorId) {
    ObjectNode successor = source.deepCopy();
    successor.put(LinkedData.ID, successorId);
    successor.put("pav:version", "0.0.2");
    successor.put("bibo:status", "bibo:draft");
    successor.put("pav:previousVersion", source.path(LinkedData.ID).asText());
    return successor;
  }

  /** The definition the author submits, with the successor's identity, as the resource server sends it. */
  private static ObjectNode definitionFor(ObjectNode successor, Edit edit) {
    ObjectNode definition = successor.deepCopy();
    edit.change().accept(definition);
    return definition;
  }

  private static List<String> messages(JsonNode refusal) {
    List<String> messages = new ArrayList<>();
    refusal.path("objects").path("validationReport").path("errors")
        .forEach(error -> messages.add(error.path("message").asText() + " at " + error.path("location").asText()));
    messages.sort(String::compareTo);
    messages.add(0, refusal.path("message").asText());
    return messages;
  }

  private static String url(String id) {
    return baseTestUrl + "/templates/" + URLEncoder.encode(id, StandardCharsets.UTF_8);
  }

  private static Invocation.Builder request(String url) {
    return testClient.target(url).request().header(AUTHORIZATION, authHeaderTestUser1);
  }
}
