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
 * Demonstrates the enhanced StateMachineBuilder API that allows seamless mixing
 * of explicit state configuration and StateHandler pattern.
 */
public class EnhancedApiDemo {

    // Example StateHandlers implementing the core interface
    public static class SubmittedStateHandler implements StateHandler<JobState, JobEvent, JobApplicant> {
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
        @Override
        public JobState getState() {
            return JobState.INITIAL_SCREENING;
        }

        @Override
        public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
            return configuration
                // Priority-based routing logic encapsulated in the handler
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

    @Test
    public void testEnhancedApiMixingHandlersAndExplicitConfiguration() {
        // Demonstrate the enhanced API that allows seamless mixing
        StateMachine<JobState, JobEvent, JobApplicant> workflow = FlowMachine.<JobState, JobEvent, JobApplicant>builder()
            .initialState(JobState.SUBMITTED)

            // Use StateHandler for complex states
            .configure(new SubmittedStateHandler())
            .configure(new InitialScreeningStateHandler())

            // Mix with explicit configuration for simpler states
            .configure(JobState.HR_INTERVIEW)
                .permitIf(JobEvent.PROCEED, JobState.FINAL_REVIEW,
                    (t, applicant) -> applicant.getScreeningScore() >= 8.0)
                .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                    (t, applicant) -> applicant.getScreeningScore() < 7.0)
                .permit(JobEvent.REJECT, JobState.REJECTED)
                .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
                .and()

            .configure(JobState.FINAL_REVIEW)
                .permitIf(JobEvent.PROCEED, JobState.HIRED,
                    (t, applicant) -> applicant.getScreeningScore() >= 7.5)
                .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                    (t, applicant) -> applicant.getScreeningScore() < 7.5)
                .permit(JobEvent.REJECT, JobState.REJECTED)
                .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
                .and()

            // Final states can be configured inline
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

        // Test the workflow
        JobApplicant applicant = new JobApplicant("TestCandidate");
        applicant.setExperienceYears(5);
        applicant.setScreeningScore(8.5);
        applicant.setTechnicalScore(7.0);
        applicant.setRedFlags(false);

        JobState currentState = JobState.SUBMITTED;

        // Should go from SUBMITTED to INITIAL_SCREENING (via SubmittedStateHandler)
        currentState = workflow.fire(currentState, JobEvent.PROCEED, applicant);
        assertEquals(JobState.INITIAL_SCREENING, currentState);

        // Should go to HR_INTERVIEW (via InitialScreeningStateHandler)
        currentState = workflow.fire(currentState, JobEvent.PROCEED, applicant);
        assertEquals(JobState.HR_INTERVIEW, currentState);

        // Should go to FINAL_REVIEW (via explicit configuration - score >= 8.0)
        currentState = workflow.fire(currentState, JobEvent.PROCEED, applicant);
        assertEquals(JobState.FINAL_REVIEW, currentState);

        // Should go to HIRED (via explicit configuration - score >= 7.5)
        currentState = workflow.fire(currentState, JobEvent.PROCEED, applicant);
        assertEquals(JobState.HIRED, currentState);

        assertTrue(workflow.isFinalState(currentState));
        System.out.println("✅ Enhanced API mixing test completed successfully!");
    }

    @Test
    public void testPureStateHandlerApproach() {
        // You can also use pure StateHandler approach for all states
        StateMachine<JobState, JobEvent, JobApplicant> workflow = FlowMachine.<JobState, JobEvent, JobApplicant>builder()
            .initialState(JobState.SUBMITTED)
            .configure(new SubmittedStateHandler())
            .configure(new InitialScreeningStateHandler())
            .configure(new SimpleStateHandler(JobState.HIRED, true))  // Final state handler
            .configure(new SimpleStateHandler(JobState.REJECTED, true))  // Final state handler
            .configure(new SimpleStateHandler(JobState.WITHDRAWN, true))  // Final state handler
            .build();

        assertTrue(workflow.validate().isValid());
        System.out.println("✅ Pure StateHandler approach test completed successfully!");
    }

    @Test
    public void testPureExplicitConfigurationApproach() {
        // Traditional explicit configuration still works exactly as before
        StateMachine<JobState, JobEvent, JobApplicant> workflow = FlowMachine.<JobState, JobEvent, JobApplicant>builder()
            .initialState(JobState.SUBMITTED)

            .configure(JobState.SUBMITTED)
                .permitIf(JobEvent.PROCEED, JobState.INITIAL_SCREENING,
                    (t, applicant) -> applicant.meetsBasicRequirements())
                .permit(JobEvent.REJECT, JobState.REJECTED)
                .and()

            .configure(JobState.INITIAL_SCREENING)
                .permitIf(JobEvent.PROCEED, JobState.HR_INTERVIEW,
                    (t, applicant) -> !applicant.shouldBeRejected())
                .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                    (t, applicant) -> applicant.shouldBeRejected())
                .and()

            .configure(JobState.HR_INTERVIEW)
                .permit(JobEvent.PROCEED, JobState.HIRED)
                .and()

            .configure(JobState.HIRED)
                .asFinal()
                .and()

            .configure(JobState.REJECTED)
                .asFinal()
                .and()

            .build();

        assertTrue(workflow.validate().isValid());
        System.out.println("✅ Pure explicit configuration approach test completed successfully!");
    }

    // Helper StateHandler for simple states
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
}