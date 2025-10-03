package com.flowmachine.examples.cucumber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.flowmachine.core.FlowMachine;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.api.StateConfiguration;
import com.flowmachine.core.api.StateMachineBuilder;
import com.flowmachine.core.model.StateMachineInfo;
import com.flowmachine.core.model.TransitionResult;
import com.flowmachine.core.model.ValidationResult;
import com.flowmachine.examples.cucumber.JobApplicantSteps.JobState;
import com.flowmachine.examples.cucumber.JobApplicantSteps.JobEvent;
import com.flowmachine.examples.cucumber.JobApplicantSteps.JobApplicant;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Refactored version of JobApplicantSteps demonstrating state handler pattern.
 * Shows how to break down long StateConfiguration into manageable, focused classes.
 */
public class JobApplicantStepsRefactored {

    // State Handler Interface
    public interface StateHandler {
        JobState getState();
        StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration);
    }

    // State Handler Registry
    public static class StateHandlerRegistry {
        private final List<StateHandler> stateHandlers = new ArrayList<>();

        public StateHandlerRegistry register(StateHandler stateHandler) {
            stateHandlers.add(stateHandler);
            return this;
        }

        public StateMachineBuilder<JobState, JobEvent, JobApplicant> applyTo(
                StateMachineBuilder<JobState, JobEvent, JobApplicant> builder,
                JobState initialState) {

            builder.initialState(initialState);

            for (StateHandler handler : stateHandlers) {
                StateConfiguration<JobState, JobEvent, JobApplicant> config =
                    builder.configure(handler.getState());
                config = handler.configure(config);
                config.and();
            }

            return builder;
        }

        public int size() {
            return stateHandlers.size();
        }

        public void clear() {
            stateHandlers.clear();
        }
    }

    // Individual State Handlers
    public static class SubmittedStateHandler implements StateHandler {
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

    public static class InitialScreeningStateHandler implements StateHandler {
        @Override
        public JobState getState() {
            return JobState.INITIAL_SCREENING;
        }

        @Override
        public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
            return configuration
                // Highest priority: Exceptional candidates go to final review
                .permitIf(JobEvent.PROCEED, JobState.FINAL_REVIEW,
                    (t, applicant) -> applicant.isExceptionalCandidate())

                // High priority: Technical roles need technical review
                .permitIf(JobEvent.PROCEED, JobState.TECHNICAL_REVIEW,
                    (t, applicant) -> applicant.needsTechnicalAssessment() && !applicant.isExceptionalCandidate())

                // Medium priority: Priority roles go to HR interview
                .permitIf(JobEvent.PROCEED, JobState.HR_INTERVIEW,
                    (t, applicant) -> applicant.isPriorityRole() && !applicant.needsTechnicalAssessment()
                                      && !applicant.isExceptionalCandidate())

                // Default path: Standard candidates go to HR interview
                .permitIf(JobEvent.PROCEED, JobState.HR_INTERVIEW,
                    (t, applicant) -> !applicant.shouldBeRejected() && !applicant.isExceptionalCandidate()
                                      && !applicant.needsTechnicalAssessment() && !applicant.isPriorityRole())

                // Lowest priority: Candidates who should be rejected
                .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                    (t, applicant) -> applicant.shouldBeRejected())

                .permit(JobEvent.REJECT, JobState.REJECTED)
                .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
                .permit(JobEvent.PUT_ON_HOLD, JobState.ON_HOLD);
        }
    }

    public static class TechnicalReviewStateHandler implements StateHandler {
        @Override
        public JobState getState() {
            return JobState.TECHNICAL_REVIEW;
        }

        @Override
        public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
            return configuration
                .permitIf(JobEvent.PROCEED, JobState.FINAL_REVIEW,
                    (t, applicant) -> applicant.getTechnicalScore() >= 7.0)
                .permitIf(JobEvent.PROCEED, JobState.HR_INTERVIEW,
                    (t, applicant) -> applicant.getTechnicalScore() >= 6.0 && applicant.getTechnicalScore() < 7.0)
                .permitIf(JobEvent.PROCEED, JobState.TECHNICAL_INTERVIEW,
                    (t, applicant) -> applicant.getTechnicalScore() >= 5.5 && applicant.getTechnicalScore() < 6.0)
                .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                    (t, applicant) -> applicant.getTechnicalScore() < 5.5)
                .permit(JobEvent.REJECT, JobState.REJECTED)
                .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
                .permit(JobEvent.PUT_ON_HOLD, JobState.ON_HOLD);
        }
    }

    public static class HrInterviewStateHandler implements StateHandler {
        @Override
        public JobState getState() {
            return JobState.HR_INTERVIEW;
        }

        @Override
        public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
            return configuration
                .permitIf(JobEvent.PROCEED, JobState.FINAL_REVIEW,
                    (t, applicant) -> applicant.getScreeningScore() >= 8.0)
                .permitIf(JobEvent.PROCEED, JobState.TECHNICAL_INTERVIEW,
                    (t, applicant) -> applicant.getScreeningScore() >= 7.0 && applicant.getScreeningScore() < 8.0)
                .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                    (t, applicant) -> applicant.getScreeningScore() < 7.0)
                .permit(JobEvent.REJECT, JobState.REJECTED)
                .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN);
        }
    }

    public static class TechnicalInterviewStateHandler implements StateHandler {
        @Override
        public JobState getState() {
            return JobState.TECHNICAL_INTERVIEW;
        }

        @Override
        public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
            return configuration
                .permitIf(JobEvent.PROCEED, JobState.FINAL_REVIEW,
                    (t, applicant) -> applicant.getTechnicalScore() >= 7.5)
                .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                    (t, applicant) -> applicant.getTechnicalScore() < 7.5)
                .permit(JobEvent.REJECT, JobState.REJECTED)
                .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN);
        }
    }

    public static class FinalReviewStateHandler implements StateHandler {
        @Override
        public JobState getState() {
            return JobState.FINAL_REVIEW;
        }

        @Override
        public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
            return configuration
                .permitIf(JobEvent.PROCEED, JobState.BACKGROUND_CHECK,
                    (t, applicant) -> applicant.getScreeningScore() >= 7.5)
                .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                    (t, applicant) -> applicant.getScreeningScore() < 7.5)
                .permit(JobEvent.REJECT, JobState.REJECTED)
                .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN);
        }
    }

    public static class BackgroundCheckStateHandler implements StateHandler {
        @Override
        public JobState getState() {
            return JobState.BACKGROUND_CHECK;
        }

        @Override
        public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
            return configuration
                .permitIf(JobEvent.PROCEED, JobState.OFFER_EXTENDED,
                    (t, applicant) -> !applicant.hasRedFlags())
                .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                    (t, applicant) -> applicant.hasRedFlags())
                .permit(JobEvent.REJECT, JobState.REJECTED)
                .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN);
        }
    }

    public static class OfferExtendedStateHandler implements StateHandler {
        @Override
        public JobState getState() {
            return JobState.OFFER_EXTENDED;
        }

        @Override
        public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
            return configuration
                .permit(JobEvent.PROCEED, JobState.HIRED)
                .permit(JobEvent.REJECT, JobState.REJECTED)
                .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN);
        }
    }

    public static class OnHoldStateHandler implements StateHandler {
        @Override
        public JobState getState() {
            return JobState.ON_HOLD;
        }

        @Override
        public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
            return configuration
                .permit(JobEvent.PROCEED, JobState.HR_INTERVIEW)
                .permit(JobEvent.REJECT, JobState.REJECTED)
                .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN);
        }
    }

    public static class FinalStateHandler implements StateHandler {
        private final JobState state;

        public FinalStateHandler(JobState state) {
            this.state = state;
        }

        @Override
        public JobState getState() {
            return state;
        }

        @Override
        public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
                StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
            return configuration.asFinal();
        }
    }

    // Test context
    private StateMachine<JobState, JobEvent, JobApplicant> workflow;
    private JobApplicant currentApplicant;
    private TransitionResult<JobState> lastTransitionResult;
    private final Map<String, JobApplicant> applicants = new HashMap<>();

    @Given("I have a refactored job applicant workflow")
    public void i_have_a_refactored_job_applicant_workflow() {
        // Create workflow using state handler registry
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

        workflow = registry
            .applyTo(FlowMachine.<JobState, JobEvent, JobApplicant>builder(), JobState.SUBMITTED)
            .build();

        assertTrue(workflow.validate().isValid());
    }

    // Test method to demonstrate the refactored approach
    @Then("the refactored workflow should behave identically to the original")
    public void the_refactored_workflow_should_behave_identically_to_the_original() {
        // Create test applicant
        JobApplicant applicant = new JobApplicant("TestRefactored");
        applicant.setExperienceYears(5);
        applicant.setScreeningScore(7.0);
        applicant.setTechnicalScore(7.0);
        applicant.setRedFlags(false);

        // Test basic flow
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
    }
}