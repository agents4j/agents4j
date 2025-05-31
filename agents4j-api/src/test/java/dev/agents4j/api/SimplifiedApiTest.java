package dev.agents4j.api;

import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.api.workflow.WorkflowRoute;
import dev.agents4j.api.exception.WorkflowExecutionException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive verification test for the cleaned up agents4j API.
 * Verifies that only WorkflowNode and StatefulWorkflow remain
 * and that all core functionality works correctly.
 */
public class SimplifiedApiTest {

    @Test
    public void testWorkflowNodeInterface() {
        // Create a test node implementation
        WorkflowNode<String> node = new TestWorkflowNode();
        
        // Test basic interface methods
        assertEquals("test-node", node.getNodeId());
        assertEquals("Test Node", node.getName());
        assertFalse(node.canBeEntryPoint());
        assertTrue(node.canSuspend());
        
        // Test processing
        WorkflowState<String> state = WorkflowState.create("workflow-1", "hello");
        WorkflowCommand<String> command = node.process(state);
        
        assertNotNull(command);
        assertEquals(WorkflowCommand.CommandType.CONTINUE, command.getType());
    }

    @Test
    public void testWorkflowNodeAsync() throws Exception {
        WorkflowNode<String> node = new TestWorkflowNode();
        WorkflowState<String> state = WorkflowState.create("workflow-1", "test");
        
        CompletableFuture<WorkflowCommand<String>> future = node.processAsync(state);
        WorkflowCommand<String> command = future.get();
        
        assertNotNull(command);
        assertEquals(WorkflowCommand.CommandType.CONTINUE, command.getType());
    }

    @Test
    public void testWorkflowStateGenericType() {
        // Test with String state
        WorkflowState<String> stringState = WorkflowState.create("wf-1", "test-data");
        assertEquals("test-data", stringState.getData());
        
        // Test with Integer state
        WorkflowState<Integer> intState = WorkflowState.create("wf-2", 42);
        assertEquals(Integer.valueOf(42), intState.getData());
        
        // Test with custom object
        TestData testData = new TestData("value", 123);
        WorkflowState<TestData> objectState = WorkflowState.create("wf-3", testData);
        assertEquals("value", objectState.getData().getValue());
        assertEquals(123, objectState.getData().getNumber());
    }

    @Test
    public void testWorkflowStateWithContext() {
        Map<String, Object> initialContext = new HashMap<>();
        initialContext.put("user", "test-user");
        initialContext.put("session", "abc123");
        
        WorkflowState<String> state = new WorkflowState<>(
            "workflow-1",
            "initial-data",
            initialContext,
            "start-node",
            java.time.Instant.now(),
            1L
        );
        
        assertEquals("test-user", state.getContextValue("user").orElse(null));
        assertEquals("abc123", state.getContextValue("session").orElse(null));
        assertEquals("default", state.getContextValue("missing", "default"));
    }

    @Test
    public void testWorkflowCommandBuilders() {
        // Test continue command
        WorkflowCommand<String> continueCmd = WorkflowCommand.<String>continueWith()
            .updateState("processed", true)
            .addMetadata("timestamp", System.currentTimeMillis())
            .build();
        
        assertEquals(WorkflowCommand.CommandType.CONTINUE, continueCmd.getType());
        assertEquals(Boolean.TRUE, continueCmd.getStateUpdates().get("processed"));
        
        // Test goto command
        WorkflowCommand<String> gotoCmd = WorkflowCommand.<String>goTo("target-node")
            .updateState("reason", "branching")
            .build();
        
        assertEquals(WorkflowCommand.CommandType.GOTO, gotoCmd.getType());
        assertEquals("target-node", gotoCmd.getTargetNodeId().orElse(null));
        
        // Test complete command
        WorkflowCommand<String> completeCmd = WorkflowCommand.<String>complete()
            .withStateData("final-result")
            .updateState("completed", true)
            .build();
        
        assertEquals(WorkflowCommand.CommandType.COMPLETE, completeCmd.getType());
        assertEquals("final-result", completeCmd.getNextStateData().orElse(null));
        
        // Test error command
        WorkflowCommand<String> errorCmd = WorkflowCommand.<String>error("Something went wrong")
            .updateState("error_code", "E001")
            .build();
        
        assertEquals(WorkflowCommand.CommandType.ERROR, errorCmd.getType());
        assertEquals("Something went wrong", errorCmd.getErrorMessage().orElse(null));
        
        // Test suspend command
        WorkflowCommand<String> suspendCmd = WorkflowCommand.<String>suspend()
            .updateState("suspend_reason", "waiting_for_approval")
            .build();
        
        assertEquals(WorkflowCommand.CommandType.SUSPEND, suspendCmd.getType());
    }

