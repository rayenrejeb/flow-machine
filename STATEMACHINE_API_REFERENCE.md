# 📖 StateMachine API Reference

**Complete reference documentation for the FlowMachine StateMachine interface**

## 🎯 Interface Overview

```java
public interface StateMachine<TState, TEvent, TContext>
```

The core interface for state machine execution and introspection. All operations are thread-safe and designed for high-performance concurrent usage.

**Type Parameters:**
- `TState` - The type of states in this state machine (typically enums)
- `TEvent` - The type of events that trigger transitions (typically enums)
- `TContext` - The type of context object that flows through the workflow (your business objects)

---

## 🚀 Execution Methods

### `.fire(currentState, event, context)`

**Primary execution method** - Fires an event and returns the resulting state.

```java
TState fire(TState currentState, TEvent event, TContext context)
```

**Purpose:** Execute a state transition by firing an event from the current state.

**Parameters:**
- `currentState` - The current state of the workflow
- `event` - The event to trigger
- `context` - The context object containing business data

**Returns:** The new state after transition (or current state if transition fails)

**Behavior:**
- ✅ Never throws exceptions
- ✅ Always returns a valid state
- ✅ Returns current state if transition fails
- ✅ Executes entry/exit actions and listeners
- ✅ Evaluates guard conditions
- ✅ Handles auto-transitions

**Examples:**

```java
// Simple transition
OrderState newState = workflow.fire(OrderState.CREATED, OrderEvent.PAY, order);

// Chaining transitions
OrderState current = OrderState.CREATED;
current = workflow.fire(current, OrderEvent.PAY, order);        // CREATED → PAID
current = workflow.fire(current, OrderEvent.SHIP, order);       // PAID → SHIPPED
current = workflow.fire(current, OrderEvent.DELIVER, order);    // SHIPPED → DELIVERED

// Safe error handling (no exceptions thrown)
OrderState result = workflow.fire(current, OrderEvent.INVALID, order);
if (result.equals(current)) {
    // Transition failed - state unchanged
    System.out.println("Transition was not successful");
}

// You can start from any state (not just initial state)
OrderState resumedState = workflow.fire(OrderState.SHIPPED, OrderEvent.DELIVER, order);
```

**When to use:**
- Standard workflow execution
- When you don't need detailed failure information
- Simple state progression
- Production code where performance matters

---

### `.fireWithResult(currentState, event, context)`

**Detailed execution method** - Returns comprehensive transition information.

```java
TransitionResult<TState> fireWithResult(TState currentState, TEvent event, TContext context)
```

**Purpose:** Execute transition and get detailed result information including success/failure status and reasons.

**Parameters:**
- `currentState` - The current state of the workflow
- `event` - The event to trigger
- `context` - The context object containing business data

**Returns:** `TransitionResult<TState>` containing:
- `state()` - The resulting state
- `wasTransitioned()` - Whether transition was successful
- `reason()` - Detailed explanation (especially for failures)
- `debugInfo()` - Debug information when available

**Behavior:**
- ✅ Never throws exceptions
- ✅ Always returns a result object
- ✅ Provides detailed failure reasons
- ✅ Same execution path as `.fire()` but with detailed reporting

**Examples:**

```java
// Basic usage with result checking
TransitionResult<OrderState> result = workflow.fireWithResult(
    OrderState.CREATED, OrderEvent.PAY, order);

if (result.wasTransitioned()) {
    OrderState newState = result.state();
    System.out.println("Successfully transitioned to: " + newState);
} else {
    System.err.println("Transition failed: " + result.reason());
    // Handle failure gracefully
}

// Error-prone approval workflow
TransitionResult<ApplicantState> approvalResult = workflow.fireWithResult(
    ApplicantState.REVIEWED, ApplicantEvent.APPROVE, applicant);

if (approvalResult.wasTransitioned()) {
    notifyApproval(applicant);
    applicant.setState(approvalResult.state());
} else {
    logger.warn("Approval failed for {}: {}",
        applicant.getId(), approvalResult.reason());
    // Continue with alternative flow
    sendForAdditionalReview(applicant);
}

// Debugging complex workflows
TransitionResult<PaymentState> paymentResult = workflow.fireWithResult(
    PaymentState.PROCESSING, PaymentEvent.AUTHORIZE, payment);

if (!paymentResult.wasTransitioned()) {
    logger.error("Payment authorization failed: {}", paymentResult.reason());
    if (paymentResult.hasDebugInfo()) {
        logger.debug("Debug info: {}", paymentResult.debugInfo());
    }
}
```

