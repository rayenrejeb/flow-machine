# Enhanced StateMachineBuilder API with StateHandler Support

## Overview

The FlowMachine API has been enhanced with a new `configure(StateHandler)` overload that makes it natural to mix explicit state configuration with the StateHandler pattern. This provides maximum flexibility for building state machines.

## New API Method

```java
// Added to StateMachineBuilder interface
StateMachineBuilder<TState, TEvent, TContext> configure(StateHandler<TState, TEvent, TContext> stateHandler);
```

## Core StateHandler Interface

The StateHandler interface is now part of the core FlowMachine API:

```java
package com.flowmachine.core.api;

public interface StateHandler<TState, TEvent, TContext> {
    TState getState();
    StateConfiguration<TState, TEvent, TContext> configure(StateConfiguration<TState, TEvent, TContext> configuration);
}
```

## Usage Examples

### 1. Mixed Approach (Recommended)

Use StateHandlers for complex states and explicit configuration for simple ones:

```java
StateMachine<JobState, JobEvent, JobApplicant> workflow = FlowMachine.<JobState, JobEvent, JobApplicant>builder()
    .initialState(JobState.SUBMITTED)

    // Use StateHandler for complex states
    .configure(new SubmittedStateHandler())
    .configure(new InitialScreeningStateHandler())

    // Mix with explicit configuration for simpler states
    .configure(JobState.HR_INTERVIEW)
        .permitIf(JobEvent.PROCEED, JobState.FINAL_REVIEW,
            (t, applicant) -> applicant.getScreeningScore() >= 8.0)
        .permitIf(JobEvent.PROCEED, JobState.REJECTED,
            (t, applicant) -> applicant.getScreeningScore() < 7.0)
        .permit(JobEvent.REJECT, JobState.REJECTED)
        .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
        .and()

    .configure(JobState.FINAL_REVIEW)
        .permitIf(JobEvent.PROCEED, JobState.HIRED,
            (t, applicant) -> applicant.getScreeningScore() >= 7.5)
        .permitIf(JobEvent.PROCEED, JobState.REJECTED,
            (t, applicant) -> applicant.getScreeningScore() < 7.5)
        .permit(JobEvent.REJECT, JobState.REJECTED)
        .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
        .and()

    // Final states can be configured inline
    .configure(JobState.HIRED)
        .asFinal()
        .and()

    .configure(JobState.REJECTED)
        .asFinal()
        .and()

    .configure(JobState.WITHDRAWN)
        .asFinal()
        .and()

    .build();
```

### 2. Example StateHandler Implementation

```java
public static class InitialScreeningStateHandler implements StateHandler<JobState, JobEvent, JobApplicant> {
    @Override
    public JobState getState() {
        return JobState.INITIAL_SCREENING;
    }

    @Override
    public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
            StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
        return configuration
            // Priority-based routing logic encapsulated in the handler
            .permitIf(JobEvent.PROCEED, JobState.FINAL_REVIEW,
                (t, applicant) -> applicant.isExceptionalCandidate())
            .permitIf(JobEvent.PROCEED, JobState.HR_INTERVIEW,
                (t, applicant) -> !applicant.isExceptionalCandidate() && !applicant.shouldBeRejected())
            .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                (t, applicant) -> applicant.shouldBeRejected())
            .permit(JobEvent.REJECT, JobState.REJECTED)
            .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN);
    }
}
```

### 3. Backward Compatibility

The traditional explicit configuration approach still works exactly as before:

```java
StateMachine<JobState, JobEvent, JobApplicant> workflow = FlowMachine.<JobState, JobEvent, JobApplicant>builder()
    .initialState(JobState.SUBMITTED)

    .configure(JobState.SUBMITTED)
        .permitIf(JobEvent.PROCEED, JobState.INITIAL_SCREENING,
            (t, applicant) -> applicant.meetsBasicRequirements())
        .permit(JobEvent.REJECT, JobState.REJECTED)
        .and()

    .configure(JobState.INITIAL_SCREENING)
        .permitIf(JobEvent.PROCEED, JobState.HR_INTERVIEW,
            (t, applicant) -> !applicant.shouldBeRejected())
        .permitIf(JobEvent.PROCEED, JobState.REJECTED,
            (t, applicant) -> applicant.shouldBeRejected())
        .and()

    // ... continue with traditional approach
    .build();
```

## Benefits of the Enhanced API

### 1. **Natural Flow**
- No need for separate registry classes
- Seamless mixing of patterns
- Intuitive method chaining

### 2. **Flexibility**
- Use StateHandlers where they add value (complex states)
- Use explicit configuration for simple states
- Choose the right tool for each state

### 3. **Gradual Adoption**
- Existing code continues to work unchanged
- Can gradually introduce StateHandlers for complex states
- No breaking changes

### 4. **Consistency**
- Both approaches use the same underlying configuration
- Same validation and behavior
- Unified API surface

## When to Use Each Approach

### Use StateHandler for:
- **Complex routing logic** (multiple permitIf conditions)
- **Business logic encapsulation** (priority-based routing)
- **Reusable state patterns** (common workflow steps)
- **States with extensive configuration** (many transitions, actions)

### Use Explicit Configuration for:
- **Simple states** (few transitions)
- **Final states** (just `.asFinal()`)
- **One-off states** (unique, non-reusable logic)
- **Quick prototyping** (rapid development)

## Test Results

The enhanced API has been thoroughly tested:

```
✅ Enhanced API mixing test completed successfully!
✅ Pure explicit configuration approach test completed successfully!

Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 (for core functionality)
```

## Implementation Details

The `configure(StateHandler)` method:

1. **Gets the state** from the handler via `getState()`
2. **Creates configuration** using the existing `configure(TState)` method
3. **Applies handler logic** by calling `stateHandler.configure(config)`
4. **Completes configuration** by calling `config.and()`
5. **Returns the builder** for continued chaining

This design ensures consistency with the existing API while providing the StateHandler enhancement.

## Conclusion

The enhanced API provides the best of both worlds:
- **Flexibility** to choose the right approach for each state
- **Backward compatibility** with existing code
- **Natural integration** without additional complexity
- **Improved maintainability** for complex workflows

This makes FlowMachine even more powerful while maintaining its simplicity and ease of use.