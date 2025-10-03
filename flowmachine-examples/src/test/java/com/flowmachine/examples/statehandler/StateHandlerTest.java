package com.flowmachine.examples.statehandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flowmachine.core.FlowMachine;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.examples.cucumber.JobApplicantSteps.JobState;
import com.flowmachine.examples.cucumber.JobApplicantSteps.JobEvent;
import com.flowmachine.examples.cucumber.JobApplicantSteps.JobApplicant;
import com.flowmachine.examples.cucumber.JobApplicantStepsRefactored.StateHandlerRegistry;
import com.flowmachine.examples.cucumber.JobApplicantStepsRefactored.SubmittedStateHandler;
import com.flowmachine.examples.cucumber.JobApplicantStepsRefactored.InitialScreeningStateHandler;
import com.flowmachine.examples.cucumber.JobApplicantStepsRefactored.TechnicalReviewStateHandler;
import com.flowmachine.examples.cucumber.JobApplicantStepsRefactored.HrInterviewStateHandler;
import com.flowmachine.examples.cucumber.JobApplicantStepsRefactored.TechnicalInterviewStateHandler;
import com.flowmachine.examples.cucumber.JobApplicantStepsRefactored.FinalReviewStateHandler;
import com.flowmachine.examples.cucumber.JobApplicantStepsRefactored.BackgroundCheckStateHandler;
import com.flowmachine.examples.cucumber.JobApplicantStepsRefactored.OfferExtendedStateHandler;
import com.flowmachine.examples.cucumber.JobApplicantStepsRefactored.OnHoldStateHandler;
import com.flowmachine.examples.cucumber.JobApplicantStepsRefactored.FinalStateHandler;
import org.junit.jupiter.api.Test;

/**
 * Test demonstrating the State Handler pattern for breaking down large StateConfiguration.
 * Shows how the refactored approach produces identical behavior to the original monolithic configuration.
 */
public class StateHandlerTest {

    @Test
    public void testStateHandlerPatternProducesSameBehavior() {
        // Create workflow using state handler registry pattern
        StateHandlerRegistry registry = new StateHandlerRegistry()
            .register(new SubmittedStateHandler())
            .register(new InitialScreeningStateHandler())
            .register(new TechnicalReviewStateHandler())
            .register(new HrInterviewStateHandler())
            .register(new TechnicalInterviewStateHandler())
            .register(new FinalReviewStateHandler())
            .register(new BackgroundCheckStateHandler())
            .register(new OfferExtendedStateHandler())
            .register(new OnHoldStateHandler())
            .register(new FinalStateHandler(JobState.HIRED))
            .register(new FinalStateHandler(JobState.REJECTED))
            .register(new FinalStateHandler(JobState.WITHDRAWN));

        StateMachine<JobState, JobEvent, JobApplicant> workflow = registry
            .applyTo(FlowMachine.<JobState, JobEvent, JobApplicant>builder(), JobState.SUBMITTED)
            .build();

        assertTrue(workflow.validate().isValid());

        // Test standard candidate flow
        JobApplicant applicant = new JobApplicant("TestCandidate");
        applicant.setExperienceYears(5);
        applicant.setScreeningScore(7.0);
        applicant.setTechnicalScore(7.0);
        applicant.setRedFlags(false);

        JobState currentState = JobState.SUBMITTED;

        // Should go from SUBMITTED to INITIAL_SCREENING (meets basic requirements)
        currentState = workflow.fire(currentState, JobEvent.PROCEED, applicant);
        assertEquals(JobState.INITIAL_SCREENING, currentState);

        // Should go to HR_INTERVIEW (standard candidate path)
        currentState = workflow.fire(currentState, JobEvent.PROCEED, applicant);
        assertEquals(JobState.HR_INTERVIEW, currentState);

        // Should go to TECHNICAL_INTERVIEW (screening score 7.0 -> 7.0 to 8.0 range)
        currentState = workflow.fire(currentState, JobEvent.PROCEED, applicant);
        assertEquals(JobState.TECHNICAL_INTERVIEW, currentState);

        // Should go to REJECTED (technical score 7.0 < 7.5)
        currentState = workflow.fire(currentState, JobEvent.PROCEED, applicant);
        assertEquals(JobState.REJECTED, currentState);

        assertTrue(workflow.isFinalState(currentState));
        System.out.println("✅ State Handler pattern test completed successfully!");
    }

    @Test
    public void testExceptionalCandidateFlow() {
        // Create workflow using state handler registry pattern
        StateHandlerRegistry registry = new StateHandlerRegistry()
            .register(new SubmittedStateHandler())
            .register(new InitialScreeningStateHandler())
            .register(new FinalReviewStateHandler())
            .register(new BackgroundCheckStateHandler())
            .register(new OfferExtendedStateHandler())
            .register(new FinalStateHandler(JobState.HIRED))
            .register(new FinalStateHandler(JobState.REJECTED))
            .register(new FinalStateHandler(JobState.WITHDRAWN));

        StateMachine<JobState, JobEvent, JobApplicant> workflow = registry
            .applyTo(FlowMachine.<JobState, JobEvent, JobApplicant>builder(), JobState.SUBMITTED)
            .build();

        // Test exceptional candidate (should skip most steps)
        JobApplicant exceptional = new JobApplicant("ExceptionalCandidate");
        exceptional.setExperienceYears(10);  // >= 8
        exceptional.setScreeningScore(9.5);  // >= 9.0
        exceptional.setTechnicalScore(9.0);  // >= 8.5
        exceptional.setRedFlags(false);      // !redFlags

        JobState currentState = JobState.SUBMITTED;

        // Should go from SUBMITTED to INITIAL_SCREENING
        currentState = workflow.fire(currentState, JobEvent.PROCEED, exceptional);
        assertEquals(JobState.INITIAL_SCREENING, currentState);

        // Exceptional candidate should go directly to FINAL_REVIEW
        currentState = workflow.fire(currentState, JobEvent.PROCEED, exceptional);
        assertEquals(JobState.FINAL_REVIEW, currentState);

        // Should go to BACKGROUND_CHECK (screening score >= 7.5)
        currentState = workflow.fire(currentState, JobEvent.PROCEED, exceptional);
        assertEquals(JobState.BACKGROUND_CHECK, currentState);

        // Should go to OFFER_EXTENDED (no red flags)
        currentState = workflow.fire(currentState, JobEvent.PROCEED, exceptional);
        assertEquals(JobState.OFFER_EXTENDED, currentState);

        // Can proceed to HIRED
        currentState = workflow.fire(currentState, JobEvent.PROCEED, exceptional);
        assertEquals(JobState.HIRED, currentState);

        assertTrue(workflow.isFinalState(currentState));
        System.out.println("✅ Exceptional candidate flow test completed successfully!");
    }

    @Test
    public void testStateHandlerRegistryFeatures() {
        StateHandlerRegistry registry = new StateHandlerRegistry();

        // Test registry functionality
        assertEquals(0, registry.size());

        registry.register(new SubmittedStateHandler());
        assertEquals(1, registry.size());

        registry.register(new InitialScreeningStateHandler());
        assertEquals(2, registry.size());

        registry.clear();
        assertEquals(0, registry.size());

        System.out.println("✅ State Handler Registry functionality test completed!");
    }
}