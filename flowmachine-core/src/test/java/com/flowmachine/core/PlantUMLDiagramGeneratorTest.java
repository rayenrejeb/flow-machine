package com.flowmachine.core;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.diagram.PlantUMLDiagramGenerator;
import com.flowmachine.core.diagram.DiagramGenerator;
import com.flowmachine.core.diagram.DiagramFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlantUMLDiagramGeneratorTest {

    enum SimpleState { START, MIDDLE, END }
    enum SimpleEvent { GO, FINISH }

    @Test
    void shouldGenerateBasicPlantUMLDiagram() {
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

        DiagramGenerator<SimpleState, SimpleEvent, String> generator = new PlantUMLDiagramGenerator<>();
        String diagram = generator.generate(machine, "Simple Test Machine");

        // Verify basic structure
        assertTrue(diagram.contains("@startuml"));
        assertTrue(diagram.contains("@enduml"));
        assertTrue(diagram.contains("title Simple Test Machine"));
        assertTrue(diagram.contains("[*] --> START"));
        assertTrue(diagram.contains("START --> MIDDLE : GO"));
        assertTrue(diagram.contains("MIDDLE --> END : FINISH"));
        assertTrue(diagram.contains("END --> [*]"));

        // Verify styling
        assertTrue(diagram.contains("state START <<Initial>>"));
        assertTrue(diagram.contains("state END <<Final>>"));
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

        DiagramGenerator<SimpleState, SimpleEvent, String> generator = new PlantUMLDiagramGenerator<>();
        String diagram = generator.generate(machine, null);

        // Should not contain title
        assertFalse(diagram.contains("title"));
        assertTrue(diagram.contains("@startuml"));
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

        DiagramGenerator<SimpleState, SimpleEvent, String> generator = new PlantUMLDiagramGenerator<>();
        String diagram = generator.generateDetailed(machine, "Test Machine");

        // Should contain enhanced title with statistics
        assertTrue(diagram.contains("Test Machine\\n(2 states, 1 transitions)"));

        // Should contain note with machine info
        assertTrue(diagram.contains("note top"));
        assertTrue(diagram.contains("**State Machine Information**"));
        assertTrue(diagram.contains("Initial State: START"));
        assertTrue(diagram.contains("Total States: 2"));
        assertTrue(diagram.contains("Total Events: 1"));
        assertTrue(diagram.contains("Total Transitions: 1"));
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

        DiagramGenerator<SimpleState, SimpleEvent, String> generator = new PlantUMLDiagramGenerator<>();
        String diagram = generator.generateForEvent(machine, SimpleEvent.GO, "GO Event Only");

        // Should contain only GO transitions
        assertTrue(diagram.contains("START --> MIDDLE : GO"));
        assertFalse(diagram.contains("FINISH"));

        // Should contain filtering information
        assertTrue(diagram.contains("GO Event Only\\nEvent: GO"));
        assertTrue(diagram.contains("note bottom"));
        assertTrue(diagram.contains("Showing only transitions for event: **GO**"));
        assertTrue(diagram.contains("Total transitions for this event: 1"));

        // Should highlight involved states
        assertTrue(diagram.contains("state MIDDLE <<Highlighted>>"));
    }

    @Test
    void shouldHandleErrorStates() {
        enum ErrorState { START, ERROR_STATE, FAILED_STATE, REJECTED, END }
        enum ErrorEvent { PROCEED, FAIL }

        StateMachine<ErrorState, ErrorEvent, String> machine = FlowMachine.<ErrorState, ErrorEvent, String>builder()
            .initialState(ErrorState.START)
            .configure(ErrorState.START)
                .permit(ErrorEvent.PROCEED, ErrorState.END)
                .permit(ErrorEvent.FAIL, ErrorState.ERROR_STATE)
            .and()
            .configure(ErrorState.ERROR_STATE)
                .permit(ErrorEvent.PROCEED, ErrorState.FAILED_STATE)
            .and()
            .configure(ErrorState.FAILED_STATE)
                .permit(ErrorEvent.PROCEED, ErrorState.REJECTED)
            .and()
            .configure(ErrorState.REJECTED)
                .asFinal()
            .and()
            .configure(ErrorState.END)
                .asFinal()
            .and()
            .build();

        DiagramGenerator<ErrorState, ErrorEvent, String> generator = new PlantUMLDiagramGenerator<>();
        String diagram = generator.generate(machine, "Error Handling");

        // Should mark error-related states with special styling (non-final states only)
        assertTrue(diagram.contains("state ERROR_STATE <<Error>>"));
        assertTrue(diagram.contains("state FAILED_STATE <<Error>>"));

        // REJECTED is final, so it gets Final styling instead of Error styling
        assertTrue(diagram.contains("state REJECTED <<Final>>"));

        // Should not mark END as error state
        assertFalse(diagram.contains("state END <<Error>>"));
    }

    @Test
    void shouldWorkWithDiagramGeneratorsInterface() {
        StateMachine<SimpleState, SimpleEvent, String> machine = FlowMachine.<SimpleState, SimpleEvent, String>builder()
            .initialState(SimpleState.START)
            .configure(SimpleState.START)
                .permit(SimpleEvent.GO, SimpleState.END)
            .and()
            .configure(SimpleState.END)
                .asFinal()
            .and()
            .build();

        DiagramGenerator<SimpleState, SimpleEvent, String> generator = new PlantUMLDiagramGenerator<>();

        assertEquals(DiagramFormat.PLANTUML, generator.getFormatName());

        String diagram = generator.generate(machine, "Interface Test");
        assertTrue(diagram.contains("@startuml"));
        assertTrue(diagram.contains("title Interface Test"));
        assertTrue(diagram.contains("START --> END : GO"));
    }
}