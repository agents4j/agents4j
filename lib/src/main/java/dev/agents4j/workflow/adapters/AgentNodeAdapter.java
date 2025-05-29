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
public class AgentNodeAdapter<I> implements StatefulAgentNode<I> {
    
    private final AgentNode<I, ?> wrappedNode;
    private final String nodeId;
    private final boolean isEntryPoint;
    private final boolean isLastNode;
    
    public AgentNodeAdapter(AgentNode<I, ?> wrappedNode, String nodeId, 
                           boolean isEntryPoint, boolean isLastNode) {
        this.wrappedNode = Objects.requireNonNull(wrappedNode, "Wrapped node cannot be null");
        this.nodeId = Objects.requireNonNull(nodeId, "Node ID cannot be null");
        this.isEntryPoint = isEntryPoint;
        this.isLastNode = isLastNode;
    }
    
    @Override
    public WorkflowCommand<I> process(I input, WorkflowState state, Map<String, Object> context) {
        try {
            // Execute the wrapped AgentNode
            Object output = wrappedNode.process(input, context);
            
            // Store the output in state for debugging and tracking
            String outputKey = "node_output_" + nodeId;
            
            if (isLastNode) {
                // Final node - complete the workflow with the output
                return WorkflowCommand.<I>complete()
                        .updateState(outputKey, output)
                        .updateState("final_output", output)
                        .addMetadata("completed_at", System.currentTimeMillis())
                        .build();
            } else {
                // Intermediate node - continue to next node
                // In a chain workflow, output becomes the input to the next node
                @SuppressWarnings("unchecked")
                I nextInput = (I) output;
                
                return WorkflowCommand.<I>continueWith()
                        .updateState(outputKey, output)
                        .updateState("current_step", state.get("current_step", 0) + 1)
                        .withInput(nextInput)
                        .addMetadata("processed_at", System.currentTimeMillis())
                        .build();
            }
        } catch (Exception e) {
            // Handle errors gracefully
            return WorkflowCommand.<I>error("Error in node " + nodeId + ": " + e.getMessage())
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
    public AgentNode<I, ?> getWrappedNode() {
        return wrappedNode;
    }
    
    @Override
    public String toString() {
        return String.format("AgentNodeAdapter{nodeId='%s', name='%s', isEntryPoint=%s, isLastNode=%s}", 
                nodeId, getName(), isEntryPoint, isLastNode);
    }
}