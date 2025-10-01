# 📘 FlowMachine Usage Examples

This document provides practical examples of how to use FlowMachine in your projects.

## 🚀 Getting Started

### Maven Setup

1. Add JitPack repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

2. Add FlowMachine dependency:

```xml
<dependency>
    <groupId>com.github.rayenrejeb</groupId>
    <artifactId>flow-machine</artifactId>
    <version>1.0.0</version> <!-- Use latest version -->
</dependency>
```

### Gradle Setup

1. Add JitPack repository to your `build.gradle`:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

2. Add FlowMachine dependency:

```gradle
dependencies {
    implementation 'com.github.rayenrejeb:flow-machine:1.0.0' // Use latest version
}
```

## 📝 Basic Example: Order Processing

Here's a complete example of an order processing workflow:

```java
import com.flowmachine.core.FlowMachine;
import com.flowmachine.core.api.StateMachine;

public class OrderProcessingExample {

    // Define your states
    public enum OrderState {
        CREATED, PAID, SHIPPED, DELIVERED, CANCELLED
    }

    // Define your events
    public enum OrderEvent {
        PAY, SHIP, DELIVER, CANCEL
    }

    // Your business object
    public static class Order {
        private final String id;
        private final double amount;
        private OrderState currentState;

        public Order(String id, double amount) {
            this.id = id;
            this.amount = amount;
            this.currentState = OrderState.CREATED;
        }

        // Getters and setters
        public String getId() { return id; }
        public double getAmount() { return amount; }
        public OrderState getCurrentState() { return currentState; }
        public void setCurrentState(OrderState state) { this.currentState = state; }
    }

    public static void main(String[] args) {
        // Create the state machine
        StateMachine<OrderState, OrderEvent, Order> workflow =
            FlowMachine.<OrderState, OrderEvent, Order>builder()
                .initialState(OrderState.CREATED)

                .configure(OrderState.CREATED)
                    .permit(OrderEvent.PAY, OrderState.PAID)
                    .permit(OrderEvent.CANCEL, OrderState.CANCELLED)
                    .onEntry((transition, order) ->
                        System.out.println("Order " + order.getId() + " created"))
                .and()

                .configure(OrderState.PAID)
                    .permit(OrderEvent.SHIP, OrderState.SHIPPED)
                    .permit(OrderEvent.CANCEL, OrderState.CANCELLED)
                    .onEntry((transition, order) ->
                        System.out.println("Order " + order.getId() + " paid: $" + order.getAmount()))
                .and()

                .configure(OrderState.SHIPPED)
                    .permit(OrderEvent.DELIVER, OrderState.DELIVERED)
                    .onEntry((transition, order) ->
                        System.out.println("Order " + order.getId() + " shipped"))
                .and()

                .configure(OrderState.DELIVERED)
                    .asFinal()
                    .onEntry((transition, order) ->
                        System.out.println("Order " + order.getId() + " delivered successfully"))
                .and()

                .configure(OrderState.CANCELLED)
                    .asFinal()
                    .onEntry((transition, order) ->
                        System.out.println("Order " + order.getId() + " cancelled"))
                .and()

                .build();

        // Use the workflow
        Order order = new Order("ORD-001", 99.99);

        // Process the order
        order.setCurrentState(workflow.fire(order.getCurrentState(), OrderEvent.PAY, order));
        order.setCurrentState(workflow.fire(order.getCurrentState(), OrderEvent.SHIP, order));
        order.setCurrentState(workflow.fire(order.getCurrentState(), OrderEvent.DELIVER, order));

        System.out.println("Final state: " + order.getCurrentState());
        System.out.println("Is final state: " + workflow.isFinalState(order.getCurrentState()));
    }
}
```

## 🎯 Advanced Example: Job Application Workflow

