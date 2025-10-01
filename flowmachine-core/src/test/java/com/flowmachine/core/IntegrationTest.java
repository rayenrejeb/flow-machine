package com.flowmachine.core;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.model.ValidationResult;
import com.flowmachine.core.model.StateMachineInfo;
import com.flowmachine.core.model.TransitionResult;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {

    enum OrderState {
        CREATED, VALIDATION_PENDING, VALIDATED, PAYMENT_PENDING, PAID,
        PROCESSING, SHIPPED, DELIVERED, CANCELLED, REFUNDED
    }

    enum OrderEvent {
        VALIDATE, VALIDATION_FAILED, PAY, PAYMENT_FAILED, PROCESS,
        SHIP, DELIVER, CANCEL, REFUND
    }

    static class Order {
        private final String id;
        private boolean valid = true;
        private boolean paymentSuccessful = true;
        private final List<String> auditLog = new ArrayList<>();
        private final AtomicInteger stateChanges = new AtomicInteger(0);

        Order(String id) {
            this.id = id;
        }

        String getId() { return id; }
        boolean isValid() { return valid; }
        boolean isPaymentSuccessful() { return paymentSuccessful; }
        List<String> getAuditLog() { return new ArrayList<>(auditLog); }
        int getStateChanges() { return stateChanges.get(); }

        void setValid(boolean valid) { this.valid = valid; }
        void setPaymentSuccessful(boolean paymentSuccessful) { this.paymentSuccessful = paymentSuccessful; }

        void addAuditEntry(String entry) {
            auditLog.add(entry);
            stateChanges.incrementAndGet();
        }

        @Override
        public String toString() {
            return "Order{id='" + id + "', valid=" + valid +
                   ", paymentSuccessful=" + paymentSuccessful + "}";
        }
    }

    @Test
    void shouldHandleCompleteOrderWorkflow() {
        StateMachine<OrderState, OrderEvent, Order> machine = createOrderStateMachine();

        Order order = new Order("ORDER-001");
        OrderState currentState = OrderState.CREATED;

        currentState = machine.fire(currentState, OrderEvent.VALIDATE, order);
        assertEquals(OrderState.VALIDATION_PENDING, currentState);

        currentState = machine.fire(currentState, OrderEvent.VALIDATE, order);
        assertEquals(OrderState.VALIDATED, currentState);
        assertTrue(order.getAuditLog().contains("Order validated"));

        currentState = machine.fire(currentState, OrderEvent.PAY, order);
        assertEquals(OrderState.PAYMENT_PENDING, currentState);

        currentState = machine.fire(currentState, OrderEvent.PAY, order);
        assertEquals(OrderState.PAID, currentState);
        assertTrue(order.getAuditLog().contains("Payment successful"));

        currentState = machine.fire(currentState, OrderEvent.PROCESS, order);
        assertEquals(OrderState.PROCESSING, currentState);

        currentState = machine.fire(currentState, OrderEvent.SHIP, order);
        assertEquals(OrderState.SHIPPED, currentState);

        currentState = machine.fire(currentState, OrderEvent.DELIVER, order);
        assertEquals(OrderState.DELIVERED, currentState);

        assertTrue(order.getStateChanges() >= 5);

        ValidationResult validation = machine.validate();
        assertTrue(validation.isValid());
    }

    @Test
    void shouldHandleValidationFailure() {
        StateMachine<OrderState, OrderEvent, Order> machine = createOrderStateMachine();

        Order order = new Order("ORDER-002");
        order.setValid(false);

        OrderState currentState = OrderState.CREATED;
        currentState = machine.fire(currentState, OrderEvent.VALIDATE, order);
        assertEquals(OrderState.VALIDATION_PENDING, currentState);

        currentState = machine.fire(currentState, OrderEvent.VALIDATE, order);
        assertEquals(OrderState.CANCELLED, currentState);
        assertTrue(order.getAuditLog().contains("Order validation failed"));
    }

    @Test
    void shouldHandlePaymentFailure() {
        StateMachine<OrderState, OrderEvent, Order> machine = createOrderStateMachine();

        Order order = new Order("ORDER-003");
        order.setPaymentSuccessful(false);

        OrderState currentState = OrderState.CREATED;
        currentState = machine.fire(currentState, OrderEvent.VALIDATE, order);
        assertEquals(OrderState.VALIDATION_PENDING, currentState);

        currentState = machine.fire(currentState, OrderEvent.VALIDATE, order);
        assertEquals(OrderState.VALIDATED, currentState);

        currentState = machine.fire(currentState, OrderEvent.PAY, order);
        assertEquals(OrderState.PAYMENT_PENDING, currentState);

        currentState = machine.fire(currentState, OrderEvent.PAY, order);
        assertEquals(OrderState.CANCELLED, currentState);
        assertTrue(order.getAuditLog().contains("Payment failed"));
    }

    @Test
    void shouldAllowCancellationAtMultipleStages() {
        StateMachine<OrderState, OrderEvent, Order> machine = createOrderStateMachine();

        Order order1 = new Order("ORDER-004");
        OrderState state1 = machine.fire(OrderState.VALIDATED, OrderEvent.CANCEL, order1);
        assertEquals(OrderState.CANCELLED, state1);

        Order order2 = new Order("ORDER-005");
        OrderState state2 = machine.fire(OrderState.PROCESSING, OrderEvent.CANCEL, order2);
        assertEquals(OrderState.CANCELLED, state2);

        Order order3 = new Order("ORDER-006");
        OrderState state3 = machine.fire(OrderState.SHIPPED, OrderEvent.CANCEL, order3);
        assertEquals(OrderState.REFUNDED, state3);
    }

    @Test
    void shouldProvideComprehensiveStateMachineInfo() {
        StateMachine<OrderState, OrderEvent, Order> machine = createOrderStateMachine();

        StateMachineInfo<OrderState, OrderEvent, Order> info = machine.getInfo();

        assertEquals(OrderState.CREATED, info.initialState());
        assertEquals(10, info.states().size());
        assertTrue(info.states().contains(OrderState.CREATED));
        assertTrue(info.states().contains(OrderState.DELIVERED));

        assertTrue(info.events().size() >= 6, "Expected at least 6 events, got: " + info.events().size() + " events: " + info.events());
        assertTrue(info.events().contains(OrderEvent.VALIDATE));
        assertTrue(info.events().contains(OrderEvent.DELIVER));

        assertTrue(info.transitions().size() >= 10, "Expected at least 10 transitions, got: " + info.transitions().size());
    }

    @Test
    void shouldHandleIgnoredEvents() {
        StateMachine<OrderState, OrderEvent, Order> machine = createOrderStateMachine();

        Order order = new Order("ORDER-007");

        TransitionResult<OrderState> result = machine.fireWithResult(
            OrderState.DELIVERED, OrderEvent.SHIP, order);

        assertEquals(OrderState.DELIVERED, result.state());
        assertFalse(result.wasTransitioned());
        assertTrue(result.reason().contains("ignored"));
    }

    @Test
    void shouldUseInternalTransitions() {
        StateMachine<OrderState, OrderEvent, Order> machine = createOrderStateMachine();

        Order order = new Order("ORDER-008");
        int initialStateChanges = order.getStateChanges();

        OrderState result = machine.fire(OrderState.PROCESSING, OrderEvent.PROCESS, order);

        assertEquals(OrderState.PROCESSING, result);
        assertTrue(order.getStateChanges() > initialStateChanges);
        assertTrue(order.getAuditLog().contains("Order processing updated"));
    }

    private StateMachine<OrderState, OrderEvent, Order> createOrderStateMachine() {
        return FlowMachine.<OrderState, OrderEvent, Order>builder()
            .initialState(OrderState.CREATED)

            .configure(OrderState.CREATED)
                .permit(OrderEvent.VALIDATE, OrderState.VALIDATION_PENDING)
                .permit(OrderEvent.CANCEL, OrderState.CANCELLED)
            .and()

            .configure(OrderState.VALIDATION_PENDING)
                .permitIf(OrderEvent.VALIDATE, OrderState.VALIDATED,
                         (t, order) -> order.isValid())
                .permitIf(OrderEvent.VALIDATE, OrderState.CANCELLED,
                         (t, order) -> !order.isValid())
                .onEntry((t, order) -> order.addAuditEntry("Starting validation"))
            .and()

            .configure(OrderState.VALIDATED)
                .permit(OrderEvent.PAY, OrderState.PAYMENT_PENDING)
                .permit(OrderEvent.CANCEL, OrderState.CANCELLED)
                .onEntry((t, order) -> order.addAuditEntry("Order validated"))
            .and()

            .configure(OrderState.PAYMENT_PENDING)
                .permitIf(OrderEvent.PAY, OrderState.PAID,
                         (t, order) -> order.isPaymentSuccessful())
                .permitIf(OrderEvent.PAY, OrderState.CANCELLED,
                         (t, order) -> !order.isPaymentSuccessful())
                .onEntry((t, order) -> order.addAuditEntry("Processing payment"))
            .and()

            .configure(OrderState.PAID)
                .permit(OrderEvent.PROCESS, OrderState.PROCESSING)
                .permit(OrderEvent.CANCEL, OrderState.CANCELLED)
                .onEntry((t, order) -> order.addAuditEntry("Payment successful"))
            .and()

            .configure(OrderState.PROCESSING)
                .permit(OrderEvent.SHIP, OrderState.SHIPPED)
                .permit(OrderEvent.CANCEL, OrderState.CANCELLED)
                .internal(OrderEvent.PROCESS, (t, order) ->
                    order.addAuditEntry("Order processing updated"))
                .onEntry((t, order) -> order.addAuditEntry("Order processing started"))
            .and()

            .configure(OrderState.SHIPPED)
                .permit(OrderEvent.DELIVER, OrderState.DELIVERED)
                .permit(OrderEvent.CANCEL, OrderState.REFUNDED)
                .ignore(OrderEvent.SHIP)
                .onEntry((t, order) -> order.addAuditEntry("Order shipped"))
            .and()

            .configure(OrderState.DELIVERED)
                .ignore(OrderEvent.DELIVER)
                .ignore(OrderEvent.SHIP)
                .onEntry((t, order) -> order.addAuditEntry("Order delivered"))
            .and()

            .configure(OrderState.CANCELLED)
                .onEntry((t, order) -> {
                    if (order.isValid()) {
                        order.addAuditEntry("Payment failed");
                    } else {
                        order.addAuditEntry("Order validation failed");
                    }
                })
            .and()

            .configure(OrderState.REFUNDED)
                .onEntry((t, order) -> order.addAuditEntry("Order refunded"))
            .and()

            .onError((state, event, order, error) -> {
                order.addAuditEntry("Error in state " + state + ": " + error.getMessage());
                return OrderState.CANCELLED;
            })

            .build();
    }
}