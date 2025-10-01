package com.flowmachine.core.api;

import com.flowmachine.core.model.TransitionInfo;

@FunctionalInterface
public interface Guard<TState, TEvent, TContext> {

  boolean test(TransitionInfo<TState, TEvent> transition, TContext context);
}