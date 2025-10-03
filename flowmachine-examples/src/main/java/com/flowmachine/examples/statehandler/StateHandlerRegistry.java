package com.flowmachine.examples.statehandler;

import com.flowmachine.core.api.StateConfiguration;
import com.flowmachine.core.api.StateMachineBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Registry for managing state handlers and applying them to a FlowMachine builder. This allows breaking down large
 * state configurations into manageable, focused classes.
 *
 * @param <TState>   the state type
 * @param <TEvent>   the event type
 * @param <TContext> the context type
 */
public class StateHandlerRegistry<TState, TEvent, TContext> {

  private final List<StateHandler<TState, TEvent, TContext>> stateHandlers = new ArrayList<>();

  /**
   * Registers a state handler
   *
   * @param stateHandler the state handler to register
   * @return this registry for method chaining
   */
  public StateHandlerRegistry<TState, TEvent, TContext> register(StateHandler<TState, TEvent, TContext> stateHandler) {
    stateHandlers.add(stateHandler);
    return this;
  }

  /**
   * Applies all registered state handlers to a StateMachine builder
   *
   * @param builder      the StateMachine builder to configure
   * @param initialState the initial state for the state machine
   * @return the configured StateMachine builder
   */
  public StateMachineBuilder<TState, TEvent, TContext> applyTo(
      StateMachineBuilder<TState, TEvent, TContext> builder,
      TState initialState) {

    // Set initial state
    builder.initialState(initialState);

    // Apply each state handler
    for (StateHandler<TState, TEvent, TContext> handler : stateHandlers) {
      StateConfiguration<TState, TEvent, TContext> config = builder.configure(handler.getState());
      config = handler.configure(config);
      config.and(); // Complete the configuration for this state
    }

    return builder;
  }

  /**
   * Gets the number of registered state handlers
   *
   * @return the number of state handlers
   */
  public int size() {
    return stateHandlers.size();
  }

  /**
   * Clears all registered state handlers
   */
  public void clear() {
    stateHandlers.clear();
  }
}