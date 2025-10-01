package com.flowmachine.core.impl;

import com.flowmachine.core.api.Action;
import com.flowmachine.core.api.Guard;
import com.flowmachine.core.api.StateConfiguration;
import com.flowmachine.core.api.StateMachineBuilder;

class StateConfigurationImpl<TState, TEvent, TContext> implements StateConfiguration<TState, TEvent, TContext> {

  private final StateMachineBuilderImpl<TState, TEvent, TContext> builder;
  private final StateDefinition<TState, TEvent, TContext> stateDefinition;

  StateConfigurationImpl(
      StateMachineBuilderImpl<TState, TEvent, TContext> builder,
      StateDefinition<TState, TEvent, TContext> stateDefinition) {
    this.builder = builder;
    this.stateDefinition = stateDefinition;
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> permit(TEvent event, TState targetState) {
    return permitIf(event, targetState, null);
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> permitIf(TEvent event, TState targetState,
      Guard<TState, TEvent, TContext> guard) {
    stateDefinition.addTransition(TransitionRule.permit(event, targetState, guard));
    return this;
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> permitReentry(TEvent event) {
    return permitReentryIf(event, null);
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> permitReentryIf(
      TEvent event,
      Guard<TState, TEvent, TContext> guard) {
    stateDefinition.addTransition(TransitionRule.permitReentry(event, guard));
    return this;
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> ignore(TEvent event) {
    return ignoreIf(event, null);
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> ignoreIf(
      TEvent event,
      Guard<TState, TEvent, TContext> guard) {
    stateDefinition.addTransition(TransitionRule.ignore(event, guard));
    return this;
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> internal(
      TEvent event,
      Action<TState, TEvent, TContext> action) {
    return internalIf(event, action, null);
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> internalIf(
      TEvent event,
      Action<TState, TEvent, TContext> action,
      Guard<TState, TEvent, TContext> guard) {
    stateDefinition.addTransition(TransitionRule.internal(event, action, guard));
    return this;
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> onEntry(Action<TState, TEvent, TContext> action) {
    stateDefinition.addEntryAction(action);
    return this;
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> onExit(Action<TState, TEvent, TContext> action) {
    stateDefinition.addExitAction(action);
    return this;
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> asFinal() {
    stateDefinition.setFinal(true);
    return this;
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> autoTransition(TState targetState) {
    return autoTransitionIf(targetState, null);
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> autoTransitionIf(
      TState targetState,
      Guard<TState, TEvent, TContext> guard) {
    stateDefinition.addTransition(TransitionRule.autoTransition(targetState, guard));
    return this;
  }

  @Override
  public StateMachineBuilder<TState, TEvent, TContext> and() {
    return builder;
  }
}