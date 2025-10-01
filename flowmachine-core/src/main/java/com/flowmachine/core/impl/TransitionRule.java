package com.flowmachine.core.impl;

import com.flowmachine.core.api.Action;
import com.flowmachine.core.api.Guard;

record TransitionRule<TState, TEvent, TContext>(
    TEvent event,
    TState targetState,
    Guard<TState, TEvent, TContext> guard,
    Action<TState, TEvent, TContext> action,
    TransitionType type
) {

  static <TState, TEvent, TContext> TransitionRule<TState, TEvent, TContext> permit(
      TEvent event,
      TState targetState,
      Guard<TState, TEvent, TContext> guard) {
    return new TransitionRule<>(event, targetState, guard, null, TransitionType.PERMIT);
  }

  static <TState, TEvent, TContext> TransitionRule<TState, TEvent, TContext> permitReentry(
      TEvent event,
      Guard<TState, TEvent, TContext> guard) {
    return new TransitionRule<>(event, null, guard, null, TransitionType.PERMIT_REENTRY);
  }

  static <TState, TEvent, TContext> TransitionRule<TState, TEvent, TContext> ignore(
      TEvent event,
      Guard<TState, TEvent, TContext> guard) {
    return new TransitionRule<>(event, null, guard, null, TransitionType.IGNORE);
  }

  static <TState, TEvent, TContext> TransitionRule<TState, TEvent, TContext> internal(
      TEvent event,
      Action<TState, TEvent, TContext> action,
      Guard<TState, TEvent, TContext> guard) {
    return new TransitionRule<>(event, null, guard, action, TransitionType.INTERNAL);
  }

  static <TState, TEvent, TContext> TransitionRule<TState, TEvent, TContext> autoTransition(
      TState targetState,
      Guard<TState, TEvent, TContext> guard) {
    return new TransitionRule<>(null, targetState, guard, null, TransitionType.AUTO_TRANSITION);
  }

  boolean isAutoTransition() {
    return type == TransitionType.AUTO_TRANSITION;
  }

  enum TransitionType {
    PERMIT,
    PERMIT_REENTRY,
    IGNORE,
    INTERNAL,
    AUTO_TRANSITION
  }
}