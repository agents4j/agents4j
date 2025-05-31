package dev.agents4j.workflow;

import dev.agents4j.api.StatefulAgentNode;
import dev.agents4j.api.StatefulWorkflow;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowRoute;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.workflow.api.WorkflowExecutionMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test that demonstrates intended workflow behavior.
 * These tests should pass after implementing the improved workflow architecture.
 * 
 * Currently disabled (@Disabled) as they represent the target behavior, not current implementation.
 */
class WorkflowBehaviorTest {

    private StatefulWorkflow<String, String> workflow;
    private TestExecutionMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new TestExecutionMonitor();
        
        // Create nodes that preserve original input
        PreservingAgentNode startNode = new PreservingAgentNode("start", "Starting workflow", true);
        PreservingAgentNode processNode = new PreservingAgentNode("process", "Processing data", false);
        PreservingAgentNode endNode = new PreservingAgentNode("end", "Ending workflow", false);

        // Create routes
        WorkflowRoute<String> startToProcess = WorkflowRoute.<String>builder()
                .id("start-to-process")
                .from("start")
                .to("process")
                .condition((input, state) -> input.contains("process"))
                .priority(10)
                .build();

        WorkflowRoute<String> processToEnd = WorkflowRoute.<String>builder()
                .id("process-to-end")
                .from("process")
                .to("end")
                .build();

