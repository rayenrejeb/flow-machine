Feature: Job Applicant Processing Workflow
  As an HR system
  I want to process job applicants through different stages
  So that we can efficiently evaluate and hire the best candidates

  Background:
    Given I have a job applicant workflow
    And the workflow supports PROCEED event with priority-based routing

  Scenario: Exceptional candidate fast-track approval
    Given I have an applicant "Alice Smith" with the following profile:
      | experience_years | 10    |
      | screening_score  | 9.5   |
      | technical_score  | 9.0   |
      | red_flags        | false |
    And the applicant is in "SUBMITTED" state
    When I fire job "PROCEED" event
    Then the applicant should transition to "INITIAL_SCREENING" state
    When I fire job "PROCEED" event
    Then the applicant should transition to "FINAL_REVIEW" state
    And the transition should be successful due to exceptional candidate criteria

  Scenario: Standard candidate workflow path
    Given I have an applicant "Bob Johnson" with the following profile:
      | experience_years | 5     |
      | screening_score  | 7.8   |
      | technical_score  | 6.5   |
      | red_flags        | false |
    And the applicant is in "SUBMITTED" state
    When I fire job "PROCEED" event
    Then the applicant should transition to "INITIAL_SCREENING" state
    When I fire job "PROCEED" event
    Then the applicant should transition to "TECHNICAL_REVIEW" state
    When I fire job "PROCEED" event
    Then the applicant should transition to "HR_INTERVIEW" state

  Scenario: Applicant rejection at different stages
    Given I have an applicant "Charlie Brown" with the following profile:
      | experience_years | 2    |
      | screening_score  | 5.0  |
      | technical_score  | 4.0  |
      | red_flags        | true |
    And the applicant is in "SUBMITTED" state
    When I fire job "PROCEED" event
    Then the applicant should transition to "REJECTED" state
    And the applicant should be in a final state

  Scenario: Manual rejection at any stage
    Given I have an applicant "David Wilson" with standard profile
    And the applicant is in "HR_INTERVIEW" state
    When I fire job "REJECT" event
    Then the applicant should transition to "REJECTED" state
    And the applicant should be in a final state

  Scenario: Applicant withdrawal
    Given I have an applicant "Eve Davis" with standard profile
    And the applicant is in "TECHNICAL_REVIEW" state
    When I fire job "WITHDRAW" event
    Then the applicant should transition to "WITHDRAWN" state
    And the applicant should be in a final state

  Scenario: Priority role special handling
    Given I have an applicant "Frank Miller" with the following profile:
      | experience_years | 3     |
      | screening_score  | 6.5   |
      | technical_score  | 0.0   |
      | red_flags        | false |
      | priority_role    | true  |
    And the applicant is in "INITIAL_SCREENING" state
    When I fire job "PROCEED" event
    Then the applicant should transition to "HR_INTERVIEW" state
    And the transition should be due to priority role handling

  Scenario: Checking transition availability for different applicant types
    Given I have an applicant "Grace Lee" with exceptional profile
    And the applicant is in "INITIAL_SCREENING" state
    Then I should be able to fire "PROCEED" event to "FINAL_REVIEW"
    And I should be able to fire job "REJECT" event
    And I should be able to fire job "WITHDRAW" event
    But I should be able to fire job "PUT_ON_HOLD" event

  Scenario: Applicant put on hold and resume
    Given I have an applicant "Henry Kim" with standard profile
    And the applicant is in "TECHNICAL_REVIEW" state
    When I fire job "PUT_ON_HOLD" event
    Then the applicant should transition to "ON_HOLD" state
    When I fire job "PROCEED" event
    Then the applicant should transition to "HR_INTERVIEW" state

  Scenario: Complete hiring process from submission to hire
    Given I have an applicant "Isabel Garcia" with the following profile:
      | experience_years | 8     |
      | screening_score  | 8.5   |
      | technical_score  | 8.8   |
      | red_flags        | false |
    And the applicant is in "SUBMITTED" state
    When I process the applicant through the complete workflow
    Then the applicant should reach "HIRED" state
    And the final job state should be "HIRED"

  Scenario Outline: Different applicant profiles and their routing
    Given I have an applicant "<name>" with the following profile:
      | experience_years | <experience> |
      | screening_score  | <screening>  |
      | technical_score  | <technical>  |
      | red_flags        | <red_flags>  |
    And the applicant is in "INITIAL_SCREENING" state
    When I fire job "PROCEED" event
    Then the applicant should transition to "<expected_state>" state

    Examples:
      | name              | experience | screening | technical | red_flags | expected_state   |
      | Super Candidate   | 12         | 9.8       | 9.5       | false     | FINAL_REVIEW     |
      | Good Candidate    | 6          | 8.0       | 7.5       | false     | TECHNICAL_REVIEW |
      | Average Candidate | 4          | 7.0       | 6.0       | false     | HR_INTERVIEW     |
      | Poor Candidate    | 1          | 4.0       | 3.0       | true      | REJECTED         |

  Scenario: Workflow validation and introspection
    Given I have a job applicant workflow
    When I validate the job workflow configuration
    Then the job configuration should be valid
    And the workflow should support Event.PROCEED pattern
    And the workflow should have multiple permitIf conditions for priority routing
    When I get job workflow information
    Then the initial job state should be "SUBMITTED"
    And the workflow should have final states: "HIRED", "REJECTED", "WITHDRAWN"

  Scenario: Error handling for invalid transitions
    Given I have an applicant "Error Test" with standard profile
    And the applicant is in "HIRED" state
    When I try to fire job "PROCEED" event
    Then the applicant should remain in "HIRED" state
    And the job transition should be unsuccessful
    And I should get an error message about final state restrictions