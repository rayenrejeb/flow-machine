Feature: StateMachine API Methods
  As a developer using FlowMachine
  I want to test all StateMachine interface methods
  So that I can ensure the API works correctly and safely

  Background:
    Given I have a simple workflow with states: CREATED, PROCESSING, COMPLETED, FAILED
    And the workflow has transitions:
      | from       | event   | to         |
      | CREATED    | START   | PROCESSING |
      | PROCESSING | FINISH  | COMPLETED  |
      | PROCESSING | FAIL    | FAILED     |

  Scenario: Testing fire() method - successful transitions
    Given I have a task in "CREATED" state
    When I call fire() with "START" event
    Then the task should be in "PROCESSING" state
    And the fire() method should return the new state

  Scenario: Testing fire() method - failed transition returns current state
    Given I have a task in "CREATED" state
    When I call fire() with "FINISH" event (invalid transition)
    Then the task should remain in "CREATED" state
    And the fire() method should return the current state

  Scenario: Testing fireWithResult() method - successful transition
    Given I have a task in "PROCESSING" state
    When I call fireWithResult() with "FINISH" event
    Then the result should indicate success
    And the result state should be "COMPLETED"
    And the result reason should indicate success

  Scenario: Testing fireWithResult() method - failed transition
    Given I have a task in "COMPLETED" state
    When I call fireWithResult() with "START" event (invalid transition)
    Then the result should indicate failure
    And the result state should be "COMPLETED"
    And the result reason should contain "Cannot transition from final state"

  Scenario: Testing canFire() method - valid transitions
    Given I have a task in "PROCESSING" state
    Then canFire() should return true for "FINISH" event
    And canFire() should return true for "FAIL" event
    But canFire() should return false for "START" event

  Scenario: Testing canFire() method - null safety
    Given I have a task in "PROCESSING" state
    When I call canFire() with null event
    Then canFire() should return false without throwing exceptions

  Scenario: Testing isFinalState() method
    Given I have a workflow with final states marked
    Then isFinalState() should return true for "COMPLETED" state
    And isFinalState() should return true for "FAILED" state
    But isFinalState() should return false for "CREATED" state
    And isFinalState() should return false for "PROCESSING" state

  Scenario: Testing isFinalState() method - null safety
    When I call isFinalState() with null state
    Then isFinalState() should return false without throwing exceptions

  Scenario: Testing getInfo() method
    Given I have a configured workflow
    When I call getInfo()
    Then the info should contain 4 states
    And the info should contain 3 events
    And the info should contain 3 transitions
    And the initial state should be "CREATED"

  Scenario: Testing validate() method - valid configuration
    Given I have a properly configured workflow
    When I call validate()
    Then the validation should be successful
    And there should be no validation errors

  Scenario: Testing validate() method - invalid configuration
    Given I have a workflow with unreachable states
    When I call validate()
    Then the validation should fail
    And there should be validation errors about unreachable states

  Scenario: State machine operations with null parameters
    Given I have a simple workflow
    When I call various methods with null parameters
    Then the methods should handle nulls gracefully:
      | method         | parameter | expected_result |
      | fire           | event     | current_state   |
      | fireWithResult | event     | failed_result   |
      | canFire        | event     | false          |
      | isFinalState   | state     | false          |

  Scenario: Complex workflow with auto-transitions
    Given I have a workflow with auto-transitions
    And I have a task in "CREATED" state
    When I call fire() with "START" event
    Then the task should automatically progress through intermediate states
    And the final state should be determined by auto-transition logic

  Scenario: Error handling and recovery
    Given I have a workflow with error handling configured
    And I have a task in "PROCESSING" state
    When an error occurs during state transition
    Then the error handler should be invoked
    And the task should transition to an appropriate error state
    And the error should be logged appropriately

  Scenario: Workflow introspection for documentation
    Given I have a complex workflow
    When I use getInfo() to extract workflow structure
    Then I should be able to generate complete documentation including:
      | element      | description                    |
      | states       | All configured states          |
      | events       | All configured events          |
      | transitions  | All state transitions          |
      | final_states | States marked as final         |
      | initial_state| The starting state             |

  Scenario: Batch processing with canFire() validation
    Given I have multiple tasks in different states:
      | task_id | state      |
      | TASK-1  | CREATED    |
      | TASK-2  | PROCESSING |
      | TASK-3  | COMPLETED  |
      | TASK-4  | PROCESSING |
    When I filter tasks that can be processed with "FINISH" event
    Then only tasks in "PROCESSING" state should be selected
    And I should be able to safely process all selected tasks

  Scenario: Performance testing with high-frequency operations
    Given I have a simple workflow
    When I perform 10000 fire operations in less than 1000 ms
    Then all operations should complete within acceptable time limits
    And memory usage should remain stable
    And no performance degradation should occur