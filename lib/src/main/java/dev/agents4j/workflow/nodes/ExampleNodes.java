package dev.agents4j.workflow.nodes;

import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;

/**
 * Example implementations of StatefulAgentNode for demonstration purposes.
 */
public class ExampleNodes {
    
    /**
     * A simple input validation node that checks if input meets certain criteria.
     */
    public static class ValidationNode extends AbstractStatefulAgentNode<String> {
        
        public ValidationNode() {
            super("validation", "Input Validation Node", true);
        }
        
        @Override
        protected WorkflowCommand<String> doProcess(String input, WorkflowState state, Map<String, Object> context) {
            if (input == null || input.trim().isEmpty()) {
                return continueWith()
                        .updateState("valid", false)
                        .updateState("error", "Input cannot be empty")
                        .build();
            }
            
            if (input.length() < 3) {
                return continueWith()
                        .updateState("valid", false)
                        .updateState("error", "Input too short")
                        .build();
            }
            
            return continueWith()
                    .updateState("valid", true)
                    .updateState("processedInput", input.trim().toLowerCase())
                    .build();
        }
    }
    
    /**
     * A processing node that transforms the input based on state.
     */
    public static class ProcessingNode extends AbstractStatefulAgentNode<String> {
        
        public ProcessingNode() {
            super("processing", "Data Processing Node");
        }
        
        @Override
        protected WorkflowCommand<String> doProcess(String input, WorkflowState state, Map<String, Object> context) {
            boolean isValid = getStateValue(state, "valid", false);
            
            if (!isValid) {
                return goTo("error").build();
            }
            
            String processedInput = getStateValue(state, "processedInput", input);
            String result = "PROCESSED: " + processedInput.toUpperCase();
            
            return continueWith()
                    .updateState("result", result)
                    .updateState("processingComplete", true)
                    .withInput(result)
                    .build();
        }
    }
    
    /**
     * A decision node that routes based on processing results.
     */
    public static class DecisionNode extends AbstractStatefulAgentNode<String> {
        
        public DecisionNode() {
            super("decision", "Decision Node");
        }
        
        @Override
        protected WorkflowCommand<String> doProcess(String input, WorkflowState state, Map<String, Object> context) {
            String result = getStateValue(state, "result", "");
            
            if (result.contains("ERROR") || result.contains("FAIL")) {
                return goTo("error")
                        .updateState("decision", "error")
                        .build();
            }
            
            if (result.length() > 20) {
                return goTo("summary")
                        .updateState("decision", "summarize")
                        .build();
            }
            
            return goTo("output")
                    .updateState("decision", "direct")
                    .build();
        }
    }
    
    /**
     * A summarization node for long results.
     */
    public static class SummaryNode extends AbstractStatefulAgentNode<String> {
        
        public SummaryNode() {
            super("summary", "Summary Node");
        }
        
        @Override
        protected WorkflowCommand<String> doProcess(String input, WorkflowState state, Map<String, Object> context) {
            String result = getStateValue(state, "result", input);
            String summary = "SUMMARY: " + result.substring(0, Math.min(15, result.length())) + "...";
            
            return complete()
                    .updateState("finalResult", summary)
                    .updateState("summarized", true)
                    .withInput(summary)
                    .build();
        }
    }
    
    /**
     * An output node that finalizes the result.
     */
    public static class OutputNode extends AbstractStatefulAgentNode<String> {
        
        public OutputNode() {
            super("output", "Output Node");
        }
        
        @Override
        protected WorkflowCommand<String> doProcess(String input, WorkflowState state, Map<String, Object> context) {
            String result = getStateValue(state, "result", input);
            
            return complete()
                    .updateState("finalResult", result)
                    .updateState("completed", true)
                    .withInput(result)
                    .build();
        }
    }
    
    /**
     * An error handling node.
     */
    public static class ErrorNode extends AbstractStatefulAgentNode<String> {
        
        public ErrorNode() {
            super("error", "Error Handler Node");
        }
        
        @Override
        protected WorkflowCommand<String> doProcess(String input, WorkflowState state, Map<String, Object> context) {
            String error = getStateValue(state, "error", "Unknown error");
            
            return complete()
                    .updateState("finalResult", "ERROR: " + error)
                    .updateState("hasError", true)
                    .withInput("ERROR: " + error)
                    .build();
        }
    }
    
    /**
     * A retry node that can attempt processing multiple times.
     */
    public static class RetryNode extends AbstractStatefulAgentNode<String> {
        
        private final int maxRetries;
        
        public RetryNode(int maxRetries) {
            super("retry", "Retry Node");
            this.maxRetries = maxRetries;
        }
        
        @Override
        protected WorkflowCommand<String> doProcess(String input, WorkflowState state, Map<String, Object> context) {
            int attempts = incrementCounter(state, "attempts");
            
            // Simulate processing with potential failure
            boolean success = Math.random() > 0.3; // 70% success rate
            
            if (success) {
                return continueWith()
                        .updateState("attempts", attempts)
                        .updateState("success", true)
                        .updateState("result", "SUCCESS after " + attempts + " attempts")
                        .build();
            }
            
            if (attempts >= maxRetries) {
                return continueWith()
                        .updateState("attempts", attempts)
                        .updateState("success", false)
                        .updateState("result", "FAILED after " + attempts + " attempts")
                        .build();
            }
            
            // Retry by staying in the same node
            return goTo("retry")
                    .updateState("attempts", attempts)
                    .updateState("lastAttempt", System.currentTimeMillis())
                    .build();
        }
    }
    
    /**
     * A suspension node that demonstrates workflow suspension.
     */
    public static class SuspensionNode extends AbstractStatefulAgentNode<String> {
        
        public SuspensionNode() {
            super("suspension", "Suspension Demo Node");
        }
        
        @Override
        protected WorkflowCommand<String> doProcess(String input, WorkflowState state, Map<String, Object> context) {
            // Check if this is a resume operation
            boolean isResume = stateContains(state, "suspended");
            
            if (isResume) {
                String suspendedAt = getStateValue(state, "suspendedAt", "unknown");
                return continueWith()
                        .updateState("resumed", true)
                        .updateState("resumedAt", System.currentTimeMillis())
                        .updateState("result", "Resumed from suspension at " + suspendedAt)
                        .build();
            }
            
            // Suspend the workflow
            return suspend()
                    .updateState("suspended", true)
                    .updateState("suspendedAt", System.currentTimeMillis())
                    .updateState("reason", "Demonstrating suspension")
                    .build();
        }
    }
    
    /**
     * A conditional branching node that demonstrates multiple routing paths.
     */
    public static class BranchingNode extends AbstractStatefulAgentNode<String> {
        
        public BranchingNode() {
            super("branching", "Branching Node", true);
        }
        
        @Override
        protected WorkflowCommand<String> doProcess(String input, WorkflowState state, Map<String, Object> context) {
            if (input.startsWith("URGENT")) {
                return continueWith()
                        .updateState("priority", "high")
                        .updateState("branch", "urgent")
                        .build();
            }
            
            if (input.startsWith("REVIEW")) {
                return continueWith()
                        .updateState("priority", "medium")
                        .updateState("branch", "review")
                        .build();
            }
            
            return continueWith()
                    .updateState("priority", "normal")
                    .updateState("branch", "standard")
                    .build();
        }
    }
}