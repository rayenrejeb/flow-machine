package com.flowmachine.core.impl;

import com.flowmachine.core.api.Action;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

class StateDefinition<TState, TEvent, TContext> {

  private final TState state;
  private final List<TransitionRule<TState, TEvent, TContext>> transitions = new ArrayList<>();
  private final List<Action<TState, TEvent, TContext>> entryActions = new ArrayList<>();
  private final List<Action<TState, TEvent, TContext>> exitActions = new ArrayList<>();
  private boolean isFinal = false;


  StateDefinition(TState state) {
    this.state = state;
  }

  TState getState() {
    return state;
  }

  List<TransitionRule<TState, TEvent, TContext>> getTransitions() {
    return Collections.unmodifiableList(transitions);
  }

  List<Action<TState, TEvent, TContext>> getEntryActions() {
    return Collections.unmodifiableList(entryActions);
  }

  List<Action<TState, TEvent, TContext>> getExitActions() {
    return Collections.unmodifiableList(exitActions);
  }

  boolean isFinal() {
    return isFinal;
  }

  void setFinal(boolean isFinal) {
    this.isFinal = isFinal;
  }

  List<TransitionRule<TState, TEvent, TContext>> getAutoTransitions() {
    return transitions.stream().filter(TransitionRule::isAutoTransition).toList();
  }


  void addEntryAction(Action<TState, TEvent, TContext> action) {
    entryActions.add(action);
  }

  void addExitAction(Action<TState, TEvent, TContext> action) {
    exitActions.add(action);
  }

  void addTransition(TransitionRule<TState, TEvent, TContext> transition) {
    transitions.add(transition);
  }

  List<TransitionRule<TState, TEvent, TContext>> getTransitionsForEvent(TEvent event) {
    return transitions.stream().filter(rule -> Objects.equals(rule.event(), event)).toList();
  }
}