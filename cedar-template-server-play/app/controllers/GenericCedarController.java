package controllers;

import play.Configuration;
import play.Play;
import play.mvc.Controller;

import static org.metadatacenter.server.Constants.*;

public class GenericCedarController extends Controller {
  protected static Configuration config;

  static {
    config = Play.application().configuration();
  }

  protected static boolean requestIsForLinkedData() {
    return config.getString(SERVER_LINKED_DATA).equals(request().host());
  }

  protected static boolean requestIsForRESTAPI() {
    return config.getString(SERVER_REST_API).equals(request().host());
  }

  protected static void checkServerName() {
    if (!requestIsForLinkedData() && !requestIsForRESTAPI()) {
      throw new IllegalArgumentException("Unknown server name:" + request().getHeader(HTTP_HEADER_HOST) + "!");
    }
  }
}
