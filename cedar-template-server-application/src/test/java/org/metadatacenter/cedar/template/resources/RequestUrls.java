package org.metadatacenter.cedar.template.resources;

import javax.annotation.Nonnull;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public class RequestUrls {

  public static final String SERVICE_BASE_URL = "http://localhost";

  public static String forCreatingInstances(int portNumber, @Nonnull String importMode) {
    checkNotNull(importMode);
    return String.format("%s:%d/template-instances?import_mode=%s", SERVICE_BASE_URL, portNumber, importMode);
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
    return String.format("%s:%d/templates/commands/validate", SERVICE_BASE_URL, portNumber);
  }
}
