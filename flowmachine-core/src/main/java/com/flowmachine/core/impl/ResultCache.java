package com.flowmachine.core.impl;

import com.flowmachine.core.model.TransitionResult;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class ResultCache<TState> {

  private static final int MAX_CACHE_SIZE = 100;

  private final ConcurrentMap<String, TransitionResult<TState>> successCache = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, TransitionResult<TState>> ignoredCache = new ConcurrentHashMap<>();

  TransitionResult<TState> getSuccess(TState state) {
    if (successCache.size() > MAX_CACHE_SIZE) {
      return TransitionResult.success(state);
    }

    String key = String.valueOf(state);
    return successCache.computeIfAbsent(key, k -> TransitionResult.success(state));
  }

  TransitionResult<TState> getIgnored(TState state, String reason) {
    if (ignoredCache.size() > MAX_CACHE_SIZE) {
      return TransitionResult.ignored(state, reason);
    }

    String key = state + ":" + reason;
    return ignoredCache.computeIfAbsent(key, k -> TransitionResult.ignored(state, reason));
  }

  void clear() {
    successCache.clear();
    ignoredCache.clear();
  }
}