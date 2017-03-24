package org.metadatacenter.cedar.template.resources;

import java.util.List;

public interface ValidationReport {

  String getValidationStatus();

  List<String> getWarningMessages();

  List<String> getErrorMessages();
}
