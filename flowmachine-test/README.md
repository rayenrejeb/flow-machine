# FlowMachine Test Framework

A production-ready test framework for FlowMachine workflows, providing comprehensive utilities for validating state machine behavior, including scenario testing, transition validation, and guard condition verification.

## Features

- **Fluent Test API**: Builder pattern for readable and maintainable tests
- **Comprehensive Assertions**: AssertJ-style assertions for all test results
- **Scenario Testing**: Execute predefined transition sequences with validation
- **Guard Isolation Testing**: Verify guard conditions don't have side effects
- **Configuration Validation**: Validate state machine setup before execution
- **Transition Analysis**: Test all possible transitions from a given state

## Quick Start

### 1. Add Dependency

Add the FlowMachine Test Framework to your project:

```xml
<dependency>
    <groupId>com.flowmachine</groupId>
    <artifactId>flowmachine-test</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

### 2. Basic Usage

```java
import static com.flowmachine.testing.assertion.FlowMachineAssertions.assertThatFlowResult;
import static org.assertj.core.api.Assertions.assertThat;

@Test
void testOrderWorkflow() {
    // Create your state machine
    StateMachine<OrderState, OrderEvent, Order> orderWorkflow = createOrderWorkflow();
    Order sampleOrder = new Order("ORDER-123", 99.99);

    // Test the workflow
    TestResult<OrderState, OrderEvent, Order> result = FlowMachineTester.forWorkflow(orderWorkflow)
        .startingAt(OrderState.CREATED)
        .withContext(sampleOrder)
        .expectTransition(OrderEvent.PAY, OrderState.PAID)
        .expectTransition(OrderEvent.SHIP, OrderState.SHIPPED)
        .expectTransition(OrderEvent.DELIVER, OrderState.DELIVERED)
        .expectFinalState(OrderState.DELIVERED)
        .runScenario();

    // Assert the results
    assertThatFlowResult(result)
        .isSuccessful()
        .hasNoErrors()
        .hasSuccessfulTransitions()
        .endedInState(OrderState.DELIVERED);
}
```

### 3. Context Actions Between Transitions

Execute actions on the context between transitions to simulate complex scenarios:

```java
@Test
void testShoppingWorkflowWithContextActions() {
    ShoppingCart cart = new ShoppingCart();

    TestResult<ShoppingState, ShoppingEvent, ShoppingCart> result = FlowMachineTester.forWorkflow(shoppingWorkflow)
        .startingAt(ShoppingState.BROWSING)
        .withContext(cart)

        // Add items to cart before transitioning
        .executeAction(c -> c.addItem(29.99), "Add first item")
        .executeAction(c -> c.addItem(15.50), "Add second item")
        .expectTransition(ShoppingEvent.ADD_TO_CART, ShoppingState.CART_FILLED)

        // Apply discount before checkout
        .executeAction(c -> c.applyDiscount(10.0), "Apply 10% discount")
        .expectTransition(ShoppingEvent.PROCEED_TO_CHECKOUT, ShoppingState.CHECKOUT)

        .expectTransition(ShoppingEvent.PAY, ShoppingState.PAID)
        .expectFinalState(ShoppingState.PAID)

        .validateContext(c -> c.getItemCount() == 2)
        .validateContext(c -> c.isDiscountApplied())
        .build()
        .runScenario();

    assertThatFlowResult(result).isSuccessful().hasNoErrors();
}
```

## Core Components

### FlowMachineTester

The main entry point for testing workflows:

```java
FlowMachineTester<TState, TEvent, TContext> tester = FlowMachineTester.forWorkflow(stateMachine)
    .startingAt(initialState)
    .withContext(context)
    .expectTransition(event, targetState)
    .expectFinalState(finalState)
    .build();
```

### Test Configuration Options

#### Timeouts
```java
.withTimeout(5, TimeUnit.SECONDS)
```

#### Context Validation
```java
.validateContext(order -> order.isPaid())
.validateContext(order -> order.getAmount() > 0)
```

#### Context Actions
```java
.executeAction(context -> context.modifyData())  // Without description
.executeAction(context -> context.applyDiscount(10.0), "Apply 10% discount")  // With description
```

## Testing Capabilities

### 1. Scenario Testing

Test predefined sequences of transitions:

```java
TestResult<OrderState, OrderEvent, Order> result = tester.runScenario();
assertThatFlowResult(result).isSuccessful().hasNoErrors();
```

### 2. Configuration Validation

Validate state machine setup:

```java
ValidationTestResult result = tester.validateConfiguration();
assertThatFlowResult(result).isValid().hasNoErrors();
```

### 3. Transition Analysis

Test all possible transitions from a state:

```java
TransitionTestResult<OrderState, OrderEvent, Order> result =
    tester.testAllTransitions(OrderState.PAID, context);
