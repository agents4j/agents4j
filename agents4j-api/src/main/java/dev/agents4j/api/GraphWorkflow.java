package dev.agents4j.api;

import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.EdgeId;
import dev.agents4j.api.graph.GraphEdge;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.graph.WorkflowId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.WorkflowError;
import dev.agents4j.api.validation.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for graph-based workflow implementations.
 *
 * @param <S> The input state type for the workflow
 * @param <O> The output type for the workflow
 */
public interface GraphWorkflow<S, O> {
    /**
     * Starts a new workflow execution with the given input and empty context.
     *
     * @param input The workflow input
     * @return Result of the workflow execution
     */
    WorkflowResult<O, WorkflowError> start(S input);

    /**
     * Starts a new workflow execution with the given input and context.
     *
     * @param input The workflow input
     * @param context Initial context for the workflow
     * @return Result of the workflow execution
     */
    WorkflowResult<O, WorkflowError> start(S input, WorkflowContext context);

    /**
     * Starts a new workflow execution with the given ID, input and context.
     *
     * @param workflowId The workflow ID to use
     * @param input The workflow input
     * @param context Initial context for the workflow
     * @return Result of the workflow execution
     */
    WorkflowResult<O, WorkflowError> start(
        WorkflowId workflowId,
        S input,
        WorkflowContext context
    );

    /**
     * Resumes a suspended workflow execution with the given state.
     *
     * @param state The suspended workflow state to resume
     * @return Result of the resumed workflow execution
     */
    WorkflowResult<O, WorkflowError> resume(GraphWorkflowState<S> state);

    /**
     * Resumes a suspended workflow execution with the given state and context updates.
     *
     * @param state The suspended workflow state to resume
     * @param contextUpdates Additional context updates to apply
     * @return Result of the resumed workflow execution
     */
    WorkflowResult<O, WorkflowError> resume(
        GraphWorkflowState<S> state,
        WorkflowContext contextUpdates
    );

    /**
     * Starts the workflow execution asynchronously.
     *
     * @param input The workflow input
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<WorkflowResult<O, WorkflowError>> startAsync(S input);

    /**
     * Starts the workflow execution asynchronously with the given context.
     *
     * @param input The workflow input
     * @param context Initial context for the workflow
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<WorkflowResult<O, WorkflowError>> startAsync(
        S input,
        WorkflowContext context
    );

    /**
     * Resumes a suspended workflow asynchronously.
     *
     * @param state The suspended workflow state
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<WorkflowResult<O, WorkflowError>> resumeAsync(
        GraphWorkflowState<S> state
    );

    /**
     * Resumes a suspended workflow asynchronously with additional context updates.
     *
     * @param state The suspended workflow state
     * @param contextUpdates Additional context updates to apply
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<WorkflowResult<O, WorkflowError>> resumeAsync(
        GraphWorkflowState<S> state,
        WorkflowContext contextUpdates
    );

    /**
     * Gets the name of this workflow.
     *
     * @return The workflow name
     */
    String getName();

    /**
     * Gets all nodes in this workflow.
     *
     * @return Map of node IDs to nodes
     */
    Map<NodeId, GraphWorkflowNode<S>> getNodes();

    /**
     * Gets all edges in this workflow.
     *
     * @return Map of edge IDs to edges
     */
    Map<EdgeId, GraphEdge> getEdges();

    /**
     * Gets the node with the specified ID.
     *
     * @param nodeId The node ID
     * @return The node, or null if not found
     */
    GraphWorkflowNode<S> getNode(NodeId nodeId);

    /**
     * Gets all edges originating from the specified node.
     *
     * @param nodeId The source node ID
     * @return Set of edges from the node
     */
    Set<GraphEdge> getEdgesFrom(NodeId nodeId);

    /**
     * Gets all entry points for this workflow.
     *
     * @return Set of entry point node IDs
     */
    Set<NodeId> getEntryPoints();

    /**
     * Validates the workflow structure and configuration.
     *
     * @return The validation result
     */
    ValidationResult validate();
}
