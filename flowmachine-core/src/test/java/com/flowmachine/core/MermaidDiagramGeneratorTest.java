package com.flowmachine.core;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.diagram.MermaidDiagramGenerator;
import com.flowmachine.core.diagram.DiagramGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MermaidDiagramGeneratorTest {

    enum SimpleState { START, MIDDLE, END }
    enum SimpleEvent { GO, FINISH }

    @Test
    void shouldGenerateBasicMermaidDiagram() {
        StateMachine<SimpleState, SimpleEvent, String> machine = FlowMachine.<SimpleState, SimpleEvent, String>builder()
            .initialState(SimpleState.START)
            .configure(SimpleState.START)
                .permit(SimpleEvent.GO, SimpleState.MIDDLE)
            .and()
            .configure(SimpleState.MIDDLE)
                .permit(SimpleEvent.FINISH, SimpleState.END)
            .and()
            .configure(SimpleState.END)
                .asFinal()
            .and()
            .build();

        DiagramGenerator<SimpleState, SimpleEvent, String> generator = new MermaidDiagramGenerator<>();
        String diagram = generator.generate(machine, "Simple Test Machine");

        // Verify basic structure
        assertTrue(diagram.contains("stateDiagram-v2"));
        assertTrue(diagram.contains("title: Simple Test Machine"));
        assertTrue(diagram.contains("[*] --> START"));
        assertTrue(diagram.contains("START --> MIDDLE : GO"));
        assertTrue(diagram.contains("MIDDLE --> END : FINISH"));
        assertTrue(diagram.contains("END --> [*]"));

        // Verify styling
        assertTrue(diagram.contains("classDef initialState"));
        assertTrue(diagram.contains("classDef finalState"));
        assertTrue(diagram.contains("class START initialState"));
        assertTrue(diagram.contains("class END finalState"));
    }

    @Test
    void shouldGenerateBasicDiagramWithoutTitle() {
        StateMachine<SimpleState, SimpleEvent, String> machine = FlowMachine.<SimpleState, SimpleEvent, String>builder()
            .initialState(SimpleState.START)
            .configure(SimpleState.START)
                .permit(SimpleEvent.GO, SimpleState.END)
            .and()
            .configure(SimpleState.END)
                .asFinal()
            .and()
            .build();

        DiagramGenerator<SimpleState, SimpleEvent, String> generator = new MermaidDiagramGenerator<>();
        String diagram = generator.generate(machine);

        // Should not contain title section
        assertFalse(diagram.contains("title:"));
        assertTrue(diagram.contains("stateDiagram-v2"));
        assertTrue(diagram.contains("[*] --> START"));
        assertTrue(diagram.contains("START --> END : GO"));
        assertTrue(diagram.contains("END --> [*]"));
    }

    @Test
    void shouldGenerateDetailedDiagram() {
        StateMachine<SimpleState, SimpleEvent, String> machine = FlowMachine.<SimpleState, SimpleEvent, String>builder()
            .initialState(SimpleState.START)
            .configure(SimpleState.START)
                .permit(SimpleEvent.GO, SimpleState.END)
            .and()
            .configure(SimpleState.END)
                .asFinal()
            .and()
            .build();

        DiagramGenerator<SimpleState, SimpleEvent, String> generator = new MermaidDiagramGenerator<>();
        String diagram = generator.generateDetailed(machine, "Test Machine");

        // Should contain enhanced title with statistics
        assertTrue(diagram.contains("Test Machine (2 states, 1 transitions)"));

        // Should contain comments with machine info
        assertTrue(diagram.contains("%% State Machine Information:"));
        assertTrue(diagram.contains("%% Initial State: START"));
        assertTrue(diagram.contains("%% Total States: 2"));
        assertTrue(diagram.contains("%% Total Events: 1"));
        assertTrue(diagram.contains("%% Total Transitions: 1"));
    }

    @Test
    void shouldGenerateEventSpecificDiagram() {
        StateMachine<SimpleState, SimpleEvent, String> machine = FlowMachine.<SimpleState, SimpleEvent, String>builder()
            .initialState(SimpleState.START)
            .configure(SimpleState.START)
                .permit(SimpleEvent.GO, SimpleState.MIDDLE)
                .permit(SimpleEvent.FINISH, SimpleState.END)
            .and()
            .configure(SimpleState.MIDDLE)
                .permit(SimpleEvent.FINISH, SimpleState.END)
            .and()
            .configure(SimpleState.END)
                .asFinal()
            .and()
            .build();

        DiagramGenerator<SimpleState, SimpleEvent, String> generator = new MermaidDiagramGenerator<>();
        String diagram = generator.generateForEvent(machine, SimpleEvent.GO, "GO Event Only");

        // Should contain only GO transitions
        assertTrue(diagram.contains("START --> MIDDLE : GO"));
        assertFalse(diagram.contains("FINISH"));

        // Should contain filtering information
        assertTrue(diagram.contains("GO Event Only - Event: GO"));
        assertTrue(diagram.contains("%% Showing only transitions for event: GO"));
        assertTrue(diagram.contains("%% Total transitions for this event: 1"));
    }

    @Test
    void shouldHandleSpecialCharactersInStateNames() {
        // Create an enum with problematic characters
        enum SpecialState {
            START_STATE,
            MIDDLE_STATE,
            END_STATE
        }

        enum SpecialEvent {
            GO_EVENT,
            FINISH_EVENT
        }

        StateMachine<SpecialState, SpecialEvent, String> machine = FlowMachine.<SpecialState, SpecialEvent, String>builder()
            .initialState(SpecialState.START_STATE)
            .configure(SpecialState.START_STATE)
                .permit(SpecialEvent.GO_EVENT, SpecialState.END_STATE)
            .and()
            .configure(SpecialState.END_STATE)
                .asFinal()
            .and()
            .build();

        DiagramGenerator<SpecialState, SpecialEvent, String> generator = new MermaidDiagramGenerator<>();
        String diagram = generator.generate(machine);

        // Should handle underscores in state names
        assertTrue(diagram.contains("START_STATE"));
        assertTrue(diagram.contains("END_STATE"));
        assertTrue(diagram.contains("GO_EVENT"));
    }

    @Test
    void shouldWorkWithAnyStateMachineConfiguration() {
        // Test with the same configuration used in the examples
        StateMachine<SimpleState, SimpleEvent, String> machine = FlowMachine.<SimpleState, SimpleEvent, String>builder()
            .initialState(SimpleState.START)
            .configure(SimpleState.START)
                .permitIf(SimpleEvent.GO, SimpleState.MIDDLE, (t, ctx) -> ctx.equals("proceed"))
                .permitIf(SimpleEvent.GO, SimpleState.END, (t, ctx) -> ctx.equals("skip"))
            .and()
            .configure(SimpleState.MIDDLE)
                .permit(SimpleEvent.FINISH, SimpleState.END)
            .and()
            .configure(SimpleState.END)
                .asFinal()
            .and()
            .build();

        DiagramGenerator<SimpleState, SimpleEvent, String> generator = new MermaidDiagramGenerator<>();
        String diagram = generator.generate(machine, "Conditional Transitions");

        // Should show both conditional transitions
        assertTrue(diagram.contains("START --> MIDDLE : GO"));
        assertTrue(diagram.contains("START --> END : GO"));
        assertTrue(diagram.contains("MIDDLE --> END : FINISH"));

        // Basic structure should be intact
        assertTrue(diagram.contains("[*] --> START"));
        assertTrue(diagram.contains("END --> [*]"));
    }
}