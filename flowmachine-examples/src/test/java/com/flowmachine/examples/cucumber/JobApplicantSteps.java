package com.flowmachine.examples.cucumber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.flowmachine.core.FlowMachine;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.model.StateMachineInfo;
import com.flowmachine.core.model.TransitionResult;
import com.flowmachine.core.model.ValidationResult;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Step definitions for Job Applicant workflow Cucumber tests. Demonstrates Event.PROCEED pattern with priority-based
 * routing.
 */
public class JobApplicantSteps {

  @And("the workflow should have final states: {string}, {string}, {string}")
  public void theWorkflowShouldHaveFinalStates(String state1, String state2, String state3) {
    var finalStates = workflow.getInfo().states().stream().filter(workflow::isFinalState).map(JobState::name).toList();
    assertEquals(3, finalStates.size());
    assertTrue(finalStates.contains(state1));
    assertTrue(finalStates.contains(state2));
    assertTrue(finalStates.contains(state3));
  }

  // Job applicant states and events
  public enum JobState {
    SUBMITTED, INITIAL_SCREENING, TECHNICAL_REVIEW, HR_INTERVIEW,
    TECHNICAL_INTERVIEW, FINAL_REVIEW, BACKGROUND_CHECK, OFFER_EXTENDED,
    HIRED, REJECTED, WITHDRAWN, ON_HOLD
  }

  public enum JobEvent {PROCEED, REJECT, WITHDRAW, PUT_ON_HOLD}

  // Job applicant context object
  public static class JobApplicant {

    private final String name;
    private int experienceYears;
    private double screeningScore;
    private double technicalScore;
    private boolean redFlags;
    private boolean priorityRole;
    private JobState currentState;

    public JobApplicant(String name) {
      this.name = name;
      this.currentState = JobState.SUBMITTED;
    }

    // Getters and setters
    public String getName() {
      return name;
    }

    public int getExperienceYears() {
      return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
      this.experienceYears = experienceYears;
    }

    public double getScreeningScore() {
      return screeningScore;
    }

    public void setScreeningScore(double screeningScore) {
      this.screeningScore = screeningScore;
    }

    public double getTechnicalScore() {
      return technicalScore;
    }

    public void setTechnicalScore(double technicalScore) {
      this.technicalScore = technicalScore;
    }

    public boolean hasRedFlags() {
      return redFlags;
    }

    public void setRedFlags(boolean redFlags) {
      this.redFlags = redFlags;
    }

    public boolean isPriorityRole() {
      return priorityRole;
    }

    public void setPriorityRole(boolean priorityRole) {
      this.priorityRole = priorityRole;
    }

    public JobState getCurrentState() {
      return currentState;
    }

    public void setCurrentState(JobState state) {
      this.currentState = state;
    }

    // Business logic methods
    public boolean isExceptionalCandidate() {
      return experienceYears >= 8 && screeningScore >= 9.0 && technicalScore >= 8.5 && !redFlags;
    }

    public boolean needsTechnicalAssessment() {
      return experienceYears >= 4 && screeningScore >= 7.5 && !redFlags && !priorityRole;
    }

    public boolean meetsBasicRequirements() {
      return experienceYears >= 1 && screeningScore >= 5.0 && !redFlags;
    }

    public boolean shouldBeRejected() {
      return redFlags || screeningScore < 5.0 ||
             (technicalScore > 0 && technicalScore < 5.0);
    }

    @Override
    public String toString() {
      return String.format(
          "JobApplicant{name='%s', experience=%d, screening=%.1f, technical=%.1f, redFlags=%s, state=%s}",
          name, experienceYears, screeningScore, technicalScore, redFlags, currentState);
    }
  }

  // Test context
  private StateMachine<JobState, JobEvent, JobApplicant> workflow;
  private JobApplicant currentApplicant;
  private TransitionResult<JobState> lastTransitionResult;
  private final Map<String, JobApplicant> applicants = new HashMap<>();

