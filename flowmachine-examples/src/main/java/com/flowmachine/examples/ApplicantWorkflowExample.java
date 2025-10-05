package com.flowmachine.examples;

import com.flowmachine.core.FlowMachine;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.diagram.MermaidDiagramGenerator;
import com.flowmachine.core.diagram.DiagramGenerator;
import com.flowmachine.core.model.StateMachineInfo;
import com.flowmachine.core.model.TransitionInfo;
import java.time.LocalDateTime;

public class ApplicantWorkflowExample {

    public enum ApplicantState {
        CREATED,
        PENDING_CUSTOMER_CREATION,
        CUSTOMER_CREATED,
        IDENTIFICATION_PROCESS_STARTED,
        IDENTIFICATION_PROCESS_COMPLETED,
        APPROVED,
        REJECTED,
        ARCHIVED
    }

    public enum ApplicantEvent {
        START_CUSTOMER_CREATION,
        CUSTOMER_CREATION_COMPLETED,
        START_IDENTIFICATION,
        IDENTIFICATION_COMPLETED,
        APPROVE,
        REJECT,
        ARCHIVE
    }

    public static class Applicant {

        private final String id;
        private final String email;
        private final LocalDateTime createdAt;
        private boolean customerCreated;
        private boolean identificationCompleted;
        private boolean eligibleForApproval;

        public Applicant(String id, String email) {
            this.id = id;
            this.email = email;
            this.createdAt = LocalDateTime.now();
        }

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public boolean isCustomerCreated() {
            return customerCreated;
        }

        public boolean isIdentificationCompleted() {
            return identificationCompleted;
        }

        public boolean isEligibleForApproval() {
            return eligibleForApproval;
        }

        public void setCustomerCreated(boolean customerCreated) {
            this.customerCreated = customerCreated;
        }

        public void setIdentificationCompleted(boolean identificationCompleted) {
            this.identificationCompleted = identificationCompleted;
        }

        public void setEligibleForApproval(boolean eligibleForApproval) {
            this.eligibleForApproval = eligibleForApproval;
        }
    }

    public static StateMachine<ApplicantState, ApplicantEvent, Applicant> createApplicantStateMachine() {
        return FlowMachine.<ApplicantState, ApplicantEvent, Applicant>builder()
            .initialState(ApplicantState.CREATED)

            .configure(ApplicantState.CREATED)
                .permit(ApplicantEvent.START_CUSTOMER_CREATION, ApplicantState.PENDING_CUSTOMER_CREATION)
                .onEntry((t, applicant) ->
                    System.out.println("Applicant " + applicant.getId() + " created at " + applicant.getCreatedAt()))
            .and()

            .configure(ApplicantState.PENDING_CUSTOMER_CREATION)
                .permit(ApplicantEvent.CUSTOMER_CREATION_COMPLETED, ApplicantState.CUSTOMER_CREATED)
                .onEntry((t, applicant) ->
                    System.out.println("Starting customer creation for " + applicant.getId()))
            .and()

            .configure(ApplicantState.CUSTOMER_CREATED)
                .permit(ApplicantEvent.START_IDENTIFICATION, ApplicantState.IDENTIFICATION_PROCESS_STARTED)
                .permitIf(ApplicantEvent.APPROVE, ApplicantState.APPROVED,
                    (t, applicant) -> applicant.isEligibleForApproval())
                .permit(ApplicantEvent.REJECT, ApplicantState.REJECTED)
                .onEntry((t, applicant) -> {
                    applicant.setCustomerCreated(true);
                    System.out.println("Customer created for applicant " + applicant.getId());
                })
            .and()

            .configure(ApplicantState.IDENTIFICATION_PROCESS_STARTED)
                .permit(ApplicantEvent.IDENTIFICATION_COMPLETED, ApplicantState.IDENTIFICATION_PROCESS_COMPLETED)
                .permit(ApplicantEvent.REJECT, ApplicantState.REJECTED)
                .onEntry((t, applicant) ->
                    System.out.println("Starting identification process for " + applicant.getId()))
            .and()

            .configure(ApplicantState.IDENTIFICATION_PROCESS_COMPLETED)
                .permitIf(ApplicantEvent.APPROVE, ApplicantState.APPROVED,
                    (t, applicant) -> applicant.isEligibleForApproval())
                .permit(ApplicantEvent.REJECT, ApplicantState.REJECTED)
                .onEntry((t, applicant) -> {
                    applicant.setIdentificationCompleted(true);
                    System.out.println("Identification completed for applicant " + applicant.getId());
                })
            .and()

            .configure(ApplicantState.APPROVED)
                .permit(ApplicantEvent.ARCHIVE, ApplicantState.ARCHIVED)
                .onEntry((t, applicant) ->
                    System.out.println("Applicant " + applicant.getId() + " approved!"))
            .and()

            .configure(ApplicantState.REJECTED)
                .permit(ApplicantEvent.ARCHIVE, ApplicantState.ARCHIVED)
                .onEntry((t, applicant) ->
                    System.out.println("Applicant " + applicant.getId() + " rejected"))
            .and()

            .configure(ApplicantState.ARCHIVED)
                .onEntry((t, applicant) ->
                    System.out.println("Applicant " + applicant.getId() + " archived"))
            .and()

            .onError((state, event, applicant, error) -> {
                System.err.println("Error processing applicant " + applicant.getId() +
                    " in state " + state + " with event " + event + ": " + error.getMessage());
                return state;
            })

            .build();
    }

