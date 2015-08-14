package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.templates.TemplateServerNames;
import org.metadatacenter.templates.mongodb.TemplatesServiceMongoDB;
import org.metadatacenter.templates.service.TemplatesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.Play;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import javax.management.InstanceNotFoundException;
import java.util.List;

public class TemplateServerController extends Controller
{
  private static Logger log = LoggerFactory.getLogger(TemplateServerController.class);

  public static final TemplatesService<String, JsonNode> templatesService;

  static {
    Configuration config = Play.application().configuration();
    templatesService = new TemplatesServiceMongoDB(config.getString(TemplateServerNames.MONGODB_DATABASE_NAME),
      config.getString(TemplateServerNames.TEMPLATES_COLLECTION_NAME),
      config.getString(TemplateServerNames.TEMPLATE_ELEMENTS_COLLECTION_NAME),
      config.getString(TemplateServerNames.TEMPLATE_INSTANCES_COLLECTION_NAME));
  }

  /* Templates */

  public static Result createTemplate()
  {
    log.info("Received create template request");

    try {
      JsonNode template = request().body().asJson();
      return ok(templatesService.createTemplateLinkedData(template));
    } catch (IllegalArgumentException e) {
      return badRequest();
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result findTemplate(String templateId, boolean expanded, boolean validation)
  {
    log.info("Received findTemplate request with template ID "+ templateId);

    try {
      JsonNode template = templatesService.findTemplateByLinkedDataId(templateId, expanded, validation);
      if (template!=null)
        return ok(template);
      return notFound();
    } catch (IllegalArgumentException e) {
      return badRequest();
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
      JsonNode updatedTemplate = templatesService.updateTemplateByLinkedDataId(templateId, modifications);
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
      templatesService.deleteTemplateByLinkedDataId(templateId);
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
      return ok(templatesService.createTemplateElementLinkedData(templateElement));
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
      JsonNode templateElement = templatesService.findTemplateElementByLinkedDataId(templateElementId, expanded,
        validation);
      if (templateElement!=null)
        return ok(templateElement);
      return notFound();
    } catch (IllegalArgumentException e) {
      return badRequest();
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result updateTemplateElement(String templateElementId)
  {
    JsonNode modifications = request().body().asJson();
    try {
      JsonNode updatedTemplateElement = templatesService.updateTemplateElementByLinkedDataId(templateElementId,
        modifications);
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
      templatesService.deleteTemplateElementByLinkedDataId(templateElementId);
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
      return ok(templatesService.createTemplateInstanceLinkedData(templateInstance));
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
      JsonNode templateInstance = templatesService.findTemplateInstanceByLinkedDataId(templateInstanceId);
      if (templateInstance!=null)
        return ok(templateInstance);
      return notFound();
    } catch (IllegalArgumentException e) {
      return badRequest();
    } catch (Exception e) {
      return internalServerError();
    }
  }

  public static Result updateTemplateInstance(String templateInstanceId)
  {
    JsonNode modifications = request().body().asJson();
    try {
      JsonNode updatedTemplateInstance = templatesService.updateTemplateInstanceByLinkedDataId(templateInstanceId,
        modifications);
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
      templatesService.deleteTemplateInstanceByLinkedDataId(templateInstanceId);
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
