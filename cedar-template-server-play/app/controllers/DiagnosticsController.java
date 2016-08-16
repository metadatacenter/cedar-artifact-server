package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.metadatacenter.server.service.DiagnosticsService;
import play.mvc.Result;

public class DiagnosticsController extends AbstractTemplateServerController {

  private static DiagnosticsService<JsonNode> diagnosticsService;

  public static void injectDiagnosticsService(DiagnosticsService<JsonNode> ds) {
    diagnosticsService = ds;
  }

  public static Result heartbeat() {
    try {
      ObjectNode heartbeat = (ObjectNode) diagnosticsService.heartbeat();
      return ok(heartbeat);
    } catch (Exception e) {
      return internalServerErrorWithError(e);
    }
  }
}
