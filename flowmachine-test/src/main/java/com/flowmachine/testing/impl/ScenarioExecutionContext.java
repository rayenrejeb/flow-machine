package com.flowmachine.testing.impl;

import com.flowmachine.core.api.StateMachine;

/**
 * Holds the execution context for a test scenario, encapsulating the current state, context, and state machine during
 * test execution.
 *
 * @param <TState>   the state type
 * @param <TEvent>   the event type
 * @param <TContext> the context type
 */
public class ScenarioExecutionContext<TState, TEvent, TContext> {

  private final StateMachine<TState, TEvent, TContext> stateMachine;
  private TState currentState;
  private final TContext currentContext;

  public ScenarioExecutionContext(
      StateMachine<TState, TEvent, TContext> stateMachine,
      TState currentState,
      TContext currentContext) {
    this.stateMachine = stateMachine;
    this.currentState = currentState;
    this.currentContext = currentContext;
  }

  public StateMachine<TState, TEvent, TContext> getStateMachine() {
    return stateMachine;
  }

  public TState getCurrentState() {
    return currentState;
  }

  public void setCurrentState(TState currentState) {
    this.currentState = currentState;
  }

  public TContext getCurrentContext() {
    return currentContext;
  }
}