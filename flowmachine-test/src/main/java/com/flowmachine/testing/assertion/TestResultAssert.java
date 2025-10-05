package com.flowmachine.testing.assertion;

import com.flowmachine.testing.result.TestResult;
import java.time.Duration;
import java.util.Objects;
import org.assertj.core.api.AbstractAssert;

/**
 * AssertJ-style assertions for TestResult.
 *
 * @param <TState>   the state type
 * @param <TEvent>   the event type
 * @param <TContext> the context type
 */
public class TestResultAssert<TState, TEvent, TContext>
    extends AbstractAssert<TestResultAssert<TState, TEvent, TContext>, TestResult<TState, TEvent, TContext>> {

  public TestResultAssert(TestResult<TState, TEvent, TContext> actual) {
    super(actual, TestResultAssert.class);
  }

  /**
   * Verifies that the test result is successful.
   *
   * @return this assertion object
   */
  public TestResultAssert<TState, TEvent, TContext> isSuccessful() {
    isNotNull();
    if (!actual.isSuccessful()) {
      failWithMessage("Expected test result to be successful but was not. Errors: %s", actual.getErrors());
    }
    return this;
  }

  /**
   * Verifies that the test result is not successful.
   *
   * @return this assertion object
   */
  public TestResultAssert<TState, TEvent, TContext> isNotSuccessful() {
    isNotNull();
    if (actual.isSuccessful()) {
      failWithMessage("Expected test result to not be successful but it was");
    }
    return this;
  }

  /**
   * Verifies that the test result has no errors.
   *
   * @return this assertion object
   */
  public TestResultAssert<TState, TEvent, TContext> hasNoErrors() {
    isNotNull();
    if (actual.hasErrors()) {
      failWithMessage("Expected no errors but found: %s", actual.getErrors());
    }
    return this;
  }

  /**
   * Verifies that the test result has errors.
   *
   * @return this assertion object
   */
  public TestResultAssert<TState, TEvent, TContext> hasErrors() {
    isNotNull();
    if (!actual.hasErrors()) {
      failWithMessage("Expected errors but found none");
    }
    return this;
  }

  /**
   * Verifies that the test result has the specified number of errors.
   *
   * @param expectedCount the expected error count
   * @return this assertion object
   */
  public TestResultAssert<TState, TEvent, TContext> hasErrorCount(int expectedCount) {
    isNotNull();
    int actualCount = actual.getErrors().size();
    if (actualCount != expectedCount) {
      failWithMessage("Expected <%d> errors but found <%d>. Errors: %s",
          expectedCount, actualCount, actual.getErrors());
    }
    return this;
  }

  /**
   * Verifies that the test result has an error containing the specified text.
   *
   * @param expectedErrorText the expected error text
   * @return this assertion object
   */
  public TestResultAssert<TState, TEvent, TContext> hasErrorContaining(String expectedErrorText) {
    isNotNull();
    boolean found = actual
        .getErrors()
        .stream()
        .anyMatch(error -> error.message()
                               .contains(expectedErrorText) ||
                           (Objects.nonNull(error.exception()) && error.exception().getMessage()
                               .contains(expectedErrorText)));

    if (!found) {
      failWithMessage("Expected error containing <%s> but found errors: %s",
          expectedErrorText, actual.getErrors());
    }
    return this;
  }

  /**
   * Verifies that all transitions were successful.
   *
   * @return this assertion object
   */
  public TestResultAssert<TState, TEvent, TContext> hasSuccessfulTransitions() {
    isNotNull();
    long failedCount = actual.getFailedTransitionCount();
    if (failedCount > 0) {
      failWithMessage("Expected all transitions to be successful but <%d> failed", failedCount);
    }
    return this;
  }

  /**
   * Verifies that the test ended in the expected final state.
   *
   * @param expectedState the expected final state
   * @return this assertion object
   */
  public TestResultAssert<TState, TEvent, TContext> endedInState(TState expectedState) {
    isNotNull();
    if (!Objects.equals(actual.getFinalState(), expectedState)) {
      failWithMessage("Expected final state <%s> but was <%s>", expectedState, actual.getFinalState());
    }
    return this;
  }

  /**
   * Verifies that the test completed within the specified duration.
   *
   * @param maxDuration the maximum allowed duration
   * @return this assertion object
   */
  public TestResultAssert<TState, TEvent, TContext> completedWithin(Duration maxDuration) {
    isNotNull();
    if (actual.getExecutionTime().compareTo(maxDuration) > 0) {
      failWithMessage("Expected execution time to be within <%s> but was <%s>",
          maxDuration, actual.getExecutionTime());
    }
    return this;
  }

  /**
   * Verifies that the specified number of transitions were executed.
   *
   * @param expectedCount the expected transition count
   * @return this assertion object
   */
  public TestResultAssert<TState, TEvent, TContext> hasTransitionCount(int expectedCount) {
    isNotNull();
    int actualCount = actual.getTransitionResults().size();
    if (actualCount != expectedCount) {
      failWithMessage("Expected <%d> transitions but found <%d>", expectedCount, actualCount);
    }
    return this;
  }

  /**
   * Verifies that the test result has no warnings.
   *
   * @return this assertion object
   */
  public TestResultAssert<TState, TEvent, TContext> hasNoWarnings() {
    isNotNull();
    if (actual.hasWarnings()) {
      failWithMessage("Expected no warnings but found: %s", actual.getWarnings());
    }
    return this;
  }
}