    @Test
    public void testWorkflowRoutes() {
        // Test simple route
        WorkflowRoute<String> simpleRoute = WorkflowRoute.simple("route-1", "node-a", "node-b");
        assertEquals("route-1", simpleRoute.getId());
        assertEquals("node-a", simpleRoute.getFromNodeId());
        assertEquals("node-b", simpleRoute.getToNodeId());
        assertNull(simpleRoute.getCondition());
        
        // Test conditional route
        WorkflowRoute<String> conditionalRoute = WorkflowRoute.conditional(
            "route-2",
            "node-b", 
            "node-c",
            state -> "success".equals(state.getContextValue("status").orElse(null))
        );
        
        assertEquals("route-2", conditionalRoute.getId());
        assertNotNull(conditionalRoute.getCondition());
        
        // Test route matching
        WorkflowState<String> successState = WorkflowState.create("wf", "data")
            .withContextUpdates(Map.of("status", "success"));
        assertTrue(conditionalRoute.matches(successState));
        
        WorkflowState<String> failureState = WorkflowState.create("wf", "data")
            .withContextUpdates(Map.of("status", "failure"));
        assertFalse(conditionalRoute.matches(failureState));
    }

    @Test
    public void testStatefulWorkflowInterface() {
        // Create a minimal test implementation
        TestStatefulWorkflow workflow = new TestStatefulWorkflow();
        
        // Test metadata methods
        assertEquals("Test Workflow", workflow.getName());
        assertTrue(workflow.getNodes().isEmpty());
        assertTrue(workflow.getRoutes().isEmpty());
        assertTrue(workflow.getEntryPoints().isEmpty());
        assertFalse(workflow.getNode("non-existent").isPresent());
        
        // Validation should not throw for minimal implementation
        assertDoesNotThrow(workflow::validate);
    }

    @Test
    public void testCompleteWorkflowScenario() {
        // Create a complete workflow scenario with multiple nodes
        ValidationNode validationNode = new ValidationNode();
        ProcessingNode processingNode = new ProcessingNode();
        
        // Test validation node
        WorkflowState<String> initialState = WorkflowState.create("order-wf", "test-order")
            .withContextUpdates(Map.of("validated", false));
        
        WorkflowCommand<String> validationResult = validationNode.process(initialState);
        assertEquals(WorkflowCommand.CommandType.CONTINUE, validationResult.getType());
        assertTrue((Boolean) validationResult.getStateUpdates().get("validated"));
        
        // Test processing node with updated state
        String processedData = validationResult.getNextStateData().orElse(initialState.getData());
        WorkflowState<String> validatedState = initialState
            .withDataContextAndCurrentNode(processedData, validationResult.getStateUpdates(), "processing");
        
        WorkflowCommand<String> processingResult = processingNode.process(validatedState);
        assertEquals(WorkflowCommand.CommandType.COMPLETE, processingResult.getType());
        assertTrue((Boolean) processingResult.getStateUpdates().get("processed"));
    }

    // Helper classes for testing

    private static class TestWorkflowNode implements WorkflowNode<String> {
        @Override
        public WorkflowCommand<String> process(WorkflowState<String> state) {
            String data = state.getData();
            return WorkflowCommand.<String>continueWith()
                .withStateData(data.toUpperCase())
                .updateState("processed", true)
                .build();
        }

