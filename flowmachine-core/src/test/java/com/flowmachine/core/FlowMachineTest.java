package com.flowmachine.core;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.model.ValidationResult;
import com.flowmachine.core.model.StateMachineInfo;
import com.flowmachine.core.model.TransitionResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlowMachineTest {

    enum OrderStatus {
        CREATED, PAID, CANCELLED, SHIPPED
    }

    enum OrderEvent {
        PAY, CANCEL, SHIP
    }

    static class Order {
        private final String id;
        private final boolean payable;

        Order(String id, boolean payable) {
            this.id = id;
            this.payable = payable;
        }

        String getId() {
            return id;
        }

        boolean isPayable() {
            return payable;
        }
    }

    @Test
    void shouldCreateBasicStateMachine() {
        StateMachine<OrderStatus, OrderEvent, Order> machine = FlowMachine
            .<OrderStatus, OrderEvent, Order>builder()
            .initialState(OrderStatus.CREATED)
            .configure(OrderStatus.CREATED)
                .permit(OrderEvent.PAY, OrderStatus.PAID)
                .permit(OrderEvent.CANCEL, OrderStatus.CANCELLED)
            .and()
            .configure(OrderStatus.PAID)
                .permit(OrderEvent.SHIP, OrderStatus.SHIPPED)
                .permit(OrderEvent.CANCEL, OrderStatus.CANCELLED)
            .and()
            .configure(OrderStatus.CANCELLED)
            .and()
            .configure(OrderStatus.SHIPPED)
            .and()
            .build();

        assertNotNull(machine);

        ValidationResult validation = machine.validate();
        assertTrue(validation.isValid(), "State machine should be valid");
    }

    @Test
    void shouldFireSimpleTransition() {
        StateMachine<OrderStatus, OrderEvent, Order> machine = FlowMachine
            .<OrderStatus, OrderEvent, Order>builder()
            .initialState(OrderStatus.CREATED)
            .configure(OrderStatus.CREATED)
                .permit(OrderEvent.PAY, OrderStatus.PAID)
                .permit(OrderEvent.CANCEL, OrderStatus.CANCELLED)
            .and()
            .build();

        Order order = new Order("123", true);

        OrderStatus result = machine.fire(OrderStatus.CREATED, OrderEvent.PAY, order);
        assertEquals(OrderStatus.PAID, result);
    }

    @Test
    void shouldRespectGuardConditions() {
        StateMachine<OrderStatus, OrderEvent, Order> machine = FlowMachine
            .<OrderStatus, OrderEvent, Order>builder()
            .initialState(OrderStatus.CREATED)
            .configure(OrderStatus.CREATED)
                .permitIf(OrderEvent.PAY, OrderStatus.PAID, (t, order) -> order.isPayable())
                .permit(OrderEvent.CANCEL, OrderStatus.CANCELLED)
            .and()
            .build();

        Order payableOrder = new Order("123", true);
        Order nonPayableOrder = new Order("456", false);

        OrderStatus result1 = machine.fire(OrderStatus.CREATED, OrderEvent.PAY, payableOrder);
        assertEquals(OrderStatus.PAID, result1);

        TransitionResult<OrderStatus> result2 = machine.fireWithResult(OrderStatus.CREATED, OrderEvent.PAY, nonPayableOrder);
        assertEquals(OrderStatus.CREATED, result2.state());
        assertFalse(result2.wasTransitioned());
    }

    @Test
    void shouldCheckCanFire() {
        StateMachine<OrderStatus, OrderEvent, Order> machine = FlowMachine
            .<OrderStatus, OrderEvent, Order>builder()
            .initialState(OrderStatus.CREATED)
            .configure(OrderStatus.CREATED)
                .permitIf(OrderEvent.PAY, OrderStatus.PAID, (t, order) -> order.isPayable())
            .and()
            .build();

        Order payableOrder = new Order("123", true);
        Order nonPayableOrder = new Order("456", false);

        assertTrue(machine.canFire(OrderStatus.CREATED, OrderEvent.PAY, payableOrder));
        assertFalse(machine.canFire(OrderStatus.CREATED, OrderEvent.PAY, nonPayableOrder));
    }

    @Test
    void shouldProvideStateMachineInfo() {
        StateMachine<OrderStatus, OrderEvent, Order> machine = FlowMachine
            .<OrderStatus, OrderEvent, Order>builder()
            .initialState(OrderStatus.CREATED)
            .configure(OrderStatus.CREATED)
                .permit(OrderEvent.PAY, OrderStatus.PAID)
                .permit(OrderEvent.CANCEL, OrderStatus.CANCELLED)
            .and()
            .configure(OrderStatus.PAID)
                .permit(OrderEvent.SHIP, OrderStatus.SHIPPED)
            .and()
            .build();

        StateMachineInfo<OrderStatus, OrderEvent, Order> info = machine.getInfo();

        assertEquals(OrderStatus.CREATED, info.initialState());
        assertTrue(info.states().contains(OrderStatus.CREATED));
        assertTrue(info.states().contains(OrderStatus.PAID));
        assertTrue(info.events().contains(OrderEvent.PAY));
        assertTrue(info.events().contains(OrderEvent.CANCEL));
        assertTrue(info.events().contains(OrderEvent.SHIP));
    }
}