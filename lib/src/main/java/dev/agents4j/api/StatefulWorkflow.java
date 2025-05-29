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
 * Interface for stateful workflows that can be suspended and resumed.
 * StatefulWorkflows maintain state across executions and support
 * graph-based routing between nodes.
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public interface StatefulWorkflow<I, O> {
    
    /**
     * Starts a new workflow execution with the given input.
     *
     * @param input The initial input
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<O> start(I input) throws WorkflowExecutionException;
    
    /**
     * Starts a new workflow execution with input and context.
     *
     * @param input The initial input
     * @param context Additional context
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<O> start(I input, Map<String, Object> context) throws WorkflowExecutionException;
    
    /**
     * Starts a new workflow execution with initial state.
     *
     * @param input The initial input
     * @param initialState The initial workflow state
     * @param context Additional context
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<O> start(I input, WorkflowState initialState, Map<String, Object> context) 
            throws WorkflowExecutionException;
    
    /**
     * Resumes a suspended workflow execution.
     *
     * @param input The input for resumption
     * @param state The saved workflow state
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<O> resume(I input, WorkflowState state) throws WorkflowExecutionException;
    
    /**
     * Resumes a suspended workflow execution with context.
     *
     * @param input The input for resumption
     * @param state The saved workflow state
     * @param context Additional context
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<O> resume(I input, WorkflowState state, Map<String, Object> context) 
            throws WorkflowExecutionException;
    
    /**
     * Starts workflow execution asynchronously.
     *
     * @param input The initial input
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<StatefulWorkflowResult<O>> startAsync(I input);
    
    /**
     * Starts workflow execution asynchronously with context.
     *
     * @param input The initial input
     * @param context Additional context
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<StatefulWorkflowResult<O>> startAsync(I input, Map<String, Object> context);
    
    /**
     * Resumes workflow execution asynchronously.
     *
     * @param input The input for resumption
     * @param state The saved workflow state
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<StatefulWorkflowResult<O>> resumeAsync(I input, WorkflowState state);
    
    /**
     * Resumes workflow execution asynchronously with context.
     *
     * @param input The input for resumption
     * @param state The saved workflow state
     * @param context Additional context
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<StatefulWorkflowResult<O>> resumeAsync(I input, WorkflowState state, Map<String, Object> context);
    
    /**
     * Gets the workflow name.
     *
     * @return The workflow name
     */
    String getName();
    
    /**
     * Gets all registered nodes in this workflow.
     *
     * @return List of all nodes
     */
    List<StatefulAgentNode<I>> getNodes();
    
    /**
     * Gets all routes in this workflow.
     *
     * @return List of all routes
     */
    List<WorkflowRoute<I>> getRoutes();
    
    /**
     * Gets a node by its ID.
     *
     * @param nodeId The node ID
     * @return The node wrapped in Optional
     */
    Optional<StatefulAgentNode<I>> getNode(String nodeId);
    
    /**
     * Gets routes from a specific node.
     *
     * @param fromNodeId The source node ID
     * @return List of routes from the node
     */
    List<WorkflowRoute<I>> getRoutesFrom(String fromNodeId);
    
    /**
     * Gets the entry point nodes for this workflow.
     *
     * @return List of entry point nodes
     */
    List<StatefulAgentNode<I>> getEntryPoints();
    
    /**
     * Validates the workflow configuration.
     *
     * @throws IllegalStateException if the workflow configuration is invalid
     */
    void validate() throws IllegalStateException;
}