# ⚡ FlowMachine Quick Start Guide

Get up and running with FlowMachine in under 5 minutes!

## 📦 Installation

### Option 1: Maven

Add to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.rayenrejeb</groupId>
        <artifactId>flow-machine</artifactId>
        <version>1.0.0</version> <!-- Use latest version -->
    </dependency>
</dependencies>
```

### Option 2: Gradle

Add to your `build.gradle`:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.rayenrejeb:flow-machine:1.0.0'
}
```

## 🚀 Your First State Machine (2 minutes)

Create a simple document approval workflow:

```java
import com.flowmachine.core.FlowMachine;
import com.flowmachine.core.api.StateMachine;

public class DocumentApproval {

    // 1. Define your states
    enum State { DRAFT, REVIEW, APPROVED, REJECTED }

    // 2. Define your events
    enum Event { SUBMIT, APPROVE, REJECT, REVISE }

    // 3. Your business object
    static class Document {
        String id;
        State currentState = State.DRAFT;
        // constructors, getters, setters...
    }

    public static void main(String[] args) {
        // 4. Create the state machine
        StateMachine<State, Event, Document> workflow =
            FlowMachine.<State, Event, Document>builder()
                .initialState(State.DRAFT)

                .configure(State.DRAFT)
                    .permit(Event.SUBMIT, State.REVIEW)
                .and()

                .configure(State.REVIEW)
                    .permit(Event.APPROVE, State.APPROVED)
                    .permit(Event.REJECT, State.REJECTED)
                    .permit(Event.REVISE, State.DRAFT)
                .and()

                .configure(State.APPROVED)
                    .asFinal()  // Terminal state
                .and()

                .configure(State.REJECTED)
                    .asFinal()  // Terminal state
                .and()

                .build();

        // 5. Use it!
        Document doc = new Document();

        // Submit for review
        doc.currentState = workflow.fire(doc.currentState, Event.SUBMIT, doc);
        System.out.println("State: " + doc.currentState); // REVIEW

        // Approve
        doc.currentState = workflow.fire(doc.currentState, Event.APPROVE, doc);
        System.out.println("State: " + doc.currentState); // APPROVED

        // Check if done
        System.out.println("Is final: " + workflow.isFinalState(doc.currentState)); // true
    }
}
```

## 🎯 Common Patterns

### Conditional Transitions

```java
.configure(State.REVIEW)
    .permitIf(Event.APPROVE, State.APPROVED,
        (transition, doc) -> doc.score >= 8.0)  // Only approve if score is high
    .permitIf(Event.APPROVE, State.CONDITIONAL_APPROVAL,
        (transition, doc) -> doc.score >= 6.0)  // Conditional approval for medium scores
    .permit(Event.REJECT, State.REJECTED)
```

### Entry/Exit Actions

```java
.configure(State.REVIEW)
    .onEntry((transition, doc) ->
        System.out.println("Document " + doc.id + " submitted for review"))
    .onExit((transition, doc) ->
        System.out.println("Review completed for " + doc.id))
    .permit(Event.APPROVE, State.APPROVED)
```

### Safe Transitions

```java
// Check before firing
if (workflow.canFire(doc.currentState, Event.APPROVE, doc)) {
    doc.currentState = workflow.fire(doc.currentState, Event.APPROVE, doc);
}

// Or use fireWithResult for detailed feedback
TransitionResult<State> result = workflow.fireWithResult(doc.currentState, Event.APPROVE, doc);
if (result.wasTransitioned()) {
    doc.currentState = result.state();
    System.out.println("Success: " + result.reason());
} else {
    System.out.println("Failed: " + result.reason());
}
```

## 🛠️ Next Steps

1. **Read the [Full Documentation](README.md)** - Complete guide with advanced features
2. **Check [Usage Examples](USAGE_EXAMPLES.md)** - Real-world scenarios
3. **Browse [API Reference](STATEMACHINE_API_REFERENCE.md)** - Detailed API docs
4. **See [Examples Module](flowmachine-examples/)** - Working code samples

## 🆘 Common Issues

**Q: "Cannot transition from X to Y"**
A: Check your workflow configuration. Use `workflow.canFire()` to verify valid transitions.

**Q: "How do I handle complex business logic?"**
A: Use `permitIf()` with guard conditions and entry/exit actions.

**Q: "Can I have multiple state machines?"**
A: Yes! Each StateMachine instance is independent and thread-safe.

## 📚 Learn More

- [GitHub Repository](https://github.com/rayenrejeb/flow-machine)
- [JitPack Page](https://jitpack.io/#rayenrejeb/flow-machine)
- [Issue Tracker](https://github.com/rayenrejeb/flow-machine/issues)

Happy coding! 🎉