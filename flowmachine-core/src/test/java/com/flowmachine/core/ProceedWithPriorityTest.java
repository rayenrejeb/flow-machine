package com.flowmachine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.flowmachine.core.api.StateMachine;
import org.junit.jupiter.api.Test;

class ProceedWithPriorityTest {

  enum State {
    START, PROCESSING, VALIDATION, COMPLETED, ERROR_HANDLING, CANCELLED, RETRY_QUEUE
  }

  enum Event {
    PROCEED, CANCEL, RESET
  }

  static class ProcessingContext {

    private boolean isValid = true;
    private boolean hasErrors = false;
    private int priority = 1;
    private boolean requiresValidation = false;

    ProcessingContext() {
    }

    ProcessingContext(boolean isValid, boolean hasErrors, int priority, boolean requiresValidation) {
      this.isValid = isValid;
      this.hasErrors = hasErrors;
      this.priority = priority;
      this.requiresValidation = requiresValidation;
    }

    // Getters and setters
    public boolean isValid() {
      return isValid;
    }

    public void setValid(boolean valid) {
      isValid = valid;
    }

    public boolean hasErrors() {
      return hasErrors;
    }

    public void setHasErrors(boolean hasErrors) {
      this.hasErrors = hasErrors;
    }

    public int getPriority() {
      return priority;
    }

    public void setPriority(int priority) {
      this.priority = priority;
    }

    public boolean requiresValidation() {
      return requiresValidation;
    }

    public void setRequiresValidation(boolean requiresValidation) {
      this.requiresValidation = requiresValidation;
    }

    @Override
    public String toString() {
      return String.format("ProcessingContext{valid=%s, errors=%s, priority=%d, needsValidation=%s}",
          isValid, hasErrors, priority, requiresValidation);
    }
  }

  private StateMachine<State, Event, ProcessingContext> createPriorityStateMachine() {
    return FlowMachine.<State, Event, ProcessingContext>builder()
        .initialState(State.START)

        .configure(State.START)
        .permit(Event.PROCEED, State.PROCESSING)
        .permit(Event.CANCEL, State.CANCELLED)
        .and()

        .configure(State.PROCESSING)
        // Priority 1: Critical errors go to error handling immediately
        .permitIf(Event.PROCEED, State.ERROR_HANDLING,
            (transition, context) -> context.hasErrors())

        // Priority 2: High priority + valid items go directly to completion
        .permitIf(Event.PROCEED, State.COMPLETED,
            (transition, context) -> context.getPriority() >= 5 && context.isValid())

        // Priority 3: Items requiring validation go to validation first
        .permitIf(Event.PROCEED, State.VALIDATION,
            (transition, context) -> context.requiresValidation())

        // Priority 4: Invalid items with low priority go to retry queue
        .permitIf(Event.PROCEED, State.RETRY_QUEUE,
            (transition, context) -> !context.isValid() && context.getPriority() < 3)

        // Priority 5: Default - valid items with normal priority go to completion
        .permit(Event.PROCEED, State.COMPLETED)

        // Alternative events
        .permit(Event.CANCEL, State.CANCELLED)
        .permit(Event.RESET, State.START)
        .and()

        .configure(State.VALIDATION)
        .permitIf(Event.PROCEED, State.COMPLETED,
            (transition, context) -> context.isValid())
        .permitIf(Event.PROCEED, State.ERROR_HANDLING,
            (transition, context) -> !context.isValid())
        .permit(Event.CANCEL, State.CANCELLED)
        .and()

        .configure(State.COMPLETED)
        .asFinal()
        .and()

        .configure(State.ERROR_HANDLING)
        .permit(Event.PROCEED, State.RETRY_QUEUE)
        .permit(Event.RESET, State.START)
        .permit(Event.CANCEL, State.CANCELLED)
        .and()

        .configure(State.RETRY_QUEUE)
        .permit(Event.PROCEED, State.PROCESSING)
        .permit(Event.CANCEL, State.CANCELLED)
        .and()

        .configure(State.CANCELLED)
        .asFinal()
        .and()

        .build();
  }

