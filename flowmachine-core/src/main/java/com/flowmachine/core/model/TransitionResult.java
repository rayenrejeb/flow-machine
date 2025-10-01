package com.flowmachine.core.model;

public record TransitionResult<TState>(
    TState state,
    boolean wasTransitioned,
    String reason,
    DebugInfo<TState, ?> debugInfo
) {

  public static <TState> TransitionResult<TState> success(TState state) {
    return new TransitionResult<>(state, true, "Transition successful", null);
  }

  public static <TState> TransitionResult<TState> ignored(TState state, String reason) {
    return new TransitionResult<>(state, false, reason, null);
  }

  public static <TState> TransitionResult<TState> failed(TState state, String reason) {
    return new TransitionResult<>(state, false, reason, null);
  }

  public static <TState, TEvent> TransitionResult<TState> failed(TState state, String reason,
      DebugInfo<TState, TEvent> debugInfo) {
    return new TransitionResult<>(state, false, reason, debugInfo);
  }

  public boolean hasDebugInfo() {
    return debugInfo != null;
  }
}