**When to use:**
- Error-prone workflows requiring detailed failure information
- Debugging and troubleshooting
- Audit trails and logging
- When you need to handle failures gracefully
- Complex conditional logic based on transition results

---

## 🔍 Validation Methods

### `.canFire(currentState, event, context)`

**Pre-execution validation** - Tests if a transition is possible without executing it.

```java
boolean canFire(TState currentState, TEvent event, TContext context)
```

**Purpose:** Check whether firing an event would result in a valid transition without actually performing the transition.

**Parameters:**
- `currentState` - The current state to test from
- `event` - The event to test
- `context` - The context object for guard evaluation

**Returns:** `true` if transition can be executed, `false` otherwise

**Behavior:**
- ✅ Never throws exceptions
- ✅ Returns `false` for null/invalid parameters
- ✅ Evaluates guard conditions
- ✅ Checks transition configuration
- ✅ No side effects (no actions executed)

**Examples:**

```java
// Basic validation before execution
if (workflow.canFire(OrderState.PAID, OrderEvent.SHIP, order)) {
    OrderState newState = workflow.fire(OrderState.PAID, OrderEvent.SHIP, order);
    System.out.println("Order shipped successfully");
} else {
    System.out.println("Cannot ship order in current state");
}

// UI button enablement
payButton.setEnabled(workflow.canFire(currentState, OrderEvent.PAY, order));
shipButton.setEnabled(workflow.canFire(currentState, OrderEvent.SHIP, order));
deliverButton.setEnabled(workflow.canFire(currentState, OrderEvent.DELIVER, order));

// Conditional workflow logic with fallbacks
if (workflow.canFire(currentState, ApplicantEvent.FAST_TRACK, applicant)) {
    // Exceptional candidate - fast track approval
    current = workflow.fire(currentState, ApplicantEvent.FAST_TRACK, applicant);
} else if (workflow.canFire(currentState, ApplicantEvent.PROCEED, applicant)) {
    // Normal processing path
    current = workflow.fire(currentState, ApplicantEvent.PROCEED, applicant);
} else {
    // No valid transitions available
    handleStuckWorkflow(applicant);
}

// Batch processing with filtering
List<Order> processableOrders = orders.stream()
    .filter(order -> workflow.canFire(order.getState(), OrderEvent.PROCESS, order))
    .collect(Collectors.toList());

processableOrders.forEach(order -> {
    OrderState newState = workflow.fire(order.getState(), OrderEvent.PROCESS, order);
    order.setState(newState);
});

// Guard condition validation
if (workflow.canFire(LoanState.APPLICATION, LoanEvent.APPROVE, loan)) {
    // All approval criteria are met (credit score, income, etc.)
    loan.setState(workflow.fire(LoanState.APPLICATION, LoanEvent.APPROVE, loan));
} else {
    // Send for manual review
    loan.setState(workflow.fire(LoanState.APPLICATION, LoanEvent.MANUAL_REVIEW, loan));
}
```

**When to use:**
- UI component enablement/disablement
- Preventing invalid operations
- Conditional business logic
- Batch operation filtering
- Workflow validation before execution
- Menu/button state management

---

## 🏁 State Inquiry Methods

### `.isFinalState(state)`

**Final state detection** - Checks if a state is terminal.

```java
boolean isFinalState(TState state)
```

**Purpose:** Determine whether the given state is a final (terminal) state marked with `.asFinal()`.

**Parameters:**
- `state` - The state to check

**Returns:** `true` if the state is final, `false` otherwise

**Behavior:**
- ✅ Never throws exceptions
- ✅ Returns `false` for null/unknown states
- ✅ Based on configuration-time `.asFinal()` declarations

**Examples:**