  @Test
  void shouldRouteToCriticalErrorHandlingFirst() {
    StateMachine<State, Event, ProcessingContext> machine = createPriorityStateMachine();

    // Even with high priority, errors take precedence
    ProcessingContext context = new ProcessingContext(true, true, 10, false);

    State result = machine.fire(State.START, Event.PROCEED, context);
    assertEquals(State.PROCESSING, result);

    result = machine.fire(State.PROCESSING, Event.PROCEED, context);
    assertEquals(State.ERROR_HANDLING, result); // Errors have highest priority
  }

  @Test
  void shouldRouteHighPriorityValidItemsDirectlyToCompletion() {
    StateMachine<State, Event, ProcessingContext> machine = createPriorityStateMachine();

    ProcessingContext context = new ProcessingContext(true, false, 7, false);

    State result = machine.fire(State.START, Event.PROCEED, context);
    assertEquals(State.PROCESSING, result);

    result = machine.fire(State.PROCESSING, Event.PROCEED, context);
    assertEquals(State.COMPLETED, result); // High priority + valid = direct completion
  }

  @Test
  void shouldRouteToValidationWhenRequired() {
    StateMachine<State, Event, ProcessingContext> machine = createPriorityStateMachine();

    ProcessingContext context = new ProcessingContext(true, false, 3, true);

    State result = machine.fire(State.START, Event.PROCEED, context);
    assertEquals(State.PROCESSING, result);

    result = machine.fire(State.PROCESSING, Event.PROCEED, context);
    assertEquals(State.VALIDATION, result); // Requires validation
  }

  @Test
  void shouldRouteLowPriorityInvalidItemsToRetryQueue() {
    StateMachine<State, Event, ProcessingContext> machine = createPriorityStateMachine();

    ProcessingContext context = new ProcessingContext(false, false, 2, false);

    State result = machine.fire(State.START, Event.PROCEED, context);
    assertEquals(State.PROCESSING, result);

    result = machine.fire(State.PROCESSING, Event.PROCEED, context);
    assertEquals(State.RETRY_QUEUE, result); // Low priority + invalid = retry queue
  }

  @Test
  void shouldUseDefaultRouteForNormalValidItems() {
    StateMachine<State, Event, ProcessingContext> machine = createPriorityStateMachine();

    ProcessingContext context = new ProcessingContext(true, false, 3, false);

    State result = machine.fire(State.START, Event.PROCEED, context);
    assertEquals(State.PROCESSING, result);

    result = machine.fire(State.PROCESSING, Event.PROCEED, context);
    assertEquals(State.COMPLETED, result); // Default route for normal valid items
  }

  @Test
  void shouldHandleValidationWorkflow() {
    StateMachine<State, Event, ProcessingContext> machine = createPriorityStateMachine();

    ProcessingContext validContext = new ProcessingContext(true, false, 3, true);

    // Go through validation for items that require it
    State result = machine.fire(State.START, Event.PROCEED, validContext);
    assertEquals(State.PROCESSING, result);

    result = machine.fire(State.PROCESSING, Event.PROCEED, validContext);
    assertEquals(State.VALIDATION, result);

    result = machine.fire(State.VALIDATION, Event.PROCEED, validContext);
    assertEquals(State.COMPLETED, result); // Valid items complete after validation

    // Test validation failure
    ProcessingContext invalidContext = new ProcessingContext(false, false, 3, true);
    result = machine.fire(State.VALIDATION, Event.PROCEED, invalidContext);
    assertEquals(State.ERROR_HANDLING, result); // Invalid items go to error handling
  }

  @Test
  void shouldHandleRetryWorkflow() {
    StateMachine<State, Event, ProcessingContext> machine = createPriorityStateMachine();

    ProcessingContext context = new ProcessingContext(false, false, 1, false);

    // Low priority invalid item goes to retry queue
    State result = machine.fire(State.START, Event.PROCEED, context);
    assertEquals(State.PROCESSING, result);

    result = machine.fire(State.PROCESSING, Event.PROCEED, context);
    assertEquals(State.RETRY_QUEUE, result);

    // From retry queue, can go back to processing
    result = machine.fire(State.RETRY_QUEUE, Event.PROCEED, context);
    assertEquals(State.PROCESSING, result);
  }

