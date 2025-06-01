# Agents4J API

This module contains the core API interfaces and contracts for the Agents4J framework. It defines the fundamental abstractions that all other modules depend on, providing a clean separation between API definitions and their implementations. This module has no external dependencies, ensuring maximum compatibility and minimal dependency conflicts.

## Overview

The `agents4j-api` module is the foundation of the Agents4J framework, containing:

- **Core Interfaces**: Fundamental contracts for workflows, graph execution, and state management
- **Graph-based Workflow**: Flexible directed graph workflow execution model
- **Stateful Workflows**: Support for persisting and resuming workflow state
- **Result Types**: Comprehensive result representations with success/failure/suspended states
- **Context Management**: Type-safe workflow context with isolation and propagation
- **Observability**: Metrics, tracing, and logging abstractions
- **Validation Framework**: Extensible validation system for configurations and workflows
- **Routing Capabilities**: Content-based routing for intelligent workflow paths

## Key Components

### Core Workflow Interfaces

#### `GraphWorkflow<S, O>`
The primary interface for graph-based workflow execution.

```java
public interface GraphWorkflow<S, O> {
    WorkflowResult<O, WorkflowError> start(S input);
    WorkflowResult<O, WorkflowError> resume(GraphWorkflowState<S> state);
    CompletableFuture<WorkflowResult<O, WorkflowError>> startAsync(S input);
    Map<NodeId, GraphWorkflowNode<S>> getNodes();
    Map<EdgeId, GraphEdge> getEdges();
    Set<NodeId> getEntryPoints();
    ValidationResult validate();
}
```

#### `StatefulWorkflow<S, O>`
Modern interface for workflows with persistent state and lifecycle management.

```java
public interface StatefulWorkflow<S, O> {
    String getWorkflowId();
    String getName();
    WorkflowResult<WorkflowExecution<S, O>, WorkflowError> start();
    WorkflowResult<WorkflowExecution<S, O>, WorkflowError> start(S initialState, WorkflowContext context);
    CompletableFuture<WorkflowResult<WorkflowExecution<S, O>, WorkflowError>> startAsync(S initialState);
    CompletableFuture<WorkflowResult<WorkflowExecution<S, O>, WorkflowError>> resumeAsync(String executionId);
    ValidationResult validate();
}
```

#### `GraphWorkflowNode<S>`
The core processing unit for graph-based workflows.

```java
public interface GraphWorkflowNode<S> {
    WorkflowResult<GraphCommand<S>, WorkflowError> process(GraphWorkflowState<S> state);
    NodeId getId();
    String getName();
    NodeType getType();
    ValidationResult validate();
}
```

### Graph Navigation

#### `GraphCommand<S>`
Commands for graph navigation and state management.

```java
public sealed interface GraphCommand<S>
    permits Traverse, Fork, Join, Suspend, Complete, Error {
    
    record Traverse(NodeId targetNodeId, WorkflowContext contextUpdates) implements GraphCommand<S> {}
    
    record Fork(Set<NodeId> targetNodeIds, WorkflowContext contextUpdates) implements GraphCommand<S> {}
    
    record Join(WorkflowContext contextUpdates) implements GraphCommand<S> {}
    
    record Suspend(Duration timeout, WorkflowContext contextUpdates) implements GraphCommand<S> {}
    
    record Complete(S result, WorkflowContext contextUpdates) implements GraphCommand<S> {}
    
    record Error(WorkflowError error) implements GraphCommand<S> {}
}
```

### Result Types

#### `WorkflowResult<T, E>`
Comprehensive result type for workflow operations.

```java
public sealed interface WorkflowResult<T, E extends WorkflowError>
    permits Success, Failure, Suspended {
    
    record Success<T, E extends WorkflowError>(T value, Map<String, Object> metadata) 
        implements WorkflowResult<T, E> {}
    
    record Failure<T, E extends WorkflowError>(E error, Map<String, Object> metadata) 
        implements WorkflowResult<T, E> {}
    
    record Suspended<T, E extends WorkflowError>(String suspensionId, Duration timeout, 
        Map<String, Object> metadata) implements WorkflowResult<T, E> {}
}
```

### Content Routing

#### `ContentRouter<T>`
Interface for intelligent content-based routing.

```java
public interface ContentRouter<T> extends GraphWorkflowNode<T> {
    RoutingDecision route(T content, Set<NodeId> availableRoutes, WorkflowContext context);
    Map<String, Object> getRoutingMetadata();
}
```

#### `RoutingDecision`
Result of content analysis and routing logic.

```java
public record RoutingDecision(
    NodeId selectedRoute,
    double confidence,
    String reasoning,
    Map<NodeId, Double> alternativeRoutes,
    Map<String, Object> metadata
) {}
```

### Context Management

#### `WorkflowContext`
Type-safe context for sharing data between workflow nodes.

