package com.flowmachine.testing.impl;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.model.TransitionResult;
import com.flowmachine.testing.FlowMachineTester;
import com.flowmachine.testing.result.TestFailure;
import com.flowmachine.testing.result.TestResult;
import com.flowmachine.testing.result.TestResult.Builder;
import com.flowmachine.testing.result.TransitionExecutionResult;
import com.flowmachine.testing.result.TransitionTestResult;
import com.flowmachine.testing.result.ValidationTestResult;
import com.flowmachine.testing.scenario.ExpectedTransition;
import com.flowmachine.testing.scenario.TestScenario;
import com.flowmachine.testing.scenario.step.ContextActionStep;
import com.flowmachine.testing.scenario.step.TestStep;
import com.flowmachine.testing.scenario.step.TransitionStep;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of FlowMachineTester providing comprehensive testing capabilities.
 *
 * @param <TState>   the state type
 * @param <TEvent>   the event type
 * @param <TContext> the context type
 */
public class FlowMachineTesterImpl<TState, TEvent, TContext> implements FlowMachineTester<TState, TEvent, TContext> {

  private static final Logger logger = LoggerFactory.getLogger(FlowMachineTesterImpl.class);

  private final TestScenario<TState, TEvent, TContext> scenario;

  public FlowMachineTesterImpl(TestScenario<TState, TEvent, TContext> scenario) {
    this.scenario = Objects.requireNonNull(scenario, "TestScenario cannot be null");
  }

  @Override
  public TestResult<TState, TEvent, TContext> runScenario() {
    logger.info("Starting test scenario execution");
    Instant startTime = Instant.now();

    TestResult.Builder<TState, TEvent, TContext> resultBuilder = TestResult.<TState, TEvent, TContext>builder()
        .startTime(startTime)
        .initialState(scenario.getStartingState());

    try {
      ScenarioExecutionContext<TState, TEvent, TContext> executionContext = initializeExecution();
      List<TransitionExecutionResult<TState, TEvent, TContext>> transitionResults =
          executeTestSteps(executionContext, resultBuilder);

      resultBuilder.transitionResults(transitionResults);
      validateFinalState(executionContext, resultBuilder);
      validateContext(executionContext, resultBuilder);

      resultBuilder
          .finalState(executionContext.getCurrentState())
          .finalContext(executionContext.getCurrentContext());

    } catch (Exception e) {
      logger.error("Test scenario execution failed", e);
      resultBuilder.addError(new TestFailure("Scenario execution failed", e));
    }

    Instant endTime = Instant.now();
    resultBuilder.endTime(endTime);

    TestResult<TState, TEvent, TContext> result = resultBuilder.build();
    logger.info("Test scenario completed: {}", result);
    return result;
  }

  private ScenarioExecutionContext<TState, TEvent, TContext> initializeExecution() {
    StateMachine<TState, TEvent, TContext> stateMachine = scenario.getStateMachine();
    TState currentState = scenario.getStartingState();
    TContext currentContext = scenario.getInitialContext();

    return new ScenarioExecutionContext<>(stateMachine, currentState, currentContext);
  }

  private List<TransitionExecutionResult<TState, TEvent, TContext>> executeTestSteps(
      ScenarioExecutionContext<TState, TEvent, TContext> context,
      TestResult.Builder<TState, TEvent, TContext> resultBuilder) {

    List<TransitionExecutionResult<TState, TEvent, TContext>> transitionResults = new ArrayList<>();

    for (TestStep<TState, TEvent, TContext> step : scenario.getTestSteps()) {
      if (step.isTransition()) {
        TransitionExecutionResult<TState, TEvent, TContext> transitionResult =
            executeTransitionStep(step.asTransition(), context, resultBuilder);
        transitionResults.add(transitionResult);
      } else if (step.isContextAction()) {
        executeContextActionStep(step.asContextAction(), context, resultBuilder);
      }
    }

    return transitionResults;
  }

  private TransitionExecutionResult<TState, TEvent, TContext> executeTransitionStep(
      TransitionStep<TState, TEvent, TContext> transitionStep,
      ScenarioExecutionContext<TState, TEvent, TContext> context,
      TestResult.Builder<TState, TEvent, TContext> resultBuilder) {

    TEvent event = transitionStep.getEvent();
    TState expectedTargetState = transitionStep.getExpectedTargetState();
    TState currentState = context.getCurrentState();

    logger.debug("Testing transition: {} -> {} with event {}", currentState, expectedTargetState, event);

    Instant transitionStartTime = Instant.now();
    TransitionExecutionResult.Builder<TState, TEvent, TContext> transitionResultBuilder =
        TransitionExecutionResult.<TState, TEvent, TContext>builder()
            .event(event)
            .sourceState(currentState)
            .expectedTargetState(expectedTargetState)
            .contextBefore(context.getCurrentContext())
            .startTime(transitionStartTime);

    try {
      TransitionResult<TState> coreResult = context.getStateMachine()
          .fireWithResult(currentState, event, context.getCurrentContext());
      Instant transitionEndTime = Instant.now();

      TState actualTargetState = coreResult.state();
      boolean successful = Objects.equals(actualTargetState, expectedTargetState);

      transitionResultBuilder
          .targetState(actualTargetState)
          .contextAfter(context.getCurrentContext())
          .endTime(transitionEndTime)
          .successful(successful);

      if (!successful) {
        String error = String.format("Expected transition to state %s but got %s", expectedTargetState,
            actualTargetState);
        transitionResultBuilder.errorMessage(error);
        resultBuilder.addError(new TestFailure(error));
      }

      context.setCurrentState(actualTargetState);

    } catch (Exception e) {
      handleTransitionException(resultBuilder, transitionResultBuilder, e);
    }

    return transitionResultBuilder.build();
  }

