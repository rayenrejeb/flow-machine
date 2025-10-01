package com.flowmachine.core.exception;

@FunctionalInterface
public interface ErrorHandler<TState, TEvent, TContext> {

  TState handle(TState currentState, TEvent event, TContext context, Exception error);
}