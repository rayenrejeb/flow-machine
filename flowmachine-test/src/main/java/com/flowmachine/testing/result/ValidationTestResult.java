package com.flowmachine.testing.result;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the results of validating a state machine configuration.
 */
public class ValidationTestResult {

  private final boolean valid;
  private final List<String> errors;
  private final List<String> warnings;

  private ValidationTestResult(Builder builder) {
    this.valid = builder.valid;
    this.errors = new ArrayList<>(builder.errors);
    this.warnings = new ArrayList<>(builder.warnings);
  }

  public static Builder builder() {
    return new Builder();
  }

  public boolean isValid() {
    return valid;
  }

  public List<String> getErrors() {
    return new ArrayList<>(errors);
  }

  public List<String> getWarnings() {
    return new ArrayList<>(warnings);
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  @Override
  public String toString() {
    return String.format("ValidationTestResult{valid=%s, errors=%d, warnings=%d}",
        valid, errors.size(), warnings.size());
  }

  public static class Builder {

    private boolean valid = true;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public Builder valid(boolean valid) {
      this.valid = valid;
      return this;
    }

    public Builder errors(List<String> errors) {
      this.errors = new ArrayList<>(errors);
      return this;
    }

    public Builder addError(String error) {
      this.errors.add(error);
      this.valid = false;
      return this;
    }

    public Builder warnings(List<String> warnings) {
      this.warnings = new ArrayList<>(warnings);
      return this;
    }

    public Builder addWarning(String warning) {
      this.warnings.add(warning);
      return this;
    }

    public ValidationTestResult build() {
      return new ValidationTestResult(this);
    }
  }
}