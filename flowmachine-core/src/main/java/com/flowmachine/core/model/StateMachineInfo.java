package com.flowmachine.core.model;

import java.util.Set;

public record StateMachineInfo<TState, TEvent, TContext>(
    TState initialState,
    Set<TState> states,
    Set<TEvent> events,
    Set<TransitionInfo<TState, TEvent>> transitions
) {

}