package dev.agents4j.langchain4j.workflow.history;

import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.graph.NodeId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Tracks the history of node interactions in a workflow.
 * Maintains an ordered list of all node interactions.
 */
public class ProcessingHistory {
    
    public static final ContextKey<ProcessingHistory> HISTORY_KEY = 
        ContextKey.of("processing_history", ProcessingHistory.class);
    
    private final List<NodeInteraction> interactions = new ArrayList<>();
    
    /**
     * Adds a new interaction to the history.
     * 
     * @param interaction The interaction to add
     */
    public void addInteraction(NodeInteraction interaction) {
        interactions.add(interaction);
    }
    
    /**
     * Gets all interactions in the history.
     * 
     * @return An unmodifiable list of all interactions
     */
    public List<NodeInteraction> getAllInteractions() {
        return Collections.unmodifiableList(interactions);
    }
    
    /**
     * Gets the most recent interaction from a specific node.
     * 
     * @param nodeId The ID of the node
     * @return An Optional containing the most recent interaction, or empty if none found
     */
    public Optional<NodeInteraction> getLatestFromNode(NodeId nodeId) {
        for (int i = interactions.size() - 1; i >= 0; i--) {
            NodeInteraction interaction = interactions.get(i);
            if (interaction.nodeId().equals(nodeId)) {
                return Optional.of(interaction);
            }
        }
        return Optional.empty();
    }
}