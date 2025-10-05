package com.flowmachine.testing.scenario;

import java.util.Objects;

/**
 * Represents an expected transition in a test scenario.
 *
 * @param <TState> the state type
 * @param <TEvent> the event type
 */
public record ExpectedTransition<TState, TEvent>(TEvent event, TState expectedTargetState) {

    public ExpectedTransition(TEvent event, TState expectedTargetState) {
        this.event = Objects.requireNonNull(event, "Event cannot be null");
        this.expectedTargetState = Objects.requireNonNull(expectedTargetState, "Expected target state cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExpectedTransition<?, ?> that = (ExpectedTransition<?, ?>) o;
        return Objects.equals(event, that.event) && Objects.equals(expectedTargetState, that.expectedTargetState);
    }

    @Override
    public String toString() {
        return String.format("ExpectedTransition{event=%s, expectedTargetState=%s}", event, expectedTargetState);
    }
}