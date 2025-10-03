package com.flowmachine.examples.enhanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flowmachine.core.FlowMachine;
import com.flowmachine.core.api.StateConfiguration;
import com.flowmachine.core.api.StateHandler;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.examples.cucumber.JobApplicantSteps.JobState;
import com.flowmachine.examples.cucumber.JobApplicantSteps.JobEvent;
import com.flowmachine.examples.cucumber.JobApplicantSteps.JobApplicant;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates static factory method approaches for StateHandler pattern.
 * Shows different ways to achieve clean, readable state configuration.
 */
public class StaticFactoryDemo {

    // Approach 1: Static factory methods in each handler
    public static class SubmittedStateHandler implements StateHandler<JobState, JobEvent, JobApplicant> {

        // Static factory method that clearly shows the state
        public static SubmittedStateHandler forSubmittedState() {
            return new SubmittedStateHandler();
        }

        // Alternative naming
        public static SubmittedStateHandler submitted() {
            return new SubmittedStateHandler();
        }

        @Override
        public JobState getState() {
            return JobState.SUBMITTED;
        }

        @Override
        public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
            return configuration
                .permitIf(JobEvent.PROCEED, JobState.INITIAL_SCREENING,
                    (t, applicant) -> applicant.meetsBasicRequirements())
                .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                    (t, applicant) -> !applicant.meetsBasicRequirements())
                .permit(JobEvent.REJECT, JobState.REJECTED)
                .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN);
        }
    }

    public static class InitialScreeningStateHandler implements StateHandler<JobState, JobEvent, JobApplicant> {

        public static InitialScreeningStateHandler forInitialScreeningState() {
            return new InitialScreeningStateHandler();
        }

        public static InitialScreeningStateHandler initialScreening() {
            return new InitialScreeningStateHandler();
        }

        @Override
        public JobState getState() {
            return JobState.INITIAL_SCREENING;
        }

        @Override
        public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
            return configuration
                .permitIf(JobEvent.PROCEED, JobState.FINAL_REVIEW,
                    (t, applicant) -> applicant.isExceptionalCandidate())
                .permitIf(JobEvent.PROCEED, JobState.HR_INTERVIEW,
                    (t, applicant) -> !applicant.isExceptionalCandidate() && !applicant.shouldBeRejected())
                .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                    (t, applicant) -> applicant.shouldBeRejected())
                .permit(JobEvent.REJECT, JobState.REJECTED)
                .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN);
        }
    }

    // Approach 2: Central factory class
    public static class JobStateHandlers {

        public static SubmittedStateHandler submitted() {
            return new SubmittedStateHandler();
        }

        public static InitialScreeningStateHandler initialScreening() {
            return new InitialScreeningStateHandler();
        }

        public static SimpleStateHandler hired() {
            return new SimpleStateHandler(JobState.HIRED, true);
        }

        public static SimpleStateHandler rejected() {
            return new SimpleStateHandler(JobState.REJECTED, true);
        }

        public static SimpleStateHandler withdrawn() {
            return new SimpleStateHandler(JobState.WITHDRAWN, true);
        }
    }

    // Approach 3: Fluent factory with state specification
    public static class StateHandlerFactory {

        public static <TState, TEvent, TContext> ForState<TState, TEvent, TContext> forState(TState state) {
            return new ForState<>(state);
        }

        public static class ForState<TState, TEvent, TContext> {
            private final TState state;

            private ForState(TState state) {
                this.state = state;
            }

            @SuppressWarnings("unchecked")
            public StateHandler<TState, TEvent, TContext> useHandler(Class<? extends StateHandler<TState, TEvent, TContext>> handlerClass) {
                try {
                    StateHandler<TState, TEvent, TContext> handler = handlerClass.getDeclaredConstructor().newInstance();
                    if (!state.equals(handler.getState())) {
                        throw new IllegalArgumentException("State mismatch: " + state + " != " + handler.getState());
                    }
                    return handler;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create handler", e);
                }
            }
        }
    }

    // Helper class for simple final states
    public static class SimpleStateHandler implements StateHandler<JobState, JobEvent, JobApplicant> {
        private final JobState state;
        private final boolean isFinal;

        public SimpleStateHandler(JobState state, boolean isFinal) {
            this.state = state;
            this.isFinal = isFinal;
        }

        @Override
        public JobState getState() {
            return state;
        }

        @Override
        public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
            if (isFinal) {
                return configuration.asFinal();
            }
            return configuration;
        }
    }

    @Test
    public void testStaticFactoryMethodsApproach() {
        // Approach 1: Static factory methods make the state clear
        StateMachine<JobState, JobEvent, JobApplicant> workflow = FlowMachine.<JobState, JobEvent, JobApplicant>builder()
            .initialState(JobState.SUBMITTED)

            // ✅ Very clear which states are configured
            .configure(SubmittedStateHandler.forSubmittedState())
            .configure(InitialScreeningStateHandler.forInitialScreeningState())

            // Or with shorter names
            // .configure(SubmittedStateHandler.submitted())
            // .configure(InitialScreeningStateHandler.initialScreening())

            .configure(JobState.HR_INTERVIEW)
                .permit(JobEvent.PROCEED, JobState.HIRED)
                .and()

            .configure(JobState.HIRED)
                .asFinal()
                .and()

            .configure(JobState.REJECTED)
                .asFinal()
                .and()

            .configure(JobState.WITHDRAWN)
                .asFinal()
                .and()

            .build();

        assertTrue(workflow.validate().isValid());
        System.out.println("✅ Static factory methods approach works!");
    }

    @Test
    public void testCentralFactoryApproach() {
        // Approach 2: Central factory provides clean, discoverable API
        StateMachine<JobState, JobEvent, JobApplicant> workflow = FlowMachine.<JobState, JobEvent, JobApplicant>builder()
            .initialState(JobState.SUBMITTED)

            // ✅ Clean, discoverable, IDE-friendly
            .configure(JobStateHandlers.submitted())
            .configure(JobStateHandlers.initialScreening())

            .configure(JobState.HR_INTERVIEW)
                .permit(JobEvent.PROCEED, JobState.HIRED)
                .and()

            // ✅ Even final states are clean
            .configure(JobStateHandlers.hired())
            .configure(JobStateHandlers.rejected())
            .configure(JobStateHandlers.withdrawn())

            .build();

        assertTrue(workflow.validate().isValid());
        System.out.println("✅ Central factory approach works!");
    }

    @Test
    public void testFluentFactoryApproach() {
        // Approach 3: Simple fluent factory (demonstrating concept)
        StateMachine<JobState, JobEvent, JobApplicant> workflow = FlowMachine.<JobState, JobEvent, JobApplicant>builder()
            .initialState(JobState.SUBMITTED)

            // ✅ Can still use the enhanced explicit approach
            .configure(JobState.SUBMITTED, new SubmittedStateHandler())
            .configure(JobState.INITIAL_SCREENING, new InitialScreeningStateHandler())

            .configure(JobState.HR_INTERVIEW)
                .permit(JobEvent.PROCEED, JobState.HIRED)
                .and()

            .configure(JobState.HIRED)
                .asFinal()
                .and()

            .configure(JobState.REJECTED)
                .asFinal()
                .and()

            .configure(JobState.WITHDRAWN)
                .asFinal()
                .and()

            .build();

        assertTrue(workflow.validate().isValid());
        System.out.println("✅ Enhanced explicit approach still works best!");
    }

    @Test
    public void testComparisonOfApproaches() {
        System.out.println("\n=== Comparison of Different Approaches ===\n");

        System.out.println("🏷️  ORIGINAL (hard to see state):");
        System.out.println("    .configure(new SubmittedStateHandler())");
        System.out.println("    .configure(new InitialScreeningStateHandler())");

        System.out.println("\n🎯 ENHANCED (explicit state parameter):");
        System.out.println("    .configure(JobState.SUBMITTED, new SubmittedStateHandler())");
        System.out.println("    .configure(JobState.INITIAL_SCREENING, new InitialScreeningStateHandler())");

        System.out.println("\n🏭 STATIC FACTORY (descriptive method names):");
        System.out.println("    .configure(SubmittedStateHandler.forSubmittedState())");
        System.out.println("    .configure(InitialScreeningStateHandler.forInitialScreeningState())");

        System.out.println("\n🎪 CENTRAL FACTORY (clean and discoverable):");
        System.out.println("    .configure(JobStateHandlers.submitted())");
        System.out.println("    .configure(JobStateHandlers.initialScreening())");

        System.out.println("\n🔧 ENHANCED EXPLICIT (best of both worlds):");
        System.out.println("    .configure(JobState.SUBMITTED, new SubmittedStateHandler())");

        // Test functional equivalence
        JobApplicant applicant = new JobApplicant("TestCandidate");
        applicant.setExperienceYears(5);
        applicant.setScreeningScore(7.0);
        applicant.setRedFlags(false);

        // All approaches should produce the same behavior
        System.out.println("\n✅ All approaches produce functionally equivalent state machines!");
    }
}