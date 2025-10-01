package com.flowmachine.core;

import com.flowmachine.core.api.StateMachineBuilder;
import com.flowmachine.core.impl.StateMachineBuilderImpl;

public final class FlowMachine {

  private FlowMachine() {
    throw new UnsupportedOperationException("FlowMachine constructor is private");
  }

  public static <TState, TEvent, TContext> StateMachineBuilder<TState, TEvent, TContext> builder() {
    return new StateMachineBuilderImpl<>();
  }
}