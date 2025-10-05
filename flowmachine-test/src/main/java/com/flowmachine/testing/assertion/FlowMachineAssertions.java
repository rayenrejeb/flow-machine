package com.flowmachine.testing.assertion;

import com.flowmachine.testing.result.TestResult;
import com.flowmachine.testing.result.TransitionTestResult;
import com.flowmachine.testing.result.ValidationTestResult;

/**
 * Entry point for FlowMachine testing assertions. Provides static factory methods for creating assertion objects.
 */
public final class FlowMachineAssertions {

  private FlowMachineAssertions() {
    // Utility class
  }

  /**
   * Creates an assertion for TestResult.
   *
   * @param actual the test result to assert
   * @return a TestResultAssert instance
   */
  public static <TState, TEvent, TContext> TestResultAssert<TState, TEvent, TContext> assertThatFlowResult(
      TestResult<TState, TEvent, TContext> actual) {
    return new TestResultAssert<>(actual);
  }

  /**
   * Creates an assertion for ValidationTestResult.
   *
   * @param actual the validation result to assert
   * @return a ValidationTestResultAssert instance
   */
  public static ValidationTestResultAssert assertThatFlowResult(ValidationTestResult actual) {
    return new ValidationTestResultAssert(actual);
  }

  /**
   * Creates an assertion for TransitionTestResult.
   *
   * @param actual the transition test result to assert
   * @return a TransitionTestResultAssert instance
   */
  public static <TState, TEvent, TContext> TransitionTestResultAssert<TState, TEvent, TContext> assertThatFlowResult(
      TransitionTestResult<TState, TEvent, TContext> actual) {
    return new TransitionTestResultAssert<>(actual);
  }

}