assertThatFlowResult(result)
    .allowsEvents(OrderEvent.SHIP, OrderEvent.CANCEL)
    .blocksEvents(OrderEvent.PAY, OrderEvent.DELIVER);
```

## Assertions

The framework provides comprehensive AssertJ-style assertions using `assertThatFlowResult()` to avoid naming conflicts with standard AssertJ assertions:

### TestResult Assertions
```java
assertThatFlowResult(result)
    .isSuccessful()
    .hasNoErrors()
    .hasSuccessfulTransitions()
    .endedInState(expectedState)
    .completedWithin(Duration.ofSeconds(1))
    .hasTransitionCount(3);
```

### ValidationTestResult Assertions
```java
assertThatFlowResult(validationResult)
    .isValid()
    .hasNoErrors()
    .hasNoWarnings();
```


## Advanced Examples

### Testing Error Conditions

```java
@Test
void testInvalidTransition() {
    TestResult<OrderState, OrderEvent, Order> result = FlowMachineTester.forWorkflow(orderWorkflow)
        .startingAt(OrderState.CREATED)
        .withContext(sampleOrder)
        .expectTransition(OrderEvent.SHIP, OrderState.SHIPPED) // Invalid: can't ship before paying
        .build()
        .runScenario();

    assertThatFlowResult(result)
        .isNotSuccessful()
        .hasErrors()
        .hasErrorContaining("Expected transition to state");
}
```

### Context Validation

```java
@Test
void testContextModification() {
    TestResult<OrderState, OrderEvent, Order> result = FlowMachineTester.forWorkflow(orderWorkflow)
        .startingAt(OrderState.CREATED)
        .withContext(new Order("TEST", 100.0))
        .expectTransition(OrderEvent.PAY, OrderState.PAID)
        .validateContext(order -> order.isPaid()) // Verify action modified context
        .validateContext(order -> order.getAmount() == 100.0) // Verify amount unchanged
        .build()
        .runScenario();

    assertThatFlowResult(result).isSuccessful().hasNoErrors();
}
```


## Best Practices

1. **Use Descriptive Test Names**: Make test intent clear from the method name
2. **Test Both Success and Failure Paths**: Validate expected behavior and error conditions
3. **Validate Context Changes**: Ensure actions modify context as expected
4. **Test Guard Isolation**: Verify guards are pure functions without side effects
5. **Combine Multiple Validation Types**: Use configuration validation alongside scenario testing
6. **Use Context Actions for Complex Scenarios**: Execute actions between transitions to:
   - Simulate external system interactions
   - Modify context state between transitions
   - Test edge cases where context changes affect subsequent transitions
   - Validate business logic that operates on context data

## Integration with Testing Frameworks

### JUnit 5

The framework is designed to work seamlessly with JUnit 5:

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderWorkflowTest {

    private StateMachine<OrderState, OrderEvent, Order> workflow;

    @BeforeAll
    void setupWorkflow() {
        workflow = createOrderWorkflow();
    }

    @ParameterizedTest
    @ValueSource(doubles = {10.0, 50.0, 100.0, 500.0})
    void testOrdersWithDifferentAmounts(double amount) {
        TestResult<OrderState, OrderEvent, Order> result = FlowMachineTester.forWorkflow(workflow)
            .startingAt(OrderState.CREATED)
            .withContext(new Order("TEST", amount))
            .expectTransition(OrderEvent.PAY, OrderState.PAID)
            .validateContext(order -> order.getAmount() == amount)
            .build()
            .runScenario();

        assertThatFlowResult(result).isSuccessful();
    }
}
```

### Mockito Integration

For testing workflows with external dependencies:

```java
@Test
void testWorkflowWithMockedServices() {
    PaymentService mockPaymentService = mock(PaymentService.class);
    when(mockPaymentService.processPayment(any())).thenReturn(true);

    StateMachine<OrderState, OrderEvent, OrderContext> workflow =
        createWorkflowWithPaymentService(mockPaymentService);

    TestResult<OrderState, OrderEvent, OrderContext> result =
        FlowMachineTester.forWorkflow(workflow)
            .startingAt(OrderState.CREATED)
            .withContext(new OrderContext(mockPaymentService))
            .expectTransition(OrderEvent.PAY, OrderState.PAID)
            .build()
            .runScenario();

    assertThatFlowResult(result).isSuccessful();
    verify(mockPaymentService).processPayment(any());
}
```
