package com.flowmachine.core.model;

public record TransitionInfo<TState, TEvent>(
    TState fromState,
    TState toState,
    TEvent event
) {

}