```java
// Workflow completion detection
if (workflow.isFinalState(currentState)) {
    System.out.println("Workflow completed in state: " + currentState);
    auditService.logCompletion(order, currentState);
    notificationService.notifyCompletion(order);
    // Clean up resources, update databases, etc.
}

// Processing loop until completion
OrderState current = OrderState.CREATED;
while (!workflow.isFinalState(current) && hasMoreEvents()) {
    OrderEvent nextEvent = determineNextEvent(current, order);
    current = workflow.fire(current, nextEvent, order);

    // Safety check to prevent infinite loops
    if (++attempts > MAX_ATTEMPTS) {
        logger.warn("Workflow processing exceeded max attempts for {}", order.getId());
        break;
    }
}

// State classification and handling
if (workflow.isFinalState(OrderState.DELIVERED)) {
    // Success terminal state
    processSuccessfulDelivery(order);
    updateCustomerSatisfaction(order);
} else if (workflow.isFinalState(OrderState.CANCELLED)) {
    // Failure terminal state
    processRefund(order);
    analyzeFailureReasons(order);
}

// Batch completion checking
List<Order> completedOrders = orders.stream()
    .filter(order -> workflow.isFinalState(order.getCurrentState()))
    .collect(Collectors.toList());

List<Order> activeOrders = orders.stream()
    .filter(order -> !workflow.isFinalState(order.getCurrentState()))
    .collect(Collectors.toList());

// Progress reporting
long totalOrders = orders.size();
long completedCount = orders.stream()
    .mapToLong(order -> workflow.isFinalState(order.getCurrentState()) ? 1 : 0)
    .sum();
double completionRate = (double) completedCount / totalOrders * 100;
System.out.printf("Progress: %.1f%% (%d/%d orders completed)%n",
    completionRate, completedCount, totalOrders);
```

**When to use:**
- Workflow completion detection
- Processing loops
- State classification
- Progress reporting
- Resource cleanup triggers
- Business logic branching

---

## 📊 Introspection Methods

### `.getInfo()`

**Structure inspection** - Returns detailed information about the state machine configuration.

```java
StateMachineInfo<TState, TEvent, TContext> getInfo()
```

**Purpose:** Provide comprehensive information about the state machine's structure including states, events, transitions, and initial state.

**Parameters:** None

**Returns:** `StateMachineInfo<TState, TEvent, TContext>` containing:
- `initialState()` - The initial state
- `states()` - Set of all configured states
- `events()` - Set of all configured events
- `transitions()` - Set of all transition information

**Behavior:**
- ✅ Never throws exceptions
- ✅ Returns immutable information
- ✅ Reflects current configuration
- ✅ Useful for debugging and documentation

**Examples:**

