package com.flowmachine.examples.enhanced;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flowmachine.core.FlowMachine;
import com.flowmachine.core.api.StateConfiguration;
import com.flowmachine.core.api.StateHandler;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.exception.StateMachineException;
import com.flowmachine.examples.cucumber.JobApplicantSteps.JobState;
import com.flowmachine.examples.cucumber.JobApplicantSteps.JobEvent;
import com.flowmachine.examples.cucumber.JobApplicantSteps.JobApplicant;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates the enhanced StateMachineBuilder API with explicit state visibility.
 * Shows both approaches: configure(handler) and configure(state, handler).
 */
public class StateVisibilityDemo {

    // Example StateHandlers
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
    public void testOriginalApproachWithoutStateVisibility() {
        // Original approach - state is not visible in the method call
        StateMachine<JobState, JobEvent, JobApplicant> workflow = FlowMachine.<JobState, JobEvent, JobApplicant>builder()
            .initialState(JobState.SUBMITTED)

            // You can't see which states these handlers configure
            .configure(new SubmittedStateHandler())           // ❓ Which state?
            .configure(new InitialScreeningStateHandler())    // ❓ Which state?

            .configure(JobState.HR_INTERVIEW)  // ✅ Clear which state
                .permit(JobEvent.PROCEED, JobState.HIRED)
                .and()

            .configure(JobState.FINAL_REVIEW)  // ✅ Clear which state
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
        System.out.println("✅ Original approach (without state visibility) works!");
    }

    @Test
    public void testEnhancedApproachWithStateVisibility() {
        // Enhanced approach - state is explicitly visible in the method call
        StateMachine<JobState, JobEvent, JobApplicant> workflow = FlowMachine.<JobState, JobEvent, JobApplicant>builder()
            .initialState(JobState.SUBMITTED)

            // Now you can clearly see which states are being configured
            .configure(JobState.SUBMITTED, new SubmittedStateHandler())           // ✅ Clear: SUBMITTED state
            .configure(JobState.INITIAL_SCREENING, new InitialScreeningStateHandler())  // ✅ Clear: INITIAL_SCREENING state

            .configure(JobState.HR_INTERVIEW)  // ✅ Explicit configuration still works
                .permit(JobEvent.PROCEED, JobState.HIRED)
                .and()

            .configure(JobState.FINAL_REVIEW)
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
        System.out.println("✅ Enhanced approach (with state visibility) works!");
    }

    @Test
    public void testStateMismatchValidation() {
        // Test that state mismatch is properly detected
        assertThrows(StateMachineException.class, () -> {
            FlowMachine.<JobState, JobEvent, JobApplicant>builder()
                .initialState(JobState.SUBMITTED)
                // This should fail: INITIAL_SCREENING != SUBMITTED
                .configure(JobState.INITIAL_SCREENING, new SubmittedStateHandler())
                .build();
        }, "Should throw exception when state doesn't match handler state");

        System.out.println("✅ State mismatch validation works correctly!");
    }

    @Test
    public void testMixedApproaches() {
        // You can mix both approaches in the same builder
        StateMachine<JobState, JobEvent, JobApplicant> workflow = FlowMachine.<JobState, JobEvent, JobApplicant>builder()
            .initialState(JobState.SUBMITTED)

            // Use explicit state visibility where it adds clarity
            .configure(JobState.SUBMITTED, new SubmittedStateHandler())

            // Use original approach where state is obvious from context
            .configure(new InitialScreeningStateHandler())

            // Mix with explicit configuration
            .configure(JobState.HR_INTERVIEW)
                .permit(JobEvent.PROCEED, JobState.HIRED)
                .and()

            .configure(JobState.FINAL_REVIEW)
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

        // Test the workflow
        JobApplicant applicant = new JobApplicant("TestCandidate");
        applicant.setExperienceYears(5);
        applicant.setScreeningScore(7.0);
        applicant.setRedFlags(false);

        JobState currentState = JobState.SUBMITTED;

        // Should go from SUBMITTED to INITIAL_SCREENING
        currentState = workflow.fire(currentState, JobEvent.PROCEED, applicant);
        assertEquals(JobState.INITIAL_SCREENING, currentState);

        // Should go to HR_INTERVIEW
        currentState = workflow.fire(currentState, JobEvent.PROCEED, applicant);
        assertEquals(JobState.HR_INTERVIEW, currentState);

        // Should go to HIRED
        currentState = workflow.fire(currentState, JobEvent.PROCEED, applicant);
        assertEquals(JobState.HIRED, currentState);

        assertTrue(workflow.isFinalState(currentState));
        System.out.println("✅ Mixed approaches work perfectly together!");
    }

    @Test
    public void testReadabilityComparison() {
        System.out.println("\n=== Readability Comparison ===");

        System.out.println("\n❌ BEFORE (Hard to see which states are configured):");
        System.out.println("    .configure(new SubmittedStateHandler())");
        System.out.println("    .configure(new InitialScreeningStateHandler())");
        System.out.println("    .configure(new TechnicalReviewStateHandler())");

        System.out.println("\n✅ AFTER (Crystal clear which states are configured):");
        System.out.println("    .configure(JobState.SUBMITTED, new SubmittedStateHandler())");
        System.out.println("    .configure(JobState.INITIAL_SCREENING, new InitialScreeningStateHandler())");
        System.out.println("    .configure(JobState.TECHNICAL_REVIEW, new TechnicalReviewStateHandler())");

        System.out.println("\n🎯 BENEFIT: IDE autocomplete shows you the state being configured!");
        System.out.println("🎯 BENEFIT: Code reviews are easier - you can see state coverage at a glance!");
        System.out.println("🎯 BENEFIT: Refactoring is safer - state references are explicit!");

        assertTrue(true); // Just for demonstration
    }
}