package com.flowmachine.core.api;

import com.flowmachine.core.exception.ErrorHandler;

public interface StateMachineBuilder<TState, TEvent, TContext> {

  StateMachineBuilder<TState, TEvent, TContext> initialState(TState state);

  StateConfiguration<TState, TEvent, TContext> configure(TState state);

  /**
   * Configures a state using a StateHandler.
   *
   * <p>This method provides a convenient way to apply state handlers that encapsulate
   * the configuration logic for specific states. The StateHandler pattern enables breaking down large state machine
   * configurations into focused, maintainable classes.
   *
   * @param stateHandler the state handler that configures the state
   * @return this StateMachineBuilder instance for method chaining
   */
  StateMachineBuilder<TState, TEvent, TContext> configure(StateHandler<TState, TEvent, TContext> stateHandler);

  /**
   * Configures a state using a StateHandler, with explicit state visibility.
   *
   * <p>This method provides the same functionality as {@link #configure(StateHandler)} but makes
   * the state being configured explicit in the method call, improving code readability and enabling IDE autocomplete to
   * show which state is being configured.
   *
   * <p>The state parameter must match the state returned by {@code stateHandler.getState()},
   * otherwise an exception will be thrown.
   *
   * @param state        the state being configured (must match stateHandler.getState())
   * @param stateHandler the state handler that configures the state
   * @return this StateMachineBuilder instance for method chaining
   * @throws IllegalArgumentException if state does not match stateHandler.getState()
   */
  StateMachineBuilder<TState, TEvent, TContext> configure(TState state,
      StateHandler<TState, TEvent, TContext> stateHandler);

  StateMachineBuilder<TState, TEvent, TContext> onAnyEntry(Action<TState, TEvent, TContext> action);

  StateMachineBuilder<TState, TEvent, TContext> onAnyExit(Action<TState, TEvent, TContext> action);

  StateMachineBuilder<TState, TEvent, TContext> onAnyTransition(Action<TState, TEvent, TContext> action);

  StateMachineBuilder<TState, TEvent, TContext> onError(ErrorHandler<TState, TEvent, TContext> errorHandler);

  StateMachineBuilder<TState, TEvent, TContext> addListener(StateMachineListener<TState, TEvent, TContext> listener);

  StateMachine<TState, TEvent, TContext> build();
}