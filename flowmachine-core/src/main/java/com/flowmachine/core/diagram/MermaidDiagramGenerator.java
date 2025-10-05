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
 * Mermaid diagram generator implementation for StateMachine instances.
 *
 * @param <TState>   the type of states in the state machine
 * @param <TEvent>   the type of events in the state machine
 * @param <TContext> the type of context in the state machine
 * @author FlowMachine
 */
public class MermaidDiagramGenerator<TState, TEvent, TContext> implements DiagramGenerator<TState, TEvent, TContext> {

  private static final Logger logger = LoggerFactory.getLogger(MermaidDiagramGenerator.class.getName());

  @Override
  public String generate(StateMachine<TState, TEvent, TContext> stateMachine) {
    return generate(stateMachine, null);
  }

  @Override
  public String generate(StateMachine<TState, TEvent, TContext> stateMachine, String title) {
    return generateMermaidDiagram(stateMachine, title);
  }

  @Override
  public String generateDetailed(StateMachine<TState, TEvent, TContext> stateMachine, String title) {
    return generateDetailedMermaidDiagram(stateMachine, title);
  }

  @Override
  public String generateForEvent(StateMachine<TState, TEvent, TContext> stateMachine, TEvent focusEvent, String title) {
    return generateMermaidDiagramForEvent(stateMachine, focusEvent, title);
  }

  @Override
  public void printToConsole(StateMachine<TState, TEvent, TContext> stateMachine, String title) {
    printMermaidToConsole(stateMachine, title);
  }

  @Override
  public DiagramFormat getFormatName() {
    return DiagramFormat.MERMAID;
  }

  /**
   * Generates a Mermaid state diagram from any StateMachine instance.
   *
   * @param stateMachine the state machine to visualize
   * @param title        optional title for the diagram (can be null)
   * @return Mermaid diagram as a string
   */
  private String generateMermaidDiagram(StateMachine<TState, TEvent, TContext> stateMachine, String title) {

    StringBuilder mermaid = new StringBuilder();

    // Add title if provided
    if (Objects.nonNull(title) && !title.trim().isEmpty()) {
      mermaid.append("---\n");
      mermaid.append("title: ").append(title).append("\n");
      mermaid.append("---\n");
    }

    mermaid.append("stateDiagram-v2\n");

    StateMachineInfo<TState, TEvent, TContext> info = stateMachine.getInfo();

    // Add start state
    mermaid.append("    [*] --> ").append(sanitizeStateName(info.initialState())).append("\n");

    // Add all transitions
    for (TransitionInfo<TState, TEvent> transition : info.transitions()) {
      String fromState = sanitizeStateName(transition.fromState());
      String toState = sanitizeStateName(transition.toState());
      String event = sanitizeEventName(transition.event());

      mermaid.append("    ").append(fromState)
          .append(" --> ").append(toState)
          .append(" : ").append(event).append("\n");
    }

    // Add final states
    Set<TState> finalStates = info.states().stream()
        .filter(stateMachine::isFinalState)
        .collect(Collectors.toSet());

    for (TState finalState : finalStates) {
      mermaid.append("    ").append(sanitizeStateName(finalState))
          .append(" --> [*]\n");
    }

    // Add state styling
    if (!finalStates.isEmpty()) {
      mermaid.append("\n");
      mermaid.append("    %% Styling\n");

      // Style initial state
      mermaid.append("    classDef initialState fill:#e1f5fe,stroke:#01579b,stroke-width:2px\n");
      mermaid.append("    class ").append(sanitizeStateName(info.initialState()))
          .append(" initialState\n");

      // Style final states
      mermaid.append("    classDef finalState fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px\n");
      mermaid.append("    class ");
      mermaid.append(finalStates.stream()
          .map(this::sanitizeStateName)
          .collect(Collectors.joining(",")));
      mermaid.append(" finalState\n");
    }

    return mermaid.toString();
  }


  /**
   * Generates a comprehensive Mermaid diagram with additional information including state counts, transition counts,
   * and legend.
   *
   * @param stateMachine the state machine to visualize
   * @param title        optional title for the diagram
   * @return enhanced Mermaid diagram as a string
   */
  private String generateDetailedMermaidDiagram(StateMachine<TState, TEvent, TContext> stateMachine, String title) {

    StringBuilder mermaid = new StringBuilder();
    StateMachineInfo<TState, TEvent, TContext> info = stateMachine.getInfo();

    // Add title with statistics
    String enhancedTitle = Objects.nonNull(title) ? title : "State Machine Diagram";
    enhancedTitle += String.format(" (%d states, %d transitions)",
        info.states().size(),
        info.transitions().size());

    mermaid.append("---\n");
    mermaid.append("title: ").append(enhancedTitle).append("\n");
    mermaid.append("---\n");

    // Generate the main diagram
    mermaid.append(generateMermaidDiagram(stateMachine, null));

    // Add notes as comments
    mermaid.append("\n    %% State Machine Information:\n");
    mermaid.append("    %% Initial State: ").append(info.initialState()).append("\n");
    mermaid.append("    %% Total States: ").append(info.states().size()).append("\n");
    mermaid.append("    %% Total Events: ").append(info.events().size()).append("\n");
    mermaid.append("    %% Total Transitions: ").append(info.transitions().size()).append("\n");

    Set<TState> finalStates = info.states().stream()
        .filter(stateMachine::isFinalState)
        .collect(Collectors.toSet());
    mermaid.append("    %% Final States: ").append(finalStates).append("\n");

    return mermaid.toString();
  }