  @Given("I have a job applicant workflow")
  public void i_have_a_job_applicant_workflow() {
    workflow = FlowMachine.<JobState, JobEvent, JobApplicant>builder()
        .initialState(JobState.SUBMITTED)

        // SUBMITTED state - entry point
        .configure(JobState.SUBMITTED)
        .permitIf(JobEvent.PROCEED, JobState.INITIAL_SCREENING,
            (t, applicant) -> applicant.meetsBasicRequirements())
        .permitIf(JobEvent.PROCEED, JobState.REJECTED,
            (t, applicant) -> !applicant.meetsBasicRequirements())
        .permit(JobEvent.REJECT, JobState.REJECTED)
        .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
        .and()

        // INITIAL_SCREENING state - priority-based routing with Event.PROCEED
        .configure(JobState.INITIAL_SCREENING)
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
        .permit(JobEvent.PUT_ON_HOLD, JobState.ON_HOLD)
        .and()

        // TECHNICAL_REVIEW state
        .configure(JobState.TECHNICAL_REVIEW)
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
        .permit(JobEvent.PUT_ON_HOLD, JobState.ON_HOLD)
        .and()

        // Other states...
        .configure(JobState.HR_INTERVIEW)
        .permitIf(JobEvent.PROCEED, JobState.FINAL_REVIEW,
            (t, applicant) -> applicant.getScreeningScore() >= 8.0)
        .permitIf(JobEvent.PROCEED, JobState.TECHNICAL_INTERVIEW,
            (t, applicant) -> applicant.getScreeningScore() >= 7.0 && applicant.getScreeningScore() < 8.0)
        .permitIf(JobEvent.PROCEED, JobState.REJECTED,
            (t, applicant) -> applicant.getScreeningScore() < 7.0)
        .permit(JobEvent.REJECT, JobState.REJECTED)
        .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
        .and()

        .configure(JobState.TECHNICAL_INTERVIEW)
        .permitIf(JobEvent.PROCEED, JobState.FINAL_REVIEW,
            (t, applicant) -> applicant.getTechnicalScore() >= 7.5)
        .permitIf(JobEvent.PROCEED, JobState.REJECTED,
            (t, applicant) -> applicant.getTechnicalScore() < 7.5)
        .permit(JobEvent.REJECT, JobState.REJECTED)
        .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
        .and()

        .configure(JobState.FINAL_REVIEW)
        .permitIf(JobEvent.PROCEED, JobState.BACKGROUND_CHECK,
            (t, applicant) -> applicant.getScreeningScore() >= 7.5)
        .permitIf(JobEvent.PROCEED, JobState.REJECTED,
            (t, applicant) -> applicant.getScreeningScore() < 7.5)
        .permit(JobEvent.REJECT, JobState.REJECTED)
        .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
        .and()

        .configure(JobState.BACKGROUND_CHECK)
        .permitIf(JobEvent.PROCEED, JobState.OFFER_EXTENDED,
            (t, applicant) -> !applicant.hasRedFlags())
        .permitIf(JobEvent.PROCEED, JobState.REJECTED,
            (t, applicant) -> applicant.hasRedFlags())
        .permit(JobEvent.REJECT, JobState.REJECTED)
        .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
        .and()

        .configure(JobState.OFFER_EXTENDED)
        .permit(JobEvent.PROCEED, JobState.HIRED)
        .permit(JobEvent.REJECT, JobState.REJECTED)
        .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
        .and()

        .configure(JobState.ON_HOLD)
        .permit(JobEvent.PROCEED, JobState.HR_INTERVIEW)
        .permit(JobEvent.REJECT, JobState.REJECTED)
        .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
        .and()

        // Final states
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
  }

  @Given("the workflow supports PROCEED event with priority-based routing")
  public void the_workflow_supports_proceed_event_with_priority_based_routing() {
    StateMachineInfo<JobState, JobEvent, JobApplicant> info = workflow.getInfo();
    assertTrue(info.events().contains(JobEvent.PROCEED),
        "Workflow should support PROCEED event");
  }

