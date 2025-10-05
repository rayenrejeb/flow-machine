package com.flowmachine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.flowmachine.core.api.StateMachine;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActionsTest {

  enum State {A, B, C}

  enum Event {GO_TO_B, GO_TO_C, REENTER, INTERNAL}

  static class ActionTracker {

    List<String> actions = new ArrayList<>();

    void record(String action) {
      actions.add(action);
    }

    void clear() {
      actions.clear();
    }
  }

  @Test
  void shouldExecuteEntryAndExitActions() {
    ActionTracker tracker = new ActionTracker();

    StateMachine<State, Event, ActionTracker> machine = FlowMachine
        .<State, Event, ActionTracker>builder()
        .initialState(State.A)
        .configure(State.A)
        .permit(Event.GO_TO_B, State.B)
        .onExit((t, ctx) -> ctx.record("A_EXIT"))
        .and()
        .configure(State.B)
        .onEntry((t, ctx) -> ctx.record("B_ENTRY"))
        .onExit((t, ctx) -> ctx.record("B_EXIT"))
        .and()
        .build();

    machine.fire(State.A, Event.GO_TO_B, tracker);

    assertEquals(2, tracker.actions.size());
    assertEquals("A_EXIT", tracker.actions.get(0));
    assertEquals("B_ENTRY", tracker.actions.get(1));
  }

  @Test
  void shouldExecuteGlobalActions() {
    ActionTracker tracker = new ActionTracker();

    StateMachine<State, Event, ActionTracker> machine = FlowMachine
        .<State, Event, ActionTracker>builder()
        .initialState(State.A)
        .configure(State.A)
        .permit(Event.GO_TO_B, State.B)
        .and()
        .configure(State.B)
        .and()
        .onAnyEntry((t, ctx) -> ctx.record("GLOBAL_ENTRY"))
        .onAnyExit((t, ctx) -> ctx.record("GLOBAL_EXIT"))
        .onAnyTransition((t, ctx) -> ctx.record("GLOBAL_TRANSITION"))
        .build();

    machine.fire(State.A, Event.GO_TO_B, tracker);

    assertEquals(3, tracker.actions.size());
    assertTrue("GLOBAL_EXIT".equals(tracker.actions.get(0)));
    assertTrue("GLOBAL_TRANSITION".equals(tracker.actions.get(1)));
    assertTrue("GLOBAL_ENTRY".equals(tracker.actions.get(2)));
  }

  @Test
  void shouldNotExecuteEntryExitActionsOnReentry() {
    ActionTracker tracker = new ActionTracker();

    StateMachine<State, Event, ActionTracker> machine = FlowMachine
        .<State, Event, ActionTracker>builder()
        .initialState(State.A)
        .configure(State.A)
        .permitReentry(Event.REENTER)
        .onEntry((t, ctx) -> ctx.record("A_ENTRY"))
        .onExit((t, ctx) -> ctx.record("A_EXIT"))
        .and()
        .onAnyTransition((t, ctx) -> ctx.record("GLOBAL_TRANSITION"))
        .build();

    machine.fire(State.A, Event.REENTER, tracker);

    assertEquals(1, tracker.actions.size());
    assertEquals("GLOBAL_TRANSITION", tracker.actions.get(0));
  }

  @Test
  void shouldExecuteInternalActions() {
    ActionTracker tracker = new ActionTracker();

    StateMachine<State, Event, ActionTracker> machine = FlowMachine
        .<State, Event, ActionTracker>builder()
        .initialState(State.A)
        .configure(State.A)
        .internal(Event.INTERNAL, (t, ctx) -> ctx.record("INTERNAL_ACTION"))
        .onEntry((t, ctx) -> ctx.record("A_ENTRY"))
        .onExit((t, ctx) -> ctx.record("A_EXIT"))
        .and()
        .onAnyTransition((t, ctx) -> ctx.record("GLOBAL_TRANSITION"))
        .build();

    State result = machine.fire(State.A, Event.INTERNAL, tracker);

    assertEquals(State.A, result);
    assertEquals(2, tracker.actions.size());
    assertTrue(tracker.actions.contains("INTERNAL_ACTION"));
    assertTrue(tracker.actions.contains("GLOBAL_TRANSITION"));
    assertFalse(tracker.actions.contains("A_ENTRY"));
    assertFalse(tracker.actions.contains("A_EXIT"));
  }

  @Test
  void shouldExecuteActionsInCorrectOrder() {
    ActionTracker tracker = new ActionTracker();

    StateMachine<State, Event, ActionTracker> machine = FlowMachine
        .<State, Event, ActionTracker>builder()
        .initialState(State.A)
        .configure(State.A)
        .permit(Event.GO_TO_B, State.B)
        .onExit((t, ctx) -> ctx.record("A_EXIT"))
        .and()
        .configure(State.B)
        .onEntry((t, ctx) -> ctx.record("B_ENTRY"))
        .and()
        .onAnyExit((t, ctx) -> ctx.record("GLOBAL_EXIT"))
        .onAnyEntry((t, ctx) -> ctx.record("GLOBAL_ENTRY"))
        .onAnyTransition((t, ctx) -> ctx.record("GLOBAL_TRANSITION"))
        .build();

    machine.fire(State.A, Event.GO_TO_B, tracker);

    assertEquals(5, tracker.actions.size());
    assertEquals("GLOBAL_EXIT", tracker.actions.get(0));
    assertEquals("A_EXIT", tracker.actions.get(1));
    assertEquals("GLOBAL_TRANSITION", tracker.actions.get(2));
    assertEquals("GLOBAL_ENTRY", tracker.actions.get(3));
    assertEquals("B_ENTRY", tracker.actions.get(4));
  }

  @Test
  void shouldExecuteConditionalInternalActions() {
    ActionTracker tracker = new ActionTracker();

    StateMachine<State, Event, ActionTracker> machine = FlowMachine
        .<State, Event, ActionTracker>builder()
        .initialState(State.A)
        .configure(State.A)
        .internalIf(Event.INTERNAL, (t, ctx) -> ctx.record("CONDITIONAL_INTERNAL"),
            (t, ctx) -> ctx.actions.isEmpty())
        .and()
        .build();

    State result1 = machine.fire(State.A, Event.INTERNAL, tracker);
    assertEquals(State.A, result1);
    assertTrue(tracker.actions.contains("CONDITIONAL_INTERNAL"));

    tracker.clear();
    tracker.record("existing");

    State result2 = machine.fire(State.A, Event.INTERNAL, tracker);
    assertEquals(State.A, result2);
    assertEquals(1, tracker.actions.size());
    assertEquals("existing", tracker.actions.get(0));
  }
}