```java
import com.flowmachine.core.FlowMachine;
import com.flowmachine.core.api.StateMachine;

public class JobApplicationExample {

    public enum JobState {
        SUBMITTED, INITIAL_SCREENING, TECHNICAL_REVIEW,
        HR_INTERVIEW, FINAL_REVIEW, HIRED, REJECTED
    }

    public enum JobEvent {
        PROCEED, REJECT
    }

    public static class JobApplication {
        private final String candidateName;
        private final int experienceYears;
        private final double score;
        private JobState currentState;

        public JobApplication(String candidateName, int experienceYears, double score) {
            this.candidateName = candidateName;
            this.experienceYears = experienceYears;
            this.score = score;
            this.currentState = JobState.SUBMITTED;
        }

        public boolean isExperienced() { return experienceYears >= 5; }
        public boolean hasHighScore() { return score >= 8.0; }

        // Getters and setters
        public String getCandidateName() { return candidateName; }
        public int getExperienceYears() { return experienceYears; }
        public double getScore() { return score; }
        public JobState getCurrentState() { return currentState; }
        public void setCurrentState(JobState state) { this.currentState = state; }
    }

    public static void main(String[] args) {
        StateMachine<JobState, JobEvent, JobApplication> workflow =
            FlowMachine.<JobState, JobEvent, JobApplication>builder()
                .initialState(JobState.SUBMITTED)

                .configure(JobState.SUBMITTED)
                    .permit(JobEvent.PROCEED, JobState.INITIAL_SCREENING)
                    .permit(JobEvent.REJECT, JobState.REJECTED)
                .and()

                .configure(JobState.INITIAL_SCREENING)
                    // Conditional transitions based on candidate profile
                    .permitIf(JobEvent.PROCEED, JobState.FINAL_REVIEW,
                        (transition, app) -> app.isExperienced() && app.hasHighScore())
                    .permitIf(JobEvent.PROCEED, JobState.TECHNICAL_REVIEW,
                        (transition, app) -> app.isExperienced() && !app.hasHighScore())
                    .permitIf(JobEvent.PROCEED, JobState.HR_INTERVIEW,
                        (transition, app) -> !app.isExperienced())
                    .permit(JobEvent.REJECT, JobState.REJECTED)
                .and()

                .configure(JobState.TECHNICAL_REVIEW)
                    .permit(JobEvent.PROCEED, JobState.HR_INTERVIEW)
                    .permit(JobEvent.REJECT, JobState.REJECTED)
                .and()

                .configure(JobState.HR_INTERVIEW)
                    .permit(JobEvent.PROCEED, JobState.FINAL_REVIEW)
                    .permit(JobEvent.REJECT, JobState.REJECTED)
                .and()

                .configure(JobState.FINAL_REVIEW)
                    .permit(JobEvent.PROCEED, JobState.HIRED)
                    .permit(JobEvent.REJECT, JobState.REJECTED)
                .and()

                .configure(JobState.HIRED)
                    .asFinal()
                .and()

                .configure(JobState.REJECTED)
                    .asFinal()
                .and()

                .build();

        // Test different candidate profiles
        testCandidate(workflow, "Alice (Experienced, High Score)", 8, 9.0);
        testCandidate(workflow, "Bob (Experienced, Low Score)", 6, 6.5);
        testCandidate(workflow, "Charlie (New Graduate)", 1, 7.5);
    }

    private static void testCandidate(StateMachine<JobState, JobEvent, JobApplication> workflow,
                                     String name, int experience, double score) {
        System.out.println("\n=== Testing: " + name + " ===");

        JobApplication app = new JobApplication(name, experience, score);

        // Process through workflow
        app.setCurrentState(workflow.fire(app.getCurrentState(), JobEvent.PROCEED, app));
        System.out.println("After initial screening: " + app.getCurrentState());

        app.setCurrentState(workflow.fire(app.getCurrentState(), JobEvent.PROCEED, app));
        System.out.println("After next step: " + app.getCurrentState());

        // Continue until final state
        while (!workflow.isFinalState(app.getCurrentState())) {
            app.setCurrentState(workflow.fire(app.getCurrentState(), JobEvent.PROCEED, app));
            System.out.println("Current state: " + app.getCurrentState());
        }

        System.out.println("Final result: " + app.getCurrentState());
    }
}
```