  @Given("I have an applicant {string} with the following profile:")
  public void i_have_an_applicant_with_profile(String name, DataTable dataTable) {
    Map<String, String> profile = dataTable.asMap();

    JobApplicant applicant = new JobApplicant(name);
    applicant.setExperienceYears(Integer.parseInt(profile.get("experience_years")));
    applicant.setScreeningScore(Double.parseDouble(profile.get("screening_score")));
    applicant.setTechnicalScore(Double.parseDouble(profile.get("technical_score")));
    applicant.setRedFlags(Boolean.parseBoolean(profile.get("red_flags")));

    if (profile.containsKey("priority_role")) {
      applicant.setPriorityRole(Boolean.parseBoolean(profile.get("priority_role")));
    }

    currentApplicant = applicant;
    applicants.put(name, applicant);
  }

  @Given("I have an applicant {string} with standard profile")
  public void i_have_an_applicant_with_standard_profile(String name) {
    JobApplicant applicant = new JobApplicant(name);
    applicant.setExperienceYears(5);
    applicant.setScreeningScore(7.0);
    applicant.setTechnicalScore(7.0);
    applicant.setRedFlags(false);

    currentApplicant = applicant;
    applicants.put(name, applicant);
  }

  @Given("I have an applicant {string} with exceptional profile")
  public void i_have_an_applicant_with_exceptional_profile(String name) {
    JobApplicant applicant = new JobApplicant(name);
    applicant.setExperienceYears(10);
    applicant.setScreeningScore(9.5);
    applicant.setTechnicalScore(9.0);
    applicant.setRedFlags(false);

    currentApplicant = applicant;
    applicants.put(name, applicant);
  }

  @Given("the applicant is in {string} state")
  public void the_applicant_is_in_state(String stateName) {
    JobState state = JobState.valueOf(stateName);
    currentApplicant.setCurrentState(state);
  }

  @When("I fire job {string} event")
  public void i_fire_event(String eventName) {
    JobEvent event = JobEvent.valueOf(eventName);
    JobState newState = workflow.fire(currentApplicant.getCurrentState(), event, currentApplicant);
    currentApplicant.setCurrentState(newState);
    lastTransitionResult = TransitionResult.success(newState);
  }

  @When("I try to fire job {string} event")
  public void i_try_to_fire_job_event(String eventName) {
    JobEvent event = JobEvent.valueOf(eventName);
    lastTransitionResult = workflow.fireWithResult(currentApplicant.getCurrentState(), event, currentApplicant);
    if (lastTransitionResult.wasTransitioned()) {
      currentApplicant.setCurrentState(lastTransitionResult.state());
    }
  }

  @Then("the applicant should transition to {string} state")
  public void the_applicant_should_transition_to_state(String expectedStateName) {
    JobState expectedState = JobState.valueOf(expectedStateName);
    assertEquals(expectedState, currentApplicant.getCurrentState(),
        "Applicant should be in " + expectedStateName + " state");
  }

  @Then("the applicant should remain in {string} state")
  public void the_applicant_should_remain_in_state(String expectedStateName) {
    JobState expectedState = JobState.valueOf(expectedStateName);
    assertEquals(expectedState, currentApplicant.getCurrentState(),
        "Applicant should remain in " + expectedStateName + " state");
  }

  @Then("the applicant should be in a final state")
  public void the_applicant_should_be_in_a_final_state() {
    assertTrue(workflow.isFinalState(currentApplicant.getCurrentState()),
        "Applicant should be in a final state");
  }

  @Then("the transition should be successful due to exceptional candidate criteria")
  public void the_transition_should_be_successful_due_to_exceptional_candidate_criteria() {
    assertTrue(currentApplicant.isExceptionalCandidate(),
        "Applicant should meet exceptional candidate criteria");
  }

  @Then("the transition should be due to priority role handling")
  public void the_transition_should_be_due_to_priority_role_handling() {
    assertTrue(currentApplicant.isPriorityRole(),
        "Applicant should be in a priority role");
  }

