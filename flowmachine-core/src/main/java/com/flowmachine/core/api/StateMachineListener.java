package com.flowmachine.core.api;

public interface StateMachineListener<TState, TEvent, TContext> {

  default void onStateEntry(TState state, TEvent event, TContext context) {
  }

  default void onStateExit(TState state, TEvent event, TContext context) {
  }

  default void onTransition(TState fromState, TState toState, TEvent event, TContext context) {
  }

  default void onTransitionError(TState state, TEvent event, TContext context, Exception error) {
  }
}