```java
// Basic configuration information
StateMachineInfo<OrderState, OrderEvent, Order> info = workflow.getInfo();

System.out.println("=== Workflow Configuration ===");
System.out.println("Initial state: " + info.initialState());
System.out.println("Total states: " + info.states().size());
System.out.println("Total events: " + info.events().size());
System.out.println("Total transitions: " + info.transitions().size());

// List all configured states with final state indication
System.out.println("\n=== States ===");
info.states().forEach(state -> {
    boolean isFinal = workflow.isFinalState(state);
    String status = isFinal ? " (FINAL)" : "";
    System.out.println("  " + state + status);
});

// List all configured events
System.out.println("\n=== Events ===");
info.events().forEach(event -> System.out.println("  " + event));

// List all transitions with detailed formatting
System.out.println("\n=== Transitions ===");
for (TransitionInfo<OrderState, OrderEvent> transition : info.transitions()) {
    System.out.printf("  %s --[%s]--> %s%n",
        transition.fromState(),
        transition.event(),
        transition.toState());
}

// Generate workflow documentation
public void generateWorkflowDocumentation(StateMachine<?, ?, ?> workflow) {
    var info = workflow.getInfo();

    StringBuilder doc = new StringBuilder();
    doc.append("# Workflow Documentation\n\n");
    doc.append("## Overview\n");
    doc.append("- **Initial State:** ").append(info.initialState()).append("\n");
    doc.append("- **Total States:** ").append(info.states().size()).append("\n");
    doc.append("- **Total Events:** ").append(info.events().size()).append("\n");
    doc.append("- **Total Transitions:** ").append(info.transitions().size()).append("\n\n");

    // Add state details
    doc.append("## States\n");
    info.states().forEach(state -> {
        doc.append("- **").append(state).append("**");
        if (workflow.isFinalState(state)) {
            doc.append(" _(Final State)_");
        }
        doc.append("\n");
    });

    // Add transition table
    doc.append("\n## Transitions\n");
    doc.append("| From State | Event | To State |\n");
    doc.append("|------------|-------|----------|\n");
    info.transitions().forEach(t -> {
        doc.append("| ").append(t.fromState())
           .append(" | ").append(t.event())
           .append(" | ").append(t.toState())
           .append(" |\n");
    });

    return doc.toString();
}

// Runtime analysis and debugging
public void analyzeWorkflowComplexity(StateMachine<?, ?, ?> workflow) {
    var info = workflow.getInfo();

    // Calculate metrics
    double avgTransitionsPerState = (double) info.transitions().size() / info.states().size();
    long finalStatesCount = info.states().stream()
        .filter(workflow::isFinalState)
        .count();

    System.out.println("=== Workflow Complexity Analysis ===");
    System.out.println("Average transitions per state: " + String.format("%.2f", avgTransitionsPerState));
    System.out.println("Final states ratio: " + String.format("%.1f%%",
        (double) finalStatesCount / info.states().size() * 100));

    // Find states with most outgoing transitions
    Map<Object, Long> outgoingCounts = info.transitions().stream()
        .collect(Collectors.groupingBy(
            TransitionInfo::fromState,
            Collectors.counting()));

    outgoingCounts.entrySet().stream()
        .sorted(Map.Entry.<Object, Long>comparingByValue().reversed())
        .limit(3)
        .forEach(entry -> System.out.println("State " + entry.getKey() +
            " has " + entry.getValue() + " outgoing transitions"));
}
```

**When to use:**
- Debugging and troubleshooting
- Documentation generation
- Workflow analysis and metrics
- Configuration validation
- Dynamic UI generation
- Runtime inspection

---

## ✅ Validation Methods

### `.validate()`

**Configuration validation** - Validates the state machine configuration for correctness.

```java
ValidationResult validate()
```

**Purpose:** Perform comprehensive validation of the state machine configuration to catch errors and inconsistencies.

**Parameters:** None

**Returns:** `ValidationResult` containing:
- `isValid()` - Whether configuration is valid
- `errors()` - List of validation error messages

**Validation Checks:**
- ✅ All referenced states are properly configured
- ✅ Initial state is configured
- ✅ No unreachable states exist
- ✅ Final states don't have outgoing transitions
- ✅ Configuration consistency
- ✅ Transition rule validity

**Examples:**