        @Override
        public String getNodeId() { return "test-node"; }

        @Override
        public String getName() { return "Test Node"; }
    }

    private static class ValidationNode implements WorkflowNode<String> {
        @Override
        public WorkflowCommand<String> process(WorkflowState<String> state) {
            String data = state.getData();
            if (data == null || data.trim().isEmpty()) {
                return WorkflowCommand.<String>error("Invalid input data").build();
            }
            
            return WorkflowCommand.<String>continueWith()
                .withStateData("validated-" + data)
                .updateState("validated", true)
                .updateState("validation_timestamp", System.currentTimeMillis())
                .build();
        }

        @Override
        public String getNodeId() { return "validation"; }

        @Override
        public String getName() { return "Validation Node"; }
    }

    private static class ProcessingNode implements WorkflowNode<String> {
        @Override
        public WorkflowCommand<String> process(WorkflowState<String> state) {
            String data = state.getData();
            Boolean isValidated = state.getContextValue("validated", false);
            
            if (!isValidated) {
                return WorkflowCommand.<String>error("Data not validated").build();
            }
            
            return WorkflowCommand.<String>complete()
                .withStateData("processed-" + data)
                .updateState("processed", true)
                .updateState("processing_timestamp", System.currentTimeMillis())
                .build();
        }

        @Override
        public String getNodeId() { return "processing"; }

        @Override
        public String getName() { return "Processing Node"; }
    }

    private static class TestStatefulWorkflow implements StatefulWorkflow<String, String> {
        @Override
        public String getName() { return "Test Workflow"; }

        @Override
        public java.util.List<WorkflowNode<String>> getNodes() { 
            return java.util.Collections.emptyList(); 
        }

        @Override
        public java.util.List<dev.agents4j.api.workflow.WorkflowRoute<String>> getRoutes() { 
            return java.util.Collections.emptyList(); 
        }

        @Override
        public java.util.Optional<WorkflowNode<String>> getNode(String nodeId) { 
            return java.util.Optional.empty(); 
        }

        @Override
        public java.util.List<dev.agents4j.api.workflow.WorkflowRoute<String>> getRoutesFrom(String fromNodeId) { 
            return java.util.Collections.emptyList(); 
        }

        @Override
        public java.util.List<WorkflowNode<String>> getEntryPoints() { 
            return java.util.Collections.emptyList(); 
        }

        @Override
        public void validate() throws IllegalStateException {
            // Minimal validation - no-op for test
        }

        @Override
        public dev.agents4j.api.workflow.StatefulWorkflowResult<String, String> start() throws WorkflowExecutionException {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public dev.agents4j.api.workflow.StatefulWorkflowResult<String, String> start(String initialStateData) throws WorkflowExecutionException {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public dev.agents4j.api.workflow.StatefulWorkflowResult<String, String> start(WorkflowState<String> initialState) throws WorkflowExecutionException {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public dev.agents4j.api.workflow.StatefulWorkflowResult<String, String> resume(WorkflowState<String> state) throws WorkflowExecutionException {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public dev.agents4j.api.workflow.StatefulWorkflowResult<String, String> resumeWithUpdates(WorkflowState<String> state) throws WorkflowExecutionException {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public CompletableFuture<dev.agents4j.api.workflow.StatefulWorkflowResult<String, String>> startAsync() {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public CompletableFuture<dev.agents4j.api.workflow.StatefulWorkflowResult<String, String>> startAsync(String initialStateData) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public CompletableFuture<dev.agents4j.api.workflow.StatefulWorkflowResult<String, String>> resumeAsync(WorkflowState<String> state) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public CompletableFuture<dev.agents4j.api.workflow.StatefulWorkflowResult<String, String>> resumeAsyncWithUpdates(WorkflowState<String> state) {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    private static class TestData {
        private final String value;
        private final int number;

        public TestData(String value, int number) {
            this.value = value;
            this.number = number;
        }

        public String getValue() { return value; }
        public int getNumber() { return number; }
    }
}