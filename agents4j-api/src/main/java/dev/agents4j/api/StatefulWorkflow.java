package dev.agents4j.api;

/**
 * Main interface for stateful workflows that combines all workflow capabilities.
 * Uses composition of smaller interfaces following ISP while maintaining backward compatibility.
 *
 * This interface now composes the smaller, focused interfaces:
 * - WorkflowExecutor: synchronous execution operations
 * - AsyncWorkflowExecutor: asynchronous execution operations
 * - WorkflowMetadata: workflow introspection and metadata
 *
 * @param <S> The type of the workflow state data
 * @param <O> The output type for the workflow
 */
public interface StatefulWorkflow<S, O>
    extends
        WorkflowExecutor<S, O>,
        AsyncWorkflowExecutor<S, O>,
        WorkflowMetadata<S> {
    // This interface now composes the smaller, focused interfaces
    // providing backward compatibility while following ISP
}
