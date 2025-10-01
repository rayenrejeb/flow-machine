package com.flowmachine.core.impl;

import static com.flowmachine.core.model.DebugInfoReason.ERROR_IN_ERROR_HANDLER;
import static com.flowmachine.core.model.DebugInfoReason.EXCEPTION_DURING_TRANSITION;
import static com.flowmachine.core.model.DebugInfoReason.NO_APPLICABLE_TRANSITION_FOUND;
import static com.flowmachine.core.model.DebugInfoReason.STATE_NOT_FOUND_IN_CONFIGURATION;
import static com.flowmachine.core.model.DebugInfoReason.TRANSITION_ATTEMPTED_FROM_FINAL_STATE;
import com.flowmachine.core.api.Action;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.api.StateMachineListener;
import com.flowmachine.core.exception.ErrorHandler;
import com.flowmachine.core.model.DebugInfo;
import com.flowmachine.core.model.StateMachineInfo;
import com.flowmachine.core.model.TransitionInfo;
import com.flowmachine.core.model.TransitionResult;
import com.flowmachine.core.model.ValidationResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class StateMachineImpl<TState, TEvent, TContext> implements StateMachine<TState, TEvent, TContext> {

  private final TState initialState;
  private final Map<TState, StateDefinition<TState, TEvent, TContext>> stateDefinitions;
  private final List<Action<TState, TEvent, TContext>> globalEntryActions;
  private final List<Action<TState, TEvent, TContext>> globalExitActions;
  private final List<Action<TState, TEvent, TContext>> globalTransitionActions;
  private final ErrorHandler<TState, TEvent, TContext> errorHandler;
  private final List<StateMachineListener<TState, TEvent, TContext>> listeners;
  private final ResultCache<TState> resultCache = new ResultCache<>();

  StateMachineImpl(TState initialState,
      Map<TState, StateDefinition<TState, TEvent, TContext>> stateDefinitions,
      List<Action<TState, TEvent, TContext>> globalEntryActions,
      List<Action<TState, TEvent, TContext>> globalExitActions,
      List<Action<TState, TEvent, TContext>> globalTransitionActions,
      ErrorHandler<TState, TEvent, TContext> errorHandler,
      List<StateMachineListener<TState, TEvent, TContext>> listeners) {
    this.initialState = initialState;
    this.stateDefinitions = stateDefinitions;
    this.globalEntryActions = globalEntryActions;
    this.globalExitActions = globalExitActions;
    this.globalTransitionActions = globalTransitionActions;
    this.errorHandler = errorHandler;
    this.listeners = listeners;
  }

  @Override
  public TState fire(TState currentState, TEvent event, TContext context) {
    return fireWithResult(currentState, event, context).state();
  }

  @Override
  public TransitionResult<TState> fireWithResult(TState currentState, TEvent event, TContext context) {
    try {
      StateDefinition<TState, TEvent, TContext> stateDefinition = stateDefinitions.get(currentState);
      if (stateDefinition == null) {
        DebugInfo<TState, TEvent> debugInfo = DebugInfo.of(currentState, event,
            STATE_NOT_FOUND_IN_CONFIGURATION,
            "Available states: " + stateDefinitions.keySet());
        return TransitionResult.failed(currentState, "Unknown state: " + currentState, debugInfo);
      }

      if (stateDefinition.isFinal()) {
        DebugInfo<TState, TEvent> debugInfo = DebugInfo.of(currentState, event,
            TRANSITION_ATTEMPTED_FROM_FINAL_STATE,
            "Final states do not allow transitions");
        return TransitionResult.failed(currentState, "Cannot transition from final state: " + currentState, debugInfo);
      }

      TransitionRule<TState, TEvent, TContext> applicableRule = findApplicableRule(
          stateDefinition, event, currentState, context);

      if (applicableRule == null) {
        String availableEvents = stateDefinition.getTransitions().stream()
            .map(rule -> rule.event().toString())
            .distinct()
            .reduce((a, b) -> a + ", " + b)
            .orElse("none");

        DebugInfo<TState, TEvent> debugInfo = DebugInfo.of(currentState, event,
            NO_APPLICABLE_TRANSITION_FOUND,
            "Available events in this state: " + availableEvents);
        return TransitionResult.failed(currentState,
            "No transition configured for event '" + event + "' in state '" + currentState + "'", debugInfo);
      }

      return executeTransition(currentState, event, context, applicableRule);

    } catch (Exception e) {
      notifyError(currentState, event, context, e);

      DebugInfo<TState, TEvent> debugInfo = DebugInfo.of(currentState, event,
          EXCEPTION_DURING_TRANSITION,
          "Exception type: " + e.getClass().getSimpleName() + ", Message: " + e.getMessage());

      if (errorHandler != null) {
        try {
          TState errorState = errorHandler.handle(currentState, event, context, e);
          return TransitionResult.failed(errorState, "Error handled: " + e.getMessage(), debugInfo);
        } catch (Exception handlerError) {
          DebugInfo<TState, TEvent> handlerDebugInfo = DebugInfo.of(currentState, event,
              ERROR_IN_ERROR_HANDLER,
              "Handler exception: " + handlerError.getClass().getSimpleName() + " - " + handlerError.getMessage());
          return TransitionResult.failed(currentState,
              "Error in error handler: " + handlerError.getMessage(), handlerDebugInfo);
        }
      }

      return TransitionResult.failed(currentState, "Unhandled error: " + e.getMessage(), debugInfo);
    }
  }

  @Override
  public boolean canFire(TState currentState, TEvent event, TContext context) {
    StateDefinition<TState, TEvent, TContext> stateDefinition = stateDefinitions.get(currentState);
    if (stateDefinition == null) {
      return false;
    }

    if (stateDefinition.isFinal()) {
      return false;
    }

    return findApplicableRule(stateDefinition, event, currentState, context) != null;
  }

  @Override
  public boolean isFinalState(TState state) {
    StateDefinition<TState, TEvent, TContext> stateDefinition = stateDefinitions.get(state);
    return stateDefinition != null && stateDefinition.isFinal();
  }

  @Override
  public StateMachineInfo<TState, TEvent, TContext> getInfo() {
    Set<TState> states = new HashSet<>(stateDefinitions.keySet());
    Set<TEvent> events = stateDefinitions.values().stream()
        .flatMap(def -> def.getTransitions().stream())
        .map(TransitionRule::event)
        .collect(Collectors.toSet());

    Set<TransitionInfo<TState, TEvent>> transitions = stateDefinitions.entrySet().stream()
        .flatMap(entry -> entry.getValue().getTransitions().stream()
            .filter(rule -> rule.type() != TransitionRule.TransitionType.IGNORE &&
                            rule.type() != TransitionRule.TransitionType.INTERNAL)
            .map(rule -> new TransitionInfo<>(
                entry.getKey(),
                rule.type() == TransitionRule.TransitionType.PERMIT_REENTRY ?
                    entry.getKey() : rule.targetState(),
                rule.event()
            )))
        .collect(Collectors.toSet());

    return new StateMachineInfo<>(initialState, states, events, transitions);
  }

  @Override
  public ValidationResult validate() {
    List<String> errors = new ArrayList<>();

    validateBasicConfiguration(errors);
    validateStatesAndTransitions(errors);
    validateReachability(errors);
    validateCircularDependencies(errors);

    return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
  }

  private void validateBasicConfiguration(List<String> errors) {
    if (initialState == null) {
      errors.add("Initial state is not specified");
      return;
    }

    if (!stateDefinitions.containsKey(initialState)) {
      errors.add("Initial state '" + initialState + "' is not configured");
    }

    if (stateDefinitions.isEmpty()) {
      errors.add("No states are configured");
    }
  }

  private void validateStatesAndTransitions(List<String> errors) {
    for (Map.Entry<TState, StateDefinition<TState, TEvent, TContext>> entry : stateDefinitions.entrySet()) {
      TState state = entry.getKey();
      StateDefinition<TState, TEvent, TContext> definition = entry.getValue();

      validateStateTransitions(state, definition, errors);
      validateStateDuplicateEvents(state, definition, errors);
    }
  }

  private void validateStateTransitions(
      TState state,
      StateDefinition<TState, TEvent, TContext> definition,
      List<String> errors) {
    if (definition.isFinal() && !definition.getTransitions().isEmpty()) {
      errors.add("Final state '" + state + "' should not have any transitions");
    }

    for (TransitionRule<TState, TEvent, TContext> rule : definition.getTransitions()) {
      if (rule.type() == TransitionRule.TransitionType.PERMIT) {
        TState targetState = rule.targetState();
        if (targetState != null && !stateDefinitions.containsKey(targetState)) {
          errors.add("State '" + state + "' has transition to undefined target state '" + targetState + "' for event '"
                     + rule.event() + "'");
        }
      }
    }
  }

  private void validateStateDuplicateEvents(
      TState state,
      StateDefinition<TState, TEvent, TContext> definition,
      List<String> errors) {
    Set<TEvent> seenEvents = new HashSet<>();
    for (TransitionRule<TState, TEvent, TContext> rule : definition.getTransitions()) {
      if (rule.guard() == null && seenEvents.contains(rule.event())) {
        errors.add("State '" + state + "' has multiple unconditional transitions for event '" + rule.event() + "'");
      }
      if (rule.guard() == null) {
        seenEvents.add(rule.event());
      }
    }
  }

  private void validateReachability(List<String> errors) {
    if (initialState == null || stateDefinitions.isEmpty()) {
      return;
    }

    Set<TState> reachableStates = new HashSet<>();
    Set<TState> toVisit = new HashSet<>();
    toVisit.add(initialState);

    while (!toVisit.isEmpty()) {
      TState current = toVisit.iterator().next();
      toVisit.remove(current);

      if (reachableStates.contains(current)) {
        continue;
      }

      reachableStates.add(current);
      StateDefinition<TState, TEvent, TContext> definition = stateDefinitions.get(current);

      if (definition != null) {
        for (TransitionRule<TState, TEvent, TContext> rule : definition.getTransitions()) {
          if (rule.type() == TransitionRule.TransitionType.PERMIT && rule.targetState() != null) {
            toVisit.add(rule.targetState());
          }
        }
      }
    }

    for (TState state : stateDefinitions.keySet()) {
      if (!reachableStates.contains(state)) {
        errors.add("State '" + state + "' is not reachable from initial state '" + initialState + "'");
      }
    }
  }

  private void validateCircularDependencies(List<String> errors) {
    if (initialState == null) {
      return;
    }

    Set<TState> visiting = new HashSet<>();
    Set<TState> visited = new HashSet<>();

    for (TState state : stateDefinitions.keySet()) {
      if (!visited.contains(state)) {
        if (hasCircularDependency(state, visiting, visited)) {
          errors.add("Circular dependency detected in state transitions starting from state '" + state + "'");
          break;
        }
      }
    }
  }

  private boolean hasCircularDependency(TState state, Set<TState> visiting, Set<TState> visited) {
    if (visiting.contains(state)) {
      return true;
    }

    if (visited.contains(state)) {
      return false;
    }

    visiting.add(state);
    StateDefinition<TState, TEvent, TContext> definition = stateDefinitions.get(state);

    if (definition != null) {
      for (TransitionRule<TState, TEvent, TContext> rule : definition.getTransitions()) {
        if (rule.type() == TransitionRule.TransitionType.PERMIT && rule.targetState() != null) {
          if (hasCircularDependency(rule.targetState(), visiting, visited)) {
            return true;
          }
        }
      }
    }

    visiting.remove(state);
    visited.add(state);
    return false;
  }

  private TransitionRule<TState, TEvent, TContext> findApplicableRule(
      StateDefinition<TState, TEvent, TContext> stateDefinition,
      TEvent event,
      TState currentState,
      TContext context) {

    TransitionInfo<TState, TEvent> transitionInfo = new TransitionInfo<>(currentState, null, event);

    for (TransitionRule<TState, TEvent, TContext> rule : stateDefinition.getTransitionsForEvent(event)) {
      if (rule.guard() == null || rule.guard().test(transitionInfo, context)) {
        return rule;
      }
    }
    return null;
  }

  private TransitionResult<TState> executeTransition(
      TState currentState,
      TEvent event,
      TContext context,
      TransitionRule<TState, TEvent, TContext> rule) {

    switch (rule.type()) {
      case IGNORE:
        return resultCache.getIgnored(currentState, "Event ignored");

      case INTERNAL:
        TransitionInfo<TState, TEvent> transitionInfo =
            new TransitionInfo<>(currentState, currentState, event);

        executeActions(globalTransitionActions, transitionInfo, context);
        notifyTransition(currentState, currentState, event, context);

        if (rule.action() != null) {
          rule.action().execute(transitionInfo, context);
        }
        return resultCache.getSuccess(currentState);

      case PERMIT_REENTRY:
        TState reentryFinalState = executeStateTransition(currentState, currentState, event, context);
        return resultCache.getSuccess(reentryFinalState);

      case PERMIT:
        TState targetState = rule.targetState();
        TState finalState = executeStateTransition(currentState, targetState, event, context);
        return resultCache.getSuccess(finalState);

      case AUTO_TRANSITION:
        TState autoTargetState = rule.targetState();
        TState autoFinalState = executeStateTransition(currentState, autoTargetState, null, context);
        return resultCache.getSuccess(autoFinalState);

      default:
        return TransitionResult.failed(currentState, "Unknown transition type: " + rule.type());
    }
  }

  private TState executeStateTransition(TState fromState, TState toState, TEvent event, TContext context) {
    StateDefinition<TState, TEvent, TContext> fromStateDefinition = stateDefinitions.get(fromState);
    StateDefinition<TState, TEvent, TContext> toStateDefinition = stateDefinitions.get(toState);

    TransitionInfo<TState, TEvent> transitionInfo = new TransitionInfo<>(fromState, toState, event);

    notifyStateExit(fromState, event, context);
    if (!fromState.equals(toState)) {
      executeActions(globalExitActions, transitionInfo, context);
      if (fromStateDefinition != null) {
        executeActions(fromStateDefinition.getExitActions(), transitionInfo, context);
      }
    }

    executeActions(globalTransitionActions, transitionInfo, context);
    notifyTransition(fromState, toState, event, context);

    if (!fromState.equals(toState)) {
      executeActions(globalEntryActions, transitionInfo, context);
      if (toStateDefinition != null) {
        executeActions(toStateDefinition.getEntryActions(), transitionInfo, context);
      }
    }
    notifyStateEntry(toState, event, context);

    return checkAndExecuteAutoTransitions(toState, context);
  }

  private TState checkAndExecuteAutoTransitions(TState currentState, TContext context) {
    StateDefinition<TState, TEvent, TContext> stateDefinition = stateDefinitions.get(currentState);
    if (stateDefinition == null || stateDefinition.isFinal()) {
      return currentState;
    }

    for (TransitionRule<TState, TEvent, TContext> autoRule : stateDefinition.getAutoTransitions()) {
      TransitionInfo<TState, TEvent> transitionInfo = new TransitionInfo<>(currentState, autoRule.targetState(), null);
      if (autoRule.guard() == null || autoRule.guard().test(transitionInfo, context)) {
        return executeStateTransition(currentState, autoRule.targetState(), null, context);
      }
    }

    return currentState;
  }

  private void executeActions(
      List<Action<TState, TEvent, TContext>> actions,
      TransitionInfo<TState, TEvent> transitionInfo,
      TContext context) {
    for (Action<TState, TEvent, TContext> action : actions) {
      action.execute(transitionInfo, context);
    }
  }

  private void notifyStateEntry(TState state, TEvent event, TContext context) {
    for (StateMachineListener<TState, TEvent, TContext> listener : listeners) {
      try {
        listener.onStateEntry(state, event, context);
      } catch (Exception e) {
        // Silently ignore listener exceptions to prevent them from affecting state machine operation
      }
    }
  }

  private void notifyStateExit(TState state, TEvent event, TContext context) {
    for (StateMachineListener<TState, TEvent, TContext> listener : listeners) {
      try {
        listener.onStateExit(state, event, context);
      } catch (Exception e) {
        // Silently ignore listener exceptions to prevent them from affecting state machine operation
      }
    }
  }

  private void notifyTransition(TState fromState, TState toState, TEvent event, TContext context) {
    for (StateMachineListener<TState, TEvent, TContext> listener : listeners) {
      try {
        listener.onTransition(fromState, toState, event, context);
      } catch (Exception e) {
        // Silently ignore listener exceptions to prevent them from affecting state machine operation
      }
    }
  }

  private void notifyError(TState state, TEvent event, TContext context, Exception error) {
    for (StateMachineListener<TState, TEvent, TContext> listener : listeners) {
      try {
        listener.onTransitionError(state, event, context, error);
      } catch (Exception e) {
        // Silently ignore listener exceptions to prevent them from affecting state machine operation
      }
    }
  }
}