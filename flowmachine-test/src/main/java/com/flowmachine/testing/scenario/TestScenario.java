package com.flowmachine.testing.scenario;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.testing.scenario.step.TestStep;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents a complete test scenario for a state machine.
 *
 * @param <TState> the state type
 * @param <TEvent> the event type
 * @param <TContext> the context type
 */
public class TestScenario<TState, TEvent, TContext> {

    private final StateMachine<TState, TEvent, TContext> stateMachine;
    private final TState startingState;
    private final TContext initialContext;
    private final List<ExpectedTransition<TState, TEvent>> expectedTransitions;
    private final List<TestStep<TState, TEvent, TContext>> testSteps;
    private final TState expectedFinalState;
    private final List<Predicate<TContext>> contextValidations;

    private TestScenario(Builder<TState, TEvent, TContext> builder) {
        this.stateMachine = builder.stateMachine;
        this.startingState = builder.startingState;
        this.initialContext = builder.initialContext;
        this.expectedTransitions = new ArrayList<>(builder.expectedTransitions);
        this.testSteps = new ArrayList<>(builder.testSteps);
        this.expectedFinalState = builder.expectedFinalState;
        this.contextValidations = new ArrayList<>(builder.contextValidations);
    }

    public static <TState, TEvent, TContext> Builder<TState, TEvent, TContext> builder() {
        return new Builder<>();
    }

    public StateMachine<TState, TEvent, TContext> getStateMachine() {
        return stateMachine;
    }

    public TState getStartingState() {
        return startingState;
    }

    public TContext getInitialContext() {
        return initialContext;
    }

    public List<ExpectedTransition<TState, TEvent>> getExpectedTransitions() {
        return new ArrayList<>(expectedTransitions);
    }

    public List<TestStep<TState, TEvent, TContext>> getTestSteps() {
        return new ArrayList<>(testSteps);
    }

    public TState getExpectedFinalState() {
        return expectedFinalState;
    }


    public List<Predicate<TContext>> getContextValidations() {
        return new ArrayList<>(contextValidations);
    }

    public static class Builder<TState, TEvent, TContext> {
        private StateMachine<TState, TEvent, TContext> stateMachine;
        private TState startingState;
        private TContext initialContext;
        private List<ExpectedTransition<TState, TEvent>> expectedTransitions = new ArrayList<>();
        private List<TestStep<TState, TEvent, TContext>> testSteps = new ArrayList<>();
        private TState expectedFinalState;
        private List<Predicate<TContext>> contextValidations = new ArrayList<>();

        public Builder<TState, TEvent, TContext> stateMachine(StateMachine<TState, TEvent, TContext> stateMachine) {
            this.stateMachine = stateMachine;
            return this;
        }

        public Builder<TState, TEvent, TContext> startingState(TState startingState) {
            this.startingState = startingState;
            return this;
        }

        public Builder<TState, TEvent, TContext> initialContext(TContext initialContext) {
            this.initialContext = initialContext;
            return this;
        }

        public Builder<TState, TEvent, TContext> expectedTransitions(List<ExpectedTransition<TState, TEvent>> expectedTransitions) {
            this.expectedTransitions = new ArrayList<>(expectedTransitions);
            return this;
        }

        public Builder<TState, TEvent, TContext> testSteps(List<TestStep<TState, TEvent, TContext>> testSteps) {
            this.testSteps = new ArrayList<>(testSteps);
            return this;
        }

        public Builder<TState, TEvent, TContext> expectedFinalState(TState expectedFinalState) {
            this.expectedFinalState = expectedFinalState;
            return this;
        }


        public Builder<TState, TEvent, TContext> contextValidations(List<Predicate<TContext>> contextValidations) {
            this.contextValidations = new ArrayList<>(contextValidations);
            return this;
        }

        public TestScenario<TState, TEvent, TContext> build() {
            Objects.requireNonNull(stateMachine, "StateMachine cannot be null");
            Objects.requireNonNull(startingState, "Starting state cannot be null");
            return new TestScenario<>(this);
        }
    }
}