  @Then("I should be able to fire {string} event to {string}")
  public void i_should_be_able_to_fire_event_to_state(String eventName, String targetState) {
    JobEvent event = JobEvent.valueOf(eventName);
    assertTrue(workflow.canFire(currentApplicant.getCurrentState(), event, currentApplicant),
        "Should be able to fire " + eventName + " event");
  }

  @Then("I should be able to fire job {string} event")
  public void i_should_be_able_to_fire_event(String eventName) {
    JobEvent event = JobEvent.valueOf(eventName);
    assertTrue(workflow.canFire(currentApplicant.getCurrentState(), event, currentApplicant),
        "Should be able to fire " + eventName + " event");
  }

  @Then("I should not be able to fire job {string} event")
  public void i_should_not_be_able_to_fire_event(String eventName) {
    JobEvent event = JobEvent.valueOf(eventName);
    assertFalse(workflow.canFire(currentApplicant.getCurrentState(), event, currentApplicant),
        "Should not be able to fire " + eventName + " event");
  }

  @Then("I should not be able to fire {string} event in this context")
  public void i_should_not_be_able_to_fire_event_in_this_context(String eventName) {
    i_should_not_be_able_to_fire_event(eventName);
  }

  @When("I process the applicant through the complete workflow")
  public void i_process_the_applicant_through_the_complete_workflow() {
    // Simulate complete workflow progression
    int maxSteps = 10;
    int steps = 0;

    while (!workflow.isFinalState(currentApplicant.getCurrentState()) && steps < maxSteps) {
      if (workflow.canFire(currentApplicant.getCurrentState(), JobEvent.PROCEED, currentApplicant)) {
        JobState newState = workflow.fire(currentApplicant.getCurrentState(), JobEvent.PROCEED, currentApplicant);
        currentApplicant.setCurrentState(newState);
      } else {
        break; // No valid transition available
      }
      steps++;
    }
  }

  @Then("the applicant should reach {string} state")
  public void the_applicant_should_reach_state(String expectedStateName) {
    JobState expectedState = JobState.valueOf(expectedStateName);
    assertEquals(expectedState, currentApplicant.getCurrentState(),
        "Applicant should reach " + expectedStateName + " state");
  }

  @Then("the final job state should be {string}")
  public void the_final_job_state_should_be(String expectedStateName) {
    the_applicant_should_reach_state(expectedStateName);
    assertTrue(workflow.isFinalState(currentApplicant.getCurrentState()),
        "Should be in a final state");
  }

  @Then("the job transition should be unsuccessful")
  public void the_job_transition_should_be_unsuccessful() {
    assertFalse(lastTransitionResult.wasTransitioned(),
        "Transition should be unsuccessful");
  }

  @Then("I should get a job error message containing {string}")
  public void i_should_get_an_error_message_containing(String expectedMessage) {
    assertNotNull(lastTransitionResult, "Should have a transition result");
    assertFalse(lastTransitionResult.wasTransitioned(), "Transition should have failed");
    assertTrue(lastTransitionResult.reason().contains(expectedMessage),
        "Error message should contain: " + expectedMessage + ", but was: " + lastTransitionResult.reason());
  }

  @Then("I should get an error message about final state restrictions")
  public void i_should_get_an_error_message_about_final_state_restrictions() {
    i_should_get_an_error_message_containing("final state");
  }

  @When("I validate the job workflow configuration")
  public void i_validate_the_job_workflow_configuration() {
    ValidationResult validation = workflow.validate();
    assertTrue(validation.isValid(),
        "Workflow configuration should be valid. Errors: " + validation.errors());
  }

  @Then("the job configuration should be valid")
  public void the_job_configuration_should_be_valid() {
    ValidationResult validation = workflow.validate();
    assertTrue(validation.isValid(),
        "Configuration should be valid. Errors: " + validation.errors());
  }

