# State Handler Pattern for FlowMachine

## Problem
In complex workflows, the StateConfiguration can become very long and difficult to maintain. For example, the original `JobApplicantSteps` class had a StateConfiguration spanning over 120 lines (lines 148-269), making it hard to:

- Understand individual state logic
- Maintain and modify specific states
- Test state behavior in isolation
- Follow single responsibility principle

## Solution: State Handler Pattern

The State Handler pattern breaks down large StateConfiguration into focused, manageable classes. Each state handler is responsible for configuring transitions for a single state.

### Core Components

#### 1. StateHandler Interface
```java
public interface StateHandler {
    JobState getState();
    StateConfiguration<JobState, JobEvent, JobApplicant> configure(
            StateConfiguration<JobState, JobEvent, JobApplicant> configuration);
}
```

#### 2. StateHandlerRegistry
```java
public static class StateHandlerRegistry {
    private final List<StateHandler> stateHandlers = new ArrayList<>();

    public StateHandlerRegistry register(StateHandler stateHandler) {
        stateHandlers.add(stateHandler);
        return this;
    }

    public StateMachineBuilder<JobState, JobEvent, JobApplicant> applyTo(
            StateMachineBuilder<JobState, JobEvent, JobApplicant> builder,
            JobState initialState) {
        // Configure all states using registered handlers
    }
}
```

#### 3. Individual State Handlers
Each state gets its own focused handler class:

```java
public static class SubmittedStateHandler implements StateHandler {
    @Override
    public JobState getState() {
        return JobState.SUBMITTED;
    }

    @Override
    public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
            StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
        return configuration
            .permitIf(JobEvent.PROCEED, JobState.INITIAL_SCREENING,
                (t, applicant) -> applicant.meetsBasicRequirements())
            .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                (t, applicant) -> !applicant.meetsBasicRequirements())
            .permit(JobEvent.REJECT, JobState.REJECTED)
            .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN);
    }
}
```

### Usage

Instead of a monolithic configuration:

```java
// OLD: Monolithic approach (120+ lines)
workflow = FlowMachine.<JobState, JobEvent, JobApplicant>builder()
    .initialState(JobState.SUBMITTED)
    .configure(JobState.SUBMITTED)
    .permitIf(JobEvent.PROCEED, JobState.INITIAL_SCREENING, ...)
    .permitIf(JobEvent.PROCEED, JobState.REJECTED, ...)
    // ... 100+ more lines
    .build();
```

Use the registry pattern:

```java
// NEW: State Handler pattern
StateHandlerRegistry registry = new StateHandlerRegistry()
    .register(new SubmittedStateHandler())
    .register(new InitialScreeningStateHandler())
    .register(new TechnicalReviewStateHandler())
    .register(new HrInterviewStateHandler())
    .register(new TechnicalInterviewStateHandler())
    .register(new FinalReviewStateHandler())
    .register(new BackgroundCheckStateHandler())
    .register(new OfferExtendedStateHandler())
    .register(new OnHoldStateHandler())
    .register(new FinalStateHandler(JobState.HIRED))
    .register(new FinalStateHandler(JobState.REJECTED))
    .register(new FinalStateHandler(JobState.WITHDRAWN));

workflow = registry
    .applyTo(FlowMachine.<JobState, JobEvent, JobApplicant>builder(), JobState.SUBMITTED)
    .build();
```

## Benefits

### 1. **Single Responsibility**
Each handler focuses on one state's logic:
- `SubmittedStateHandler` - handles application submission validation
- `InitialScreeningStateHandler` - handles priority-based routing logic
- `TechnicalReviewStateHandler` - handles technical assessment routing

### 2. **Maintainability**
- Easy to find and modify specific state logic
- Changes to one state don't affect others
- Clear separation of concerns

### 3. **Testability**
- Each state handler can be unit tested independently
- Mock specific state behaviors for integration tests
- Easier to verify complex routing logic

### 4. **Reusability**
- State handlers can be reused across different workflows
- Common patterns (like `FinalStateHandler`) can be parameterized
- Handlers can be composed into different workflow configurations

### 5. **Readability**
- Complex routing logic is contained and documented within each handler
- Business rules are co-located with their relevant state
- Overall workflow structure is clear in the registry setup

## Example: Complex State Logic

The `InitialScreeningStateHandler` demonstrates how complex priority-based routing can be cleanly encapsulated:

```java
public static class InitialScreeningStateHandler implements StateHandler {
    @Override
    public StateConfiguration<JobState, JobEvent, JobApplicant> configure(
            StateConfiguration<JobState, JobEvent, JobApplicant> configuration) {
        return configuration
            // Highest priority: Exceptional candidates go to final review
            .permitIf(JobEvent.PROCEED, JobState.FINAL_REVIEW,
                (t, applicant) -> applicant.isExceptionalCandidate())

            // High priority: Technical roles need technical review
            .permitIf(JobEvent.PROCEED, JobState.TECHNICAL_REVIEW,
                (t, applicant) -> applicant.needsTechnicalAssessment() && !applicant.isExceptionalCandidate())

            // Medium priority: Priority roles go to HR interview
            .permitIf(JobEvent.PROCEED, JobState.HR_INTERVIEW,
                (t, applicant) -> applicant.isPriorityRole() && !applicant.needsTechnicalAssessment()
                                  && !applicant.isExceptionalCandidate())

            // Default path: Standard candidates go to HR interview
            .permitIf(JobEvent.PROCEED, JobState.HR_INTERVIEW,
                (t, applicant) -> !applicant.shouldBeRejected() && !applicant.isExceptionalCandidate()
                                  && !applicant.needsTechnicalAssessment() && !applicant.isPriorityRole())

            // Lowest priority: Candidates who should be rejected
            .permitIf(JobEvent.PROCEED, JobState.REJECTED,
                (t, applicant) -> applicant.shouldBeRejected())

            .permit(JobEvent.REJECT, JobState.REJECTED)
            .permit(JobEvent.WITHDRAW, JobState.WITHDRAWN)
            .permit(JobEvent.PUT_ON_HOLD, JobState.ON_HOLD);
    }
}
```

This encapsulates complex business logic that would otherwise be buried in a large configuration block.

## Test Results

The implementation has been verified to produce identical behavior to the original monolithic approach:

```
✅ State Handler pattern test completed successfully!
✅ Exceptional candidate flow test completed successfully!
✅ State Handler Registry functionality test completed!

Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

## Recommendation

For workflows with more than 5-7 states or complex routing logic, use the State Handler pattern to:

1. **Break down large StateConfiguration** into focused, single-purpose handlers
2. **Improve maintainability** by isolating state-specific logic
3. **Enhance testability** through independent state handler testing
4. **Increase readability** by making workflow structure and business rules clearer

The pattern scales well and can handle both simple states (like final states) and complex routing logic (like the initial screening priority system) within the same framework.