package com.flowmachine.testing.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Contains the results of testing all possible transitions from a state.
 *
 * @param <TState>   the state type
 * @param <TEvent>   the event type
 * @param <TContext> the context type
 */
public class TransitionTestResult<TState, TEvent, TContext> {

  private final TState sourceState;
  private final TContext context;
  private final Map<TEvent, TransitionExecutionResult<TState, TEvent, TContext>> transitionResults;
  private final List<TEvent> allowedEvents;
  private final List<TEvent> blockedEvents;
  private final List<String> errors;

  private TransitionTestResult(Builder<TState, TEvent, TContext> builder) {
    this.sourceState = builder.sourceState;
    this.context = builder.context;
    this.transitionResults = Map.copyOf(builder.transitionResults);
    this.allowedEvents = new ArrayList<>(builder.allowedEvents);
    this.blockedEvents = new ArrayList<>(builder.blockedEvents);
    this.errors = new ArrayList<>(builder.errors);
  }

  public static <TState, TEvent, TContext> Builder<TState, TEvent, TContext> builder() {
    return new Builder<>();
  }

  public TState getSourceState() {
    return sourceState;
  }

  public TContext getContext() {
    return context;
  }

  public Map<TEvent, TransitionExecutionResult<TState, TEvent, TContext>> getTransitionResults() {
    return transitionResults;
  }

  public List<TEvent> getAllowedEvents() {
    return new ArrayList<>(allowedEvents);
  }

  public List<TEvent> getBlockedEvents() {
    return new ArrayList<>(blockedEvents);
  }

  public List<String> getErrors() {
    return new ArrayList<>(errors);
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public int getTotalEventsCount() {
    return allowedEvents.size() + blockedEvents.size();
  }

  public int getAllowedEventsCount() {
    return allowedEvents.size();
  }

  public int getBlockedEventsCount() {
    return blockedEvents.size();
  }

  @Override
  public String toString() {
    return String.format("TransitionTestResult{sourceState=%s, allowed=%d, blocked=%d, errors=%d}",
        sourceState, allowedEvents.size(), blockedEvents.size(), errors.size());
  }

  public static class Builder<TState, TEvent, TContext> {

    private TState sourceState;
    private TContext context;
    private Map<TEvent, TransitionExecutionResult<TState, TEvent, TContext>> transitionResults = Map.of();
    private List<TEvent> allowedEvents = new ArrayList<>();
    private List<TEvent> blockedEvents = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public Builder<TState, TEvent, TContext> sourceState(TState sourceState) {
      this.sourceState = sourceState;
      return this;
    }

    public Builder<TState, TEvent, TContext> context(TContext context) {
      this.context = context;
      return this;
    }

    public Builder<TState, TEvent, TContext> transitionResults(
        Map<TEvent, TransitionExecutionResult<TState, TEvent, TContext>> transitionResults) {
      this.transitionResults = Map.copyOf(transitionResults);
      return this;
    }

    public Builder<TState, TEvent, TContext> allowedEvents(List<TEvent> allowedEvents) {
      this.allowedEvents = new ArrayList<>(allowedEvents);
      return this;
    }

    public Builder<TState, TEvent, TContext> blockedEvents(List<TEvent> blockedEvents) {
      this.blockedEvents = new ArrayList<>(blockedEvents);
      return this;
    }

    public Builder<TState, TEvent, TContext> errors(List<String> errors) {
      this.errors = new ArrayList<>(errors);
      return this;
    }

    public Builder<TState, TEvent, TContext> addError(String error) {
      this.errors.add(error);
      return this;
    }

    public TransitionTestResult<TState, TEvent, TContext> build() {
      return new TransitionTestResult<>(this);
    }
  }
}