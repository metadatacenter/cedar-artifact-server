package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.templates.TemplatesService;
import org.metadatacenter.templates.TemplatesServiceMongoDB;
import play.Configuration;
import play.Play;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import javax.management.InstanceNotFoundException;
import java.util.List;

public class CrudController extends Controller
{

  public static TemplatesService<String, JsonNode> templatesService;

  static {
    Configuration config = Play.application().configuration();
    templatesService = new TemplatesServiceMongoDB(config.getString("mongodb.db"),
      config.getString("mongodb.collections.templates"), config.getString("mongodb.collections.template_elements"),
      config.getString("mongodb.collections.template_instances"));
  }

  /* Templates */

  public static Result createTemplate()
  {
    try {
      JsonNode template = request().body().asJson();
      return ok(templatesService.createTemplate(template));
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result findTemplate(String templateId, boolean expanded, boolean validation)
  {
    try {
      JsonNode template = templatesService.findTemplate(templateId, expanded, validation);
      return ok(template);
    } catch (IllegalArgumentException e) {
      return badRequest();
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result findAllTemplates()
  {
    try {
      List<JsonNode> templates = templatesService.findAllTemplates();
      return ok(Json.toJson(templates));
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result updateTemplate(String templateId)
  {
    JsonNode modifications = request().body().asJson();
    try {
      JsonNode updatedTemplate = templatesService.updateTemplate(templateId, modifications);
      return ok(updatedTemplate);
    } catch (IllegalArgumentException e) {
      return badRequest();
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result deleteTemplate(String templateId)
  {
    try {
      templatesService.deleteTemplate(templateId);
      return ok();
    } catch (IllegalArgumentException e) {
      return badRequest();
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (Exception e) {
      return internalServerError();
    }
  }

  /* Template Elements */

  public static Result createTemplateElement()
  {
    try {
      JsonNode templateElement = request().body().asJson();
      return ok(templatesService.createTemplateElement(templateElement));
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result findAllTemplateElements()
  {
    try {
      List<JsonNode> templateElements = templatesService.findAllTemplateElements();
      return ok(Json.toJson(templateElements));
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result findTemplateElement(String templateElementId, boolean expanded, boolean validation)
  {
    try {
      JsonNode templateElement = templatesService.findTemplateElement(templateElementId, expanded, validation);
      return ok(templateElement);
    } catch (IllegalArgumentException e) {
      return badRequest();
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result updateTemplateElement(String templateElementId)
  {
    JsonNode modifications = request().body().asJson();
    try {
      JsonNode updatedTemplateElement = templatesService.updateTemplateElement(templateElementId, modifications);
      return ok(updatedTemplateElement);
    } catch (IllegalArgumentException e) {
      return badRequest();
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result deleteTemplateElement(String templateElementId)
  {
    try {
      templatesService.deleteTemplateElement(templateElementId);
      return ok();
    } catch (IllegalArgumentException e) {
      return badRequest();
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (Exception e) {
      return internalServerError();
    }
  }

  /* Template Instances */

  public static Result createTemplateInstance()
  {
    try {
      JsonNode templateInstance = request().body().asJson();
      return ok(templatesService.createTemplateInstance(templateInstance));
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result findAllTemplateInstances()
  {
    try {
      List<JsonNode> templateInstances = templatesService.findAllTemplateInstances();
      return ok(Json.toJson(templateInstances));
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result findTemplateInstance(String templateInstanceId)
  {
    try {
      JsonNode templateInstance = templatesService.findTemplateInstance(templateInstanceId);
      return ok(templateInstance);
    } catch (IllegalArgumentException e) {
      return badRequest();
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result updateTemplateInstance(String templateInstanceId)
  {
    JsonNode modifications = request().body().asJson();
    try {
      JsonNode updatedTemplateInstance = templatesService.updateTemplateInstance(templateInstanceId, modifications);
      return ok(updatedTemplateInstance);
    } catch (IllegalArgumentException e) {
      return badRequest();
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result deleteTemplateInstance(String templateInstanceId)
  {
    try {
      templatesService.deleteTemplateInstance(templateInstanceId);
      return ok();
    } catch (IllegalArgumentException e) {
      return badRequest();
    } catch (InstanceNotFoundException e) {
      return notFound();
    } catch (Exception e) {
      return internalServerError();
    }
  }

}
