package com.flowmachine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.model.TransitionResult;
import com.flowmachine.core.model.ValidationResult;
import org.junit.jupiter.api.Test;

class FinalStatesTest {

  enum State {START, PROCESSING, COMPLETED, FAILED}

  enum Event {PROCESS, COMPLETE, FAIL, RETRY}

  @Test
  void shouldMarkStateAsFinal() {
    StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
        .initialState(State.START)
        .configure(State.START)
        .permit(Event.PROCESS, State.PROCESSING)
        .and()
        .configure(State.PROCESSING)
        .permit(Event.COMPLETE, State.COMPLETED)
        .permit(Event.FAIL, State.FAILED)
        .and()
        .configure(State.COMPLETED)
        .asFinal()
        .and()
        .configure(State.FAILED)
        .asFinal()
        .and()
        .build();

    assertTrue(machine.isFinalState(State.COMPLETED));
    assertTrue(machine.isFinalState(State.FAILED));
    assertFalse(machine.isFinalState(State.START));
    assertFalse(machine.isFinalState(State.PROCESSING));
  }

  @Test
  void shouldPreventTransitionsFromFinalState() {
    StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
        .initialState(State.START)
        .configure(State.START)
        .permit(Event.PROCESS, State.PROCESSING)
        .and()
        .configure(State.PROCESSING)
        .permit(Event.COMPLETE, State.COMPLETED)
        .and()
        .configure(State.COMPLETED)
        .asFinal()
        .and()
        .build();

    // Transition to final state should work
    State state = machine.fire(State.START, Event.PROCESS, "context");
    assertEquals(State.PROCESSING, state);

    state = machine.fire(State.PROCESSING, Event.COMPLETE, "context");
    assertEquals(State.COMPLETED, state);

    // Cannot fire events from final state
    assertFalse(machine.canFire(State.COMPLETED, Event.RETRY, "context"));

    TransitionResult<State> result = machine.fireWithResult(State.COMPLETED, Event.RETRY, "context");
    assertFalse(result.wasTransitioned());
    assertEquals(State.COMPLETED, result.state());
    assertTrue(result.reason().contains("Cannot transition from final state"));
  }

  @Test
  void shouldValidateFinalStatesWithTransitions() {
    StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
        .initialState(State.START)
        .configure(State.START)
        .permit(Event.PROCESS, State.COMPLETED)
        .and()
        .configure(State.COMPLETED)
        .asFinal()
        .permit(Event.RETRY, State.START) // This should fail validation
        .and()
        .build();

    ValidationResult result = machine.validate();
    assertFalse(result.isValid());
    assertTrue(result.errors().stream().anyMatch(error ->
        error.contains("Final state") && error.contains("should not have any transitions")));
  }

  @Test
  void shouldAllowEntryAndExitActionsOnFinalStates() {
    StringBuilder log = new StringBuilder();

    StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
        .initialState(State.START)
        .configure(State.START)
        .permit(Event.PROCESS, State.COMPLETED)
        .and()
        .configure(State.COMPLETED)
        .asFinal()
        .onEntry((transition, context) -> log.append("Entered final state"))
        .onExit((transition, context) -> log.append("Exited final state"))
        .and()
        .build();

    machine.fire(State.START, Event.PROCESS, "context");

    assertEquals("Entered final state", log.toString());
  }
}