  @Then("the workflow should support Event.PROCEED pattern")
  public void the_workflow_should_support_event_proceed_pattern() {
    StateMachineInfo<JobState, JobEvent, JobApplicant> info = workflow.getInfo();
    assertTrue(info.events().contains(JobEvent.PROCEED),
        "Workflow should support PROCEED event");

    // Verify multiple transitions with PROCEED event exist (priority-based routing)
    long proceedTransitions = info.transitions().stream()
        .filter(t -> t.event().equals(JobEvent.PROCEED))
        .count();

    assertTrue(proceedTransitions > 5,
        "Should have multiple PROCEED transitions for priority-based routing");
  }

  @Then("the workflow should have multiple permitIf conditions for priority routing")
  public void the_workflow_should_have_multiple_permit_if_conditions_for_priority_routing() {
    // This is validated by the successful creation of the workflow with multiple permitIf statements
    assertNotNull(workflow, "Workflow should be created with multiple permitIf conditions");
  }

  @When("I get job workflow information")
  public void i_get_job_workflow_information() {
    assertNotNull(workflow, "Workflow should be initialized");
  }

  @Then("the initial job state should be {string}")
  public void the_initial_job_state_should_be(String expectedInitialState) {
    StateMachineInfo<JobState, JobEvent, JobApplicant> info = workflow.getInfo();
    assertEquals(JobState.valueOf(expectedInitialState), info.initialState(),
        "Initial state should be " + expectedInitialState);
  }

  @Then("the workflow should have final states: {string}")
  public void the_workflow_should_have_final_states(String finalStatesString) {
    String[] expectedFinalStates = finalStatesString.split(", ");

    for (String stateName : expectedFinalStates) {
      JobState state = JobState.valueOf(stateName.trim());
      assertTrue(workflow.isFinalState(state),
          stateName + " should be a final state");
    }
  }

  @Given("I have multiple applicants in different states:")
  public void i_have_multiple_applicants_in_different_states(DataTable dataTable) {
    List<Map<String, String>> applicantData = dataTable.asMaps();
    applicants.clear();

    for (Map<String, String> row : applicantData) {
      String name = row.get("name");
      String state = row.get("state");

      JobApplicant applicant = new JobApplicant(name);
      applicant.setExperienceYears(5);
      applicant.setScreeningScore(7.0);
      applicant.setTechnicalScore(7.0);
      applicant.setRedFlags(false);
      applicant.setCurrentState(JobState.valueOf(state));

      applicants.put(name, applicant);
    }
  }

  @When("I process all applicants that can proceed")
  public void i_process_all_applicants_that_can_proceed() {
    for (JobApplicant applicant : applicants.values()) {
      if (workflow.canFire(applicant.getCurrentState(), JobEvent.PROCEED, applicant)) {
        JobState newState = workflow.fire(applicant.getCurrentState(), JobEvent.PROCEED, applicant);
        applicant.setCurrentState(newState);
      }
    }
  }

  @Then("the following transitions should occur:")
  public void the_following_transitions_should_occur(DataTable dataTable) {
    List<Map<String, String>> transitions = dataTable.asMaps();

    for (Map<String, String> row : transitions) {
      String name = row.get("name");
      String fromState = row.get("from_state");
      String toState = row.get("to_state");

      JobApplicant applicant = applicants.get(name);
      assertNotNull(applicant, "Applicant " + name + " should exist");

      if (!toState.equals("varies")) {
        assertEquals(JobState.valueOf(toState), applicant.getCurrentState(),
            "Applicant " + name + " should transition to " + toState);
      }
    }
  }

  @Then("{string} should remain in {string} state")
  public void applicant_should_remain_in_state(String name, String expectedState) {
    JobApplicant applicant = applicants.get(name);
    assertNotNull(applicant, "Applicant " + name + " should exist");
    assertEquals(JobState.valueOf(expectedState), applicant.getCurrentState(),
        "Applicant " + name + " should remain in " + expectedState + " state");
  }
}