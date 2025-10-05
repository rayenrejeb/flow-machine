package com.flowmachine.core.service;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.model.StateMachineInfo;
import com.flowmachine.core.model.TransitionResult;
import com.flowmachine.core.model.ValidationResult;

/**
 * Service for creating and managing workflow state machines.
 * This service provides a clean way to build StateMachine instances that can be injected
 * into other services without exposing the complex builder pattern.
 *
 * @param <TState> the type of states in the state machine
 * @param <TEvent> the type of events that trigger transitions
 * @param <TContext> the type of context object that flows through the workflow
 */
public abstract class AbstractStateMachine<TState, TEvent, TContext> implements StateMachine<TState, TEvent, TContext> {

  private final StateMachine<TState, TEvent, TContext> delegate;

  public AbstractStateMachine() {
    this.delegate = workflow();
  }

  @Override
  public TState fire(TState currentState, TEvent tEvent, TContext tContext) {
    return delegate.fire(currentState, tEvent, tContext);
  }

  @Override
  public TransitionResult<TState> fireWithResult(TState currentState, TEvent tEvent, TContext tContext) {
    return delegate.fireWithResult(currentState, tEvent, tContext);
  }

  @Override
  public boolean canFire(TState currentState, TEvent tEvent, TContext tContext) {
    return delegate.canFire(currentState, tEvent, tContext);
  }

  @Override
  public boolean isFinalState(TState tState) {
    return delegate.isFinalState(tState);
  }

  @Override
  public StateMachineInfo<TState, TEvent, TContext> getInfo() {
    return delegate.getInfo();
  }

  @Override
  public ValidationResult validate() {
    return delegate.validate();
  }

  protected abstract StateMachine<TState, TEvent, TContext> workflow();
}