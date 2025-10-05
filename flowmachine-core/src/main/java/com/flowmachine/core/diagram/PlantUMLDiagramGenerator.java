package com.flowmachine.core.diagram;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.model.StateMachineInfo;
import com.flowmachine.core.model.TransitionInfo;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PlantUML diagram generator implementation for StateMachine instances.
 *
 * @param <TState>   the type of states in the state machine
 * @param <TEvent>   the type of events in the state machine
 * @param <TContext> the type of context in the state machine
 * @author FlowMachine
 */
public class PlantUMLDiagramGenerator<TState, TEvent, TContext> implements DiagramGenerator<TState, TEvent, TContext> {

  private static final Logger logger = LoggerFactory.getLogger(PlantUMLDiagramGenerator.class.getName());

  @Override
  public String generate(StateMachine<TState, TEvent, TContext> stateMachine) {
    return generatePlantUMLDiagram(stateMachine, null);
  }

  @Override
  public String generate(StateMachine<TState, TEvent, TContext> stateMachine, String title) {
    return generatePlantUMLDiagram(stateMachine, title);
  }

  @Override
  public String generateDetailed(StateMachine<TState, TEvent, TContext> stateMachine, String title) {
    return generateDetailedPlantUMLDiagram(stateMachine, title);
  }

  @Override
  public String generateForEvent(StateMachine<TState, TEvent, TContext> stateMachine, TEvent focusEvent, String title) {
    return generatePlantUMLDiagramForEvent(stateMachine, focusEvent, title);
  }

  @Override
  public void printToConsole(StateMachine<TState, TEvent, TContext> stateMachine, String title) {
    printPlantUMLToConsole(stateMachine, title);
  }

  @Override
  public DiagramFormat getFormatName() {
    return DiagramFormat.PLANTUML;
  }

  /**
   * Generates a PlantUML state diagram from any StateMachine instance.
   *
   * @param stateMachine the state machine to visualize
   * @param title        optional title for the diagram (can be null)
   * @return PlantUML diagram as a string
   */
  private String generatePlantUMLDiagram(StateMachine<TState, TEvent, TContext> stateMachine, String title) {

    StringBuilder plantuml = new StringBuilder();

    plantuml.append("@startuml\n");

    // Add title if provided
    if (Objects.nonNull(title) && !title.trim().isEmpty()) {
      plantuml.append("title ").append(title).append("\n");
    }

    // Apply theme and styling
    plantuml.append("!theme plain\n");
    plantuml.append("skinparam state {\n");
    plantuml.append("  BackgroundColor<<Final>> LightGreen\n");
    plantuml.append("  BackgroundColor<<Initial>> LightBlue\n");
    plantuml.append("  BackgroundColor<<Error>> LightCoral\n");
    plantuml.append("  BorderColor<<Final>> DarkGreen\n");
    plantuml.append("  BorderColor<<Initial>> DarkBlue\n");
    plantuml.append("  BorderColor<<Error>> DarkRed\n");
    plantuml.append("  FontStyle<<Final>> bold\n");
    plantuml.append("  FontStyle<<Initial>> bold\n");
    plantuml.append("}\n\n");

    StateMachineInfo<TState, TEvent, TContext> info = stateMachine.getInfo();

    // Mark initial state
    plantuml.append("state ").append(sanitizeStateName(info.initialState()))
        .append(" <<Initial>>\n");

    // Mark final states
    Set<TState> finalStates = info.states().stream()
        .filter(stateMachine::isFinalState)
        .collect(Collectors.toSet());

    for (TState finalState : finalStates) {
      plantuml.append("state ").append(sanitizeStateName(finalState))
          .append(" <<Final>>\n");
    }

    // Mark error states (states containing "ERROR", "FAIL", "REJECT" in their name)
    for (TState state : info.states()) {
      String stateName = state.toString().toUpperCase();
      if ((stateName.contains("ERROR") || stateName.contains("FAIL") || stateName.contains("REJECT"))
          && !stateMachine.isFinalState(state)) {
        plantuml.append("state ").append(sanitizeStateName(state))
            .append(" <<Error>>\n");
      }
    }

    plantuml.append("\n");

    // Add start point
    plantuml.append("[*] --> ").append(sanitizeStateName(info.initialState())).append("\n");

    // Add all transitions
    for (TransitionInfo<TState, TEvent> transition : info.transitions()) {
      String fromState = sanitizeStateName(transition.fromState());
      String toState = sanitizeStateName(transition.toState());
      String event = sanitizeEventName(transition.event());

      plantuml.append(fromState)
          .append(" --> ").append(toState)
          .append(" : ").append(event).append("\n");
    }

    // Add end points for final states
    for (TState finalState : finalStates) {
      plantuml.append(sanitizeStateName(finalState)).append(" --> [*]\n");
    }

    plantuml.append("\n@enduml\n");
    return plantuml.toString();
  }

