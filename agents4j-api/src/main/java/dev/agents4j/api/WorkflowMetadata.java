package dev.agents4j.api;

import dev.agents4j.api.workflow.WorkflowRoute;
import java.util.List;
import java.util.Optional;

/**
 * Interface for workflow metadata and introspection.
 * Separated from execution concerns following ISP.
 *
 * @param <S> The type of the workflow state data
 */
public interface WorkflowMetadata<S> {
    
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
    List<StatefulAgentNode<S>> getNodes();
    
    /**
     * Gets all routes in this workflow.
     *
     * @return List of all routes
     */
    List<WorkflowRoute<S>> getRoutes();
    
    /**
     * Gets a node by its ID.
     *
     * @param nodeId The node ID
     * @return The node wrapped in Optional
     */
    Optional<StatefulAgentNode<S>> getNode(String nodeId);
    
    /**
     * Gets routes from a specific node.
     *
     * @param fromNodeId The source node ID
     * @return List of routes from the node
     */
    List<WorkflowRoute<S>> getRoutesFrom(String fromNodeId);
    
    /**
     * Gets the entry point nodes for this workflow.
     *
     * @return List of entry point nodes
     */
    List<StatefulAgentNode<S>> getEntryPoints();
    
    /**
     * Validates the workflow configuration.
     *
     * @throws IllegalStateException if the workflow configuration is invalid
     */
    void validate() throws IllegalStateException;
}