# StatefulWorkflow Documentation

## Overview

The StatefulWorkflow system provides a powerful framework for building complex, graph-based workflows that can maintain state, suspend execution, and resume from where they left off. Unlike traditional linear workflows, StatefulWorkflows allow for dynamic routing, conditional branching, and persistent state management.

## Key Features

- **Graph-based Architecture**: Nodes connected by conditional routes
- **State Management**: Serializable state that persists across executions
- **Suspension/Resumption**: Workflows can be paused and resumed later
- **Dynamic Routing**: Route selection based on current state and input
- **Command-based Control**: Nodes return commands that control workflow flow
- **Async Support**: Full asynchronous execution capabilities
- **Validation**: Built-in workflow validation and error handling

## Core Components

### StatefulAgentNode<I>
Represents a processing node that returns WorkflowCommand instead of direct output:
```java
public interface StatefulAgentNode<I> {
    WorkflowCommand<I> process(I input, WorkflowState state, Map<String, Object> context);
    String getNodeId();
    String getName();
    boolean canBeEntryPoint();
    boolean canSuspend();
}
```

### WorkflowCommand<I>
Commands that control workflow execution:
- `CONTINUE` - Proceed to next node based on routes
- `GOTO` - Jump directly to a specific node
- `SUSPEND` - Pause workflow execution
- `COMPLETE` - Finish workflow execution
- `ERROR` - Indicate an error occurred

### WorkflowState
Immutable, serializable state container:
```java
WorkflowState state = WorkflowState.create("workflow-id");
state = state.withUpdates(Map.of("key", "value"));
state = state.withCurrentNode("nodeId");
```

### WorkflowRoute<I>
Defines connections between nodes with optional conditions:
```java
WorkflowRoute<String> route = WorkflowRoute.<String>builder()
    .id("route-1")
    .from("nodeA")
    .to("nodeB")
    .condition((input, state) -> state.get("valid", false))
    .priority(10)
    .build();
```

## Quick Start

### 1. Create Nodes
```java
public class ValidationNode extends AbstractStatefulAgentNode<String> {
    public ValidationNode() {
        super("validation", "Input Validation", true); // can be entry point
    }
    
    @Override
    protected WorkflowCommand<String> doProcess(String input, WorkflowState state, Map<String, Object> context) {
        if (input.length() < 3) {
            return continueWith()
                .updateState("valid", false)
                .updateState("error", "Input too short")
                .build();
        }
        return continueWith()
            .updateState("valid", true)
            .build();
    }
}
```

### 2. Build Workflow
```java
StatefulWorkflow<String, String> workflow = StatefulWorkflowFactory.conditional(
    "My Workflow",
    new ValidationNode(),
    (input, state, context) -> state.get("result", input)
)
.addNode(new ProcessingNode())
.addNode(new ErrorNode())
.addConditionalRoute("validation-to-processing", "validation", "processing",
    (input, state) -> Boolean.TRUE.equals(state.get("valid")))
.addDefaultRoute("validation-to-error", "validation", "error")
.build();
```

### 3. Execute Workflow
```java
// Start new workflow
StatefulWorkflowResult<String> result = workflow.start("input data");

if (result.isCompleted()) {
    String output = result.getOutput().orElse("No output");
    System.out.println("Result: " + output);
} else if (result.isSuspended()) {
    // Save state for later resumption
    WorkflowState savedState = result.getState();
    
    // Resume later
    result = workflow.resume("new input", savedState);
}
```

## Factory Patterns

### Sequential Workflow
```java
StatefulWorkflow<String, String> workflow = StatefulWorkflowFactory.sequential(
    "Sequential Process",
    (input, state, context) -> state.get("result", input),
    new ValidationNode(),
    new ProcessingNode(),
    new OutputNode()
);
```

### Conditional Workflow
```java
StatefulWorkflow<String, String> workflow = StatefulWorkflowFactory.conditional(
    "Conditional Process",
    new EntryNode(),
    outputExtractor
)
.addNode(new ProcessingNode())
.addConditionalRoute("entry-to-processing", "entry", "processing",
    (input, state) -> state.get("shouldProcess", false))
.build();
```