  private void handleTransitionException(
      Builder<TState, TEvent, TContext> resultBuilder,
      TransitionExecutionResult.Builder<TState, TEvent, TContext> transitionResultBuilder,
      Exception e) {
    logger.error("Transition failed with exception", e);
    Instant transitionEndTime = Instant.now();
    transitionResultBuilder
        .endTime(transitionEndTime)
        .exception(e)
        .successful(false);
    resultBuilder.addError(new TestFailure("Transition failed", e));
  }

  private void executeContextActionStep(
      ContextActionStep<TState, TEvent, TContext> actionStep,
      ScenarioExecutionContext<TState, TEvent, TContext> context,
      TestResult.Builder<TState, TEvent, TContext> resultBuilder) {

    logger.debug("Executing context action: {}", actionStep.getDescription());

    try {
      actionStep.getAction().accept(context.getCurrentContext());
      logger.debug("Context action completed successfully: {}", actionStep.getDescription());
    } catch (Exception e) {
      logger.error("Context action failed: {}", actionStep.getDescription(), e);
      resultBuilder.addError(new TestFailure("Context action failed (" + actionStep.getDescription() + ")", e));
    }
  }

  private void validateFinalState(
      ScenarioExecutionContext<TState, TEvent, TContext> context,
      TestResult.Builder<TState, TEvent, TContext> resultBuilder) {

    if (Objects.nonNull(scenario.getExpectedFinalState()) &&
        !Objects.equals(context.getCurrentState(), scenario.getExpectedFinalState())) {
      String error = String.format("Expected final state %s but got %s",
          scenario.getExpectedFinalState(), context.getCurrentState());
      resultBuilder.addError(new TestFailure(error));
    }
  }

  private void validateContext(
      ScenarioExecutionContext<TState, TEvent, TContext> context,
      TestResult.Builder<TState, TEvent, TContext> resultBuilder) {

    for (Predicate<TContext> validation : scenario.getContextValidations()) {
      try {
        if (!validation.test(context.getCurrentContext())) {
          resultBuilder.addError(new TestFailure("Context validation failed"));
        }
      } catch (Exception e) {
        resultBuilder.addError(new TestFailure("Context validation threw exception", e));
      }
    }
  }

  @Override
  public ValidationTestResult validateConfiguration() {
    logger.info("Validating state machine configuration");
    ValidationTestResult.Builder builder = ValidationTestResult.builder();

    try {
      StateMachine<TState, TEvent, TContext> stateMachine = scenario.getStateMachine();

      if (Objects.isNull(stateMachine)) {
        builder.addError("StateMachine is null");
        return builder.build();
      }

      if (Objects.isNull(scenario.getStartingState())) {
        builder.addError("Starting state is null");
      }

      if (scenario.getExpectedTransitions().isEmpty() && Objects.isNull(scenario.getExpectedFinalState())) {
        builder.addWarning("No expected transitions or final state specified");
      }

      for (ExpectedTransition<TState, TEvent> transition : scenario.getExpectedTransitions()) {
        if (Objects.isNull(transition.event())) {
          builder.addError("Expected transition has null event");
        }
        if (Objects.isNull(transition.expectedTargetState())) {
          builder.addError("Expected transition has null target state");
        }
      }

    } catch (Exception e) {
      logger.error("Configuration validation failed", e);
      builder.addError("Validation failed: " + e.getMessage());
    }

    ValidationTestResult result = builder.build();
    logger.info("Configuration validation completed: {}", result);
    return result;
  }

  @Override
  public TransitionTestResult<TState, TEvent, TContext> testAllTransitions(TState currentState, TContext context) {
    logger.info("Testing all transitions from state: {}", currentState);

    TransitionTestResult.Builder<TState, TEvent, TContext> builder = TransitionTestResult.<TState, TEvent, TContext>builder()
        .sourceState(currentState)
        .context(context);

    try {
      logger.warn(
          "Note: testAllTransitions requires enumeration of all possible events, which is not implemented in this version");
      builder.addError("Event enumeration not implemented - cannot test all possible transitions");

    } catch (Exception e) {
      logger.error("Transition testing failed", e);
      builder.addError("Transition testing failed: " + e.getMessage());
    }

    TransitionTestResult<TState, TEvent, TContext> result = builder.build();
    logger.info("Transition testing completed: {}", result);
    return result;
  }


}