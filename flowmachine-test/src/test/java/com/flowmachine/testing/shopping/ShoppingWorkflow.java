package com.flowmachine.testing.shopping;

import com.flowmachine.core.api.StateMachine;
import com.flowmachine.core.impl.StateMachineBuilderImpl;
import com.flowmachine.core.service.AbstractStateMachine;
import com.flowmachine.testing.shopping.model.ShoppingCart;
import com.flowmachine.testing.shopping.model.ShoppingEvent;
import com.flowmachine.testing.shopping.model.ShoppingState;

public class ShoppingWorkflow extends AbstractStateMachine<ShoppingState, ShoppingEvent, ShoppingCart> {

  @Override
  protected StateMachine<ShoppingState, ShoppingEvent, ShoppingCart> workflow() {
    return new StateMachineBuilderImpl<ShoppingState, ShoppingEvent, ShoppingCart>()
        .initialState(ShoppingState.BROWSING)
        .configure(ShoppingState.BROWSING)
        .permit(ShoppingEvent.ADD_TO_CART, ShoppingState.CART_FILLED)
        .and()
        .configure(ShoppingState.CART_FILLED)
        .permit(ShoppingEvent.PROCEED_TO_CHECKOUT, ShoppingState.CHECKOUT)
        .and()
        .configure(ShoppingState.CHECKOUT)
        .permit(ShoppingEvent.PAY, ShoppingState.PAID)
        .and()
        .configure(ShoppingState.PAID)
        .permit(ShoppingEvent.SHIP, ShoppingState.SHIPPED)
        .and()
        .build();
  }
}