```java
// Basic validation during development
StateMachine<OrderState, OrderEvent, Order> workflow = createOrderWorkflow();

ValidationResult validation = workflow.validate();
if (validation.isValid()) {
    System.out.println("✅ State machine configuration is valid");
} else {
    System.err.println("❌ Configuration errors found:");
    validation.errors().forEach(error -> System.err.println("  - " + error));
    throw new IllegalStateException("Invalid state machine configuration");
}

// Unit test validation
@Test
void shouldHaveValidConfiguration() {
    StateMachine<OrderState, OrderEvent, Order> workflow = createWorkflow();
    ValidationResult result = workflow.validate();

    assertTrue(result.isValid(),
        "State machine should be valid. Errors: " + result.errors());
}

// Development-time validation with detailed error handling
public StateMachine<State, Event, Context> createWorkflow() {
    StateMachine<State, Event, Context> workflow = FlowMachine
        .<State, Event, Context>builder()
        .initialState(State.START)
        // ... configuration
        .build();

    // Always validate during development
    ValidationResult validation = workflow.validate();
    if (!validation.isValid()) {
        System.err.println("Workflow validation failed:");
        validation.errors().forEach(error -> System.err.println("  ❌ " + error));

        // In development, fail fast
        throw new IllegalStateException("Invalid workflow configuration: " +
            String.join(", ", validation.errors()));
    }

    return workflow;
}

// Production validation with graceful handling
public Optional<StateMachine<State, Event, Context>> createProductionWorkflow() {
    try {
        StateMachine<State, Event, Context> workflow = FlowMachine
            .<State, Event, Context>builder()
            // ... configuration
            .build();

        ValidationResult validation = workflow.validate();
        if (validation.isValid()) {
            logger.info("Workflow configuration validated successfully");
            return Optional.of(workflow);
        } else {
            logger.error("Workflow validation failed: {}", validation.errors());
            // In production, return empty and use fallback workflow
            return Optional.empty();
        }
    } catch (Exception e) {
        logger.error("Failed to create workflow", e);
        return Optional.empty();
    }
}

// Batch validation for multiple workflows
public Map<String, ValidationResult> validateAllWorkflows(
        Map<String, StateMachine<?, ?, ?>> workflows) {

    return workflows.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().validate()
        ));
}

// Validation reporting
public void generateValidationReport(Map<String, ValidationResult> results) {
    System.out.println("=== Workflow Validation Report ===");

    long validCount = results.values().stream()
        .mapToLong(result -> result.isValid() ? 1 : 0)
        .sum();

    System.out.printf("Valid workflows: %d/%d%n", validCount, results.size());

    results.entrySet().stream()
        .filter(entry -> !entry.getValue().isValid())
        .forEach(entry -> {
            System.out.println("\n❌ " + entry.getKey() + " - INVALID:");
            entry.getValue().errors().forEach(error ->
                System.out.println("   • " + error));
        });
}
```

**When to use:**
- Development-time configuration checking
- Unit and integration tests
- Production deployment validation
- Configuration change verification
- Debugging workflow issues
- Quality assurance processes

---

## 🎯 Usage Patterns and Best Practices

### **Pattern 1: Simple State Progression**
```java
// Basic workflow advancement
OrderState current = OrderState.CREATED;
current = workflow.fire(current, OrderEvent.PAY, order);
current = workflow.fire(current, OrderEvent.SHIP, order);
current = workflow.fire(current, OrderEvent.DELIVER, order);
```

### **Pattern 2: Conditional Execution with Validation**
```java
// Check before execute
if (workflow.canFire(currentState, OrderEvent.PROCESS, order)) {
    OrderState newState = workflow.fire(currentState, OrderEvent.PROCESS, order);
    order.setState(newState);
} else {
    handleInvalidTransition(order, OrderEvent.PROCESS);
}
```

### **Pattern 3: Error-Safe Execution with Details**
```java
// Get detailed results for error handling
TransitionResult<PaymentState> result = workflow.fireWithResult(
    currentState, PaymentEvent.AUTHORIZE, payment);

if (result.wasTransitioned()) {
    handleSuccessfulPayment(payment, result.state());
} else {
    handleFailedPayment(payment, result.reason());
}
```

### **Pattern 4: Workflow Completion Detection**
```java
// Process until completion
while (!workflow.isFinalState(currentState)) {
    Event nextEvent = determineNextEvent(currentState, context);
    currentState = workflow.fire(currentState, nextEvent, context);
}
```

### **Pattern 5: Batch Processing with Filtering**
```java
// Process only valid items
List<Order> processableOrders = orders.stream()
    .filter(order -> workflow.canFire(order.getState(), OrderEvent.PROCESS, order))
    .collect(Collectors.toList());

processableOrders.forEach(order -> {
    OrderState newState = workflow.fire(order.getState(), OrderEvent.PROCESS, order);
    order.setState(newState);
});
```

---

## 🚀 Performance Characteristics

- **Thread-Safe**: All methods are thread-safe and can be used in concurrent environments
- **Memory Efficient**: Minimal object allocation during execution
- **Fast Execution**: O(1) state transitions with guard evaluation
- **No Side Effects**: Validation methods (`canFire`, `isFinalState`) have no side effects
- **Immutable Results**: All returned objects are immutable and safe to cache

---

## 🔗 Related Documentation

- **[Complete FlowMachine Documentation](README.md)** - Full library documentation
- **[Builder API Reference](BUILDER_API_REFERENCE.md)** - State machine configuration
- **[Examples Repository](flowmachine-examples/)** - Real-world usage examples