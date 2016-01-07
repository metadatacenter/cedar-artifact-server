package controllers;

import play.mvc.Result;

public class Application extends GenericCedarController {

  public static Result index() {
    if (requestIsForRESTAPI()) {
      return ok("CEDAR REST Server. Its REST API is documented here: " + request().host() + "/assets/RESTAPI.html");
    } else if (requestIsForLinkedData()) {
      return ok("CEDAR Repo Server.");
    }
    return badRequest("Unknown server requested.");
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
