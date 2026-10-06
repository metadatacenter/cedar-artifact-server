package org.metadatacenter.cedar.artifact.resources.crud;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.metadatacenter.constant.LinkedData;
import org.metadatacenter.model.CedarResourceType;
import org.metadatacenter.cedar.artifact.resources.utils.TestUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Every conditional write the artifact server takes, with every kind of If-Match, on an artifact
 * that exists and on one that has been deleted.
 *
 * <p>The rule the table holds every write to: without If-Match, an existing artifact answers 428. A
 * current tag succeeds, as does {@code *}, a list holding the current tag and the current tag with a
 * representation suffix, and a stale, weak or malformed tag answers 412. A PUT to a deleted artifact
 * without If-Match creates it again; with one it answers 412, since the revision the caller read has
 * gone. A DELETE of a deleted artifact answers 404 whatever it sends. The group server answers by the
 * same rule.
 */
public class ConditionalWriteMatrixTest extends AbstractResourceCrudTest {

  private record Kind(CedarResourceType type, JsonNode sample) {}

  /** An If-Match value, from the artifact's current tag. */
  private record Tag(String name, Function<String, String> value, boolean matches) {}

  private static final List<Tag> TAGS = List.of(
      new Tag("absent", current -> null, false),
      new Tag("blank", current -> " ", false),
      new Tag("current", current -> current, true),
      new Tag("any", current -> "*", true),
      new Tag("a list holding the current tag", current -> "\"999\", " + current, true),
      new Tag("the current tag with a representation suffix",
          current -> current.substring(0, current.length() - 1) + "-yaml\"", true),
      new Tag("stale", current -> "\"999\"", false),
      new Tag("weak", current -> "W/" + current, false),
      new Tag("malformed", current -> "not-a-tag", false),
      new Tag("a list without the current tag", current -> "\"998\", \"999\"", false));

  private static boolean absent(Tag tag) {
    return tag.name().equals("absent") || tag.name().equals("blank");
  }

  private static int expected(String method, Tag tag, boolean exists) {
    if (method.equals("DELETE"))
      return !exists ? 404 : absent(tag) ? 428 : tag.matches() ? 204 : 412;
    if (!exists)
      return absent(tag) ? 201 : 412;
    return absent(tag) ? 428 : tag.matches() ? 200 : 412;
  }

  @Test
  public void everyConditionalWriteAnswersEveryIfMatchByTheSameRule() throws Exception {
    List<Kind> kinds = List.of(new Kind(CedarResourceType.TEMPLATE, sampleTemplate),
        new Kind(CedarResourceType.ELEMENT, sampleElement), new Kind(CedarResourceType.INSTANCE, sampleInstance));
    List<String> differences = new ArrayList<>();
    for (Kind kind : kinds) {
      for (String method : List.of("PUT", "DELETE")) {
        for (Tag tag : TAGS) {
          for (boolean exists : List.of(true, false)) {
            JsonNode prepared = setSchemaIsBasedOn(sampleTemplate.deepCopy(), kind.sample().deepCopy(), kind.type());
            JsonNode created = createResource(prepared, kind.type());
            String id = created.get(LinkedData.ID).asText();
            createdResources.put(id, kind.type());
            String url = TestUtil.getResourceUrlRoute(baseTestUrl, kind.type()) + "/"
                + URLEncoder.encode(id, StandardCharsets.UTF_8);
            String current = currentEtag(url, authHeader);
            if (!exists) {
              try (Response deleted = request(url, current).delete()) {
                Assertions.assertEquals(204, deleted.getStatus(), deleted.readEntity(String.class));
              }
            }
            Invocation.Builder write = request(url, tag.value().apply(current));
            try (Response answer = method.equals("PUT") ? write.put(Entity.json(created)) : write.delete()) {
              int expected = expected(method, tag, exists);
              if (answer.getStatus() != expected)
                differences.add(kind.type() + " " + method + " / " + tag.name() + " / "
                    + (exists ? "exists" : "deleted") + ": " + answer.getStatus() + " where " + expected);
            }
          }
        }
      }
    }
    Assertions.assertEquals(List.of(), differences);
  }

  private static Invocation.Builder request(String url, String ifMatch) {
    Invocation.Builder builder = testClient.target(url).request().header(HttpHeaders.AUTHORIZATION, authHeader);
    return ifMatch == null ? builder : builder.header(HttpHeaders.IF_MATCH, ifMatch);
  }
}
