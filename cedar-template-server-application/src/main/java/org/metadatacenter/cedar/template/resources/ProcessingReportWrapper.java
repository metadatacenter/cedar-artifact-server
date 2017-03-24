package org.metadatacenter.cedar.template.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.google.common.collect.Lists;

import javax.annotation.Nonnull;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonPropertyOrder({"validates", "warnings", "errors"})
public class ProcessingReportWrapper implements ValidationReport {

  private final ProcessingReport report;

  public ProcessingReportWrapper(@Nonnull ProcessingReport report) {
    this.report = checkNotNull(report);
  }

  @Override
  @JsonProperty("validates")
  public String getValidationStatus() {
    return String.valueOf(report.isSuccess());
  }

  @Override
  @JsonProperty("warnings")
  public List<String> getWarningMessages() {
    List<String> warningMessages = Lists.newArrayList();
    report.forEach(message -> {
      if (message.getLogLevel() == LogLevel.WARNING) {
        warningMessages.add(message.getMessage());
      }
    });
    return warningMessages;
  }

  @Override
  @JsonProperty("errors")
  public List<String> getErrorMessages() {
    List<String> errorMessages = Lists.newArrayList();
    report.forEach(message -> {
      if (message.getLogLevel() == LogLevel.ERROR) {
        errorMessages.add(message.getMessage());
      }
    });
    return errorMessages;
  }
}
