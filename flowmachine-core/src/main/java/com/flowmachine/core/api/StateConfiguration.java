package com.flowmachine.core.api;

/**
 * Configuration interface for defining state behavior and transitions in a state machine.
 *
 * <p>This interface provides a fluent API for configuring individual states within a state machine.
 * Each method returns the StateConfiguration instance to enable method chaining. State configuration includes
 * transition rules, actions, guards, and special state properties.
 *
 * @param <TState>   the type of states in the state machine
 * @param <TEvent>   the type of events that trigger transitions
 * @param <TContext> the type of context object that flows through the workflow
 * @author FlowMachine
 */
public interface StateConfiguration<TState, TEvent, TContext> {

  /**
   * Defines an unconditional transition from the current state to a target state when an event occurs.
   *
   * <p>This creates a simple state transition that will always execute when the specified event
   * is fired from the current state. No guard conditions are evaluated.
   *
   * @param event       the event that triggers the transition
   * @param targetState the state to transition to when the event occurs
   * @return this StateConfiguration instance for method chaining
   */
  StateConfiguration<TState, TEvent, TContext> permit(TEvent event, TState targetState);

  /**
   * Defines a conditional transition that only occurs if the guard condition evaluates to true.
   *
   * <p>The guard is evaluated each time the event is fired. The transition only occurs if the
   * guard returns true. This enables dynamic, context-sensitive routing within the state machine.
   *
   * @param event       the event that triggers the transition
   * @param targetState the state to transition to if the guard condition is met
   * @param guard       the condition that must be satisfied for the transition to occur
   * @return this StateConfiguration instance for method chaining
   */
  StateConfiguration<TState, TEvent, TContext> permitIf(TEvent event, TState targetState,
      Guard<TState, TEvent, TContext> guard);

  /**
   * Defines a reentry transition where the state transitions to itself when an event occurs.
   *
   * <p>This creates a self-transition that re-enters the current state, causing exit and entry
   * actions to be executed. Useful for refreshing state or triggering state-specific processing.
   *
   * @param event the event that triggers the reentry transition
   * @return this StateConfiguration instance for method chaining
   */
  StateConfiguration<TState, TEvent, TContext> permitReentry(TEvent event);

  /**
   * Defines a conditional reentry transition that only occurs if the guard condition is met.
   *
   * <p>Similar to permitReentry but with a guard condition. The state will only re-enter itself
   * if the guard evaluates to true when the event is fired.
   *
   * @param event the event that triggers the conditional reentry
   * @param guard the condition that must be satisfied for the reentry to occur
   * @return this StateConfiguration instance for method chaining
   */
  StateConfiguration<TState, TEvent, TContext> permitReentryIf(TEvent event, Guard<TState, TEvent, TContext> guard);

  /**
   * Configures the state to ignore a specific event without any state change or action execution.
   *
   * <p>When an ignored event is fired, it is consumed but no transition occurs and no actions
   * are executed. This is useful for events that should be silently discarded in certain states.
   *
   * @param event the event to ignore in this state
   * @return this StateConfiguration instance for method chaining
   */
  StateConfiguration<TState, TEvent, TContext> ignore(TEvent event);

  /**
   * Configures the state to conditionally ignore an event based on a guard condition.
   *
   * <p>The event is ignored only if the guard condition evaluates to true. If the guard
   * returns false, the event is processed normally (may result in an invalid transition error).
   *
   * @param event the event to conditionally ignore
   * @param guard the condition that determines whether to ignore the event
   * @return this StateConfiguration instance for method chaining
   */
  StateConfiguration<TState, TEvent, TContext> ignoreIf(TEvent event, Guard<TState, TEvent, TContext> guard);

  /**
   * Defines an internal transition that executes an action without changing state.
   *
   * <p>Internal transitions execute the specified action when the event occurs but do not
   * cause a state change. Entry and exit actions are not executed. This is useful for handling events that trigger
   * processing within the current state.
   *
   * @param event  the event that triggers the internal transition
   * @param action the action to execute when the event occurs
   * @return this StateConfiguration instance for method chaining
   */
  StateConfiguration<TState, TEvent, TContext> internal(TEvent event, Action<TState, TEvent, TContext> action);

  /**
   * Defines a conditional internal transition that executes an action only if the guard condition is met.
   *
   * <p>The action is executed without changing state, but only if the guard evaluates to true.
   * If the guard returns false, the event is processed normally.
   *
   * @param event  the event that triggers the conditional internal transition
   * @param action the action to execute if the guard condition is satisfied
   * @param guard  the condition that must be met for the action to execute
   * @return this StateConfiguration instance for method chaining
   */
  StateConfiguration<TState, TEvent, TContext> internalIf(TEvent event, Action<TState, TEvent, TContext> action,
      Guard<TState, TEvent, TContext> guard);

  /**
   * Defines an action to execute when entering this state.
   *
   * <p>Entry actions are executed every time the state is entered, regardless of which
   * transition caused the state change. Multiple entry actions can be defined and will be executed in the order they
   * were added.
   *
   * @param action the action to execute when entering this state
   * @return this StateConfiguration instance for method chaining
   */
  StateConfiguration<TState, TEvent, TContext> onEntry(Action<TState, TEvent, TContext> action);

  /**
   * Defines an action to execute when exiting this state.
   *
   * <p>Exit actions are executed every time the state is exited, regardless of which
   * transition causes the state change. Multiple exit actions can be defined and will be executed in the order they
   * were added.
   *
   * @param action the action to execute when exiting this state
   * @return this StateConfiguration instance for method chaining
   */
  StateConfiguration<TState, TEvent, TContext> onExit(Action<TState, TEvent, TContext> action);

  /**
   * Marks this state as a final (terminal) state.
   *
   * <p>Final states represent workflow endpoints where no further transitions are allowed.
   * Attempting to fire any event from a final state will result in a failed transition. Final states are useful for
   * representing completion, failure, or cancellation endpoints.
   *
   * @return this StateConfiguration instance for method chaining
   */
  StateConfiguration<TState, TEvent, TContext> asFinal();

  /**
   * Defines an automatic transition that occurs immediately upon entering this state.
   *
   * <p>Auto-transitions are executed without requiring an explicit event. When the state
   * machine enters this state, it will automatically transition to the specified target state. This enables automatic
   * workflow progression and state chaining.
   *
   * @param targetState the state to automatically transition to
   * @return this StateConfiguration instance for method chaining
   */
  StateConfiguration<TState, TEvent, TContext> autoTransition(TState targetState);

  /**
   * Defines a conditional automatic transition that occurs only if the guard condition is met.
   *
   * <p>The auto-transition is evaluated when the state is entered. If the guard condition
   * evaluates to true, the transition to the target state occurs automatically. If false, the state machine remains in
   * the current state.
   *
   * @param targetState the state to automatically transition to if the condition is met
   * @param guard       the condition that must be satisfied for the auto-transition to occur
   * @return this StateConfiguration instance for method chaining
   */
  StateConfiguration<TState, TEvent, TContext> autoTransitionIf(TState targetState,
      Guard<TState, TEvent, TContext> guard);

  /**
   * Completes the configuration of this state and returns to the state machine builder.
   *
   * <p>This method signals the end of configuration for the current state and returns
   * control to the StateMachineBuilder for configuring additional states or building the final state machine.
   *
   * @return the StateMachineBuilder instance for continued configuration
   */
  StateMachineBuilder<TState, TEvent, TContext> and();
}