package com.flowmachine.testing.order;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.impl.StateMachineBuilderImpl;
import com.flowmachine.core.service.AbstractStateMachine;
import com.flowmachine.testing.order.model.Order;
import com.flowmachine.testing.order.model.OrderEvent;
import com.flowmachine.testing.order.model.OrderState;

/**
 * OrderWorkflow implementation that implements StateMachine interface. This allows the workflow to be injected as a
 * service in tests.
 */
public class OrderWorkflow extends AbstractStateMachine<OrderState, OrderEvent, Order> {

  @Override
  protected StateMachine<OrderState, OrderEvent, Order> workflow() {
    return new StateMachineBuilderImpl<OrderState, OrderEvent, Order>()
        .initialState(OrderState.CREATED)
        .configure(OrderState.CREATED)
        .permit(OrderEvent.PAY, OrderState.PAID)
        .permit(OrderEvent.CANCEL, OrderState.CANCELLED)
        .onExit((info, context) -> {
          if (info.toState().equals(OrderState.PAID)) {
            context.setPaid(true);
          }
        })
        .and()
        .configure(OrderState.PAID)
        .permit(OrderEvent.SHIP, OrderState.SHIPPED)
        .permit(OrderEvent.CANCEL, OrderState.CANCELLED)
        .onExit((info, context) -> {
          if (info.toState().equals(OrderState.SHIPPED)) {
            context.setShipped(true);
          }
        })
        .and()
        .configure(OrderState.SHIPPED)
        .permit(OrderEvent.DELIVER, OrderState.DELIVERED)
        .and()
        .build();
  }
}