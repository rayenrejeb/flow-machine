package com.flowmachine.testing.builder;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.testing.FlowMachineTester;
import com.flowmachine.testing.impl.FlowMachineTesterImpl;
import com.flowmachine.testing.scenario.ExpectedTransition;
import com.flowmachine.testing.scenario.TestScenario;
import com.flowmachine.testing.scenario.step.TestStep;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Builder for configuring FlowMachineTester instances.
 *
 * <p>Provides a fluent API for setting up test scenarios, initial conditions,
 * expected transitions, and validation rules for state machine testing.
 *
 * @param <TState>   the state type
 * @param <TEvent>   the event type
 * @param <TContext> the context type
 */
public class FlowMachineTesterBuilder<TState, TEvent, TContext> {

  private final StateMachine<TState, TEvent, TContext> stateMachine;
  private TState startingState;
  private TContext initialContext;
  private final List<ExpectedTransition<TState, TEvent>> expectedTransitions = new ArrayList<>();
  private final List<TestStep<TState, TEvent, TContext>> testSteps = new ArrayList<>();
  private TState expectedFinalState;
  private final List<Predicate<TContext>> contextValidations = new ArrayList<>();

  public FlowMachineTesterBuilder(StateMachine<TState, TEvent, TContext> stateMachine) {
    this.stateMachine = Objects.requireNonNull(stateMachine, "StateMachine cannot be null");
  }

  /**
   * Sets the starting state for the test scenario.
   *
   * @param state the initial state
   * @return this builder for method chaining
   */
  public FlowMachineTesterBuilder<TState, TEvent, TContext> startingAt(TState state) {
    this.startingState = Objects.requireNonNull(state, "Starting state cannot be null");
    return this;
  }

  /**
   * Sets the initial context for the test scenario.
   *
   * @param context the initial context
   * @return this builder for method chaining
   */
  public FlowMachineTesterBuilder<TState, TEvent, TContext> withContext(TContext context) {
    this.initialContext = context;
    return this;
  }

  /**
   * Adds an expected transition to the test scenario.
   *
   * @param event               the event that should trigger the transition
   * @param expectedTargetState the expected resulting state
   * @return this builder for method chaining
   * @throws NullPointerException if event or expectedTargetState is null
   */
  public FlowMachineTesterBuilder<TState, TEvent, TContext> expectTransition(TEvent event, TState expectedTargetState) {
    Objects.requireNonNull(event, "Event cannot be null");
    Objects.requireNonNull(expectedTargetState, "Expected target state cannot be null");

    expectedTransitions.add(new ExpectedTransition<>(event, expectedTargetState));
    testSteps.add(TestStep.transition(event, expectedTargetState));
    return this;
  }

  /**
   * Modifies the context during the test scenario. This allows updating the context between transitions
   * for testing purposes.
   *
   * @param contextModifier the action to execute on the context
   * @return this builder for method chaining
   * @throws NullPointerException if contextModifier is null
   */
  public FlowMachineTesterBuilder<TState, TEvent, TContext> modifyContext(Consumer<TContext> contextModifier) {
    Objects.requireNonNull(contextModifier, "Context modifier cannot be null");
    testSteps.add(TestStep.contextAction(contextModifier));
    return this;
  }

  /**
   * Modifies the context during the test scenario with a description. This allows updating the context
   * between transitions for testing purposes.
   *
   * @param contextModifier the action to execute on the context
   * @param description     a description of what the action does
   * @return this builder for method chaining
   * @throws NullPointerException if contextModifier is null
   */
  public FlowMachineTesterBuilder<TState, TEvent, TContext> modifyContext(Consumer<TContext> contextModifier,
      String description) {
    Objects.requireNonNull(contextModifier, "Context modifier cannot be null");
    testSteps.add(TestStep.contextAction(contextModifier, description));
    return this;
  }

  /**
   * Sets the expected final state after all transitions.
   *
   * @param finalState the expected final state
   * @return this builder for method chaining
   */
  public FlowMachineTesterBuilder<TState, TEvent, TContext> expectFinalState(TState finalState) {
    this.expectedFinalState = Objects.requireNonNull(finalState, "Final state cannot be null");
    return this;
  }


  /**
   * Adds a context validation predicate.
   *
   * @param validation the validation predicate
   * @return this builder for method chaining
   * @throws NullPointerException if validation is null
   */
  public FlowMachineTesterBuilder<TState, TEvent, TContext> validateContext(Predicate<TContext> validation) {
    contextValidations.add(Objects.requireNonNull(validation, "Validation cannot be null"));
    return this;
  }


  /**
   * Builds the FlowMachineTester with the configured parameters.
   * Creates defensive copies of mutable collections to ensure immutability.
   *
   * @return a configured FlowMachineTester instance
   * @throws IllegalStateException if required configuration is missing
   */
  public FlowMachineTester<TState, TEvent, TContext> build() {
    validateConfiguration();

    TestScenario<TState, TEvent, TContext> scenario = TestScenario.<TState, TEvent, TContext>builder()
        .stateMachine(stateMachine)
        .startingState(startingState)
        .initialContext(initialContext)
        .expectedTransitions(new ArrayList<>(expectedTransitions))
        .testSteps(new ArrayList<>(testSteps))
        .expectedFinalState(expectedFinalState)
        .contextValidations(new ArrayList<>(contextValidations))
        .build();

    return new FlowMachineTesterImpl<>(scenario);
  }

  private void validateConfiguration() {
    List<String> errors = new ArrayList<>();

    if (startingState == null) {
      errors.add("Starting state must be specified");
    }

    if (expectedTransitions.isEmpty() && expectedFinalState == null) {
      errors.add("At least one expected transition or final state must be specified");
    }

    // Validate that we have meaningful test steps
    if (testSteps.isEmpty()) {
      errors.add("No test steps defined - add transitions or context modifications");
    }

    // Check for consistency between expected transitions and test steps
    long transitionStepCount = testSteps.stream()
        .filter(TestStep::isTransition)
        .count();
    if (transitionStepCount != expectedTransitions.size()) {
      errors.add("Mismatch between expected transitions (" + expectedTransitions.size() +
                 ") and transition steps (" + transitionStepCount + ")");
    }

    if (!errors.isEmpty()) {
      throw new IllegalStateException("Invalid configuration: " + String.join(", ", errors));
    }
  }
}