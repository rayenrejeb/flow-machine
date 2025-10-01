package com.flowmachine.core.api;

import com.flowmachine.core.model.StateMachineInfo;
import com.flowmachine.core.model.TransitionResult;
import com.flowmachine.core.model.ValidationResult;

/**
 * Core interface for state machine execution and introspection.
 * <p>
 * StateMachine provides the main API for executing workflows, validating transitions, and querying state machine
 * properties. All operations are thread-safe and can be used in concurrent environments
 *
 * @param <TState>   the type of states in this state machine
 * @param <TEvent>   the type of events that trigger transitions
 * @param <TContext> the type of context object that flows through the workflow
 * @author FlowMachine
 */
public interface StateMachine<TState, TEvent, TContext> {

  /**
   * Executes a state transition by firing an event from the current state.
   *
   * <p>This is the primary method for advancing workflows. It attempts to find
   * a valid transition from the current state triggered by the given event, evaluates any guard conditions, executes
   * entry/exit actions, and returns the new state.
   *
   * @param currentState the current state of the workflow
   * @param event        the event to trigger
   * @param context      the context object containing business data
   * @return the new state after the transition (returns current state if transition fails)
   */
  TState fire(TState currentState, TEvent event, TContext context);

  /**
   * Executes a state transition and returns detailed result information.
   *
   * <p>Similar to {@link #fire(Object, Object, Object)} but returns a
   * {@link TransitionResult} containing both the new state and additional information about the transition execution,
   * including success/failure status and any error details.
   *
   * <p>Use this method when you need detailed information about transition
   * execution, especially for error handling, debugging, or auditing.
   *
   * @param currentState the current state of the workflow
   * @param event        the event to trigger
   * @param context      the context object containing business data
   * @return a TransitionResult containing the new state and execution details (no thrown exceptions)
   */
  TransitionResult<TState> fireWithResult(TState currentState, TEvent event, TContext context);

  /**
   * Tests whether a transition can be executed without actually performing it.
   *
   * <p>This method evaluates whether firing the given event from the current state
   * would result in a valid transition. It checks for configured transitions and evaluates guard conditions, but does
   * not execute any actions or change state.
   *
   * @param currentState the current state to test from
   * @param event        the event to test
   * @param context      the context object for guard evaluation
   * @return true if the transition can be executed, false otherwise (returns false for null/invalid parameters)
   */
  boolean canFire(TState currentState, TEvent event, TContext context);

  /**
   * Determines whether the given state is a final (terminal) state.
   *
   * <p>Final states are configured with {@code .asFinal()} and represent
   * workflow endpoints where no further transitions are allowed. This method is useful for workflow completion
   * detection and business logic decisions.
   *
   * @param state the state to check
   * @return true if the state is final, false otherwise (returns false for null/unknown states)
   */
  boolean isFinalState(TState state);

  /**
   * Returns comprehensive information about this state machine's structure.
   *
   * <p>The returned {@link StateMachineInfo} provides introspection capabilities
   * including all configured states, events, transitions, and the initial state. This information is useful for
   * debugging, documentation generation, and runtime analysis.
   *
   * @return detailed information about this state machine's configuration
   */
  StateMachineInfo<TState, TEvent, TContext> getInfo();

  /**
   * Validates the state machine configuration for correctness and completeness.
   *
   * <p>This method performs comprehensive validation including:
   * <ul>
   * <li>All referenced states are properly configured</li>
   * <li>Initial state is configured</li>
   * <li>No unreachable states exist</li>
   * <li>Configuration consistency</li>
   * <li>Final state validation</li>
   * </ul>
   *
   * <p>Use this method during development and testing to catch configuration
   * errors early. It's recommended to call this in unit tests to ensure
   * workflow integrity.
   *
   * @return validation result containing success status and any error details
   */
  ValidationResult validate();
}