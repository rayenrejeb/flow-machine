package com.flowmachine.core.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record DebugInfo<TState, TEvent>(
    TState currentState,
    TEvent event,
    DebugInfoReason reason,
    LocalDateTime timestamp,
    String context
) {

  public static <TState, TEvent> DebugInfo<TState, TEvent> of(TState state, TEvent event, DebugInfoReason reason) {
    return new DebugInfo<>(state, event, reason, LocalDateTime.now(), null);
  }

  public static <TState, TEvent> DebugInfo<TState, TEvent> of(
      TState state,
      TEvent event,
      DebugInfoReason reason,
      String context) {
    return new DebugInfo<>(state, event, reason, LocalDateTime.now(), context);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[").append(timestamp).append("] ");
    sb.append("State: ").append(currentState);
    sb.append(", Event: ").append(event);
    sb.append(", Reason: ").append(reason);
    if (Objects.nonNull(context)) {
      sb.append(", Context: ").append(context);
    }
    return sb.toString();
  }
}