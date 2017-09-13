package org.metadatacenter.cedar.template.resources;

import javax.annotation.Nonnull;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.metadatacenter.constant.CedarQueryParameters.QP_IMPORT_MODE;

public class RequestUrls {

  public static final String SERVICE_BASE_URL = "http://localhost";

  public static String forCreatingTemplate(int portNumber, @Nonnull String importMode) {
    checkNotNull(importMode);
    return String.format("%s:%d/templates?%s=%s", SERVICE_BASE_URL, portNumber, QP_IMPORT_MODE, importMode);
  }

  public static String forDeletingTemplate(int portNumber, @Nonnull String templateId) {
    checkNotNull(templateId);
    return String.format("%s:%d/templates/%s", SERVICE_BASE_URL, portNumber, templateId);
  }

  public static String forFindingTemplate(int portNumber, @Nonnull String templateId) {
    checkNotNull(templateId);
    return String.format("%s:%d/templates/%s", SERVICE_BASE_URL, portNumber, templateId);
  }

  public static String forCreatingInstances(int portNumber, @Nonnull String importMode) {
    checkNotNull(importMode);
    return String.format("%s:%d/template-instances?%s=%s", SERVICE_BASE_URL, portNumber, QP_IMPORT_MODE, importMode);
  }

  public static String forDeletingInstance(int portNumber, @Nonnull String instanceId) {
    checkNotNull(instanceId);
    return String.format("%s:%d/template-instances/%s", SERVICE_BASE_URL, portNumber, instanceId);
  }

  public static String forFindingInstance(int portNumber, @Nonnull String instanceId, @Nonnull String formatType) {
    checkNotNull(instanceId);
    checkNotNull(formatType);
    return String.format("%s:%d/template-instances/%s?format=%s", SERVICE_BASE_URL, portNumber, instanceId, formatType);
  }

  public static String forValidatingTemplate(int portNumber) {
    return String.format("%s:%d/command/validate?resource_type=template", SERVICE_BASE_URL, portNumber);
  }

  public static String forValidatingElement(int portNumber) {
    return String.format("%s:%d/command/validate?resource_type=element", SERVICE_BASE_URL, portNumber);
  }

  public static String forValidatingField(int portNumber) {
    return String.format("%s:%d/command/validate?resource_type=field", SERVICE_BASE_URL, portNumber);
  }

  public static String forValidatingInstance(int portNumber) {
    return String.format("%s:%d/command/validate?resource_type=instance", SERVICE_BASE_URL, portNumber);
  }
}
