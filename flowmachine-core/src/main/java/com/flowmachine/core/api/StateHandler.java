package com.flowmachine.core.api;

/**
 * Interface for handling individual state configurations. Each state handler is responsible for configuring transitions
 * for a specific state.
 *
 * <p>State handlers enable breaking down large state machine configurations into
 * focused, maintainable classes. Each handler encapsulates the logic for a single state, improving readability,
 * testability, and maintainability.
 *
 * @param <TState>   the state type
 * @param <TEvent>   the event type
 * @param <TContext> the context type
 * @author FlowMachine
 */
public interface StateHandler<TState, TEvent, TContext> {

  /**
   * Gets the state that this handler is responsible for configuring.
   *
   * @return the state this handler manages
   */
  TState getState();

  /**
   * Configures the state transitions and conditions.
   *
   * <p>This method receives a StateConfiguration for the handler's state
   * and should return the configured StateConfiguration. The implementation should define all transitions, guards,
   * actions, and other state behavior.
   *
   * @param configuration the state configuration builder to configure
   * @return the configured state configuration
   */
  StateConfiguration<TState, TEvent, TContext> configure(StateConfiguration<TState, TEvent, TContext> configuration);
}