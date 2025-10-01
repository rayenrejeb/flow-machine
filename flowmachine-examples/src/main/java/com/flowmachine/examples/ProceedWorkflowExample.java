package com.flowmachine.examples;

import com.flowmachine.core.FlowMachine;
import com.flowmachine.core.api.StateMachine;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates how to use Event.PROCEED for "go to next status" functionality in a realistic job applicant workflow
 * with priority-based routing.
 * <p>
 * This example shows how a single PROCEED event can route to different next states based on the applicant's context and
 * business rules.
 */
public class ProceedWorkflowExample {

  public enum ApplicantState {
    SUBMITTED,
    INITIAL_SCREENING,
    TECHNICAL_REVIEW,
    HR_INTERVIEW,
    TECHNICAL_INTERVIEW,
    FINAL_REVIEW,
    BACKGROUND_CHECK,
    OFFER_EXTENDED,
    HIRED,
    REJECTED,
    WITHDRAWN,
    ON_HOLD
  }

  public enum Event {
    PROCEED,        // "Go to next status" - the main event for smart routing
    REJECT,         // Explicit rejection
    WITHDRAW,       // Applicant withdraws
    PUT_ON_HOLD,    // Put application on hold
    RESET           // Reset to earlier stage
  }

  public static class JobApplicant {

    private String name;
    private int experienceYears;
    private double screeningScore;
    private double technicalScore;
    private boolean hasRedFlags;
    private boolean isInternalReferral;
    private boolean isPriorityRole;
    private List<String> interviewNotes = new ArrayList<>();

    public JobApplicant(String name) {
      this.name = name;
    }

    // Builder-style setters for easy setup
    public JobApplicant withExperience(int years) {
      this.experienceYears = years;
      return this;
    }

    public JobApplicant withScreeningScore(double score) {
      this.screeningScore = score;
      return this;
    }

    public JobApplicant withTechnicalScore(double score) {
      this.technicalScore = score;
      return this;
    }

    public JobApplicant withRedFlags(boolean hasRedFlags) {
      this.hasRedFlags = hasRedFlags;
      return this;
    }

    public JobApplicant asInternalReferral() {
      this.isInternalReferral = true;
      return this;
    }

    public JobApplicant forPriorityRole() {
      this.isPriorityRole = true;
      return this;
    }

    public JobApplicant addNote(String note) {
      this.interviewNotes.add(note);
      return this;
    }

    // Getters
    public String getName() {
      return name;
    }

    public int getExperienceYears() {
      return experienceYears;
    }

    public double getScreeningScore() {
      return screeningScore;
    }

    public double getTechnicalScore() {
      return technicalScore;
    }

    public boolean hasRedFlags() {
      return hasRedFlags;
    }

    public boolean isInternalReferral() {
      return isInternalReferral;
    }

    public boolean isPriorityRole() {
      return isPriorityRole;
    }

    public List<String> getInterviewNotes() {
      return new ArrayList<>(interviewNotes);
    }

    // Business logic for routing decisions
    public boolean isExceptionalCandidate() {
      return screeningScore >= 9.0 && technicalScore >= 8.5 && experienceYears >= 8;
    }

    public boolean isStrongCandidate() {
      return screeningScore >= 7.0 && !hasRedFlags && experienceYears >= 3;
    }

    public boolean isReadyForOffer() {
      return technicalScore >= 7.0 && screeningScore >= 7.0 && !hasRedFlags;
    }

    @Override
    public String toString() {
      return String.format("%s (exp:%dy, screening:%.1f, technical:%.1f, redFlags:%s)",
          name, experienceYears, screeningScore, technicalScore, hasRedFlags);
    }
  }