  /**
   * Generates a comprehensive PlantUML diagram with additional information including state counts, transition counts,
   * and legend.
   *
   * @param stateMachine the state machine to visualize
   * @param title        optional title for the diagram
   * @return enhanced PlantUML diagram as a string
   */
  private String generateDetailedPlantUMLDiagram(StateMachine<TState, TEvent, TContext> stateMachine, String title) {

    StringBuilder plantuml = new StringBuilder();
    StateMachineInfo<TState, TEvent, TContext> info = stateMachine.getInfo();

    // Add title with statistics
    String enhancedTitle = Objects.nonNull(title) ? title : "State Machine Diagram";
    enhancedTitle += String.format("\\n(%d states, %d transitions)",
        info.states().size(),
        info.transitions().size());

    // Generate the main diagram
    String mainDiagram = generatePlantUMLDiagram(stateMachine, enhancedTitle);
    plantuml.append(mainDiagram);

    // Add a note with detailed information
    plantuml.append("\nnote top\n");
    plantuml.append("**State Machine Information**\n");
    plantuml.append("• Initial State: ").append(info.initialState()).append("\n");
    plantuml.append("• Total States: ").append(info.states().size()).append("\n");
    plantuml.append("• Total Events: ").append(info.events().size()).append("\n");
    plantuml.append("• Total Transitions: ").append(info.transitions().size()).append("\n");

    Set<TState> finalStates = info.states().stream()
        .filter(stateMachine::isFinalState)
        .collect(Collectors.toSet());
    plantuml.append("• Final States: ").append(finalStates.size()).append("\n");
    plantuml.append("end note\n");

    return plantuml.toString();
  }