### Approval Workflow
```java
StatefulWorkflow<String, String> workflow = StatefulWorkflowFactory.approval(
    "Document Approval",
    new ReviewNode(),
    new ApproveNode(),
    new RejectNode(),
    outputExtractor
);
```

### Retry Workflow
```java
StatefulWorkflow<String, String> workflow = StatefulWorkflowFactory.retry(
    "Retry Process",
    new ProcessNode(),
    new SuccessNode(),
    new FailureNode(),
    3, // max retries
    outputExtractor
);
```

## Advanced Features

### Custom Commands
```java
// Continue with state updates and input transformation
return continueWith()
    .updateState("counter", counter + 1)
    .updateState("lastProcessed", System.currentTimeMillis())
    .withInput(transformedInput)
    .addMetadata("nodeId", getNodeId())
    .build();

// Jump to specific node
return goTo("errorHandler")
    .updateState("error", "Validation failed")
    .build();

// Suspend workflow
return suspend()
    .updateState("suspendReason", "Waiting for approval")
    .build();
```

### Complex Routing
```java
WorkflowRoute<String> route = WorkflowRoute.<String>builder()
    .id("complex-route")
    .from("decision")
    .to("processing")
    .condition((input, state) -> {
        String priority = state.get("priority", "normal");
        int attempts = state.get("attempts", 0);
        return "high".equals(priority) && attempts < 3;
    })
    .priority(100)
    .description("High priority with retry limit")
    .addMetadata("category", "urgent")
    .build();
```

### State Management
```java
// Read state
boolean isValid = state.get("valid", false);
String lastResult = state.get("result").orElse("none");
int counter = state.get("counter", 0);

// Update state (immutable)
WorkflowState newState = state.withUpdates(Map.of(
    "processed", true,
    "timestamp", System.currentTimeMillis(),
    "version", state.get("version", 0) + 1
));
```

### Async Execution
```java
// Async start
CompletableFuture<StatefulWorkflowResult<String>> future = workflow.startAsync("input");
StatefulWorkflowResult<String> result = future.get();

// Async resume
CompletableFuture<StatefulWorkflowResult<String>> resumeFuture = 
    workflow.resumeAsync("input", savedState);
```

## Best Practices

### Node Design
- Keep nodes focused on single responsibilities
- Use descriptive node IDs and names
- Handle errors gracefully with appropriate commands
- Update state incrementally rather than wholesale replacement

### Route Design
- Use priority to resolve conflicting conditions
- Provide default routes as fallbacks
- Keep conditions simple and testable
- Document complex routing logic

### State Management
- Use consistent naming conventions for state keys
- Keep state serializable (avoid complex objects)
- Version your state for migration compatibility
- Clean up unnecessary state data

### Error Handling
- Implement proper error nodes
- Use ERROR commands for unrecoverable failures
- Log important state for debugging
- Provide meaningful error messages

### Testing
- Test individual nodes in isolation
- Verify routing conditions thoroughly
- Test suspension/resumption scenarios
- Validate workflow structure before deployment

## Migration from Regular Workflows

To migrate from traditional AgentWorkflow to StatefulWorkflow:

1. **Convert Nodes**: Change `AgentNode<I,O>` to `StatefulAgentNode<I>` returning `WorkflowCommand<I>`
2. **Add State Logic**: Identify what data needs to persist and add state updates
3. **Define Routes**: Replace linear execution with explicit routing
4. **Handle Commands**: Replace direct returns with command-based flow control
5. **Add Entry Points**: Mark appropriate nodes as entry points

## Performance Considerations

- State serialization overhead increases with state size
- Route evaluation happens for each transition
- Maximum execution steps prevent infinite loops
- Async execution can improve throughput
- Consider batching state updates for performance

## Troubleshooting

### Common Issues
- **No matching routes**: Ensure default routes exist or conditions are comprehensive
- **Infinite loops**: Set appropriate maxExecutionSteps limit
- **State corruption**: Validate state updates and handle serialization errors
- **Missing nodes**: Verify all route targets exist in the workflow

### Debugging
- Enable workflow validation during development
- Use metadata to track execution paths
- Log state changes for troubleshooting
- Monitor execution step counts

### Monitoring
- Track workflow completion rates
- Monitor suspension/resumption patterns
- Measure execution times per node
- Alert on excessive retries or errors