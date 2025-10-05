package com.flowmachine.testing.scenario.step;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents a context action step in the test scenario.
 */
public class ContextActionStep<TState, TEvent, TContext> extends TestStep<TState, TEvent, TContext> {

  private final Consumer<TContext> action;
  private final String description;

  public ContextActionStep(Consumer<TContext> action, String description) {
    this.action = Objects.requireNonNull(action, "Action cannot be null");
    this.description = Objects.requireNonNull(description, "Description cannot be null");
  }

  public Consumer<TContext> getAction() {
    return action;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public boolean isTransition() {
    return false;
  }

  @Override
  public boolean isContextAction() {
    return true;
  }

  @Override
  public TransitionStep<TState, TEvent, TContext> asTransition() {
    throw new IllegalStateException("This is a context action step, not a transition step");
  }

  @Override
  public ContextActionStep<TState, TEvent, TContext> asContextAction() {
    return this;
  }

  @Override
  public String toString() {
    return String.format("ContextActionStep{description='%s'}", description);
  }
}