  /**
   * Generates a PlantUML diagram focusing on a specific event type. Useful for complex state machines where you want to
   * see only transitions triggered by a particular event.
   *
   * @param stateMachine the state machine to visualize
   * @param focusEvent   the event to focus on
   * @param title        optional title for the diagram
   * @return filtered PlantUML diagram as a string
   */
  private String generatePlantUMLDiagramForEvent(StateMachine<TState, TEvent, TContext> stateMachine, TEvent focusEvent,
      String title) {

    StringBuilder plantuml = new StringBuilder();

    plantuml.append("@startuml\n");

    // Add title
    String enhancedTitle = (Objects.nonNull(title) ? title : "State Machine") +
                           "\\nEvent: " + focusEvent;
    plantuml.append("title ").append(enhancedTitle).append("\n");

    // Apply theme
    plantuml.append("!theme plain\n");
    plantuml.append("skinparam state {\n");
    plantuml.append("  BackgroundColor<<Final>> LightGreen\n");
    plantuml.append("  BackgroundColor<<Initial>> LightBlue\n");
    plantuml.append("  BackgroundColor<<Highlighted>> Yellow\n");
    plantuml.append("}\n\n");

    StateMachineInfo<TState, TEvent, TContext> info = stateMachine.getInfo();

    // Mark initial and final states
    plantuml.append("state ").append(sanitizeStateName(info.initialState()))
        .append(" <<Initial>>\n");

    Set<TState> finalStates = info.states().stream()
        .filter(stateMachine::isFinalState)
        .collect(Collectors.toSet());

    for (TState finalState : finalStates) {
      plantuml.append("state ").append(sanitizeStateName(finalState))
          .append(" <<Final>>\n");
    }

    // Highlight states involved in the focused event transitions
    Set<TState> involvedStates = info.transitions().stream()
        .filter(t -> focusEvent.equals(t.event()))
        .flatMap(t -> java.util.stream.Stream.of(t.fromState(), t.toState()))
        .collect(Collectors.toSet());

    for (TState state : involvedStates) {
      if (!stateMachine.isFinalState(state) && !state.equals(info.initialState())) {
        plantuml.append("state ").append(sanitizeStateName(state))
            .append(" <<Highlighted>>\n");
      }
    }

    plantuml.append("\n");

    // Add start point
    plantuml.append("[*] --> ").append(sanitizeStateName(info.initialState())).append("\n");

    // Add only transitions for the focused event
    var filteredTransitions = info.transitions().stream()
        .filter(t -> focusEvent.equals(t.event()))
        .toList();

    for (TransitionInfo<TState, TEvent> transition : filteredTransitions) {
      String fromState = sanitizeStateName(transition.fromState());
      String toState = sanitizeStateName(transition.toState());
      String event = sanitizeEventName(transition.event());

      plantuml.append(fromState)
          .append(" --> ").append(toState)
          .append(" : ").append(event).append("\n");
    }

    // Add end points for final states
    for (TState finalState : finalStates) {
      plantuml.append(sanitizeStateName(finalState)).append(" --> [*]\n");
    }

    // Add note about filtering
    plantuml.append("\nnote bottom\n");
    plantuml.append("Showing only transitions for event: **").append(focusEvent).append("**\n");
    plantuml.append("Total transitions for this event: ").append(filteredTransitions.size()).append("\n");
    plantuml.append("end note\n");

    plantuml.append("\n@enduml\n");
    return plantuml.toString();
  }

  /**
   * Prints a PlantUML diagram to the console with instructions on how to view it.
   *
   * @param stateMachine the state machine to visualize
   * @param title        optional title for the diagram
   */
  private void printPlantUMLToConsole(StateMachine<TState, TEvent, TContext> stateMachine, String title) {
    String diagram = """
        ╔════════════════════════════════════════════════════════════════╗
        ║                     PLANTUML FLOW DIAGRAM                      ║
        ╠════════════════════════════════════════════════════════════════╣
        ║ Copy the text below and paste it into:                         ║
        ║ • https://plantuml.com/plantuml/ for online viewing            ║
        ║ • VS Code with PlantUML extension                              ║
        ║ • Local PlantUML installation                                  ║
        ║ • IntelliJ IDEA with PlantUML plugin                           ║
        ╚════════════════════════════════════════════════════════════════╝

        %s
        """.formatted(generatePlantUMLDiagram(stateMachine, title));

    logger.info(diagram);
  }

  /**
   * Sanitizes state names to be valid in PlantUML diagrams. Handles special characters and ensures valid identifiers.
   */
  private String sanitizeStateName(TState state) {
    if (Objects.isNull(state)) {
      return "NULL";
    }

    String name = state.toString();
    // PlantUML is more flexible with naming, but we still want to avoid issues
    name = name.replace("\\", "_")
        .replace("\"", "'")
        .replace("\n", "_")
        .replace("\r", "_");

    return name;
  }

  /**
   * Sanitizes event names to be valid in PlantUML diagrams.
   */
  private String sanitizeEventName(TEvent event) {
    if (Objects.isNull(event)) {
      return "NULL";
    }

    String name = event.toString();
    // Keep event names readable but safe
    name = name.replace("\\", "_")
        .replace("\"", "'")
        .replace("\n", " ")
        .replace("\r", " ");

    return name;
  }
}