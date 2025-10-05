package com.flowmachine.testing.result;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Contains the result of a single transition execution during testing.
 *
 * @param <TState>   the state type
 * @param <TEvent>   the event type
 * @param <TContext> the context type
 */
public class TransitionExecutionResult<TState, TEvent, TContext> {

  private final TEvent event;
  private final TState sourceState;
  private final TState targetState;
  private final TState expectedTargetState;
  private final TContext contextBefore;
  private final TContext contextAfter;
  private final boolean successful;
  private final String errorMessage;
  private final Throwable exception;
  private final Instant startTime;
  private final Instant endTime;
  private final Duration executionTime;

  private TransitionExecutionResult(Builder<TState, TEvent, TContext> builder) {
    this.event = builder.event;
    this.sourceState = builder.sourceState;
    this.targetState = builder.targetState;
    this.expectedTargetState = builder.expectedTargetState;
    this.contextBefore = builder.contextBefore;
    this.contextAfter = builder.contextAfter;
    this.successful = builder.successful;
    this.errorMessage = builder.errorMessage;
    this.exception = builder.exception;
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
    this.executionTime = Duration.between(startTime, endTime);
  }

  public static <TState, TEvent, TContext> Builder<TState, TEvent, TContext> builder() {
    return new Builder<>();
  }

  public TEvent getEvent() {
    return event;
  }

  public TState getSourceState() {
    return sourceState;
  }

  public TState getTargetState() {
    return targetState;
  }

  public TState getExpectedTargetState() {
    return expectedTargetState;
  }

  public TContext getContextBefore() {
    return contextBefore;
  }

  public TContext getContextAfter() {
    return contextAfter;
  }

  public boolean isSuccessful() {
    return successful;
  }

  public Optional<String> getErrorMessage() {
    return Optional.ofNullable(errorMessage);
  }

  public Optional<Throwable> getException() {
    return Optional.ofNullable(exception);
  }

  public Instant getStartTime() {
    return startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public Duration getExecutionTime() {
    return executionTime;
  }

  public boolean isStateTransitionCorrect() {
    return Objects.equals(targetState, expectedTargetState);
  }

  @Override
  public String toString() {
    return String.format("TransitionExecutionResult{event=%s, %s->%s (expected: %s), successful=%s, executionTime=%s}",
        event, sourceState, targetState, expectedTargetState, successful, executionTime);
  }

  public static class Builder<TState, TEvent, TContext> {

    private TEvent event;
    private TState sourceState;
    private TState targetState;
    private TState expectedTargetState;
    private TContext contextBefore;
    private TContext contextAfter;
    private boolean successful = true;
    private String errorMessage;
    private Throwable exception;
    private Instant startTime = Instant.now();
    private Instant endTime = Instant.now();

    public Builder<TState, TEvent, TContext> event(TEvent event) {
      this.event = event;
      return this;
    }

    public Builder<TState, TEvent, TContext> sourceState(TState sourceState) {
      this.sourceState = sourceState;
      return this;
    }

    public Builder<TState, TEvent, TContext> targetState(TState targetState) {
      this.targetState = targetState;
      return this;
    }

    public Builder<TState, TEvent, TContext> expectedTargetState(TState expectedTargetState) {
      this.expectedTargetState = expectedTargetState;
      return this;
    }

    public Builder<TState, TEvent, TContext> contextBefore(TContext contextBefore) {
      this.contextBefore = contextBefore;
      return this;
    }

    public Builder<TState, TEvent, TContext> contextAfter(TContext contextAfter) {
      this.contextAfter = contextAfter;
      return this;
    }

    public Builder<TState, TEvent, TContext> successful(boolean successful) {
      this.successful = successful;
      return this;
    }

    public Builder<TState, TEvent, TContext> errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      if (errorMessage != null) {
        this.successful = false;
      }
      return this;
    }

    public Builder<TState, TEvent, TContext> exception(Throwable exception) {
      this.exception = exception;
      if (exception != null) {
        this.successful = false;
        if (this.errorMessage == null) {
          this.errorMessage = exception.getMessage();
        }
      }
      return this;
    }

    public Builder<TState, TEvent, TContext> startTime(Instant startTime) {
      this.startTime = Objects.requireNonNull(startTime);
      return this;
    }

    public Builder<TState, TEvent, TContext> endTime(Instant endTime) {
      this.endTime = Objects.requireNonNull(endTime);
      return this;
    }

    public TransitionExecutionResult<TState, TEvent, TContext> build() {
      return new TransitionExecutionResult<>(this);
    }
  }
}