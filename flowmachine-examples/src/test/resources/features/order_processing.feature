Feature: Order Processing Workflow
  As an e-commerce system
  I want to process orders through a state machine
  So that orders follow the correct workflow and business rules

  Background:
    Given I have an order processing workflow
    And the workflow has the following states:
      | CREATED  |
      | PAID     |
      | SHIPPED  |
      | DELIVERED|
      | CANCELLED|
    And the workflow has the following events:
      | PAY     |
      | SHIP    |
      | DELIVER |
      | CANCEL  |

  Scenario: Successful order processing from creation to delivery
    Given I have an order "ORDER-001" with amount 99.99
    And the order is in "CREATED" state
    When I fire "PAY" event
    Then the order should transition to "PAID" state
    When I fire "SHIP" event
    Then the order should transition to "SHIPPED" state
    When I fire "DELIVER" event
    Then the order should transition to "DELIVERED" state
    And the order should be in a final state

  Scenario: Order cancellation from different states
    Given I have an order "ORDER-002" with amount 150.00
    And the order is in "CREATED" state
    When I fire "CANCEL" event
    Then the order should transition to "CANCELLED" state
    And the order should be in a final state

  Scenario: Payment processing for order
    Given I have an order "ORDER-003" with amount 75.50
    And the order is in "CREATED" state
    When I fire "PAY" event
    Then the order should transition to "PAID" state
    And I should be able to fire "SHIP" event
    But I should not be able to fire "PAY" event again

  Scenario: Invalid transition attempt
    Given I have an order "ORDER-004" with amount 200.00
    And the order is in "CREATED" state
    When I try to fire "SHIP" event without payment
    Then the order should remain in "CREATED" state
    And the transition should be unsuccessful
    And I should get an error message containing "No transition configured"

  Scenario: Final state restrictions
    Given I have an order "ORDER-005" with amount 120.00
    And the order is in "DELIVERED" state
    When I try to fire "SHIP" event
    Then the order should remain in "DELIVERED" state
    And the transition should be unsuccessful
    And I should get an error message containing "Cannot transition from final state"

  Scenario: Checking available transitions
    Given I have an order "ORDER-006" with amount 85.00
    And the order is in "PAID" state
    Then I should be able to fire "SHIP" event
    And I should be able to fire "CANCEL" event
    But I should not be able to fire "PAY" event
    And I should not be able to fire "DELIVER" event

  Scenario: Order workflow validation
    Given I have an order processing workflow
    When I validate the workflow configuration
    Then the configuration should be valid
    And it should have 5 states
    And it should have 4 events
    And the initial order state should be "CREATED"

  Scenario Outline: Multiple orders with different outcomes
    Given I have an order "<order_id>" with amount <amount>
    And the order is in "CREATED" state
    When I fire "<first_event>" event
    Then the order should transition to "<first_state>" state
    When I fire "<second_event>" event
    Then the order should transition to "<final_state>" state

    Examples:
      | order_id   | amount | first_event | first_state | second_event | final_state |
      | ORDER-101  | 50.00  | PAY         | PAID        | SHIP         | SHIPPED     |
      | ORDER-102  | 75.00  | PAY         | PAID        | CANCEL       | CANCELLED   |
      | ORDER-103  | 30.00  | CANCEL      | CANCELLED   | PAY          | CANCELLED   |

  Scenario: Batch order processing
    Given I have the following orders:
      | order_id  | state   | amount |
      | ORDER-201 | CREATED | 100.00 |
      | ORDER-202 | PAID    | 150.00 |
      | ORDER-203 | SHIPPED | 200.00 |
    When I process all orders that can be shipped
    Then order "ORDER-202" should transition to "SHIPPED" state
    And order "ORDER-201" should remain in "CREATED" state
    And order "ORDER-203" should remain in "SHIPPED" state

  Scenario: Order state machine introspection
    Given I have an order processing workflow
    When I get workflow information
    Then the workflow should contain state "CREATED" as initial state
    And the workflow should contain state "DELIVERED" as final state
    And the workflow should contain state "CANCELLED" as final state
    And the workflow should have transition from "CREATED" to "PAID" on "PAY" event
    And the workflow should have transition from "PAID" to "SHIPPED" on "SHIP" event