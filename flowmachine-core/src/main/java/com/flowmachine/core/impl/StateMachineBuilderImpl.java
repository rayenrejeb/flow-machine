package com.flowmachine.core.impl;

import com.flowmachine.core.api.Action;
import com.flowmachine.core.api.StateConfiguration;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.api.StateMachineBuilder;
import com.flowmachine.core.api.StateMachineListener;
import com.flowmachine.core.exception.ErrorHandler;
import com.flowmachine.core.exception.StateMachineException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StateMachineBuilderImpl<TState, TEvent, TContext> implements
    StateMachineBuilder<TState, TEvent, TContext> {

  private TState initialState;
  private final Map<TState, StateDefinition<TState, TEvent, TContext>> stateDefinitions = new HashMap<>();
  private final List<Action<TState, TEvent, TContext>> globalEntryActions = new ArrayList<>();
  private final List<Action<TState, TEvent, TContext>> globalExitActions = new ArrayList<>();
  private final List<Action<TState, TEvent, TContext>> globalTransitionActions = new ArrayList<>();
  private ErrorHandler<TState, TEvent, TContext> errorHandler;
  private final List<StateMachineListener<TState, TEvent, TContext>> listeners = new ArrayList<>();

  @Override
  public StateMachineBuilder<TState, TEvent, TContext> initialState(TState state) {
    this.initialState = state;
    return this;
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> configure(TState state) {
    StateDefinition<TState, TEvent, TContext> stateDefinition = stateDefinitions.computeIfAbsent(
        state, StateDefinition::new);
    return new StateConfigurationImpl<>(this, stateDefinition);
  }

  @Override
  public StateMachineBuilder<TState, TEvent, TContext> onAnyEntry(Action<TState, TEvent, TContext> action) {
    globalEntryActions.add(action);
    return this;
  }

  @Override
  public StateMachineBuilder<TState, TEvent, TContext> onAnyExit(Action<TState, TEvent, TContext> action) {
    globalExitActions.add(action);
    return this;
  }

  @Override
  public StateMachineBuilder<TState, TEvent, TContext> onAnyTransition(Action<TState, TEvent, TContext> action) {
    globalTransitionActions.add(action);
    return this;
  }

  @Override
  public StateMachineBuilder<TState, TEvent, TContext> onError(ErrorHandler<TState, TEvent, TContext> errorHandler) {
    this.errorHandler = errorHandler;
    return this;
  }

  @Override
  public StateMachineBuilder<TState, TEvent, TContext> addListener(
      StateMachineListener<TState, TEvent, TContext> listener) {
    listeners.add(listener);
    return this;
  }

  @Override
  public StateMachine<TState, TEvent, TContext> build() {
    if (initialState == null) {
      throw new StateMachineException("Initial state must be specified");
    }

    return new StateMachineImpl<>(
        initialState,
        new HashMap<>(stateDefinitions),
        new ArrayList<>(globalEntryActions),
        new ArrayList<>(globalExitActions),
        new ArrayList<>(globalTransitionActions),
        errorHandler,
        new ArrayList<>(listeners)
    );
  }
}