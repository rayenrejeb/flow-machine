package com.flowmachine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.flowmachine.core.api.StateMachine;
import org.junit.jupiter.api.Test;

class AutoTransitionTest {

  enum State {START, PROCESSING, VALIDATING, COMPLETED, FAILED}

  enum Event {SUBMIT, APPROVE, REJECT}

  @Test
  void shouldExecuteAutoTransition() {
    StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
        .initialState(State.START)
        .configure(State.START)
        .permit(Event.SUBMIT, State.PROCESSING)
        .and()
        .configure(State.PROCESSING)
        .autoTransition(State.VALIDATING)
        .and()
        .configure(State.VALIDATING)
        .permit(Event.APPROVE, State.COMPLETED)
        .permit(Event.REJECT, State.FAILED)
        .and()
        .configure(State.COMPLETED)
        .asFinal()
        .and()
        .configure(State.FAILED)
        .asFinal()
        .and()
        .build();

    // Should automatically transition from PROCESSING to VALIDATING
    State result = machine.fire(State.START, Event.SUBMIT, "context");
    assertEquals(State.VALIDATING, result);

    // Should be able to continue from VALIDATING
    result = machine.fire(State.VALIDATING, Event.APPROVE, "context");
    assertEquals(State.COMPLETED, result);
  }

  @Test
  void shouldExecuteConditionalAutoTransition() {
    StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
        .initialState(State.START)
        .configure(State.START)
        .permit(Event.SUBMIT, State.PROCESSING)
        .and()
        .configure(State.PROCESSING)
        .autoTransitionIf(State.COMPLETED, (transition, context) -> "valid".equals(context))
        .autoTransitionIf(State.FAILED, (transition, context) -> "invalid".equals(context))
        .and()
        .configure(State.COMPLETED)
        .asFinal()
        .and()
        .configure(State.FAILED)
        .asFinal()
        .and()
        .build();

    // Should auto-transition to COMPLETED when context is "valid"
    State result = machine.fire(State.START, Event.SUBMIT, "valid");
    assertEquals(State.COMPLETED, result);

    // Should auto-transition to FAILED when context is "invalid"
    result = machine.fire(State.START, Event.SUBMIT, "invalid");
    assertEquals(State.FAILED, result);
  }

  @Test
  void shouldExecuteChainedAutoTransitions() {
    StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
        .initialState(State.START)
        .configure(State.START)
        .permit(Event.SUBMIT, State.PROCESSING)
        .and()
        .configure(State.PROCESSING)
        .autoTransition(State.VALIDATING)
        .and()
        .configure(State.VALIDATING)
        .autoTransition(State.COMPLETED)
        .and()
        .configure(State.COMPLETED)
        .asFinal()
        .and()
        .build();

    // Should automatically chain through PROCESSING -> VALIDATING -> COMPLETED
    State result = machine.fire(State.START, Event.SUBMIT, "context");
    assertEquals(State.COMPLETED, result);
  }

  @Test
  void shouldExecuteAutoTransitionWithActions() {
    StringBuilder log = new StringBuilder();

    StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
        .initialState(State.START)
        .configure(State.START)
        .permit(Event.SUBMIT, State.PROCESSING)
        .and()
        .configure(State.PROCESSING)
        .onEntry((transition, context) -> log.append("Entered PROCESSING; "))
        .onExit((transition, context) -> log.append("Exited PROCESSING; "))
        .autoTransition(State.COMPLETED)
        .and()
        .configure(State.COMPLETED)
        .onEntry((transition, context) -> log.append("Entered COMPLETED; "))
        .asFinal()
        .and()
        .build();

    machine.fire(State.START, Event.SUBMIT, "context");

    String expected = "Entered PROCESSING; Exited PROCESSING; Entered COMPLETED; ";
    assertEquals(expected, log.toString());
  }

  @Test
  void shouldNotAutoTransitionFromFinalStates() {
    StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
        .initialState(State.START)
        .configure(State.START)
        .permit(Event.SUBMIT, State.COMPLETED)
        .and()
        .configure(State.COMPLETED)
        .asFinal()
        .autoTransition(State.FAILED) // This should not execute
        .and()
        .configure(State.FAILED)
        .asFinal()
        .and()
        .build();

    State result = machine.fire(State.START, Event.SUBMIT, "context");
    assertEquals(State.COMPLETED, result); // Should stay at COMPLETED, not auto-transition to FAILED
  }
}