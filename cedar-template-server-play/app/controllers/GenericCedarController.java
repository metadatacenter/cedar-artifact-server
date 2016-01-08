package controllers;

import play.Configuration;
import play.Play;
import play.mvc.Controller;

public class GenericCedarController extends Controller {
  protected static Configuration config;

  static {
    config = Play.application().configuration();
  }

}
