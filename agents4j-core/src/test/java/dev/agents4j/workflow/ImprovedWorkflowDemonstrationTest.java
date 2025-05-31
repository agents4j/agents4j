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
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstration test showing the improved StatefulWorkflow architecture
 * following SOLID principles with better separation of concerns.
 */
class ImprovedWorkflowDemonstrationTest {

    private StatefulWorkflow<String, String> workflow;
    private TestExecutionMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new TestExecutionMonitor();
        
        // Create test nodes
        TestAgentNode startNode = new TestAgentNode("start", "Starting workflow", true);
        TestAgentNode processNode = new TestAgentNode("process", "Processing data", false);
        TestAgentNode endNode = new TestAgentNode("end", "Ending workflow", false);

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

        // Build workflow using improved architecture
        workflow = StatefulWorkflowImpl.<String, String>builder()
                .name("ImprovedDemoWorkflow")
                .addNode(startNode)
                .addNode(processNode)
                .addNode(endNode)
                .addRoute(startToProcess)
                .addRoute(processToEnd)
                .defaultEntryPoint(startNode)
                .outputExtractor((input, state, context) -> "Completed: " + input)
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
    void testImprovedWorkflowExecution() throws WorkflowExecutionException {
        String input = "process this data";
        
        StatefulWorkflowResult<String> result = workflow.start(input);
        
        assertTrue(result.isCompleted());
        assertFalse(result.isError());
        assertFalse(result.isSuspended());
        assertTrue(result.getOutput().isPresent());
        assertEquals("Completed: process this data", result.getOutput().get());
        
        // Verify monitoring was called
        assertTrue(monitor.workflowStarted);
        assertTrue(monitor.workflowCompleted);
        assertEquals(3, monitor.nodeExecutions.size()); // start, process, end
    }

    @Test
    void testAsyncWorkflowExecution() throws Exception {
        String input = "process async data";
        
        CompletableFuture<StatefulWorkflowResult<String>> future = workflow.startAsync(input);
        StatefulWorkflowResult<String> result = future.get();
        
        assertTrue(result.isCompleted());
        assertEquals("Completed: process async data", result.getOutput().get());
    }

    @Test
    void testWorkflowSuspensionAndResumption() throws WorkflowExecutionException {
        TestAgentNode suspendingNode = new TestAgentNode("suspend", "Suspending node", true) {
            @Override
            public WorkflowCommand<String> process(String input, WorkflowState state, Map<String, Object> context) {
                return WorkflowCommand.<String>suspend()
                        .updateState("suspended_at", System.currentTimeMillis())
                        .build();
            }
        };

        StatefulWorkflow<String, String> suspendWorkflow = StatefulWorkflowImpl.<String, String>builder()
                .name("SuspendTestWorkflow")
                .addNode(suspendingNode)
                .defaultEntryPoint(suspendingNode)
                .outputExtractor((input, state, context) -> "Resumed: " + input)
                .monitor(monitor)
                .build();

        // Start workflow and expect suspension
        StatefulWorkflowResult<String> result = suspendWorkflow.start("suspend me");
        
        assertTrue(result.isSuspended());
        assertFalse(result.isCompleted());
        assertFalse(result.isError());
        
        // Resume workflow
        WorkflowState suspendedState = result.getState();
        StatefulWorkflowResult<String> resumeResult = suspendWorkflow.resume("resumed data", suspendedState);
        
        assertTrue(resumeResult.isSuspended()); // Will suspend again
        assertTrue(monitor.workflowSuspended);
        assertTrue(monitor.workflowResumed);
    }

    @Test
    void testWorkflowMetadata() {
        // Test ISP - workflow metadata interface
        assertEquals("ImprovedDemoWorkflow", workflow.getName());
        assertEquals(3, workflow.getNodes().size());
        assertEquals(2, workflow.getRoutes().size());
        assertEquals(1, workflow.getEntryPoints().size());
        
        assertTrue(workflow.getNode("start").isPresent());
        assertTrue(workflow.getNode("process").isPresent());
        assertTrue(workflow.getNode("end").isPresent());
        assertFalse(workflow.getNode("nonexistent").isPresent());
        
        assertEquals(1, workflow.getRoutesFrom("start").size());
        assertEquals(1, workflow.getRoutesFrom("process").size());
        assertEquals(0, workflow.getRoutesFrom("end").size());
    }

    @Test
    void testWorkflowValidation() {
        assertDoesNotThrow(() -> workflow.validate());
    }

    @Test
    void testErrorHandling() throws WorkflowExecutionException {
        TestAgentNode errorNode = new TestAgentNode("error", "Error node", true) {
            @Override
            public WorkflowCommand<String> process(String input, WorkflowState state, Map<String, Object> context) {
                if (input.contains("error")) {
                    return WorkflowCommand.<String>error("Simulated error occurred")
                            .updateState("error_time", System.currentTimeMillis())
                            .build();
                }
                return WorkflowCommand.<String>complete().build();
            }
        };

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

        // Test error scenario
        StatefulWorkflowResult<String> result = errorWorkflow.start("trigger error");
        
        assertTrue(result.isError());
        assertFalse(result.isCompleted());
        assertFalse(result.isSuspended());
        assertTrue(result.getErrorMessage().isPresent());
        assertEquals("Simulated error occurred", result.getErrorMessage().get());
        
        assertTrue(monitor.workflowError);
    }

    /**
     * Test implementation of StatefulAgentNode
     */
    private static class TestAgentNode implements StatefulAgentNode<String> {
        private final String nodeId;
        private final String name;
        private final boolean canBeEntryPoint;

        public TestAgentNode(String nodeId, String name, boolean canBeEntryPoint) {
            this.nodeId = nodeId;
            this.name = name;
            this.canBeEntryPoint = canBeEntryPoint;
        }

        @Override
        public WorkflowCommand<String> process(String input, WorkflowState state, Map<String, Object> context) {
            context.put("processed_by", nodeId);
            context.put("processing_time", System.currentTimeMillis());
            
            return WorkflowCommand.<String>continueWith()
                    .updateState(nodeId + "_processed", true)
                    .withInput(input + " [processed by " + nodeId + "]")
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
     * Test implementation of WorkflowExecutionMonitor to verify monitoring works
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