package dev.agents4j.api;

import dev.agents4j.api.workflow.AsyncWorkflowExecutor;
import dev.agents4j.api.workflow.WorkflowExecutor;
import dev.agents4j.api.workflow.WorkflowMetadata;

/**
 * Main interface for agent workflows combining all workflow capabilities.
 * This interface follows the Interface Segregation Principle by composing
 * smaller, focused interfaces rather than defining all methods directly.
 * 
 * A workflow coordinates the execution of multiple agent nodes
 * and manages the flow of information between them.
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public interface AgentWorkflow<I, O> extends 
    WorkflowExecutor<I, O>, 
    AsyncWorkflowExecutor<I, O>, 
    WorkflowMetadata {
    
    // This interface now composes focused interfaces rather than
    // defining all methods directly, following ISP (Interface Segregation Principle)
}
