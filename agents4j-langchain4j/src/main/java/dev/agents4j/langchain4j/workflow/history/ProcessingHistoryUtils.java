package dev.agents4j.langchain4j.workflow.history;

import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;

import java.util.Optional;

/**
 * Utility methods for working with the processing history.
 */
public class ProcessingHistoryUtils {

    /**
     * Gets the processing history from the workflow state, creating a new one if it doesn't exist.
     *
     * @param state The workflow state
     * @return The processing history
     */
    public static ProcessingHistory getOrCreateHistory(GraphWorkflowState<?> state) {
        return state.context().get(ProcessingHistory.HISTORY_KEY)
            .orElse(new ProcessingHistory());
    }
    
    /**
     * Gets the latest output from a specific node.
     *
     * @param state The workflow state
     * @param nodeId The ID of the node
     * @return An Optional containing the output, or empty if not found
     */
    public static Optional<String> getLatestOutputFromNode(GraphWorkflowState<?> state, NodeId nodeId) {
        return state.context().get(ProcessingHistory.HISTORY_KEY)
            .flatMap(history -> history.getLatestFromNode(nodeId))
            .map(NodeInteraction::output);
    }
    
    /**
     * Gets the latest output from a specific node.
     *
     * @param state The workflow state
     * @param nodeIdString The string ID of the node
     * @return An Optional containing the output, or empty if not found
     */
    public static Optional<String> getLatestOutputFromNode(GraphWorkflowState<?> state, String nodeIdString) {
        return getLatestOutputFromNode(state, NodeId.of(nodeIdString));
    }
}