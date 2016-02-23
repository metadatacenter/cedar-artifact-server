package controllers;

import org.metadatacenter.server.play.AbstractCedarController;
import play.mvc.Result;

public class MainTemplateServerController extends AbstractTemplateServerController {

  public static Result index() {
    return ok("CEDAR REST Server. Its REST API is documented here: " + request().host() + "/assets/RESTAPI.html");
  }

  /* For CORS */
  public static Result preflight(String all) {
    return AbstractCedarController.preflight(all);
  }

}
