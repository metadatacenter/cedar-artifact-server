package org.metadatacenter.cedar.artifact.resources;

import org.metadatacenter.model.validation.ValidationFinding;
import org.metadatacenter.model.validation.report.CedarValidationReport;
import org.metadatacenter.model.validation.report.ErrorItem;
import org.metadatacenter.model.validation.report.ValidationReport;
import org.metadatacenter.model.validation.report.WarningItem;
import org.slf4j.Logger;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Writes a write's validation report to the log in the form of {@code ReportUtils.outputLogger},
 * leaving out values whose semantic verification was unavailable. {@link SemanticValueVerification}
 * logs those as one line per write, and a large instance holds hundreds of them. The report returned
 * to the caller still lists each one.
 */
final class ValidationReportLog {
  private ValidationReportLog() {}

  static void write(Logger logger, ValidationReport report) {
    Set<WarningItem> unavailable = report instanceof CedarValidationReport cedarReport
        ? cedarReport.getFindings().stream()
            .filter(f -> f.verificationStatus() == ValidationFinding.VerificationStatus.UNAVAILABLE)
            .map(f -> new WarningItem(f.message(), f.path()))
            .collect(Collectors.toSet())
        : Set.of();
    StringBuilder message = new StringBuilder();
    if (report.getValidationStatus().equals("false")) {
      for (ErrorItem error : report.getErrors())
        message.append(String.format("%s: error: %s", error.getLocation(), error.getMessage())).append('\n');
    }
    for (WarningItem warning : report.getWarnings()) {
      if (unavailable.contains(warning)) continue;
      message.append(String.format("%s: warning: %s", warning.getLocation(), warning.getMessage())).append('\n');
    }
    logger.info(message.toString());
  }
}
