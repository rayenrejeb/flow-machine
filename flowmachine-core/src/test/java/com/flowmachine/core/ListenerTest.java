package com.flowmachine.core;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.api.StateMachineListener;
import com.flowmachine.core.model.TransitionResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ListenerTest {

    enum State { START, MIDDLE, END, ERROR }
    enum Event { PROCEED, FAIL, FINISH }

    static class TestListener implements StateMachineListener<State, Event, String> {
        private final List<String> events = new ArrayList<>();
        private final AtomicInteger stateEntryCount = new AtomicInteger(0);
        private final AtomicInteger stateExitCount = new AtomicInteger(0);
        private final AtomicInteger transitionCount = new AtomicInteger(0);
        private final AtomicInteger errorCount = new AtomicInteger(0);

        @Override
        public void onStateEntry(State state, Event event, String context) {
            events.add("ENTRY:" + state + ":" + event + ":" + context);
            stateEntryCount.incrementAndGet();
        }

        @Override
        public void onStateExit(State state, Event event, String context) {
            events.add("EXIT:" + state + ":" + event + ":" + context);
            stateExitCount.incrementAndGet();
        }

        @Override
        public void onTransition(State fromState, State toState, Event event, String context) {
            events.add("TRANSITION:" + fromState + "->" + toState + ":" + event + ":" + context);
            transitionCount.incrementAndGet();
        }

        @Override
        public void onTransitionError(State state, Event event, String context, Exception error) {
            events.add("ERROR:" + state + ":" + event + ":" + context + ":" + error.getMessage());
            errorCount.incrementAndGet();
        }

        List<String> getEvents() { return new ArrayList<>(events); }
        int getStateEntryCount() { return stateEntryCount.get(); }
        int getStateExitCount() { return stateExitCount.get(); }
        int getTransitionCount() { return transitionCount.get(); }
        int getErrorCount() { return errorCount.get(); }
        void reset() {
            events.clear();
            stateEntryCount.set(0);
            stateExitCount.set(0);
            transitionCount.set(0);
            errorCount.set(0);
        }
    }

    @Test
    void shouldNotifyListenerOnStateTransition() {
        TestListener listener = new TestListener();

        StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
            .initialState(State.START)
            .configure(State.START)
                .permit(Event.PROCEED, State.MIDDLE)
            .and()
            .configure(State.MIDDLE)
                .permit(Event.FINISH, State.END)
            .and()
            .configure(State.END)
                .asFinal()
            .and()
            .addListener(listener)
            .build();

        State result = machine.fire(State.START, Event.PROCEED, "test-context");
        assertEquals(State.MIDDLE, result);

        List<String> events = listener.getEvents();
        assertEquals(3, events.size());
        assertTrue(events.contains("EXIT:START:PROCEED:test-context"));
        assertTrue(events.contains("TRANSITION:START->MIDDLE:PROCEED:test-context"));
        assertTrue(events.contains("ENTRY:MIDDLE:PROCEED:test-context"));

        assertEquals(1, listener.getStateEntryCount());
        assertEquals(1, listener.getStateExitCount());
        assertEquals(1, listener.getTransitionCount());
        assertEquals(0, listener.getErrorCount());
    }

    @Test
    void shouldNotifyMultipleListeners() {
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();

        StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
            .initialState(State.START)
            .configure(State.START)
                .permit(Event.PROCEED, State.MIDDLE)
            .and()
            .configure(State.MIDDLE)
                .asFinal()
            .and()
            .addListener(listener1)
            .addListener(listener2)
            .build();

        machine.fire(State.START, Event.PROCEED, "test");

        // Both listeners should receive the same events
        assertEquals(listener1.getEvents(), listener2.getEvents());
        assertEquals(1, listener1.getTransitionCount());
        assertEquals(1, listener2.getTransitionCount());
    }

    @Test
    void shouldNotifyListenerOnPermitReentry() {
        TestListener listener = new TestListener();

        StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
            .initialState(State.START)
            .configure(State.START)
                .permitReentry(Event.PROCEED)
            .and()
            .addListener(listener)
            .build();

        State result = machine.fire(State.START, Event.PROCEED, "reentry");
        assertEquals(State.START, result);

        List<String> events = listener.getEvents();
        assertEquals(3, events.size());
        assertTrue(events.contains("EXIT:START:PROCEED:reentry"));
        assertTrue(events.contains("TRANSITION:START->START:PROCEED:reentry"));
        assertTrue(events.contains("ENTRY:START:PROCEED:reentry"));
    }

    @Test
    void shouldNotifyListenerOnInternalTransition() {
        TestListener listener = new TestListener();

        StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
            .initialState(State.START)
            .configure(State.START)
                .internal(Event.PROCEED, (transition, context) -> {
                    // Internal action
                })
            .and()
            .addListener(listener)
            .build();

        State result = machine.fire(State.START, Event.PROCEED, "internal");
        assertEquals(State.START, result);

        List<String> events = listener.getEvents();
        assertEquals(1, events.size());
        assertTrue(events.contains("TRANSITION:START->START:PROCEED:internal"));

        // Internal transitions should not trigger entry/exit events
        assertEquals(0, listener.getStateEntryCount());
        assertEquals(0, listener.getStateExitCount());
        assertEquals(1, listener.getTransitionCount());
    }

    @Test
    void shouldNotifyListenerOnIgnoredEvents() {
        TestListener listener = new TestListener();

        StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
            .initialState(State.START)
            .configure(State.START)
                .ignore(Event.PROCEED)
            .and()
            .addListener(listener)
            .build();

        State result = machine.fire(State.START, Event.PROCEED, "ignored");
        assertEquals(State.START, result);

        // Ignored events should not trigger any listener notifications
        List<String> events = listener.getEvents();
        assertEquals(0, events.size());
        assertEquals(0, listener.getStateEntryCount());
        assertEquals(0, listener.getStateExitCount());
        assertEquals(0, listener.getTransitionCount());
    }

    @Test
    void shouldNotifyListenerOnErrors() {
        TestListener listener = new TestListener();

        StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
            .initialState(State.START)
            .configure(State.START)
                .permit(Event.PROCEED, State.MIDDLE)
            .and()
            .configure(State.MIDDLE)
                .onEntry((transition, context) -> {
                    throw new RuntimeException("Test error");
                })
                .asFinal()
            .and()
            .addListener(listener)
            .build();

        TransitionResult<State> result = machine.fireWithResult(State.START, Event.PROCEED, "error-test");
        assertFalse(result.wasTransitioned());

        assertEquals(1, listener.getErrorCount());
        List<String> events = listener.getEvents();
        assertTrue(events.stream().anyMatch(event ->
            event.startsWith("ERROR:START:PROCEED:error-test:Test error")));
    }

    @Test
    void shouldNotifyListenerOnAutoTransitions() {
        TestListener listener = new TestListener();

        StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
            .initialState(State.START)
            .configure(State.START)
                .permit(Event.PROCEED, State.MIDDLE)
            .and()
            .configure(State.MIDDLE)
                .autoTransition(State.END)
            .and()
            .configure(State.END)
                .asFinal()
            .and()
            .addListener(listener)
            .build();

        State result = machine.fire(State.START, Event.PROCEED, "auto");
        assertEquals(State.END, result);

        List<String> events = listener.getEvents();
        // Should have events for START->MIDDLE transition and MIDDLE->END auto-transition
        assertTrue(events.stream().anyMatch(event ->
            event.equals("TRANSITION:START->MIDDLE:PROCEED:auto")));
        assertTrue(events.stream().anyMatch(event ->
            event.equals("TRANSITION:MIDDLE->END:null:auto")));

        assertEquals(2, listener.getStateEntryCount()); // MIDDLE and END
        assertEquals(2, listener.getStateExitCount());  // START and MIDDLE
        assertEquals(2, listener.getTransitionCount()); // START->MIDDLE, MIDDLE->END
    }

    @Test
    void shouldNotifyListenerOnConditionalTransitions() {
        TestListener listener = new TestListener();

        StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
            .initialState(State.START)
            .configure(State.START)
                .permitIf(Event.PROCEED, State.MIDDLE, (transition, context) -> context.equals("success"))
                .permitIf(Event.PROCEED, State.ERROR, (transition, context) -> context.equals("failure"))
            .and()
            .configure(State.MIDDLE)
                .asFinal()
            .and()
            .configure(State.ERROR)
                .asFinal()
            .and()
            .addListener(listener)
            .build();

        // Test successful condition
        State result1 = machine.fire(State.START, Event.PROCEED, "success");
        assertEquals(State.MIDDLE, result1);

        assertTrue(listener.getEvents().stream().anyMatch(event ->
            event.equals("TRANSITION:START->MIDDLE:PROCEED:success")));

        listener.reset();

        // Test failure condition
        State result2 = machine.fire(State.START, Event.PROCEED, "failure");
        assertEquals(State.ERROR, result2);

        assertTrue(listener.getEvents().stream().anyMatch(event ->
            event.equals("TRANSITION:START->ERROR:PROCEED:failure")));
    }

    @Test
    void shouldHandleListenerExceptions() {
        StateMachineListener<State, Event, String> faultyListener = new StateMachineListener<State, Event, String>() {
            @Override
            public void onStateEntry(State state, Event event, String context) {
                throw new RuntimeException("Listener error");
            }
        };

        TestListener goodListener = new TestListener();

        StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
            .initialState(State.START)
            .configure(State.START)
                .permit(Event.PROCEED, State.MIDDLE)
            .and()
            .configure(State.MIDDLE)
                .asFinal()
            .and()
            .addListener(faultyListener)
            .addListener(goodListener)
            .build();

        // The state machine should continue working even if a listener throws an exception
        State result = machine.fire(State.START, Event.PROCEED, "test");
        assertEquals(State.MIDDLE, result);

        // The good listener should still receive events
        assertEquals(1, goodListener.getTransitionCount());
        assertEquals(1, goodListener.getStateEntryCount());
    }

    @Test
    void shouldPreserveListenerOrder() {
        List<String> executionOrder = new ArrayList<>();

        StateMachineListener<State, Event, String> listener1 = new StateMachineListener<State, Event, String>() {
            @Override
            public void onTransition(State fromState, State toState, Event event, String context) {
                executionOrder.add("listener1");
            }
        };

        StateMachineListener<State, Event, String> listener2 = new StateMachineListener<State, Event, String>() {
            @Override
            public void onTransition(State fromState, State toState, Event event, String context) {
                executionOrder.add("listener2");
            }
        };

        StateMachine<State, Event, String> machine = FlowMachine.<State, Event, String>builder()
            .initialState(State.START)
            .configure(State.START)
                .permit(Event.PROCEED, State.MIDDLE)
            .and()
            .configure(State.MIDDLE)
                .asFinal()
            .and()
            .addListener(listener1)
            .addListener(listener2)
            .build();

        machine.fire(State.START, Event.PROCEED, "test");

        assertEquals(2, executionOrder.size());
        assertEquals("listener1", executionOrder.get(0));
        assertEquals("listener2", executionOrder.get(1));
    }
}