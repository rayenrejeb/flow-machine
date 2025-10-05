package com.flowmachine.testing.shopping;

import static com.flowmachine.testing.assertion.FlowMachineAssertions.assertThatFlowResult;
import static org.assertj.core.api.Assertions.assertThat;
import com.flowmachine.core.api.StateMachine;
import com.flowmachine.testing.FlowMachineTester;
import com.flowmachine.testing.result.TestResult;
import com.flowmachine.testing.shopping.model.ShoppingCart;
import com.flowmachine.testing.shopping.model.ShoppingEvent;
import com.flowmachine.testing.shopping.model.ShoppingState;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

/**
 * Example test demonstrating context action execution between transitions.
 */
class ShoppingWorkflowTest {

  private final StateMachine<ShoppingState, ShoppingEvent, ShoppingCart> shoppingWorkflow = new ShoppingWorkflow();

  @Test
  void testShoppingWorkflowWithContextActions() {
    ShoppingCart cart = new ShoppingCart();

    TestResult<ShoppingState, ShoppingEvent, ShoppingCart> result = FlowMachineTester.forWorkflow(shoppingWorkflow)
        .startingAt(ShoppingState.BROWSING)
        .withContext(cart)

        // Add some items to cart before transitioning
        .modifyContext(c -> c.addItem(29.99), "Add first item")
        .modifyContext(c -> c.addItem(15.50), "Add second item")
        .expectTransition(ShoppingEvent.ADD_TO_CART, ShoppingState.CART_FILLED)

        // Apply discount and promotion code before checkout
        .modifyContext(c -> c.applyDiscount(10.0), "Apply 10% discount")
        .modifyContext(c -> c.setPromotionCode("SAVE10"), "Set promotion code")
        .expectTransition(ShoppingEvent.PROCEED_TO_CHECKOUT, ShoppingState.CHECKOUT)

        // Add one more item during checkout (e.g., suggested item)
        .modifyContext(c -> c.addItem(5.99), "Add suggested item during checkout")
        .expectTransition(ShoppingEvent.PAY, ShoppingState.PAID)

        .expectTransition(ShoppingEvent.SHIP, ShoppingState.SHIPPED)
        .expectFinalState(ShoppingState.SHIPPED)

        // Validate the final state of the cart
        .validateContext(c -> c.getItemCount() == 3)
        .validateContext(ShoppingCart::isDiscountApplied)
        .validateContext(c -> "SAVE10".equals(c.getPromotionCode()))
        .validateContext(
            c -> c.getTotalAmount() > 40.0) // Original total was 45.49, with 10% discount on first two items

        .build()
        .runScenario();

    assertThatFlowResult(result)
        .isSuccessful()
        .hasNoErrors()
        .hasSuccessfulTransitions()
        .endedInState(ShoppingState.SHIPPED)
        .hasTransitionCount(4);

    // Verify the final cart state
    ShoppingCart finalCart = result.getFinalContext();
    assertThat(finalCart.getItemCount()).isEqualTo(3);
    assertThat(finalCart.isDiscountApplied()).isTrue();
    assertThat(finalCart.getPromotionCode()).isEqualTo("SAVE10");

    // Expected calculation: (29.99 + 15.50) * 0.9 + 5.99 = 40.94 + 5.99 = 46.93
    assertThat(finalCart.getTotalAmount()).isCloseTo(46.93, within(0.01));
  }

  @Test
  void testContextActionFailure() {
    ShoppingCart cart = new ShoppingCart();

    TestResult<ShoppingState, ShoppingEvent, ShoppingCart> result = FlowMachineTester.forWorkflow(shoppingWorkflow)
        .startingAt(ShoppingState.BROWSING)
        .withContext(cart)

        // This action will cause an exception
        .modifyContext(c -> {
          throw new RuntimeException("Simulated failure");
        }, "Failing action")
        .expectTransition(ShoppingEvent.ADD_TO_CART, ShoppingState.CART_FILLED)

        .build()
        .runScenario();

    assertThatFlowResult(result)
        .isNotSuccessful()
        .hasErrors()
        .hasErrorContaining("Context action failed")
        .hasErrorContaining("Simulated failure");
  }

  @Test
  void testComplexCartModification() {
    ShoppingCart cart = new ShoppingCart();

    TestResult<ShoppingState, ShoppingEvent, ShoppingCart> result = FlowMachineTester.forWorkflow(shoppingWorkflow)
        .startingAt(ShoppingState.BROWSING)
        .withContext(cart)

        // Simulate a complex shopping scenario
        .modifyContext(c -> {
          // Add multiple items
          c.addItem(19.99);
          c.addItem(24.99);
          c.addItem(9.99);
        }, "Add initial items to cart")

        .expectTransition(ShoppingEvent.ADD_TO_CART, ShoppingState.CART_FILLED)

        // Customer changes their mind and applies discount
        .modifyContext(c -> c.applyDiscount(15.0), "Apply 15% discount")

        .expectTransition(ShoppingEvent.PROCEED_TO_CHECKOUT, ShoppingState.CHECKOUT)

        // Last minute addition and promotion
        .modifyContext(c -> {
          c.addItem(4.99); // Last minute add
          c.setPromotionCode("LASTCHANCE15");
        }, "Last minute modifications")

        .expectTransition(ShoppingEvent.PAY, ShoppingState.PAID)
        .expectTransition(ShoppingEvent.SHIP, ShoppingState.SHIPPED)
        .expectFinalState(ShoppingState.SHIPPED)

        .validateContext(c -> c.getItemCount() == 4)
        .validateContext(ShoppingCart::isDiscountApplied)
        .validateContext(c -> "LASTCHANCE15".equals(c.getPromotionCode()))

        .build()
        .runScenario();

    assertThatFlowResult(result)
        .isSuccessful()
        .hasNoErrors()
        .hasSuccessfulTransitions()
        .endedInState(ShoppingState.SHIPPED);

    ShoppingCart finalCart = result.getFinalContext();

    // Expected: (19.99 + 24.99 + 9.99) * 0.85 + 4.99 = 54.97 * 0.85 + 4.99 = 46.72 + 4.99 = 51.71
    assertThat(finalCart.getTotalAmount()).isCloseTo(51.71, within(0.01));
  }

  private Offset<Double> within(double offset) {
    return Offset.offset(offset);
  }
}