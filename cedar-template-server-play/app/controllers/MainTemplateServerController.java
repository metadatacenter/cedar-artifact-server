package controllers;

import play.mvc.Result;

public class MainTemplateServerController extends AbstractTemplateServerController {

  public static Result index() {
    return ok("CEDAR REST Server. Its REST API is documented here: " + request().host() + "/assets/RESTAPI.html");
  }

  /* For CORS */
  public static Result preflight(String all) {
    response().setHeader("Access-Control-Allow-Origin", "*");
    response().setHeader("Allow", "*");
    response().setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
    response().setHeader("Access-Control-Allow-Headers",
        "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent");
    return ok();
  }

}
