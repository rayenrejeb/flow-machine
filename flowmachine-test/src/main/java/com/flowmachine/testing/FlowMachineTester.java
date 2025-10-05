package com.flowmachine.testing;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.testing.builder.FlowMachineTesterBuilder;
import com.flowmachine.testing.result.TestResult;
import com.flowmachine.testing.result.TransitionTestResult;
import com.flowmachine.testing.result.ValidationTestResult;

/**
 * Testing framework for FlowMachine workflows.
 *
 * <p>FlowMachineTester provides comprehensive testing utilities for validating state machine behavior,
 * including scenario testing, transition validation, and guard condition verification.
 *
 * @param <TState>   the state type
 * @param <TEvent>   the event type
 * @param <TContext> the context type
 * @author FlowMachine Testing Framework
 */
public interface FlowMachineTester<TState, TEvent, TContext> {

  /**
   * Creates a new FlowMachineTester for the specified state machine.
   *
   * @param <TState>     the state type
   * @param <TEvent>     the event type
   * @param <TContext>   the context type
   * @param stateMachine the state machine to test
   * @return a new FlowMachineTesterBuilder for configuring the test
   */
  static <TState, TEvent, TContext> FlowMachineTesterBuilder<TState, TEvent, TContext> forWorkflow(
      StateMachine<TState, TEvent, TContext> stateMachine) {
    return new FlowMachineTesterBuilder<>(stateMachine);
  }

  /**
   * Runs the configured test scenario and returns the results.
   *
   * @return the test execution result containing all transition outcomes and validation results
   */
  TestResult<TState, TEvent, TContext> runScenario();

  /**
   * Validates the state machine configuration without executing transitions.
   *
   * @return validation result containing any configuration errors
   */
  ValidationTestResult validateConfiguration();

  /**
   * Tests all possible transitions from the current state with the given context.
   *
   * @param currentState the state to test from
   * @param context      the context to use for testing
   * @return comprehensive transition test results
   */
  TransitionTestResult<TState, TEvent, TContext> testAllTransitions(TState currentState, TContext context);

}