package com.flowmachine.testing.result;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Contains the results of executing a test scenario.
 *
 * @param <TState>   the state type
 * @param <TEvent>   the event type
 * @param <TContext> the context type
 */
public class TestResult<TState, TEvent, TContext> {

  private final boolean successful;
  private final TState initialState;
  private final TState finalState;
  private final TContext finalContext;
  private final List<TransitionExecutionResult<TState, TEvent, TContext>> transitionResults;
  private final List<TestFailure> errors;
  private final List<String> warnings;
  private final Instant startTime;
  private final Instant endTime;
  private final Duration executionTime;

  private TestResult(Builder<TState, TEvent, TContext> builder) {
    this.successful = builder.successful;
    this.initialState = builder.initialState;
    this.finalState = builder.finalState;
    this.finalContext = builder.finalContext;
    this.transitionResults = new ArrayList<>(builder.transitionResults);
    this.errors = new ArrayList<>(builder.errors);
    this.warnings = new ArrayList<>(builder.warnings);
    this.startTime = builder.startTime;
    this.endTime = builder.endTime;
    this.executionTime = Duration.between(startTime, endTime);
  }

  public static <TState, TEvent, TContext> Builder<TState, TEvent, TContext> builder() {
    return new Builder<>();
  }

  public boolean isSuccessful() {
    return successful;
  }

  public TState getInitialState() {
    return initialState;
  }

  public TState getFinalState() {
    return finalState;
  }

  public TContext getFinalContext() {
    return finalContext;
  }

  public List<TransitionExecutionResult<TState, TEvent, TContext>> getTransitionResults() {
    return new ArrayList<>(transitionResults);
  }

  public List<TestFailure> getErrors() {
    return new ArrayList<>(errors);
  }

  public List<String> getWarnings() {
    return new ArrayList<>(warnings);
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

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  public long getSuccessfulTransitionCount() {
    return transitionResults.stream()
        .mapToLong(result -> result.isSuccessful() ? 1 : 0)
        .sum();
  }

  public long getFailedTransitionCount() {
    return transitionResults.stream()
        .mapToLong(result -> result.isSuccessful() ? 0 : 1)
        .sum();
  }

  @Override
  public String toString() {
    return String.format("TestResult{successful=%s, transitions=%d/%d, errors=%d, warnings=%d, executionTime=%s}",
        successful,
        getSuccessfulTransitionCount(),
        transitionResults.size(),
        errors.size(),
        warnings.size(),
        executionTime);
  }

  public static class Builder<TState, TEvent, TContext> {

    private boolean successful = true;
    private TState initialState;
    private TState finalState;
    private TContext finalContext;
    private List<TransitionExecutionResult<TState, TEvent, TContext>> transitionResults = new ArrayList<>();
    private List<TestFailure> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private Instant startTime = Instant.now();
    private Instant endTime = Instant.now();

    public Builder<TState, TEvent, TContext> successful(boolean successful) {
      this.successful = successful;
      return this;
    }

    public Builder<TState, TEvent, TContext> initialState(TState initialState) {
      this.initialState = initialState;
      return this;
    }

    public Builder<TState, TEvent, TContext> finalState(TState finalState) {
      this.finalState = finalState;
      return this;
    }

    public Builder<TState, TEvent, TContext> finalContext(TContext finalContext) {
      this.finalContext = finalContext;
      return this;
    }

    public Builder<TState, TEvent, TContext> transitionResults(
        List<TransitionExecutionResult<TState, TEvent, TContext>> transitionResults) {
      this.transitionResults = new ArrayList<>(transitionResults);
      return this;
    }

    public Builder<TState, TEvent, TContext> addTransitionResult(
        TransitionExecutionResult<TState, TEvent, TContext> result) {
      this.transitionResults.add(result);
      return this;
    }

    public Builder<TState, TEvent, TContext> errors(List<TestFailure> errors) {
      this.errors = new ArrayList<>(errors);
      return this;
    }

    public Builder<TState, TEvent, TContext> addError(TestFailure error) {
      this.errors.add(error);
      this.successful = false;
      return this;
    }

    public Builder<TState, TEvent, TContext> warnings(List<String> warnings) {
      this.warnings = new ArrayList<>(warnings);
      return this;
    }

    public Builder<TState, TEvent, TContext> addWarning(String warning) {
      this.warnings.add(warning);
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

    public TestResult<TState, TEvent, TContext> build() {
      return new TestResult<>(this);
    }
  }
}