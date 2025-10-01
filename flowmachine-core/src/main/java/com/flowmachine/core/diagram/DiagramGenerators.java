package com.flowmachine.core.diagram;

/**
 * Factory class for creating diagram generators for StateMachine instances.
 * <p>
 * This class provides convenient access to different diagram formats and utilities for working with multiple
 * generators.
 *
 * @author FlowMachine
 */
public class DiagramGenerators {

  /**
   * Creates a Mermaid diagram generator.
   *
   * @param <TState>   the type of states in the state machine
   * @param <TEvent>   the type of events in the state machine
   * @param <TContext> the type of context in the state machine
   * @return a Mermaid diagram generator
   */
  public static <TState, TEvent, TContext> DiagramGenerator<TState, TEvent, TContext> mermaid() {
    return MermaidDiagramGenerator.create();
  }

  /**
   * Creates a PlantUML diagram generator.
   *
   * @param <TState>   the type of states in the state machine
   * @param <TEvent>   the type of events in the state machine
   * @param <TContext> the type of context in the state machine
   * @return a PlantUML diagram generator
   */
  public static <TState, TEvent, TContext> DiagramGenerator<TState, TEvent, TContext> plantUML() {
    return PlantUMLDiagramGenerator.create();
  }

}