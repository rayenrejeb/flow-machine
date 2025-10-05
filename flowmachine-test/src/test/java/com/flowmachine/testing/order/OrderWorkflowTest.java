package com.flowmachine.testing.order;

import static com.flowmachine.testing.assertion.FlowMachineAssertions.assertThatFlowResult;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.testing.FlowMachineTester;
import com.flowmachine.testing.order.model.Order;
import com.flowmachine.testing.order.model.OrderEvent;
import com.flowmachine.testing.order.model.OrderState;
import com.flowmachine.testing.result.TestResult;
import com.flowmachine.testing.result.ValidationTestResult;
import org.junit.jupiter.api.Test;

/**
 * Example tests demonstrating FlowMachineTester usage with Order workflow.
 */
class OrderWorkflowTest {

  private StateMachine<OrderState, OrderEvent, Order> orderWorkflow = new OrderWorkflow();

  @Test
  void testSuccessfulOrderWorkflow() {
    Order sampleOrder = new Order("ORDER-123", 99.99);

    TestResult<OrderState, OrderEvent, Order> result = FlowMachineTester.forWorkflow(orderWorkflow)
        .startingAt(OrderState.CREATED)
        .withContext(sampleOrder)
        .expectTransition(OrderEvent.PAY, OrderState.PAID)
        .expectTransition(OrderEvent.SHIP, OrderState.SHIPPED)
        .expectTransition(OrderEvent.DELIVER, OrderState.DELIVERED)
        .expectFinalState(OrderState.DELIVERED)
        .validateContext(Order::isPaid)
        .validateContext(Order::isShipped)
        .build()
        .runScenario();

    assertThatFlowResult(result)
        .isSuccessful()
        .hasNoErrors()
        .hasSuccessfulTransitions()
        .endedInState(OrderState.DELIVERED)
        .hasTransitionCount(3)
        .completedWithin(java.time.Duration.ofSeconds(1));
  }

  @Test
  void testOrderCancellation() {
    Order sampleOrder = new Order("ORDER-456", 149.99);

    TestResult<OrderState, OrderEvent, Order> result = FlowMachineTester.forWorkflow(orderWorkflow)
        .startingAt(OrderState.CREATED)
        .withContext(sampleOrder)
        .expectTransition(OrderEvent.PAY, OrderState.PAID)
        .expectTransition(OrderEvent.CANCEL, OrderState.CANCELLED)
        .expectFinalState(OrderState.CANCELLED)
        .build()
        .runScenario();

    assertThatFlowResult(result)
        .isSuccessful()
        .hasNoErrors()
        .endedInState(OrderState.CANCELLED);
  }

  @Test
  void testWorkflowValidation() {
    ValidationTestResult result = FlowMachineTester.forWorkflow(orderWorkflow)
        .startingAt(OrderState.CREATED)
        .withContext(new Order("TEST", 1.0))
        .expectTransition(OrderEvent.PAY, OrderState.PAID)
        .build()
        .validateConfiguration();

    assertThatFlowResult(result)
        .isValid()
        .hasNoErrors()
        .hasNoWarnings();
  }

  @Test
  void testStrictModeFailure() {
    Order sampleOrder = new Order("FAIL-TEST", 25.0);

    TestResult<OrderState, OrderEvent, Order> result = FlowMachineTester.forWorkflow(orderWorkflow)
        .startingAt(OrderState.CREATED)
        .withContext(sampleOrder)
        .expectTransition(OrderEvent.PAY, OrderState.SHIPPED) // Wrong expected state
        .build()
        .runScenario();

    assertThatFlowResult(result)
        .isNotSuccessful()
        .hasErrors()
        .hasErrorContaining("Expected transition to state");
  }

  @Test
  void testContextValidation() {
    Order sampleOrder = new Order("VALIDATION-TEST", 75.0);

    TestResult<OrderState, OrderEvent, Order> result = FlowMachineTester.forWorkflow(orderWorkflow)
        .startingAt(OrderState.CREATED)
        .withContext(sampleOrder)
        .expectTransition(OrderEvent.PAY, OrderState.PAID)
        .validateContext(order -> order.getAmount() > 100.0) // This will fail
        .build()
        .runScenario();

    assertThatFlowResult(result)
        .isNotSuccessful()
        .hasErrors()
        .hasErrorContaining("Context validation failed");
  }
}