```java
public interface WorkflowContext {
    <T> Optional<T> get(String key, Class<T> type);
    <T> T getOrDefault(String key, T defaultValue, Class<T> type);
    boolean contains(String key);
    Set<String> keys();
    WorkflowContext with(String key, Object value);
    WorkflowContext withAll(Map<String, Object> values);
    WorkflowContext without(String key);
    
    static WorkflowContext empty() { /* ... */ }
    static WorkflowContext of(String key, Object value) { /* ... */ }
    static WorkflowContext fromMap(Map<String, Object> map) { /* ... */ }
}
```

### Validation System

#### `ValidationProvider<T>`
Core interface for validating objects.

```java
public interface ValidationProvider<T> {
    ValidationResult validate(T object);
    ValidationProvider<T> withRule(ValidationRule<T> rule);
    Set<ValidationRule<T>> getRules();
}
```

#### `ValidationResult`
Comprehensive validation result with errors and warnings.

```java
public record ValidationResult(
    boolean valid,
    List<ValidationError> errors,
    List<ValidationWarning> warnings,
    Map<String, Object> metadata
) {
    public ValidationResult combine(ValidationResult other) { /* ... */ }
    public static ValidationResult success() { /* ... */ }
    public static ValidationResult failure(String errorMessage) { /* ... */ }
    public static ValidationResult withWarning(String warning) { /* ... */ }
}
```

### Observability

#### `MetricsCollector`
Interface for collecting workflow execution metrics.

```java
public interface MetricsCollector {
    void recordWorkflowStart(String workflowId, String workflowName);
    void recordWorkflowCompletion(String workflowId, String workflowName, Duration duration);
    void recordNodeExecution(String workflowId, String nodeId, String nodeName, Duration duration);
    void recordError(String workflowId, String nodeId, String errorCode);
    <T> T timed(String metric, TimedOperation<T> operation) throws Exception;
}
```

#### `WorkflowTracer`
Interface for distributed tracing of workflow execution.

```java
public interface WorkflowTracer {
    Span startWorkflowSpan(String workflowId, String workflowName);
    Span startNodeSpan(String workflowId, String nodeId, String nodeName, Span parentSpan);
    void endSpan(Span span);
    void addEvent(Span span, String eventName, Map<String, Object> attributes);
    void setError(Span span, Throwable error);
}
```

## Design Principles

### 1. Type Safety
Full generic support throughout the API for compile-time safety.

### 2. Immutability
State objects and results are immutable to ensure thread safety.

### 3. Functional Style
Emphasis on immutable transformations over side effects.

### 4. Clear Semantics
Methods and interfaces have clear, consistent semantics.

### 5. Extensibility
Designed for extension through composition and sealed interfaces.

### 6. Async First
Built-in asynchronous execution support throughout.

## Usage Examples

### Creating a Simple Graph Workflow

```java
// Define workflow nodes
GraphWorkflowNode<String> startNode = new SimpleNode("start", input -> {
    // Process input and return navigation command
    return GraphCommand.traverse("process", "Processed input: " + input);
});

GraphWorkflowNode<String> processNode = new SimpleNode("process", input -> {
    // Final processing step
    return GraphCommand.complete("Final result: " + input);
});

// Build the workflow
GraphWorkflow<String, String> workflow = GraphWorkflowBuilder.create("MyWorkflow")
    .withNode(startNode)
    .withNode(processNode)
    .withEdge(startNode.getId(), processNode.getId())
    .withEntryPoint(startNode.getId())
    .build();

// Execute the workflow
WorkflowResult<String, WorkflowError> result = workflow.start("Hello World");

// Handle the result
if (result instanceof WorkflowResult.Success<String, WorkflowError> success) {
    System.out.println(success.value());
} else if (result instanceof WorkflowResult.Failure<String, WorkflowError> failure) {
    System.err.println("Error: " + failure.error().getMessage());
}
```

### Implementing a Content Router

```java
public class MyContentRouter implements ContentRouter<Document> {
    @Override
    public RoutingDecision route(Document content, Set<NodeId> availableRoutes, WorkflowContext context) {
        // Analyze content and determine best route
        String contentType = determineContentType(content);
        
        // Map content type to route node
        NodeId selectedRoute = mapToRoute(contentType, availableRoutes);
        
        // Return decision with confidence and reasoning
        return new RoutingDecision(
            selectedRoute,
            0.85,
            "Content appears to be " + contentType,
            calculateAlternatives(content, availableRoutes),
            Map.of("contentType", contentType)
        );
    }
    
    @Override
    public NodeId getId() {
        return NodeId.of("content-router");
    }
    
    @Override
    public String getName() {
        return "Content Type Router";
    }
    
    @Override
    public NodeType getType() {
        return NodeType.ROUTER;
    }
}
```

## Dependencies

- **Java 17+**: Required for sealed classes and records
- **No External Dependencies**: Pure API module with no third-party dependencies

## Integration

This API module is designed to be implemented by:

- `agents4j-core`: Core implementations of all API interfaces
- `agents4j-langchain4j`: LangChain4J-specific extensions and implementations
- Custom implementation modules for specialized use cases

## Versioning

The API follows semantic versioning with stability guarantees:

- **Major versions**: Breaking API changes
- **Minor versions**: Backward-compatible additions
- **Patch versions**: Bug fixes and clarifications

## License

This project is licensed under the MIT License - see the main project LICENSE file for details.