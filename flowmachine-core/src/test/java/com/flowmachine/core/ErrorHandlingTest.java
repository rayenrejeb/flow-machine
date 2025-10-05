package com.flowmachine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.model.DebugInfoReason;
import com.flowmachine.core.model.TransitionResult;
import org.junit.jupiter.api.Test;

class ErrorHandlingTest {

  enum State {NORMAL, ERROR, RECOVERED}

  enum Event {TRIGGER_ERROR, RECOVER, NORMAL_EVENT}

  static class TestContext {

    boolean shouldThrow = false;
    Exception lastError;
  }

  @Test
  void shouldHandleActionExceptions() {
    StateMachine<State, Event, TestContext> machine = FlowMachine
        .<State, Event, TestContext>builder()
        .initialState(State.NORMAL)
        .configure(State.NORMAL)
        .permit(Event.TRIGGER_ERROR, State.ERROR)
        .onExit((t, ctx) -> {
          if (ctx.shouldThrow) {
            throw new RuntimeException("Exit action failed");
          }
        })
        .and()
        .configure(State.ERROR)
        .and()
        .onError((state, event, ctx, error) -> {
          ctx.lastError = error;
          return State.ERROR;
        })
        .build();

    TestContext context = new TestContext();
    context.shouldThrow = true;
    machine.fire(State.NORMAL, Event.TRIGGER_ERROR, context);
    TransitionResult<State> result = machine.fireWithResult(State.NORMAL, Event.TRIGGER_ERROR, context);

    assertEquals(State.ERROR, result.state());
    assertFalse(result.wasTransitioned());
    assertNotNull(context.lastError);
    assertTrue(result.reason().contains("Error handled"));
  }

  @Test
  void shouldHandleGuardExceptions() {
    StateMachine<State, Event, TestContext> machine = FlowMachine
        .<State, Event, TestContext>builder()
        .initialState(State.NORMAL)
        .configure(State.NORMAL)
        .permitIf(Event.TRIGGER_ERROR, State.ERROR, (t, ctx) -> {
          throw new RuntimeException("Guard failed");
        })
        .and()
        .configure(State.ERROR)
        .and()
        .onError((state, event, ctx, error) -> {
          ctx.lastError = error;
          return state;
        })
        .build();

    TestContext context = new TestContext();

    TransitionResult<State> result = machine.fireWithResult(State.NORMAL, Event.TRIGGER_ERROR, context);

    assertEquals(State.NORMAL, result.state());
    assertFalse(result.wasTransitioned());
    assertNotNull(context.lastError);
    assertTrue(result.reason().contains("Error handled"));
  }

  @Test
  void shouldHandleErrorInErrorHandler() {
    StateMachine<State, Event, TestContext> machine = FlowMachine
        .<State, Event, TestContext>builder()
        .initialState(State.NORMAL)
        .configure(State.NORMAL)
        .permit(Event.TRIGGER_ERROR, State.ERROR)
        .onExit((t, ctx) -> {
          throw new RuntimeException("Exit action failed");
        })
        .and()
        .configure(State.ERROR)
        .and()
        .onError((state, event, ctx, error) -> {
          throw new RuntimeException("Error handler failed");
        })
        .build();

    TestContext context = new TestContext();

    TransitionResult<State> result = machine.fireWithResult(State.NORMAL, Event.TRIGGER_ERROR, context);

    assertEquals(State.NORMAL, result.state());
    assertFalse(result.wasTransitioned());
    assertTrue(result.reason().contains("Error in error handler"));
    assertTrue(result.hasDebugInfo());
  }

  @Test
  void shouldProvideDebugInfoOnFailure() {
    StateMachine<State, Event, TestContext> machine = FlowMachine
        .<State, Event, TestContext>builder()
        .initialState(State.NORMAL)
        .configure(State.NORMAL)
        .permit(Event.RECOVER, State.RECOVERED)
        .and()
        .configure(State.RECOVERED)
        .and()
        .build();

    TestContext context = new TestContext();

    TransitionResult<State> result = machine.fireWithResult(State.NORMAL, Event.TRIGGER_ERROR, context);

    assertEquals(State.NORMAL, result.state());
    assertFalse(result.wasTransitioned());
    assertTrue(result.hasDebugInfo());
    assertNotNull(result.debugInfo());
    assertTrue(result.debugInfo().reason().equals(DebugInfoReason.NO_APPLICABLE_TRANSITION_FOUND));
    assertTrue(result.debugInfo().context().contains("Available events"));
  }

  @Test
  void shouldHandleUnknownState() {
    StateMachine<State, Event, TestContext> machine = FlowMachine
        .<State, Event, TestContext>builder()
        .initialState(State.NORMAL)
        .configure(State.NORMAL)
        .permit(Event.NORMAL_EVENT, State.RECOVERED)
        .and()
        .configure(State.RECOVERED)
        .and()
        .build();

    TestContext context = new TestContext();

    TransitionResult<State> result = machine.fireWithResult(State.ERROR, Event.NORMAL_EVENT, context);

    assertEquals(State.ERROR, result.state());
    assertFalse(result.wasTransitioned());
    assertTrue(result.reason().contains("Unknown state"));
    assertTrue(result.hasDebugInfo());
    assertTrue(result.debugInfo().context().contains("Available states"));
  }
}