## 🔧 Spring Boot Integration

Create a Spring Boot service using FlowMachine:

```java
@Service
public class OrderWorkflowService {

    private final StateMachine<OrderState, OrderEvent, Order> workflow;

    public OrderWorkflowService() {
        this.workflow = FlowMachine.<OrderState, OrderEvent, Order>builder()
            .initialState(OrderState.CREATED)
            // ... configure your workflow
            .build();
    }

    public Order processOrder(Order order, OrderEvent event) {
        OrderState newState = workflow.fire(order.getCurrentState(), event, order);
        order.setCurrentState(newState);
        return order;
    }

    public boolean canProcessEvent(Order order, OrderEvent event) {
        return workflow.canFire(order.getCurrentState(), event, order);
    }

    public boolean isOrderComplete(Order order) {
        return workflow.isFinalState(order.getCurrentState());
    }
}
```

## 📊 Validation and Introspection

```java
public class WorkflowValidationExample {

    public static void main(String[] args) {
        StateMachine<OrderState, OrderEvent, Order> workflow = createWorkflow();

        // Validate workflow configuration
        ValidationResult validation = workflow.validate();
        if (!validation.isValid()) {
            System.err.println("Workflow validation failed:");
            validation.errors().forEach(System.err::println);
            return;
        }

        // Get workflow information
        StateMachineInfo<OrderState, OrderEvent, Order> info = workflow.getInfo();

        System.out.println("States: " + info.states());
        System.out.println("Events: " + info.events());
        System.out.println("Initial State: " + info.initialState());

        // Print all transitions
        System.out.println("\nTransitions:");
        info.transitions().forEach(transition ->
            System.out.println(transition.fromState() + " --" +
                             transition.event() + "--> " + transition.toState()));
    }

    private static StateMachine<OrderState, OrderEvent, Order> createWorkflow() {
        // ... return configured workflow
    }
}
```

## 🎨 Generating Visual Diagrams

```java
import com.flowmachine.core.diagram.MermaidDiagramGenerator;

public class DiagramExample {

    public static void main(String[] args) {
        StateMachine<OrderState, OrderEvent, Order> workflow = createWorkflow();

        // Generate Mermaid diagram
        MermaidDiagramGenerator<OrderState, OrderEvent, Order> mermaidGenerator =
            new MermaidDiagramGenerator<>();

        String mermaidDiagram = mermaidGenerator.generate(workflow.getInfo());
        System.out.println(mermaidDiagram);

        // You can then use this in Mermaid online editor or documentation
    }
}
```

## 🛡️ Error Handling

```java
public class ErrorHandlingExample {

    public static void main(String[] args) {
        StateMachine<OrderState, OrderEvent, Order> workflow = createWorkflow();
        Order order = new Order("ORD-001", 99.99);

        // Safe transition with result checking
        TransitionResult<OrderState> result =
            workflow.fireWithResult(order.getCurrentState(), OrderEvent.PAY, order);

        if (result.wasTransitioned()) {
            order.setCurrentState(result.state());
            System.out.println("Successfully transitioned to: " + result.state());
        } else {
            System.err.println("Transition failed: " + result.reason());
        }

        // Check if transition is allowed before firing
        if (workflow.canFire(order.getCurrentState(), OrderEvent.SHIP, order)) {
            order.setCurrentState(workflow.fire(order.getCurrentState(), OrderEvent.SHIP, order));
        } else {
            System.err.println("Cannot ship order in current state: " + order.getCurrentState());
        }
    }
}
```

## 📚 More Resources

- [Complete API Reference](STATEMACHINE_API_REFERENCE.md)
- [Advanced Features Guide](README.md#-advanced-features)
- [Best Practices](README.md#-best-practices)
- [GitHub Repository](https://github.com/rayenrejeb/flow-machine)

## 🤝 Contributing

Contributions are welcome! Please read the contributing guidelines and submit pull requests to the GitHub repository.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.