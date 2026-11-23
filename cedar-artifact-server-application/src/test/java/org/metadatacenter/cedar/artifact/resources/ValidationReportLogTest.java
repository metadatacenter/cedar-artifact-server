package org.metadatacenter.cedar.artifact.resources;

import org.junit.jupiter.api.Test;
import org.metadatacenter.model.validation.ValidationFinding;
import org.metadatacenter.model.validation.ValidationPolicy;
import org.metadatacenter.model.validation.report.CedarValidationReport;
import org.slf4j.Logger;

import static org.mockito.Mockito.*;

class ValidationReportLogTest {
  private static ValidationFinding finding(String code, String path, String message,
                                           ValidationFinding.VerificationStatus status) {
    return new ValidationFinding(code, ValidationPolicy.category(code), path, message, status);
  }

  @Test void unavailableValuesAreLeftOutAndOtherWarningsAreKept() {
    var report = CedarValidationReport.newInstanceReport();
    report.resolveVerification(finding("semanticUnavailable", "/a/@id", "Semantic verification is unavailable.",
        ValidationFinding.VerificationStatus.UNAVAILABLE));
    report.resolveVerification(finding("semanticUnavailable", "/b/@id", "Semantic verification is unavailable.",
        ValidationFinding.VerificationStatus.UNAVAILABLE));
    report.resolveVerification(finding("semanticNotChecked", "/c/@id", "Not checked.",
        ValidationFinding.VerificationStatus.NOT_CHECKED));
    Logger logger = mock(Logger.class);
    ValidationReportLog.write(logger, report);
    verify(logger).info("/c/@id: warning: Not checked.\n");
  }
}