  /**
   * Generates a Mermaid diagram focusing on a specific event type. Useful for complex state machines where you want to
   * see only transitions triggered by a particular event.
   *
   * @param stateMachine the state machine to visualize
   * @param focusEvent   the event to focus on
   * @param title        optional title for the diagram
   * @return filtered Mermaid diagram as a string
   */
  private String generateMermaidDiagramForEvent(StateMachine<TState, TEvent, TContext> stateMachine, TEvent focusEvent,
      String title) {

    StringBuilder mermaid = new StringBuilder();

    // Add title
    String enhancedTitle = (Objects.nonNull(title) ? title : "State Machine") +
                           " - Event: " + focusEvent;
    mermaid.append("---\n");
    mermaid.append("title: ").append(enhancedTitle).append("\n");
    mermaid.append("---\n");

    mermaid.append("stateDiagram-v2\n");

    StateMachineInfo<TState, TEvent, TContext> info = stateMachine.getInfo();

    // Add start state
    mermaid.append("    [*] --> ").append(sanitizeStateName(info.initialState())).append("\n");

    // Add only transitions for the focused event
    var filteredTransitions = info.transitions().stream()
        .filter(t -> focusEvent.equals(t.event()))
        .toList();

    for (TransitionInfo<TState, TEvent> transition : filteredTransitions) {
      String fromState = sanitizeStateName(transition.fromState());
      String toState = sanitizeStateName(transition.toState());
      String event = sanitizeEventName(transition.event());

      mermaid.append("    ").append(fromState)
          .append(" --> ").append(toState)
          .append(" : ").append(event).append("\n");
    }

    // Add final states
    Set<TState> finalStates = info.states().stream()
        .filter(stateMachine::isFinalState)
        .collect(Collectors.toSet());

    for (TState finalState : finalStates) {
      mermaid.append("    ").append(sanitizeStateName(finalState))
          .append(" --> [*]\n");
    }

    // Add note about filtering
    mermaid.append("\n    %% Showing only transitions for event: ").append(focusEvent).append("\n");
    mermaid.append("    %% Total transitions for this event: ").append(filteredTransitions.size()).append("\n");

    return mermaid.toString();
  }

  /**
   * Prints a Mermaid diagram to the console with instructions on how to view it.
   *
   * @param stateMachine the state machine to visualize
   * @param title        optional title for the diagram
   */
  private void printMermaidToConsole(StateMachine<TState, TEvent, TContext> stateMachine, String title) {
    String diagram = """
        ╔════════════════════════════════════════════════════════════════╗
        ║                      MERMAID FLOW DIAGRAM                      ║
        ╠════════════════════════════════════════════════════════════════╣
        ║ Copy the text below and paste it into:                         ║
        ║ • GitHub/GitLab markdown (in ```mermaid blocks)                ║
        ║ • https://mermaid.live/ for online viewing                     ║
        ║ • VS Code with Mermaid extension                               ║
        ╚════════════════════════════════════════════════════════════════╝

        ```mermaid
        %s
        ```
        """.formatted(generateMermaidDiagram(stateMachine, title));

    logger.info(diagram);
  }

  /**
   * Sanitizes state names to be valid in Mermaid diagrams. Handles special characters and ensures valid identifiers.
   */
  private String sanitizeStateName(TState state) {
    if (Objects.isNull(state)) {
      return "NULL";
    }

    String name = state.toString();
    // Replace characters that could cause issues in Mermaid
    name = name.replace(" ", "_")
        .replace("-", "_")
        .replace(".", "_")
        .replace("(", "_")
        .replace(")", "_")
        .replace("[", "_")
        .replace("]", "_");

    // Ensure it starts with a letter or underscore
    if (!name.matches("^[a-zA-Z_].*")) {
      name = "State_" + name;
    }

    return name;
  }

  /**
   * Sanitizes event names to be valid in Mermaid diagrams.
   */
  private String sanitizeEventName(TEvent event) {
    if (Objects.isNull(event)) {
      return "NULL";
    }

    String name = event.toString();
    // Keep event names readable but safe
    name = name.replace("\"", "'")
        .replace("\n", " ")
        .replace("\r", " ");

    return name;
  }
}