package com.flowmachine.core.api;

import com.flowmachine.core.model.TransitionInfo;

@FunctionalInterface
public interface Action<TState, TEvent, TContext> {

  void execute(TransitionInfo<TState, TEvent> transition, TContext context);
}