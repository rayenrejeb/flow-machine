package com.flowmachine.core.impl;

import com.flowmachine.core.api.Action;
import com.flowmachine.core.api.Guard;
import com.flowmachine.core.api.StateConfiguration;
import com.flowmachine.core.api.StateMachineBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class StateConfigurationImpl<TState, TEvent, TContext> implements StateConfiguration<TState, TEvent, TContext> {

  private final Logger logger = LoggerFactory.getLogger(StateConfigurationImpl.class);
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
    logger.trace("permit - event {}, targetState {}", event, targetState);
    return permitIf(event, targetState, null);
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> permitIf(TEvent event, TState targetState,
      Guard<TState, TEvent, TContext> guard) {
    logger.trace("permitIf - event {}, targetState {}", event, targetState);
    stateDefinition.addTransition(TransitionRule.permit(event, targetState, guard));
    return this;
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> permitReentry(TEvent event) {
    logger.trace("permitReentry - event {}", event);
    return permitReentryIf(event, null);
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> permitReentryIf(
      TEvent event,
      Guard<TState, TEvent, TContext> guard) {
    logger.trace("permitReentryIf - event {}", event);
    stateDefinition.addTransition(TransitionRule.permitReentry(event, guard));
    return this;
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> ignore(TEvent event) {
    logger.trace("ignore - event {}", event);
    return ignoreIf(event, null);
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> ignoreIf(
      TEvent event,
      Guard<TState, TEvent, TContext> guard) {
    logger.trace("ignoreIf - event {}", event);
    stateDefinition.addTransition(TransitionRule.ignore(event, guard));
    return this;
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> internal(
      TEvent event,
      Action<TState, TEvent, TContext> action) {
    logger.trace("internal - event {}", event);
    return internalIf(event, action, null);
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> internalIf(
      TEvent event,
      Action<TState, TEvent, TContext> action,
      Guard<TState, TEvent, TContext> guard) {
    logger.trace("internalIf - event {}", event);
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
    logger.trace("autoTransition - state {}", targetState);
    return autoTransitionIf(targetState, null);
  }

  @Override
  public StateConfiguration<TState, TEvent, TContext> autoTransitionIf(
      TState targetState,
      Guard<TState, TEvent, TContext> guard) {
    logger.trace("autoTransitionIf - state {}", targetState);
    stateDefinition.addTransition(TransitionRule.autoTransition(targetState, guard));
    return this;
  }

  @Override
  public StateMachineBuilder<TState, TEvent, TContext> and() {
    return builder;
  }
}