    public static void main(String[] args) {
        StateMachine<ApplicantState, ApplicantEvent, Applicant> stateMachine = createApplicantStateMachine();

        Applicant applicant = new Applicant("APPL-001", "john.doe@example.com");

        System.out.println("=== Applicant Workflow Demo ===");

        ApplicantState currentState = ApplicantState.CREATED;

        currentState = stateMachine.fire(currentState, ApplicantEvent.START_CUSTOMER_CREATION, applicant);
        System.out.println("Current state: " + currentState);

        currentState = stateMachine.fire(currentState, ApplicantEvent.CUSTOMER_CREATION_COMPLETED, applicant);
        System.out.println("Current state: " + currentState);

        currentState = stateMachine.fire(currentState, ApplicantEvent.START_IDENTIFICATION, applicant);
        System.out.println("Current state: " + currentState);

        currentState = stateMachine.fire(currentState, ApplicantEvent.IDENTIFICATION_COMPLETED, applicant);
        System.out.println("Current state: " + currentState);

        applicant.setEligibleForApproval(true);

        boolean canApprove = stateMachine.canFire(currentState, ApplicantEvent.APPROVE, applicant);
        System.out.println("Can approve? " + canApprove);

        if (canApprove) {
            currentState = stateMachine.fire(currentState, ApplicantEvent.APPROVE, applicant);
            System.out.println("Current state: " + currentState);
        }

        System.out.println("\n=== State Machine Info ===");
        var info = stateMachine.getInfo();
        System.out.println("Initial state: " + info.initialState());
        System.out.println("States: " + info.states());
        System.out.println("Events: " + info.events());
        System.out.println("Transitions: " + info.transitions().size());

        System.out.println("\n=== Validation ===");
        var validation = stateMachine.validate();
        System.out.println("Valid: " + validation.isValid());
        if (!validation.isValid()) {
            System.out.println("Errors: " + validation.errors());
        }

        DiagramGenerator<ApplicantState, ApplicantEvent, Applicant> generator = new MermaidDiagramGenerator<>();
        var diagram = generator.generateDetailed(stateMachine, "Applicant workflow");
        System.out.println(diagram);
        System.out.println("----------1111------");
        System.out.println(generatePlantUMLDiagram(stateMachine));
    }


    private static String generatePlantUMLDiagram(StateMachine<ApplicantState, ApplicantEvent, Applicant> workflow) {
        StringBuilder plantuml = new StringBuilder();
        plantuml.append("@startuml\n");
        plantuml.append("!theme plain\n");
        plantuml.append("skinparam state {\n");
        plantuml.append("  BackgroundColor<<Final>> LightGreen\n");
        plantuml.append("  BackgroundColor<<Initial>> LightBlue\n");
        plantuml.append("}\n\n");

        StateMachineInfo<ApplicantState, ApplicantEvent, Applicant> info = workflow.getInfo();

        // Mark initial and final states
        plantuml.append("state ").append(info.initialState()).append(" <<Initial>>\n");
        for (ApplicantState state : info.states()) {
            if (workflow.isFinalState(state)) {
                plantuml.append("state ").append(state).append(" <<Final>>\n");
            }
        }

        // Add transitions
        for (TransitionInfo<ApplicantState, ApplicantEvent> transition : info.transitions()) {
            plantuml.append(transition.fromState())
                .append(" --> ").append(transition.toState())
                .append(" : ").append(transition.event()).append("\n");
        }

        plantuml.append("@enduml\n");
        return plantuml.toString();
    }
}