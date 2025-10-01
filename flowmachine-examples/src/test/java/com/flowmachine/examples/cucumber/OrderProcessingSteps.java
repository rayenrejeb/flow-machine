package com.flowmachine.examples.cucumber;

import com.flowmachine.core.FlowMachine;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.model.StateMachineInfo;
import com.flowmachine.core.model.TransitionResult;
import com.flowmachine.core.model.ValidationResult;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions for Order Processing workflow Cucumber tests.
 */
public class OrderProcessingSteps {

    // Order states and events
    public enum OrderState { CREATED, PAID, SHIPPED, DELIVERED, CANCELLED }
    public enum OrderEvent { PAY, SHIP, DELIVER, CANCEL }

    // Order context object
    public static class Order {
        private final String id;
        private final BigDecimal amount;
        private OrderState currentState;

        public Order(String id, BigDecimal amount) {
            this.id = id;
            this.amount = amount;
            this.currentState = OrderState.CREATED;
        }

        public String getId() { return id; }
        public BigDecimal getAmount() { return amount; }
        public OrderState getCurrentState() { return currentState; }
        public void setCurrentState(OrderState state) { this.currentState = state; }

        @Override
        public String toString() {
            return String.format("Order{id='%s', amount=%s, state=%s}", id, amount, currentState);
        }
    }

    // Test context
    private StateMachine<OrderState, OrderEvent, Order> workflow;
    private Order currentOrder;
    private TransitionResult<OrderState> lastTransitionResult;
    private final Map<String, Order> orders = new HashMap<>();

    @Given("I have an order processing workflow")
    public void i_have_an_order_processing_workflow() {
        workflow = FlowMachine.<OrderState, OrderEvent, Order>builder()
            .initialState(OrderState.CREATED)

            .configure(OrderState.CREATED)
                .permit(OrderEvent.PAY, OrderState.PAID)
                .permit(OrderEvent.CANCEL, OrderState.CANCELLED)
                .onEntry((t, order) -> System.out.println("Order " + order.getId() + " created"))
            .and()

            .configure(OrderState.PAID)
                .permit(OrderEvent.SHIP, OrderState.SHIPPED)
                .permit(OrderEvent.CANCEL, OrderState.CANCELLED)
                .onEntry((t, order) -> System.out.println("Order " + order.getId() + " paid: $" + order.getAmount()))
            .and()

            .configure(OrderState.SHIPPED)
                .permit(OrderEvent.DELIVER, OrderState.DELIVERED)
                .permit(OrderEvent.CANCEL, OrderState.CANCELLED)
                .onEntry((t, order) -> System.out.println("Order " + order.getId() + " shipped"))
            .and()

            .configure(OrderState.DELIVERED)
                .asFinal()
                .onEntry((t, order) -> System.out.println("Order " + order.getId() + " delivered successfully"))
            .and()

            .configure(OrderState.CANCELLED)
                .asFinal()
                .onEntry((t, order) -> System.out.println("Order " + order.getId() + " cancelled"))
            .and()

            .build();
    }

    @Given("the workflow has the following states:")
    public void the_workflow_has_the_following_states(DataTable dataTable) {
        List<String> expectedStates = dataTable.asList();
        StateMachineInfo<OrderState, OrderEvent, Order> info = workflow.getInfo();

        for (String expectedState : expectedStates) {
            OrderState state = OrderState.valueOf(expectedState);
            assertTrue(info.states().contains(state),
                "Workflow should contain state: " + expectedState);
        }
    }

    @Given("the workflow has the following events:")
    public void the_workflow_has_the_following_events(DataTable dataTable) {
        List<String> expectedEvents = dataTable.asList();
        StateMachineInfo<OrderState, OrderEvent, Order> info = workflow.getInfo();

        for (String expectedEvent : expectedEvents) {
            OrderEvent event = OrderEvent.valueOf(expectedEvent);
            assertTrue(info.events().contains(event),
                "Workflow should contain event: " + expectedEvent);
        }
    }

