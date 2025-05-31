package dev.agents4j.api;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.api.workflow.WorkflowRoute;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Main interface for stateful workflows that combines all workflow capabilities.
 * Uses composition of smaller interfaces following ISP while maintaining backward compatibility.
 * 
 * This interface now composes the smaller, focused interfaces:
 * - WorkflowExecutor: synchronous execution operations
 * - AsyncWorkflowExecutor: asynchronous execution operations  
 * - WorkflowMetadata: workflow introspection and metadata
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public interface StatefulWorkflow<I, O> extends WorkflowExecutor<I, O>, 
                                                 AsyncWorkflowExecutor<I, O>, 
                                                 WorkflowMetadata<I> {
    // This interface now composes the smaller, focused interfaces
    // providing backward compatibility while following ISP
}