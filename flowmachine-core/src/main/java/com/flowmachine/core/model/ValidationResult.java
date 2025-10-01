package com.flowmachine.core.model;

import java.util.List;

public record ValidationResult(
    boolean isValid,
    List<String> errors
) {

  public static ValidationResult valid() {
    return new ValidationResult(true, List.of());
  }

  public static ValidationResult invalid(List<String> errors) {
    return new ValidationResult(false, errors);
  }
}