    @Given("I have an order {string} with amount {double}")
    public void i_have_an_order_with_amount(String orderId, double amount) {
        currentOrder = new Order(orderId, BigDecimal.valueOf(amount));
        orders.put(orderId, currentOrder);
    }

    @Given("the order is in {string} state")
    public void the_order_is_in_state(String stateName) {
        OrderState state = OrderState.valueOf(stateName);
        currentOrder.setCurrentState(state);
    }

    @When("I fire {string} event")
    public void i_fire_event(String eventName) {
        OrderEvent event = OrderEvent.valueOf(eventName);
        OrderState newState = workflow.fire(currentOrder.getCurrentState(), event, currentOrder);
        currentOrder.setCurrentState(newState);
        lastTransitionResult = TransitionResult.success(newState);
    }

    @When("I try to fire {string} event")
    public void i_try_to_fire_event(String eventName) {
        OrderEvent event = OrderEvent.valueOf(eventName);
        lastTransitionResult = workflow.fireWithResult(currentOrder.getCurrentState(), event, currentOrder);
        if (lastTransitionResult.wasTransitioned()) {
            currentOrder.setCurrentState(lastTransitionResult.state());
        }
    }

    @When("I try to fire {string} event without payment")
    public void i_try_to_fire_event_without_payment(String eventName) {
        i_try_to_fire_event(eventName);
    }

    @Then("the order should transition to {string} state")
    public void the_order_should_transition_to_state(String expectedStateName) {
        OrderState expectedState = OrderState.valueOf(expectedStateName);
        assertEquals(expectedState, currentOrder.getCurrentState(),
            "Order should be in " + expectedStateName + " state");
    }

    @Then("the order should remain in {string} state")
    public void the_order_should_remain_in_state(String expectedStateName) {
        OrderState expectedState = OrderState.valueOf(expectedStateName);
        assertEquals(expectedState, currentOrder.getCurrentState(),
            "Order should remain in " + expectedStateName + " state");
    }

    @Then("the order should be in a final state")
    public void the_order_should_be_in_a_final_state() {
        assertTrue(workflow.isFinalState(currentOrder.getCurrentState()),
            "Order should be in a final state");
    }

    @Then("I should be able to fire {string} event")
    public void i_should_be_able_to_fire_event(String eventName) {
        OrderEvent event = OrderEvent.valueOf(eventName);
        assertTrue(workflow.canFire(currentOrder.getCurrentState(), event, currentOrder),
            "Should be able to fire " + eventName + " event");
    }

    @Then("I should not be able to fire {string} event")
    public void i_should_not_be_able_to_fire_event(String eventName) {
        OrderEvent event = OrderEvent.valueOf(eventName);
        assertFalse(workflow.canFire(currentOrder.getCurrentState(), event, currentOrder),
            "Should not be able to fire " + eventName + " event");
    }

    @Then("I should not be able to fire {string} event again")
    public void i_should_not_be_able_to_fire_event_again(String eventName) {
        i_should_not_be_able_to_fire_event(eventName);
    }

    @Then("the transition should be unsuccessful")
    public void the_transition_should_be_unsuccessful() {
        assertFalse(lastTransitionResult.wasTransitioned(),
            "Transition should be unsuccessful");
    }

    @Then("I should get an error message containing {string}")
    public void i_should_get_an_error_message_containing(String expectedMessage) {
        assertNotNull(lastTransitionResult, "Should have a transition result");
        assertFalse(lastTransitionResult.wasTransitioned(), "Transition should have failed");
        assertTrue(lastTransitionResult.reason().contains(expectedMessage),
            "Error message should contain: " + expectedMessage + ", but was: " + lastTransitionResult.reason());
    }

    @When("I validate the workflow configuration")
    public void i_validate_the_workflow_configuration() {
        ValidationResult validation = workflow.validate();
        assertTrue(validation.isValid(),
            "Workflow configuration should be valid. Errors: " + validation.errors());
    }

    @Then("the configuration should be valid")
    public void the_configuration_should_be_valid() {
        ValidationResult validation = workflow.validate();
        assertTrue(validation.isValid(),
            "Configuration should be valid. Errors: " + validation.errors());
    }