  /**
   * Creates a comprehensive applicant workflow that demonstrates priority-based routing using a single PROCEED event
   * for "go to next status" functionality.
   */
  public static StateMachine<ApplicantState, Event, JobApplicant> createProceedWorkflow() {
    return FlowMachine.<ApplicantState, Event, JobApplicant>builder()
        .initialState(ApplicantState.SUBMITTED)

        .configure(ApplicantState.SUBMITTED)
        .permit(Event.PROCEED, ApplicantState.INITIAL_SCREENING)
        .permit(Event.REJECT, ApplicantState.REJECTED)
        .permit(Event.WITHDRAW, ApplicantState.WITHDRAWN)
        .onEntry((t, applicant) ->
            System.out.println("📝 Application submitted for: " + applicant.getName()))
        .and()

        .configure(ApplicantState.INITIAL_SCREENING)
        // Priority 1: Immediate rejection for red flags or very low scores
        .permitIf(Event.PROCEED, ApplicantState.REJECTED,
            (t, applicant) -> applicant.hasRedFlags() || applicant.getScreeningScore() < 4.0)

        // Priority 2: Fast track exceptional candidates directly to final review
        .permitIf(Event.PROCEED, ApplicantState.FINAL_REVIEW,
            (t, applicant) -> applicant.isExceptionalCandidate() && applicant.isInternalReferral())

        // Priority 3: Strong candidates go to technical review
        .permitIf(Event.PROCEED, ApplicantState.TECHNICAL_REVIEW,
            (t, applicant) -> applicant.isStrongCandidate() && applicant.getScreeningScore() >= 7.5)

        // Priority 4: Decent candidates go to technical review
        .permitIf(Event.PROCEED, ApplicantState.TECHNICAL_REVIEW,
            (t, applicant) -> applicant.getScreeningScore() >= 7.0 && !applicant.hasRedFlags())

        // Priority 5: Borderline candidates for priority roles get HR consideration
        .permitIf(Event.PROCEED, ApplicantState.HR_INTERVIEW,
            (t, applicant) -> applicant.isPriorityRole() && applicant.getScreeningScore() >= 6.0)

        // Priority 6: Put weak candidates on hold instead of rejecting immediately
        .permit(Event.PROCEED, ApplicantState.ON_HOLD)

        .permit(Event.REJECT, ApplicantState.REJECTED)
        .permit(Event.WITHDRAW, ApplicantState.WITHDRAWN)
        .permit(Event.PUT_ON_HOLD, ApplicantState.ON_HOLD)
        .onEntry((t, applicant) ->
            System.out.println("🔍 Initial screening for: " + applicant.getName()))
        .and()

        .configure(ApplicantState.TECHNICAL_REVIEW)
        // Priority 1: Poor technical performance -> rejection
        .permitIf(Event.PROCEED, ApplicantState.REJECTED,
            (t, applicant) -> applicant.getTechnicalScore() < 5.0)

        // Priority 2: Exceptional technical performance -> skip to final review
        .permitIf(Event.PROCEED, ApplicantState.FINAL_REVIEW,
            (t, applicant) -> applicant.getTechnicalScore() >= 9.0 && applicant.getExperienceYears() >= 5)

        // Priority 3: Good performance -> HR interview
        .permitIf(Event.PROCEED, ApplicantState.HR_INTERVIEW,
            (t, applicant) -> applicant.getTechnicalScore() >= 7.0)

        // Priority 4: Borderline -> technical interview for deeper assessment
        .permit(Event.PROCEED, ApplicantState.TECHNICAL_INTERVIEW)

        .permit(Event.REJECT, ApplicantState.REJECTED)
        .permit(Event.WITHDRAW, ApplicantState.WITHDRAWN)
        .permit(Event.PUT_ON_HOLD, ApplicantState.ON_HOLD)
        .onEntry((t, applicant) ->
            System.out.println("⚙️ Technical review for: " + applicant.getName()))
        .and()

        .configure(ApplicantState.HR_INTERVIEW)
        // Priority 1: Red flags discovered -> rejection
        .permitIf(Event.PROCEED, ApplicantState.REJECTED,
            (t, applicant) -> applicant.hasRedFlags())

        // Priority 2: Excellent candidates with high scores -> skip technical interview
        .permitIf(Event.PROCEED, ApplicantState.FINAL_REVIEW,
            (t, applicant) -> applicant.getTechnicalScore() >= 8.0 && applicant.getScreeningScore() >= 8.0)

        // Priority 3: Need technical validation -> technical interview
        .permitIf(Event.PROCEED, ApplicantState.TECHNICAL_INTERVIEW,
            (t, applicant) -> applicant.getTechnicalScore() == 0 ||
                              (applicant.getTechnicalScore() > 0 && applicant.getTechnicalScore() < 8.0))

        // Priority 4: Default -> final review
        .permit(Event.PROCEED, ApplicantState.FINAL_REVIEW)

        .permit(Event.REJECT, ApplicantState.REJECTED)
        .permit(Event.WITHDRAW, ApplicantState.WITHDRAWN)
        .onEntry((t, applicant) ->
            System.out.println("👥 HR interview for: " + applicant.getName()))
        .and()

        .configure(ApplicantState.TECHNICAL_INTERVIEW)
        // Priority 1: Failed technical -> rejection
        .permitIf(Event.PROCEED, ApplicantState.REJECTED,
            (t, applicant) -> applicant.getTechnicalScore() < 6.0)

        // Priority 2: Passed technical -> final review
        .permit(Event.PROCEED, ApplicantState.FINAL_REVIEW)

        .permit(Event.REJECT, ApplicantState.REJECTED)
        .permit(Event.WITHDRAW, ApplicantState.WITHDRAWN)
        .onEntry((t, applicant) ->
            System.out.println("💻 Technical interview for: " + applicant.getName()))
        .and()

        .configure(ApplicantState.FINAL_REVIEW)
        // Priority 1: Any red flags -> rejection
        .permitIf(Event.PROCEED, ApplicantState.REJECTED,
            (t, applicant) -> applicant.hasRedFlags())

        // Priority 2: Ready for offer -> background check
        .permitIf(Event.PROCEED, ApplicantState.BACKGROUND_CHECK,
            (t, applicant) -> applicant.isReadyForOffer())

        // Priority 3: Needs more review -> put on hold
        .permit(Event.PROCEED, ApplicantState.ON_HOLD)

        .permit(Event.REJECT, ApplicantState.REJECTED)
        .permit(Event.WITHDRAW, ApplicantState.WITHDRAWN)
        .onEntry((t, applicant) ->
            System.out.println("📋 Final review for: " + applicant.getName()))
        .and()

        .configure(ApplicantState.BACKGROUND_CHECK)
        // Priority 1: Background issues -> rejection
        .permitIf(Event.PROCEED, ApplicantState.REJECTED,
            (t, applicant) -> applicant.hasRedFlags())

        // Priority 2: Clean background -> offer
        .permit(Event.PROCEED, ApplicantState.OFFER_EXTENDED)

        .permit(Event.REJECT, ApplicantState.REJECTED)
        .permit(Event.WITHDRAW, ApplicantState.WITHDRAWN)
        .onEntry((t, applicant) ->
            System.out.println("🔎 Background check for: " + applicant.getName()))
        .and()

        .configure(ApplicantState.OFFER_EXTENDED)
        .permit(Event.PROCEED, ApplicantState.HIRED)
        .permit(Event.REJECT, ApplicantState.REJECTED) // Offer declined
        .permit(Event.WITHDRAW, ApplicantState.WITHDRAWN)
        .onEntry((t, applicant) ->
            System.out.println("🎉 Offer extended to: " + applicant.getName()))
        .and()

        .configure(ApplicantState.ON_HOLD)
        .permit(Event.PROCEED, ApplicantState.INITIAL_SCREENING) // Re-evaluate
        .permit(Event.REJECT, ApplicantState.REJECTED)
        .permit(Event.WITHDRAW, ApplicantState.WITHDRAWN)
        .onEntry((t, applicant) ->
            System.out.println("⏸️ Application on hold for: " + applicant.getName()))
        .and()

        .configure(ApplicantState.HIRED)
        .asFinal()
        .onEntry((t, applicant) ->
            System.out.println("✅ HIRED: " + applicant.getName()))
        .and()

        .configure(ApplicantState.REJECTED)
        .asFinal()
        .onEntry((t, applicant) ->
            System.out.println("❌ REJECTED: " + applicant.getName()))
        .and()

        .configure(ApplicantState.WITHDRAWN)
        .asFinal()
        .onEntry((t, applicant) ->
            System.out.println("🚪 WITHDRAWN: " + applicant.getName()))
        .and()

        .build();
  }

