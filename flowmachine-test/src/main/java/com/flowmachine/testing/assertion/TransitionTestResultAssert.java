package com.flowmachine.testing.assertion;

import com.flowmachine.testing.result.TransitionTestResult;
import java.util.List;
import org.assertj.core.api.AbstractAssert;

/**
 * AssertJ-style assertions for TransitionTestResult.
 *
 * @param <TState>   the state type
 * @param <TEvent>   the event type
 * @param <TContext> the context type
 */
public class TransitionTestResultAssert<TState, TEvent, TContext> extends
    AbstractAssert<TransitionTestResultAssert<TState, TEvent, TContext>, TransitionTestResult<TState, TEvent, TContext>> {

  public TransitionTestResultAssert(TransitionTestResult<TState, TEvent, TContext> actual) {
    super(actual, TransitionTestResultAssert.class);
  }

  /**
   * Verifies that the transition test has no errors.
   *
   * @return this assertion object
   */
  public TransitionTestResultAssert<TState, TEvent, TContext> hasNoErrors() {
    isNotNull();
    if (actual.hasErrors()) {
      failWithMessage("Expected no errors but found: %s", actual.getErrors());
    }
    return this;
  }

  /**
   * Verifies that the transition test has errors.
   *
   * @return this assertion object
   */
  public TransitionTestResultAssert<TState, TEvent, TContext> hasErrors() {
    isNotNull();
    if (!actual.hasErrors()) {
      failWithMessage("Expected errors but found none");
    }
    return this;
  }

  /**
   * Verifies that the specified events are allowed.
   *
   * @param expectedEvents the events that should be allowed
   * @return this assertion object
   */
  @SafeVarargs
  public final TransitionTestResultAssert<TState, TEvent, TContext> allowsEvents(TEvent... expectedEvents) {
    isNotNull();
    List<TEvent> allowedEvents = actual.getAllowedEvents();
    for (TEvent event : expectedEvents) {
      if (!allowedEvents.contains(event)) {
        failWithMessage("Expected event <%s> to be allowed but it was not. Allowed events: %s",
            event, allowedEvents);
      }
    }
    return this;
  }

  /**
   * Verifies that the specified events are blocked.
   *
   * @param expectedEvents the events that should be blocked
   * @return this assertion object
   */
  @SafeVarargs
  public final TransitionTestResultAssert<TState, TEvent, TContext> blocksEvents(TEvent... expectedEvents) {
    isNotNull();
    List<TEvent> blockedEvents = actual.getBlockedEvents();
    for (TEvent event : expectedEvents) {
      if (!blockedEvents.contains(event)) {
        failWithMessage("Expected event <%s> to be blocked but it was not. Blocked events: %s",
            event, blockedEvents);
      }
    }
    return this;
  }

  /**
   * Verifies that the number of allowed events matches the expected count.
   *
   * @param expectedCount the expected number of allowed events
   * @return this assertion object
   */
  public TransitionTestResultAssert<TState, TEvent, TContext> hasAllowedEventCount(int expectedCount) {
    isNotNull();
    int actualCount = actual.getAllowedEventsCount();
    if (actualCount != expectedCount) {
      failWithMessage("Expected <%d> allowed events but found <%d>. Allowed events: %s",
          expectedCount, actualCount, actual.getAllowedEvents());
    }
    return this;
  }

  /**
   * Verifies that the number of blocked events matches the expected count.
   *
   * @param expectedCount the expected number of blocked events
   * @return this assertion object
   */
  public TransitionTestResultAssert<TState, TEvent, TContext> hasBlockedEventCount(int expectedCount) {
    isNotNull();
    int actualCount = actual.getBlockedEventsCount();
    if (actualCount != expectedCount) {
      failWithMessage("Expected <%d> blocked events but found <%d>. Blocked events: %s",
          expectedCount, actualCount, actual.getBlockedEvents());
    }
    return this;
  }

  /**
   * Verifies that the total number of events tested matches the expected count.
   *
   * @param expectedCount the expected total number of events
   * @return this assertion object
   */
  public TransitionTestResultAssert<TState, TEvent, TContext> hasTotalEventCount(int expectedCount) {
    isNotNull();
    int actualCount = actual.getTotalEventsCount();
    if (actualCount != expectedCount) {
      failWithMessage("Expected <%d> total events but found <%d>", expectedCount, actualCount);
    }
    return this;
  }
}