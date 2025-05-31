package dev.agents4j.workflow.adapters;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.StatefulAgentNode;
import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;
import java.util.Objects;

/**
 * Adapter that wraps a regular AgentNode to work as a StatefulAgentNode.
 * This allows traditional AgentNodes to be used in StatefulWorkflows.
 */
public class AgentNodeAdapter<S> implements StatefulAgentNode<S> {
    
    private final AgentNode<?, ?> wrappedNode;
    private final String nodeId;
    private final boolean isEntryPoint;
    private final boolean isLastNode;
    
    public AgentNodeAdapter(AgentNode<?, ?> wrappedNode, String nodeId, 
                           boolean isEntryPoint, boolean isLastNode) {
        this.wrappedNode = Objects.requireNonNull(wrappedNode, "Wrapped node cannot be null");
        this.nodeId = Objects.requireNonNull(nodeId, "Node ID cannot be null");
        this.isEntryPoint = isEntryPoint;
        this.isLastNode = isLastNode;
    }
    
    @Override
    public WorkflowCommand<S> process(WorkflowState<S> state) {
        try {
            // Get input from context or state
            Object input = state.getContextValue("current_input").orElse(state.getData());
            
            // Execute the wrapped AgentNode
            @SuppressWarnings("unchecked")
            Object output = wrappedNode.process(input, state.getContext());
            
            // Store the output in context for debugging and tracking
            String outputKey = "node_output_" + nodeId;
            
            if (isLastNode) {
                // Final node - complete the workflow with the output
                return WorkflowCommand.<S>complete()
                        .updateState(outputKey, output)
                        .updateState("final_output", output)
                        .addMetadata("completed_at", System.currentTimeMillis())
                        .build();
            } else {
                // Intermediate node - continue to next node
                // Store output as next input in context
                Integer currentStep = state.getContextValue("current_step", 0);
                
                return WorkflowCommand.<S>continueWith()
                        .updateState(outputKey, output)
                        .updateState("current_input", output)
                        .updateState("current_step", currentStep + 1)
                        .addMetadata("processed_at", System.currentTimeMillis())
                        .build();
            }
        } catch (Exception e) {
            // Handle errors gracefully
            return WorkflowCommand.<S>error("Error in node " + nodeId + ": " + e.getMessage())
                    .updateState("error_node", nodeId)
                    .updateState("error_message", e.getMessage())
                    .updateState("error_class", e.getClass().getSimpleName())
                    .addMetadata("error_timestamp", System.currentTimeMillis())
                    .build();
        }
    }
    
    @Override
    public String getNodeId() {
        return nodeId;
    }
    
    @Override
    public String getName() {
        return wrappedNode.getName();
    }
    
    @Override
    public boolean canBeEntryPoint() {
        return isEntryPoint;
    }
    
    @Override
    public boolean canSuspend() {
        // Chain workflows typically don't suspend
        return false;
    }
    
    /**
     * Gets the wrapped AgentNode.
     *
     * @return The original AgentNode
     */
    public AgentNode<?, ?> getWrappedNode() {
        return wrappedNode;
    }
    
    @Override
    public String toString() {
        return String.format("AgentNodeAdapter{nodeId='%s', name='%s', isEntryPoint=%s, isLastNode=%s}", 
                nodeId, getName(), isEntryPoint, isLastNode);
    }
}