  /**
   * Simple helper method to advance an applicant through the workflow. This demonstrates the "go to next status"
   * functionality.
   */
  public static ApplicantState proceedToNext(StateMachine<ApplicantState, Event, JobApplicant> workflow,
      ApplicantState currentState,
      JobApplicant applicant) {
    return workflow.fire(currentState, Event.PROCEED, applicant);
  }

  /**
   * Main method demonstrating various workflow scenarios
   */
  public static void main(String[] args) {
    StateMachine<ApplicantState, Event, JobApplicant> workflow = createProceedWorkflow();

    System.out.println("=== FlowMachine PROCEED Event Example ===");
    System.out.println("Demonstrating 'go to next status' with Event.PROCEED");
    System.out.println("Multiple permitIf() statements create priority-based routing");
    System.out.println();

    // Scenario 1: Exceptional candidate fast track
    System.out.println("--- Scenario 1: Exceptional Candidate Fast Track ---");
    JobApplicant exceptional = new JobApplicant("Alice Senior")
        .withExperience(10)
        .withScreeningScore(9.5)
        .withTechnicalScore(9.0)
        .asInternalReferral();

    System.out.println("Candidate: " + exceptional);
    ApplicantState state = ApplicantState.SUBMITTED;

    proceedToNextUntilFinalState(workflow, state, exceptional);

    // Scenario 2: Standard candidate workflow
    System.out.println("--- Scenario 2: Standard Candidate Workflow ---");
    JobApplicant standard = new JobApplicant("Bob Engineer")
        .withExperience(5)
        .withScreeningScore(7.8)
        .withTechnicalScore(7.2);

    System.out.println("Candidate: " + standard);
    state = ApplicantState.SUBMITTED;

    proceedToNextUntilFinalState(workflow, state, standard);

    // Scenario 3: Borderline candidate requiring technical interview
    System.out.println("--- Scenario 3: Borderline Candidate ---");
    JobApplicant borderline = new JobApplicant("Charlie Junior")
        .withExperience(2)
        .withScreeningScore(7.0)
        .withTechnicalScore(0); // Will be set during process

    System.out.println("Candidate: " + borderline);
    state = ApplicantState.SUBMITTED;

    int step = 0;
    while (!workflow.isFinalState(state) && step < 8) {
      ApplicantState nextState = proceedToNext(workflow, state, borderline);
      System.out.println("  " + state + " --[PROCEED]--> " + nextState);

      // Simulate getting technical score after technical review
      if (state == ApplicantState.TECHNICAL_REVIEW && borderline.getTechnicalScore() == 0) {
        borderline.withTechnicalScore(6.5); // Borderline score
        System.out.println("    💡 Technical score assigned: 6.5");
      }

      // Simulate improvement after technical interview
      if (state == ApplicantState.TECHNICAL_INTERVIEW) {
        borderline.withTechnicalScore(7.5); // Improved score
        System.out.println("    💡 Technical score improved: 7.5");
      }

      state = nextState;
      step++;
    }
    System.out.println("📍 Final state: " + state);
    System.out.println();

    // Scenario 4: Priority role consideration
    System.out.println("--- Scenario 4: Priority Role Special Path ---");
    JobApplicant priority = new JobApplicant("Eve Urgent")
        .withExperience(2)
        .withScreeningScore(6.5)
        .withTechnicalScore(0)
        .forPriorityRole();

    System.out.println("Candidate: " + priority + " [Priority Role]");
    state = ApplicantState.SUBMITTED;

    step = 0;
    while (!workflow.isFinalState(state) && step < 6) {
      ApplicantState nextState = proceedToNext(workflow, state, priority);
      System.out.println("  " + state + " --[PROCEED]--> " + nextState);

      // Simulate technical interview result
      if (state == ApplicantState.TECHNICAL_INTERVIEW) {
        priority.withTechnicalScore(7.0);
        System.out.println("    💡 Technical score assigned: 7.0");
      }

      state = nextState;
      step++;
    }
    System.out.println("📍 Final state: " + state);
    System.out.println();

    System.out.println("=== Key Benefits of Event.PROCEED Pattern ===");
    System.out.println("✅ Simple API: workflow.fire(currentState, Event.PROCEED, context)");
    System.out.println("✅ Smart Routing: Same event routes to different states based on business logic");
    System.out.println("✅ Priority Handling: Multiple permitIf() statements create decision trees");
    System.out.println("✅ Context-Driven: Routing decisions based on data and business rules");
    System.out.println("✅ Maintainable: Business logic centralized in state machine configuration");
    System.out.println();
    System.out.println("💡 You can call proceedToNext() repeatedly to advance through the workflow!");
  }

  private static void proceedToNextUntilFinalState(
      StateMachine<ApplicantState, Event, JobApplicant> workflow,
      ApplicantState state,
      JobApplicant standard) {
    while (!workflow.isFinalState(state)) {
      ApplicantState nextState = proceedToNext(workflow, state, standard);
      System.out.println("  " + state + " --[PROCEED]--> " + nextState);
      state = nextState;
    }
    System.out.println("📍 Final state: " + state);
    System.out.println();
  }
}