        // Build workflow with improved configuration
        workflow = StatefulWorkflowImpl.<String, String>builder()
                .name("BehaviorTestWorkflow")
                .addNode(startNode)
                .addNode(processNode)
                .addNode(endNode)
                .addRoute(startToProcess)
                .addRoute(processToEnd)
                .defaultEntryPoint(startNode)
                .outputExtractor((input, state, context) -> {
                    // Extract original input, not transformed input
                    String originalInput = (String) context.get("original_input");
                    return "Completed: " + (originalInput != null ? originalInput : input);
                })
                .configuration(WorkflowExecutionConfiguration.builder()
                        .maxExecutionSteps(100)
                        .maxExecutionTime(Duration.ofMinutes(5))
                        .enableMonitoring(true)
                        .enableMetrics(true)
                        .build())
                .monitor(monitor)
                .build();
    }

    @Test
    void testWorkflowPreservesOriginalInput() throws WorkflowExecutionException {
        String originalInput = "process this data";
        
        StatefulWorkflowResult<String> result = workflow.start(originalInput);
        
        assertTrue(result.isCompleted());
        assertFalse(result.isError());
        assertTrue(result.getOutput().isPresent());
        
        // Should preserve original input in final output
        assertEquals("Completed: process this data", result.getOutput().get());
        
        // Verify all nodes were executed
        assertEquals(3, monitor.nodeExecutions.size());
        assertEquals("start", monitor.nodeExecutions.get(0));
        assertEquals("process", monitor.nodeExecutions.get(1));
        assertEquals("end", monitor.nodeExecutions.get(2));
    }

    @Test
    void testNodeProcessingContextPreservation() throws WorkflowExecutionException {
        String input = "test input";
        
        StatefulWorkflowResult<String> result = workflow.start(input);
        
        assertTrue(result.isCompleted());
        
        // Verify that processing information is captured in state, not input transformation
        WorkflowState finalState = result.getState();
        assertTrue(finalState.get("start_processed").isPresent());
        assertTrue(finalState.get("process_processed").isPresent());
        assertTrue(finalState.get("end_processed").isPresent());
        
        // Verify original input is preserved
        assertEquals("Completed: test input", result.getOutput().get());
    }

    @Test
    void testErrorHandlingWithCleanMessages() throws WorkflowExecutionException {
        CleanErrorNode errorNode = new CleanErrorNode("error", "Error node", true);

        StatefulWorkflow<String, String> errorWorkflow = StatefulWorkflowImpl.<String, String>builder()
                .name("ErrorTestWorkflow")
                .addNode(errorNode)
                .defaultEntryPoint(errorNode)
                .outputExtractor((input, state, context) -> "Success: " + input)
                .configuration(WorkflowExecutionConfiguration.builder()
                        .cleanErrorMessages(true)
                        .build())
                .monitor(monitor)
                .build();

        StatefulWorkflowResult<String> result = errorWorkflow.start("trigger error");
        
        assertTrue(result.isError());
        assertTrue(result.getErrorMessage().isPresent());
        
        // Should return clean error message without error codes
        assertEquals("Simulated error occurred", result.getErrorMessage().get());
    }

    @Test
    void testWorkflowCompletionFromFinalNode() throws WorkflowExecutionException {
        // Test that workflow completes properly when the last node explicitly completes
        CompletingAgentNode completeNode = new CompletingAgentNode("complete", "Completing node", true);

        StatefulWorkflow<String, String> completeWorkflow = StatefulWorkflowImpl.<String, String>builder()
                .name("CompleteTestWorkflow")
                .addNode(completeNode)
                .defaultEntryPoint(completeNode)
                .outputExtractor((input, state, context) -> {
                    String originalInput = (String) context.get("original_input");
                    return "Finished: " + (originalInput != null ? originalInput : input);
                })
                .build();

        StatefulWorkflowResult<String> result = completeWorkflow.start("complete this");
        
        assertTrue(result.isCompleted());
        assertEquals("Finished: complete this", result.getOutput().get());
    }

    @Test
    void testAsyncWorkflowWithProperInputHandling() throws Exception {
        String input = "async process data";
        
        CompletableFuture<StatefulWorkflowResult<String>> future = workflow.startAsync(input);
        StatefulWorkflowResult<String> result = future.get();
        
        assertTrue(result.isCompleted());
        assertEquals("Completed: async process data", result.getOutput().get());
    }

    @Test
    @Disabled("Demonstrates intended behavior - currently fails")
    void testWorkflowSuspensionAndResumptionWithStateManagement() throws WorkflowExecutionException {
        SuspendingNode suspendNode = new SuspendingNode("suspend", "Suspending node", true);
        ResumingNode resumeNode = new ResumingNode("resume", "Resuming node", false);

        WorkflowRoute<String> suspendToResume = WorkflowRoute.<String>builder()
                .id("suspend-to-resume")
                .from("suspend")
                .to("resume")
                .build();

        StatefulWorkflow<String, String> suspendWorkflow = StatefulWorkflowImpl.<String, String>builder()
                .name("SuspendTestWorkflow")
                .addNode(suspendNode)
                .addNode(resumeNode)
                .addRoute(suspendToResume)
                .defaultEntryPoint(suspendNode)
                .outputExtractor((input, state, context) -> {
                    String originalInput = (String) context.get("original_input");
                    return "Resumed: " + (originalInput != null ? originalInput : input);
                })
                .monitor(monitor)
                .build();

        // Start workflow and expect suspension
        StatefulWorkflowResult<String> result = suspendWorkflow.start("suspend me");
        
        assertTrue(result.isSuspended());
        assertFalse(result.isCompleted());
        
        // Resume workflow with new data
        WorkflowState suspendedState = result.getState();
        StatefulWorkflowResult<String> resumeResult = suspendWorkflow.resume("resumed data", suspendedState);
        
        // Should complete after resumption
        assertTrue(resumeResult.isCompleted());
        assertEquals("Resumed: suspend me", resumeResult.getOutput().get());
        
        assertTrue(monitor.workflowSuspended);
        assertTrue(monitor.workflowResumed);
        assertTrue(monitor.workflowCompleted);
    }

    /**
     * Test node that preserves original input and stores processing info in state
     */
    private static class PreservingAgentNode implements StatefulAgentNode<String> {
        private final String nodeId;
        private final String name;
        private final boolean canBeEntryPoint;

        public PreservingAgentNode(String nodeId, String name, boolean canBeEntryPoint) {
            this.nodeId = nodeId;
            this.name = name;
            this.canBeEntryPoint = canBeEntryPoint;
        }

        @Override
        public WorkflowCommand<String> process(String input, WorkflowState state, Map<String, Object> context) {
            // Store original input on first execution
            if (!context.containsKey("original_input")) {
                context.put("original_input", input);
            }
            
            // Store processing information in context and state, not input
            context.put("processed_by", nodeId);
            context.put("processing_time", System.currentTimeMillis());
            
            return WorkflowCommand.<String>continueWith()
                    .updateState(nodeId + "_processed", true)
                    .updateState("last_processed_by", nodeId)
                    // Don't transform input - pass it through unchanged
                    .withInput(input)
                    .build();
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean canBeEntryPoint() {
            return canBeEntryPoint;
        }
    }

    /**
     * Test node that returns clean error messages
     */
    private static class CleanErrorNode implements StatefulAgentNode<String> {
        private final String nodeId;
        private final String name;
        private final boolean canBeEntryPoint;

        public CleanErrorNode(String nodeId, String name, boolean canBeEntryPoint) {
            this.nodeId = nodeId;
            this.name = name;
            this.canBeEntryPoint = canBeEntryPoint;
        }

        @Override
        public WorkflowCommand<String> process(String input, WorkflowState state, Map<String, Object> context) {
            if (input.contains("error")) {
                return WorkflowCommand.<String>error("Simulated error occurred")
                        .updateState("error_time", System.currentTimeMillis())
                        .build();
            }
            return WorkflowCommand.<String>complete().build();
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean canBeEntryPoint() {
            return canBeEntryPoint;
        }
    }

    /**
     * Test node that explicitly completes the workflow
     */
    private static class CompletingAgentNode implements StatefulAgentNode<String> {
        private final String nodeId;
        private final String name;
        private final boolean canBeEntryPoint;

        public CompletingAgentNode(String nodeId, String name, boolean canBeEntryPoint) {
            this.nodeId = nodeId;
            this.name = name;
            this.canBeEntryPoint = canBeEntryPoint;
        }

        @Override
        public WorkflowCommand<String> process(String input, WorkflowState state, Map<String, Object> context) {
            // Store original input
            if (!context.containsKey("original_input")) {
                context.put("original_input", input);
            }
            
            context.put("processed_by", nodeId);
            
            return WorkflowCommand.<String>complete()
                    .updateState(nodeId + "_processed", true)
                    .build();
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean canBeEntryPoint() {
            return canBeEntryPoint;
        }
    }

    /**
     * Test node that suspends workflow execution
     */
    private static class SuspendingNode implements StatefulAgentNode<String> {
        private final String nodeId;
        private final String name;
        private final boolean canBeEntryPoint;

        public SuspendingNode(String nodeId, String name, boolean canBeEntryPoint) {
            this.nodeId = nodeId;
            this.name = name;
            this.canBeEntryPoint = canBeEntryPoint;
        }

        @Override
        public WorkflowCommand<String> process(String input, WorkflowState state, Map<String, Object> context) {
            // Store original input
            if (!context.containsKey("original_input")) {
                context.put("original_input", input);
            }
            
            return WorkflowCommand.<String>suspend()
                    .updateState("suspended_at", System.currentTimeMillis())
                    .updateState("suspend_reason", "Manual suspension for testing")
                    .build();
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean canBeEntryPoint() {
            return canBeEntryPoint;
        }
    }

    /**
     * Test node that completes workflow after resumption
     */
    private static class ResumingNode implements StatefulAgentNode<String> {
        private final String nodeId;
        private final String name;
        private final boolean canBeEntryPoint;

        public ResumingNode(String nodeId, String name, boolean canBeEntryPoint) {
            this.nodeId = nodeId;
            this.name = name;
            this.canBeEntryPoint = canBeEntryPoint;
        }

        @Override
        public WorkflowCommand<String> process(String input, WorkflowState state, Map<String, Object> context) {
            context.put("resumed_by", nodeId);
            context.put("resume_time", System.currentTimeMillis());
            
            return WorkflowCommand.<String>complete()
                    .updateState("resumed_at", System.currentTimeMillis())
                    .build();
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean canBeEntryPoint() {
            return canBeEntryPoint;
        }
    }

    /**
     * Test implementation of WorkflowExecutionMonitor
     */
    private static class TestExecutionMonitor implements WorkflowExecutionMonitor {
        boolean workflowStarted = false;
        boolean workflowCompleted = false;
        boolean workflowSuspended = false;
        boolean workflowResumed = false;
        boolean workflowError = false;
        List<String> nodeExecutions = new ArrayList<>();

        @Override
        public void onWorkflowStarted(String workflowId, String workflowName, Map<String, Object> context) {
            workflowStarted = true;
        }

        @Override
        public void onNodeStarted(String workflowId, String nodeId, WorkflowState state) {
            // Track node starts
        }

        @Override
        public void onNodeCompleted(String workflowId, String nodeId, Duration executionTime, WorkflowState newState) {
            nodeExecutions.add(nodeId);
        }

        @Override
        public void onWorkflowCompleted(String workflowId, Duration totalTime, WorkflowState finalState) {
            workflowCompleted = true;
        }

        @Override
        public void onWorkflowSuspended(String workflowId, WorkflowState state, String reason) {
            workflowSuspended = true;
        }

        @Override
        public void onWorkflowError(String workflowId, String error, WorkflowState state, Exception cause) {
            workflowError = true;
        }

        @Override
        public void onStateUpdated(String workflowId, WorkflowState oldState, WorkflowState newState) {
            // Track state updates
        }

        @Override
        public void onWorkflowResumed(String workflowId, WorkflowState state) {
            workflowResumed = true;
        }
    }
}