  @Test
  void shouldHandleErrorRecoveryWorkflow() {
    StateMachine<State, Event, ProcessingContext> machine = createPriorityStateMachine();

    ProcessingContext context = new ProcessingContext(true, true, 5, false);

    // Error items go to error handling
    State result = machine.fire(State.START, Event.PROCEED, context);
    assertEquals(State.PROCESSING, result);

    result = machine.fire(State.PROCESSING, Event.PROCEED, context);
    assertEquals(State.ERROR_HANDLING, result);

    // From error handling, can proceed to retry queue
    result = machine.fire(State.ERROR_HANDLING, Event.PROCEED, context);
    assertEquals(State.RETRY_QUEUE, result);

    // Or reset to start
    result = machine.fire(State.ERROR_HANDLING, Event.RESET, context);
    assertEquals(State.START, result);
  }

  @Test
  void shouldHandleCancellationFromAnyState() {
    StateMachine<State, Event, ProcessingContext> machine = createPriorityStateMachine();

    ProcessingContext context = new ProcessingContext();

    // Can cancel from START
    State result = machine.fire(State.START, Event.CANCEL, context);
    assertEquals(State.CANCELLED, result);

    // Can cancel from PROCESSING
    result = machine.fire(State.PROCESSING, Event.CANCEL, context);
    assertEquals(State.CANCELLED, result);

    // Can cancel from VALIDATION
    result = machine.fire(State.VALIDATION, Event.CANCEL, context);
    assertEquals(State.CANCELLED, result);
  }

  @Test
  void shouldDemonstrateCompleteWorkflow() {
    StateMachine<State, Event, ProcessingContext> machine = createPriorityStateMachine();

    // Scenario 1: High priority item - direct path
    ProcessingContext highPriority = new ProcessingContext(true, false, 8, false);
    State state = State.START;
    state = machine.fire(state, Event.PROCEED, highPriority); // START -> PROCESSING
    state = machine.fire(state, Event.PROCEED, highPriority); // PROCESSING -> COMPLETED
    assertEquals(State.COMPLETED, state);

    // Scenario 2: Normal item requiring validation
    ProcessingContext needsValidation = new ProcessingContext(true, false, 3, true);
    state = State.START;
    state = machine.fire(state, Event.PROCEED, needsValidation); // START -> PROCESSING
    state = machine.fire(state, Event.PROCEED, needsValidation); // PROCESSING -> VALIDATION
    state = machine.fire(state, Event.PROCEED, needsValidation); // VALIDATION -> COMPLETED
    assertEquals(State.COMPLETED, state);

    // Scenario 3: Error recovery
    ProcessingContext errorItem = new ProcessingContext(true, true, 4, false);
    state = State.START;
    state = machine.fire(state, Event.PROCEED, errorItem); // START -> PROCESSING
    state = machine.fire(state, Event.PROCEED, errorItem); // PROCESSING -> ERROR_HANDLING

    // Fix the error and retry
    errorItem.setHasErrors(false);
    state = machine.fire(state, Event.PROCEED, errorItem); // ERROR_HANDLING -> RETRY_QUEUE
    state = machine.fire(state, Event.PROCEED, errorItem); // RETRY_QUEUE -> PROCESSING
    state = machine.fire(state, Event.PROCEED, errorItem); // PROCESSING -> COMPLETED (now valid)
    assertEquals(State.COMPLETED, state);
  }

  @Test
  void shouldShowPriorityOrderMatters() {
    StateMachine<State, Event, ProcessingContext> machine = createPriorityStateMachine();

    // This context matches multiple conditions:
    // - hasErrors() = true (Priority 1)
    // - getPriority() >= 5 && isValid() = true (Priority 2)
    // Since Priority 1 is checked first, it should go to ERROR_HANDLING
    ProcessingContext multipleMatches = new ProcessingContext(true, true, 10, false);

    State result = machine.fire(State.START, Event.PROCEED, multipleMatches);
    assertEquals(State.PROCESSING, result);

    result = machine.fire(State.PROCESSING, Event.PROCEED, multipleMatches);
    assertEquals(State.ERROR_HANDLING, result); // First matching condition wins
  }

}