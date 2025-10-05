package com.flowmachine.testing.scenario.step;

import java.util.Objects;

/**
 * Represents a transition step in the test scenario.
 */
public class TransitionStep<TState, TEvent, TContext> extends TestStep<TState, TEvent, TContext> {

  private final TEvent event;
  private final TState expectedTargetState;

  public TransitionStep(TEvent event, TState expectedTargetState) {
    this.event = Objects.requireNonNull(event, "Event cannot be null");
    this.expectedTargetState = Objects.requireNonNull(expectedTargetState, "Expected target state cannot be null");
  }

  public TEvent getEvent() {
    return event;
  }

  public TState getExpectedTargetState() {
    return expectedTargetState;
  }

  @Override
  public boolean isTransition() {
    return true;
  }

  @Override
  public boolean isContextAction() {
    return false;
  }

  @Override
  public TransitionStep<TState, TEvent, TContext> asTransition() {
    return this;
  }

  @Override
  public ContextActionStep<TState, TEvent, TContext> asContextAction() {
    throw new IllegalStateException("This is a transition step, not a context action step");
  }

  @Override
  public String toString() {
    return String.format("TransitionStep{event=%s, expectedTargetState=%s}", event, expectedTargetState);
  }
}