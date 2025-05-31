# Agents4J API

This module contains the core API interfaces and contracts for the Agents4J framework. It defines the fundamental abstractions that all other modules depend on, providing a clean separation between API definitions and their implementations. This module has no external dependencies, ensuring maximum compatibility and minimal dependency conflicts.

## Overview

The `agents4j-api` module is the foundation of the Agents4J framework, containing:

- **Core Interfaces**: Fundamental contracts for agents, workflows, and execution
- **Exception Hierarchy**: Structured error handling with context and error codes
- **Configuration APIs**: Type-safe configuration management
- **Validation Framework**: Extensible validation system
- **Workflow Abstractions**: Stateful and stateless workflow definitions

## Key Components

### Core Interfaces

#### `AgentNode<I, O>`
The fundamental interface for processing units in workflows.

```java
public interface AgentNode<I, O> {
    O process(I input, Map<String, Object> context);
    CompletableFuture<O> processAsync(I input, Map<String, Object> context);
    String getName();
}
```

#### `AgentWorkflow<I, O>`
Base interface for all workflow types.

```java
public interface AgentWorkflow<I, O> {
    O execute(I input);
    CompletableFuture<O> executeAsync(I input);
    WorkflowMetadata getMetadata();
}
```

#### `StatefulWorkflow<I, O>`
Extended workflow interface supporting stateful execution.

```java
public interface StatefulWorkflow<I, O> extends AgentWorkflow<I, O> {
    StatefulWorkflowResult<O> start(I input);
    StatefulWorkflowResult<O> resume(String workflowId, Map<String, Object> context);
    void suspend(String workflowId);
}
```



### Exception Hierarchy

#### `AgentException`
Base class for all framework exceptions with structured error information.

```java
public abstract class AgentException extends Exception {
    public ErrorCode getErrorCode();
    public Map<String, Object> getContext();
    public AgentException addContext(String key, Object value);
}
```

#### `WorkflowExecutionException`
Specialized exception for workflow execution failures.

```java
public class WorkflowExecutionException extends AgentException {
    public String getWorkflowName();
    public String getWorkflowId();
    public String getNodeId();
    public WorkflowState getState();
}
```

#### `ValidationException`
Exception for validation failures with detailed error information.

```java
public class ValidationException extends AgentException {
    public List<String> getValidationErrors();
}
```

### Error Codes

Standardized error codes for consistent error handling:

- **Input Validation**: `INVALID_INPUT`, `MISSING_REQUIRED_PARAMETER`, `INVALID_CONFIGURATION`
- **Workflow Execution**: `WORKFLOW_EXECUTION_FAILED`, `WORKFLOW_TIMEOUT`, `WORKFLOW_INTERRUPTED`
- **Agent Processing**: `AGENT_PROCESSING_FAILED`, `AGENT_INITIALIZATION_FAILED`, `AGENT_NOT_FOUND`
- **Provider Integration**: `PROVIDER_ERROR`, `PROVIDER_RATE_LIMIT`, `PROVIDER_AUTHENTICATION_FAILED`
- **System Errors**: `INTERNAL_ERROR`, `RESOURCE_EXHAUSTED`, `CONFIGURATION_ERROR`

### Configuration Management

#### `WorkflowConfiguration`
Type-safe configuration with validation support.

```java
WorkflowConfiguration config = WorkflowConfiguration.builder()
    .withProperty("timeout", Duration.ofMinutes(5))
    .withProperty("retryCount", 3)
    .withValidator(myValidator)
    .build();

Optional<Duration> timeout = config.getProperty("timeout", Duration.class);
```

### Validation Framework

#### `Validator<T>`
Generic validation interface for extensible validation logic.

```java
public interface Validator<T> {
    ValidationResult validate(T target);
}
```

#### `ValidationResult`
Rich validation result with errors and warnings.

```java
ValidationResult result = ValidationResult.success()
    .combine(otherResult);

if (!result.isValid()) {
    List<String> errors = result.getErrors();
    // Handle validation errors
}
```

### Workflow State Management

#### `WorkflowState`
Immutable state representation for stateful workflows.

```java
public class WorkflowState {
    public String getWorkflowId();
    public Map<String, Object> getData();
    public String getCurrentNodeId();
    public long getVersion();
    public Instant getLastModified();
}
```

#### `StatefulWorkflowResult<O>`
Result container for stateful workflow execution.

```java
public class StatefulWorkflowResult<O> {
    public enum Status { COMPLETED, SUSPENDED, ERROR }
    
    public Status getStatus();
    public Optional<O> getResult();
    public Optional<WorkflowState> getState();
    public Optional<Throwable> getError();
}
```

### Routing and Strategy

#### `WorkflowRoute<I>`
Defines routing paths within workflows.

```java
public interface WorkflowRoute<I> {
    String getId();
    String getDescription();
    List<AgentNode<I, ?>> getNodes();
    Map<String, Object> getMetadata();
}
```

#### `WorkflowExecutionStrategy<I, O>`
Strategy pattern for different execution approaches.

```java
public interface WorkflowExecutionStrategy<I, O> {
    O execute(I input, List<AgentNode<I, ?>> nodes, Map<String, Object> context);
    CompletableFuture<O> executeAsync(I input, List<AgentNode<I, ?>> nodes, Map<String, Object> context);
}
```

## Design Principles

### 1. Interface Segregation
Interfaces are focused and cohesive, avoiding unnecessary dependencies.

### 2. Generic Type Safety
Full generic support ensures type safety across the framework.

### 3. Immutability
State objects are immutable where possible to ensure thread safety.

### 4. Extensibility
Interfaces support extension through composition and strategy patterns.

### 5. Error Handling
Structured exception hierarchy with context and error codes for better debugging.

### 6. Async Support
Built-in support for asynchronous execution patterns.

## Usage Patterns

### Basic Agent Implementation

```java
public class MyAgent implements AgentNode<String, String> {
    @Override
    public String process(String input, Map<String, Object> context) {
        // Process input and return result
        return "Processed: " + input;
    }
    
    @Override
    public String getName() {
        return "MyAgent";
    }
}
```

### Custom Validation

```java
public class MyValidator implements Validator<MyConfiguration> {
    @Override
    public ValidationResult validate(MyConfiguration config) {
        if (config.getTimeout().isNegative()) {
            return ValidationResult.failure("Timeout cannot be negative");
        }
        return ValidationResult.success();
    }
}
```

### Exception Handling

```java
try {
    result = workflow.execute(input);
} catch (WorkflowExecutionException e) {
    logger.error("Workflow {} failed at node {}: {}", 
        e.getWorkflowName(), e.getNodeId(), e.getMessage());
    
    // Access additional context
    Map<String, Object> context = e.getContext();
}
```

## Dependencies

- **Java 17+**: Required for modern language features
- **No External Dependencies**: Pure API module with no third-party dependencies

## Integration

This module is designed to be extended by:

- `agents4j-core`: Core implementations
- `agents4j-langchain4j`: LangChain4J integrations (includes LangChain4J-specific interfaces)
- Custom implementation modules

## Versioning

The API follows semantic versioning with stability guarantees:

- **Major versions**: Breaking API changes
- **Minor versions**: Backward-compatible additions
- **Patch versions**: Bug fixes and clarifications

## License

This project is licensed under the MIT License - see the main project LICENSE file for details.