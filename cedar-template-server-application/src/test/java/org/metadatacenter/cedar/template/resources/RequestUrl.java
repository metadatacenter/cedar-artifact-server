package org.metadatacenter.cedar.template.resources;

import javax.annotation.Nonnull;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

public class RequestUrl {

  public static final String SERVICE_BASE_URL = "http://template.metadatacenter.orgx";

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
}
