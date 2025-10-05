package com.flowmachine.testing.assertion;

import com.flowmachine.testing.result.ValidationTestResult;
import org.assertj.core.api.AbstractAssert;

/**
 * AssertJ-style assertions for ValidationTestResult.
 */
public class ValidationTestResultAssert extends AbstractAssert<ValidationTestResultAssert, ValidationTestResult> {

  public ValidationTestResultAssert(ValidationTestResult actual) {
    super(actual, ValidationTestResultAssert.class);
  }

  /**
   * Verifies that the validation result is valid.
   *
   * @return this assertion object
   */
  public ValidationTestResultAssert isValid() {
    isNotNull();
    if (!actual.isValid()) {
      failWithMessage("Expected validation to be valid but was not. Errors: %s", actual.getErrors());
    }
    return this;
  }

  /**
   * Verifies that the validation result is not valid.
   *
   * @return this assertion object
   */
  public ValidationTestResultAssert isNotValid() {
    isNotNull();
    if (actual.isValid()) {
      failWithMessage("Expected validation to not be valid but it was");
    }
    return this;
  }

  /**
   * Verifies that the validation result has no errors.
   *
   * @return this assertion object
   */
  public ValidationTestResultAssert hasNoErrors() {
    isNotNull();
    if (actual.hasErrors()) {
      failWithMessage("Expected no errors but found: %s", actual.getErrors());
    }
    return this;
  }

  /**
   * Verifies that the validation result has errors.
   *
   * @return this assertion object
   */
  public ValidationTestResultAssert hasErrors() {
    isNotNull();
    if (!actual.hasErrors()) {
      failWithMessage("Expected errors but found none");
    }
    return this;
  }

  /**
   * Verifies that the validation result has the specified number of errors.
   *
   * @param expectedCount the expected error count
   * @return this assertion object
   */
  public ValidationTestResultAssert hasErrorCount(int expectedCount) {
    isNotNull();
    int actualCount = actual.getErrors().size();
    if (actualCount != expectedCount) {
      failWithMessage("Expected <%d> errors but found <%d>. Errors: %s",
          expectedCount, actualCount, actual.getErrors());
    }
    return this;
  }

  /**
   * Verifies that the validation result has an error containing the specified text.
   *
   * @param expectedErrorText the expected error text
   * @return this assertion object
   */
  public ValidationTestResultAssert hasErrorContaining(String expectedErrorText) {
    isNotNull();
    boolean found = actual.getErrors().stream()
        .anyMatch(error -> error.contains(expectedErrorText));

    if (!found) {
      failWithMessage("Expected error containing <%s> but found errors: %s",
          expectedErrorText, actual.getErrors());
    }
    return this;
  }

  /**
   * Verifies that the validation result has no warnings.
   *
   * @return this assertion object
   */
  public ValidationTestResultAssert hasNoWarnings() {
    isNotNull();
    if (actual.hasWarnings()) {
      failWithMessage("Expected no warnings but found: %s", actual.getWarnings());
    }
    return this;
  }

  /**
   * Verifies that the validation result has warnings.
   *
   * @return this assertion object
   */
  public ValidationTestResultAssert hasWarnings() {
    isNotNull();
    if (!actual.hasWarnings()) {
      failWithMessage("Expected warnings but found none");
    }
    return this;
  }

  /**
   * Verifies that the validation result has the specified number of warnings.
   *
   * @param expectedCount the expected warning count
   * @return this assertion object
   */
  public ValidationTestResultAssert hasWarningCount(int expectedCount) {
    isNotNull();
    int actualCount = actual.getWarnings().size();
    if (actualCount != expectedCount) {
      failWithMessage("Expected <%d> warnings but found <%d>. Warnings: %s",
          expectedCount, actualCount, actual.getWarnings());
    }
    return this;
  }
}