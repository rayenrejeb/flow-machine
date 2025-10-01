package com.flowmachine.core.impl;

import com.flowmachine.core.api.Action;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class StateDefinition<TState, TEvent, TContext> {

  private final TState state;
  private final List<TransitionRule<TState, TEvent, TContext>> transitions = new ArrayList<>();
  private final List<Action<TState, TEvent, TContext>> entryActions = new ArrayList<>();
  private final List<Action<TState, TEvent, TContext>> exitActions = new ArrayList<>();
  private boolean isFinal = false;

  private volatile Map<TEvent, List<TransitionRule<TState, TEvent, TContext>>> transitionCache;
  private volatile boolean cacheBuilt = false;

  StateDefinition(TState state) {
    this.state = state;
  }

  TState getState() {
    return state;
  }

  List<TransitionRule<TState, TEvent, TContext>> getTransitions() {
    return new ArrayList<>(transitions);
  }

  List<Action<TState, TEvent, TContext>> getEntryActions() {
    return new ArrayList<>(entryActions);
  }

  List<Action<TState, TEvent, TContext>> getExitActions() {
    return new ArrayList<>(exitActions);
  }

  boolean isFinal() {
    return isFinal;
  }

  void setFinal(boolean isFinal) {
    this.isFinal = isFinal;
  }

  List<TransitionRule<TState, TEvent, TContext>> getAutoTransitions() {
    return transitions.stream()
        .filter(TransitionRule::isAutoTransition)
        .collect(Collectors.toList());
  }


  void addEntryAction(Action<TState, TEvent, TContext> action) {
    entryActions.add(action);
  }

  void addExitAction(Action<TState, TEvent, TContext> action) {
    exitActions.add(action);
  }

  void addTransition(TransitionRule<TState, TEvent, TContext> transition) {
    transitions.add(transition);
    cacheBuilt = false;
  }

  List<TransitionRule<TState, TEvent, TContext>> getTransitionsForEvent(TEvent event) {
    buildCacheIfNeeded();
    return transitionCache.getOrDefault(event, Collections.emptyList());
  }

  private void buildCacheIfNeeded() {
    if (!cacheBuilt) {
      synchronized (this) {
        if (!cacheBuilt) {
          buildTransitionCache();
          cacheBuilt = true;
        }
      }
    }
  }

  private void buildTransitionCache() {
    Map<TEvent, List<TransitionRule<TState, TEvent, TContext>>> cache = new HashMap<>();
    for (TransitionRule<TState, TEvent, TContext> rule : transitions) {
      cache.computeIfAbsent(rule.event(), k -> new ArrayList<>()).add(rule);
    }

    for (Map.Entry<TEvent, List<TransitionRule<TState, TEvent, TContext>>> entry : cache.entrySet()) {
      entry.setValue(Collections.unmodifiableList(entry.getValue()));
    }

    this.transitionCache = Collections.unmodifiableMap(cache);
  }
}