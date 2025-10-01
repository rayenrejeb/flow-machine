package com.flowmachine.core.diagram;

import com.flowmachine.core.api.StateMachine;

/**
 * Interface for generating visual diagrams from StateMachine instances.
 * <p>
 * This interface allows for different diagram formats (Mermaid, PlantUML, DOT, etc.) while providing a consistent API
 * for diagram generation.
 *
 * @param <TState>   the type of states in the state machine
 * @param <TEvent>   the type of events in the state machine
 * @param <TContext> the type of context in the state machine
 * @author FlowMachine
 */
public interface DiagramGenerator<TState, TEvent, TContext> {

  /**
   * Generates a basic diagram from the state machine.
   *
   * @param stateMachine the state machine to visualize
   * @return diagram as a string
   */
  String generate(StateMachine<TState, TEvent, TContext> stateMachine);

  /**
   * Generates a diagram with a custom title.
   *
   * @param stateMachine the state machine to visualize
   * @param title        the title for the diagram
   * @return diagram as a string
   */
  String generate(StateMachine<TState, TEvent, TContext> stateMachine, String title);

  /**
   * Generates a detailed diagram with additional information including state counts, transition counts, and metadata.
   *
   * @param stateMachine the state machine to visualize
   * @param title        optional title for the diagram
   * @return enhanced diagram as a string
   */
  String generateDetailed(StateMachine<TState, TEvent, TContext> stateMachine, String title);

  /**
   * Generates a diagram focusing on a specific event type. Useful for complex state machines where you want to see only
   * transitions triggered by a particular event.
   *
   * @param stateMachine the state machine to visualize
   * @param focusEvent   the event to focus on
   * @param title        optional title for the diagram
   * @return filtered diagram as a string
   */
  String generateForEvent(StateMachine<TState, TEvent, TContext> stateMachine, TEvent focusEvent, String title);

  /**
   * Prints a diagram to the console with instructions on how to view it.
   *
   * @param stateMachine the state machine to visualize
   * @param title        optional title for the diagram
   */
  void printToConsole(StateMachine<TState, TEvent, TContext> stateMachine, String title);

  /**
   * Returns the format name of this diagram generator (e.g., "Mermaid", "PlantUML", "DOT").
   *
   * @return the format name
   */
  DiagramFormat getFormatName();

}