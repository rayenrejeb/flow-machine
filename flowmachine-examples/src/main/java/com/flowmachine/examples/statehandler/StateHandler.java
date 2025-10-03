package com.flowmachine.examples.statehandler;

/**
 * @deprecated Use com.flowmachine.core.api.StateHandler instead.
 * This interface is now part of the core FlowMachine API.
 */
@Deprecated
public interface StateHandler<TState, TEvent, TContext> extends com.flowmachine.core.api.StateHandler<TState, TEvent, TContext> {
}