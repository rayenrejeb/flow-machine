package com.flowmachine.core.api;

import com.flowmachine.core.exception.ErrorHandler;

public interface StateMachineBuilder<TState, TEvent, TContext> {

  StateMachineBuilder<TState, TEvent, TContext> initialState(TState state);

  StateConfiguration<TState, TEvent, TContext> configure(TState state);

  StateMachineBuilder<TState, TEvent, TContext> onAnyEntry(Action<TState, TEvent, TContext> action);

  StateMachineBuilder<TState, TEvent, TContext> onAnyExit(Action<TState, TEvent, TContext> action);

  StateMachineBuilder<TState, TEvent, TContext> onAnyTransition(Action<TState, TEvent, TContext> action);

  StateMachineBuilder<TState, TEvent, TContext> onError(ErrorHandler<TState, TEvent, TContext> errorHandler);

  StateMachineBuilder<TState, TEvent, TContext> addListener(StateMachineListener<TState, TEvent, TContext> listener);

  StateMachine<TState, TEvent, TContext> build();
}