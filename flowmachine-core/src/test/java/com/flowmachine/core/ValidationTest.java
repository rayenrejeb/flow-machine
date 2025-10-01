package com.flowmachine.core;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.exception.StateMachineException;
import com.flowmachine.core.model.ValidationResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {

    enum State { A, B, C, D, UNREACHABLE }
    enum Event { GO_TO_B, GO_TO_C, GO_TO_D, INVALID }

    @Test
    void shouldDetectMissingInitialState() {
        assertThrows(StateMachineException.class, () -> {
            FlowMachine.<State, Event, String>builder()
                .configure(State.A)
                    .permit(Event.GO_TO_B, State.B)
                .and()
                .build();
        }, "Should throw exception when no initial state is specified");
    }

    @Test
    void shouldDetectUnconfiguredInitialState() {
        StateMachine<State, Event, String> machine = FlowMachine
            .<State, Event, String>builder()
            .initialState(State.A)
            .configure(State.B)
                .permit(Event.GO_TO_C, State.C)
            .and()
            .build();

        ValidationResult result = machine.validate();
        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("Initial state 'A' is not configured")));
    }

    @Test
    void shouldDetectUndefinedTargetStates() {
        StateMachine<State, Event, String> machine = FlowMachine
            .<State, Event, String>builder()
            .initialState(State.A)
            .configure(State.A)
                .permit(Event.GO_TO_B, State.B)
            .and()
            .build();

        ValidationResult result = machine.validate();
        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e ->
            e.contains("transition to undefined target state 'B'")));
    }

    @Test
    void shouldDetectUnreachableStates() {
        StateMachine<State, Event, String> machine = FlowMachine
            .<State, Event, String>builder()
            .initialState(State.A)
            .configure(State.A)
                .permit(Event.GO_TO_B, State.B)
            .and()
            .configure(State.B)
            .and()
            .configure(State.UNREACHABLE)
            .and()
            .build();

        ValidationResult result = machine.validate();
        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e ->
            e.contains("State 'UNREACHABLE' is not reachable")));
    }

    @Test
    void shouldDetectDuplicateEvents() {
        StateMachine<State, Event, String> machine = FlowMachine
            .<State, Event, String>builder()
            .initialState(State.A)
            .configure(State.A)
                .permit(Event.GO_TO_B, State.B)
                .permit(Event.GO_TO_B, State.C)
            .and()
            .configure(State.B)
            .and()
            .configure(State.C)
            .and()
            .build();

        ValidationResult result = machine.validate();
        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e ->
            e.contains("multiple unconditional transitions for event 'GO_TO_B'")));
    }

    @Test
    void shouldAllowMultipleConditionalEvents() {
        StateMachine<State, Event, String> machine = FlowMachine
            .<State, Event, String>builder()
            .initialState(State.A)
            .configure(State.A)
                .permitIf(Event.GO_TO_B, State.B, (t, ctx) -> ctx.equals("toB"))
                .permitIf(Event.GO_TO_B, State.C, (t, ctx) -> ctx.equals("toC"))
            .and()
            .configure(State.B)
            .and()
            .configure(State.C)
            .and()
            .build();

        ValidationResult result = machine.validate();
        assertTrue(result.isValid(), "Multiple conditional transitions should be allowed: " + result.errors());
    }

    @Test
    void shouldValidateComplexStateMachine() {
        StateMachine<State, Event, String> machine = FlowMachine
            .<State, Event, String>builder()
            .initialState(State.A)
            .configure(State.A)
                .permit(Event.GO_TO_B, State.B)
                .permit(Event.GO_TO_C, State.C)
            .and()
            .configure(State.B)
                .permit(Event.GO_TO_C, State.C)
                .permit(Event.GO_TO_D, State.D)
            .and()
            .configure(State.C)
                .permit(Event.GO_TO_D, State.D)
            .and()
            .configure(State.D)
            .and()
            .build();

        ValidationResult result = machine.validate();
        assertTrue(result.isValid(), "Complex state machine should be valid: " + result.errors());
    }
}