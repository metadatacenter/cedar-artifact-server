package controllers;

import play.mvc.Controller;

import java.util.ArrayList;
import java.util.List;

public class GenericElementServerController extends Controller {

  protected static List<String> FIELD_NAMES_EXCLUSION_LIST;

  static {
    FIELD_NAMES_EXCLUSION_LIST = new ArrayList<>();
    FIELD_NAMES_EXCLUSION_LIST.add("_id");
  }

}
