package com.flowmachine.testing.scenario.step;

import java.util.function.Consumer;

/**
 * Represents a single step in a test scenario, which can be either a transition or a context action.
 *
 * @param <TState>   the state type
 * @param <TEvent>   the event type
 * @param <TContext> the context type
 */
public abstract class TestStep<TState, TEvent, TContext> {

  /**
   * Creates a transition step.
   *
   * @param event               the event to trigger
   * @param expectedTargetState the expected resulting state
   * @return a transition test step
   */
  public static <TState, TEvent, TContext> TestStep<TState, TEvent, TContext> transition(TEvent event,
      TState expectedTargetState) {
    return new TransitionStep<>(event, expectedTargetState);
  }

  /**
   * Creates a context action step.
   *
   * @param action      the action to execute on the context
   * @param description optional description of the action
   * @return a context action test step
   */
  public static <TState, TEvent, TContext> TestStep<TState, TEvent, TContext> contextAction(Consumer<TContext> action,
      String description) {
    return new ContextActionStep<>(action, description);
  }

  /**
   * Creates a context action step with default description.
   *
   * @param action the action to execute on the context
   * @return a context action test step
   */
  public static <TState, TEvent, TContext> TestStep<TState, TEvent, TContext> contextAction(Consumer<TContext> action) {
    return new ContextActionStep<>(action, "Context action");
  }

  /**
   * Checks if this step is a transition step.
   *
   * @return true if this is a transition step
   */
  public abstract boolean isTransition();

  /**
   * Checks if this step is a context action step.
   *
   * @return true if this is a context action step
   */
  public abstract boolean isContextAction();

  /**
   * Gets this step as a transition step.
   *
   * @return the transition step
   * @throws IllegalStateException if this is not a transition step
   */
  public abstract TransitionStep<TState, TEvent, TContext> asTransition();

  /**
   * Gets this step as a context action step.
   *
   * @return the context action step
   * @throws IllegalStateException if this is not a context action step
   */
  public abstract ContextActionStep<TState, TEvent, TContext> asContextAction();

}