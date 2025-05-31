package dev.agents4j.workflow.api;

import dev.agents4j.api.StatefulAgentNode;
import dev.agents4j.api.workflow.WorkflowState;
import java.util.Optional;

/**
 * Strategy interface for determining the next node in workflow execution.
 * Follows Strategy pattern for different routing algorithms.
 *
 * @param <I> The input type
 */
public interface NodeRoutingStrategy<I> {
    
    /**
     * Finds the next node to execute based on the current node and state.
     *
     * @param currentNode The current node
     * @param input The current input
     * @param state The current workflow state
     * @return The next node to execute, or empty if no next node
     */
    Optional<StatefulAgentNode<I>> findNextNode(StatefulAgentNode<I> currentNode, 
                                                I input, 
                                                WorkflowState state);
    
    /**
     * Gets the name of this routing strategy.
     *
     * @return A descriptive name for this routing strategy
     */
    String getStrategyName();
    
    /**
     * Determines if this strategy can route from the given node.
     *
     * @param currentNode The node to check
     * @return true if this strategy can handle routing from the node
     */
    default boolean canRoute(StatefulAgentNode<I> currentNode) {
        return true;
    }
    
    /**
     * Gets the priority of this routing strategy. Higher numbers indicate higher priority.
     * When multiple strategies can route from the same node, the one with
     * the highest priority will be selected.
     *
     * @return The priority level (default is 0)
     */
    default int getPriority() {
        return 0;
    }
}