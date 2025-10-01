# 🥒 FlowMachine Cucumber Tests

**Behavior-Driven Development (BDD) tests for FlowMachine using Cucumber**

## 📋 Overview

This directory contains comprehensive Cucumber tests that demonstrate FlowMachine functionality through real-world scenarios written in natural language.

## 🎯 Test Coverage

### 1. **Order Processing Workflow** (`order_processing.feature`)
- Complete e-commerce order lifecycle
- State transitions from creation to delivery
- Error handling and validation
- Batch processing scenarios
- Final state restrictions

### 2. **Job Applicant Workflow** (`job_applicant_workflow.feature`)
- Priority-based routing with Event.PROCEED pattern
- Conditional transitions based on applicant profiles
- Multiple workflow paths (fast-track, standard, rejection)
- Complex business logic validation

### 3. **StateMachine API Methods** (`statemachine_api.feature`)
- All core StateMachine interface methods
- Thread safety and concurrent operations
- Null parameter handling
- Performance characteristics
- Error handling and recovery

## 🏗️ Project Structure

```
src/test/
├── java/com/flowmachine/examples/cucumber/
│   ├── CucumberTestSuite.java           # Test runner
│   ├── OrderProcessingSteps.java        # Order workflow steps
│   ├── JobApplicantSteps.java          # Job applicant steps
│   └── StateMachineApiSteps.java       # API method steps
└── resources/features/
    ├── order_processing.feature         # Order scenarios
    ├── job_applicant_workflow.feature   # Job applicant scenarios
    └── statemachine_api.feature         # API testing scenarios
```

## 🚀 Running the Tests

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- FlowMachine Core dependency

### Run All Cucumber Tests
```bash
mvn test -Dtest=CucumberTestSuite
```

### Run Specific Feature Tests
```bash
# Run only Order Processing tests
mvn test -Dcucumber.filter.tags="@order" -Dtest=CucumberTestSuite

# Run only Job Applicant tests
mvn test -Dcucumber.filter.tags="@job" -Dtest=CucumberTestSuite

# Run only API tests
mvn test -Dcucumber.filter.tags="@api" -Dtest=CucumberTestSuite
```

### Generate HTML Reports
```bash
mvn test -Dtest=CucumberTestSuite
# Reports generated in: target/cucumber-reports/
```

## 📊 Test Reports

After running tests, view detailed HTML reports at:
- **Location**: `target/cucumber-reports/index.html`
- **Content**: Scenario results, step details, execution times
- **Format**: Interactive HTML with drill-down capabilities

## 🎭 Key Test Scenarios

### Order Processing Examples

```gherkin
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
```

### Job Applicant Examples

```gherkin
Scenario: Exceptional candidate fast-track approval
  Given I have an applicant "Alice Smith" with the following profile:
    | experience_years | 10   |
    | screening_score  | 9.5  |
    | technical_score  | 9.0  |
    | red_flags        | false|
  And the applicant is in "SUBMITTED" state
  When I fire "PROCEED" event
  Then the applicant should transition to "INITIAL_SCREENING" state
  When I fire "PROCEED" event
  Then the applicant should transition to "FINAL_REVIEW" state
```

### API Testing Examples

```gherkin
Scenario: Testing fireWithResult() method - successful transition
  Given I have a task in "PROCESSING" state
  When I call fireWithResult() with "FINISH" event
  Then the result should indicate success
  And the result state should be "COMPLETED"
  And the result reason should indicate success
```

## 🔧 Running Individual Features

### To avoid step definition conflicts, run features individually:

```bash
# Order Processing only
mvn test -Dcucumber.options="src/test/resources/features/order_processing.feature --glue com.flowmachine.examples.cucumber.OrderProcessingSteps"

# Job Applicant only
mvn test -Dcucumber.options="src/test/resources/features/job_applicant_workflow.feature --glue com.flowmachine.examples.cucumber.JobApplicantSteps"

# API Testing only
mvn test -Dcucumber.options="src/test/resources/features/statemachine_api.feature --glue com.flowmachine.examples.cucumber.StateMachineApiSteps"
```

## 🎯 Test Benefits

### **1. Living Documentation**
- Tests serve as executable specifications
- Business stakeholders can read and understand scenarios
- Requirements are captured in natural language

### **2. Regression Protection**
- Comprehensive coverage of FlowMachine functionality
- Catches breaking changes early
- Validates business logic correctness

### **3. Examples for Users**
- Real-world usage patterns
- Common workflow scenarios
- API method demonstrations

### **4. Quality Assurance**
- Thread safety validation
- Error handling verification
- Performance characteristics testing

## 🧪 Adding New Tests

### 1. Create Feature File
```gherkin
Feature: Your Feature Name
  As a [role]
  I want [functionality]
  So that [benefit]

  Scenario: Your scenario description
    Given [precondition]
    When [action]
    Then [expected result]
```

### 2. Implement Step Definitions
```java
@Given("I have [something]")
public void i_have_something() {
    // Setup code
}

@When("I perform [action]")
public void i_perform_action() {
    // Action code
}

@Then("the result should be [expected]")
public void the_result_should_be_expected() {
    // Assertion code
}
```

### 3. Run and Iterate
```bash
mvn test -Dtest=CucumberTestSuite
```

## 🎉 Best Practices

### **Scenario Writing**
- Use business language, not technical jargon
- Keep scenarios focused and independent
- Use descriptive names and clear given-when-then structure

### **Step Definitions**
- Make steps reusable across scenarios
- Use meaningful variable names
- Include proper assertions and error messages

### **Test Data**
- Use realistic business data
- Include edge cases and error conditions
- Test both happy path and failure scenarios

## 📚 Related Documentation

- **[FlowMachine Documentation](../FLOWMACHINE_DOCUMENTATION.md)** - Complete library guide
- **[StateMachine API Reference](../STATEMACHINE_API_REFERENCE.md)** - API method details
- **[Examples](../src/main/java/com/flowmachine/examples/)** - Java code examples

## 🤝 Contributing

When adding new Cucumber tests:

1. **Follow BDD principles** - Write from user perspective
2. **Use consistent language** - Match existing step patterns
3. **Test real scenarios** - Base tests on actual use cases
4. **Include edge cases** - Test error conditions and boundaries
5. **Maintain documentation** - Update this README with new features

---

**Happy Testing!** 🥒✨