    @Then("it should have {int} states")
    public void it_should_have_states(int expectedStateCount) {
        StateMachineInfo<OrderState, OrderEvent, Order> info = workflow.getInfo();
        assertEquals(expectedStateCount, info.states().size(),
            "Should have " + expectedStateCount + " states");
    }

    @Then("it should have {int} events")
    public void it_should_have_events(int expectedEventCount) {
        StateMachineInfo<OrderState, OrderEvent, Order> info = workflow.getInfo();
        assertEquals(expectedEventCount, info.events().size(),
            "Should have " + expectedEventCount + " events");
    }

    @Then("the initial order state should be {string}")
    public void the_initial_order_state_should_be(String expectedInitialState) {
        StateMachineInfo<OrderState, OrderEvent, Order> info = workflow.getInfo();
        assertEquals(OrderState.valueOf(expectedInitialState), info.initialState(),
            "Initial state should be " + expectedInitialState);
    }

    @Given("I have the following orders:")
    public void i_have_the_following_orders(DataTable dataTable) {
        List<Map<String, String>> orderData = dataTable.asMaps();
        orders.clear();

        for (Map<String, String> row : orderData) {
            String orderId = row.get("order_id");
            String state = row.get("state");
            double amount = Double.parseDouble(row.get("amount"));

            Order order = new Order(orderId, BigDecimal.valueOf(amount));
            order.setCurrentState(OrderState.valueOf(state));
            orders.put(orderId, order);
        }
    }

    @When("I process all orders that can be shipped")
    public void i_process_all_orders_that_can_be_shipped() {
        for (Order order : orders.values()) {
            if (workflow.canFire(order.getCurrentState(), OrderEvent.SHIP, order)) {
                OrderState newState = workflow.fire(order.getCurrentState(), OrderEvent.SHIP, order);
                order.setCurrentState(newState);
            }
        }
    }

    @Then("order {string} should transition to {string} state")
    public void order_should_transition_to_state(String orderId, String expectedState) {
        Order order = orders.get(orderId);
        assertNotNull(order, "Order " + orderId + " should exist");
        assertEquals(OrderState.valueOf(expectedState), order.getCurrentState(),
            "Order " + orderId + " should be in " + expectedState + " state");
    }

    @Then("order {string} should remain in {string} state")
    public void order_should_remain_in_state(String orderId, String expectedState) {
        order_should_transition_to_state(orderId, expectedState);
    }

    @When("I get workflow information")
    public void i_get_workflow_information() {
        // This step is for setup - the actual assertions are in Then steps
        assertNotNull(workflow, "Workflow should be initialized");
    }

    @Then("the workflow should contain state {string} as initial state")
    public void the_workflow_should_contain_state_as_initial_state(String stateName) {
        StateMachineInfo<OrderState, OrderEvent, Order> info = workflow.getInfo();
        assertEquals(OrderState.valueOf(stateName), info.initialState(),
            stateName + " should be the initial state");
    }

    @Then("the workflow should contain state {string} as final state")
    public void the_workflow_should_contain_state_as_final_state(String stateName) {
        OrderState state = OrderState.valueOf(stateName);
        assertTrue(workflow.isFinalState(state),
            stateName + " should be a final state");
    }

    @Then("the workflow should have transition from {string} to {string} on {string} event")
    public void the_workflow_should_have_transition(String fromState, String toState, String eventName) {
        StateMachineInfo<OrderState, OrderEvent, Order> info = workflow.getInfo();
        OrderState from = OrderState.valueOf(fromState);
        OrderState to = OrderState.valueOf(toState);
        OrderEvent event = OrderEvent.valueOf(eventName);

        boolean transitionExists = info.transitions().stream()
            .anyMatch(t -> t.fromState().equals(from) &&
                          t.toState().equals(to) &&
                          t.event().equals(event));

        assertTrue(transitionExists,
            String.format("Should have transition from %s to %s on %s event", fromState, toState, eventName));
    }
}