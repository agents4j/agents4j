package dev.agents4j.workflow;

import static org.junit.jupiter.api.Assertions.*;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.*;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.WorkflowError;
import dev.agents4j.api.validation.ValidationResult;
import dev.agents4j.workflow.builder.GraphWorkflowBuilder;
import dev.agents4j.workflow.output.OutputExtractor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Simple integration tests for GraphWorkflowImpl using actual implementations
 * instead of mocks. These tests verify the basic functionality and real behavior
 * of the workflow implementation.
 */
class SimpleGraphWorkflowImplTest {

    private static final ContextKey<String> USER_ID = ContextKey.stringKey("user.id");
    private static final ContextKey<Integer> STEP_COUNT = ContextKey.intKey("step.count");
    
    private GraphWorkflowImpl<String, String> workflow;
    private CountingNode startNode;
    private CountingNode middleNode;
    private CountingNode endNode;
    
    @BeforeEach
    void setUp() {
        // Create test nodes
        startNode = new CountingNode("start", NodeAction.TRAVERSE_TO_NEXT);
        middleNode = new CountingNode("middle", NodeAction.TRAVERSE_TO_NEXT);
        endNode = new CountingNode("end", NodeAction.COMPLETE);
        
        // Create workflow
        workflow = (GraphWorkflowImpl<String, String>) GraphWorkflowImpl.<String, String>builder()
            .name("Simple Test Workflow")
            .addNode(startNode)
            .addNode(middleNode)
            .addNode(endNode)
            .addEdge(startNode.getNodeId(), middleNode.getNodeId())
            .addEdge(middleNode.getNodeId(), endNode.getNodeId())
            .defaultEntryPoint(startNode.getNodeId())
            .outputExtractor(state -> "Output from workflow: " + state.data())
            .build();
    }
    
    @Test
    @DisplayName("Should execute simple linear workflow")
    void shouldExecuteSimpleLinearWorkflow() {
        // Arrange
        String input = "test-input";
        
        // Act
        WorkflowResult<String, WorkflowError> result = workflow.start(input);
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals("Output from workflow: test-input", result.getValue().get());
        
        // Verify execution path
        assertEquals(1, startNode.getProcessCount());
        assertEquals(1, middleNode.getProcessCount());
        assertEquals(1, endNode.getProcessCount());
    }
    
    @Test
    @DisplayName("Should maintain context throughout workflow")
    void shouldMaintainContextThroughoutWorkflow() {
        // Arrange
        String input = "context-test";
        WorkflowContext initialContext = WorkflowContext.empty()
            .with(USER_ID, "user-123")
            .with(STEP_COUNT, 0);
        
        // Configure nodes to update context
        startNode.setContextUpdater(ctx -> ctx.with(STEP_COUNT, ctx.get(STEP_COUNT).orElse(0) + 1));
        middleNode.setContextUpdater(ctx -> ctx.with(STEP_COUNT, ctx.get(STEP_COUNT).orElse(0) + 1));
        endNode.setContextUpdater(ctx -> ctx.with(STEP_COUNT, ctx.get(STEP_COUNT).orElse(0) + 1));
        
        // Use a custom output extractor that returns the step count
        OutputExtractor<String, String> countExtractor = state -> 
            "Steps: " + state.getContextOrDefault(STEP_COUNT, 0);
            
        GraphWorkflow<String, String> countingWorkflow = GraphWorkflowImpl.<String, String>builder()
            .name("Context Test Workflow")
            .addNode(startNode)
            .addNode(middleNode)
            .addNode(endNode)
            .addEdge(startNode.getNodeId(), middleNode.getNodeId())
            .addEdge(middleNode.getNodeId(), endNode.getNodeId())
            .defaultEntryPoint(startNode.getNodeId())
            .outputExtractor(countExtractor)
            .build();
        
        // Act
        WorkflowResult<String, WorkflowError> result = countingWorkflow.start(input, initialContext);
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals("Steps: 3", result.getValue().get());
    }
    
    @Test
    @DisplayName("Should suspend and resume workflow")
    void shouldSuspendAndResumeWorkflow() {
        // Arrange
        String input = "suspend-test";
        
        // Configure middle node to suspend
        middleNode.setAction(NodeAction.SUSPEND);
        middleNode.setSuspensionId("test-suspension");
        
        // Act - Start workflow (will suspend at middle node)
        WorkflowResult<String, WorkflowError> startResult = workflow.start(input);
        
        // Assert - Workflow is suspended
        assertTrue(startResult.isSuspended());
        assertEquals("test-suspension", startResult.getSuspension().get().suspensionId());
        
        // Get the suspended state from the suspension result
        Object suspensionState = startResult.getSuspension().get().suspensionState();
        @SuppressWarnings("unchecked")
        GraphWorkflowState<String> suspendedState = (GraphWorkflowState<String>) suspensionState;
        
        // Configure middle node to continue after resumption
        middleNode.setAction(NodeAction.TRAVERSE_TO_NEXT);
        
        // Act - Resume workflow
        WorkflowResult<String, WorkflowError> resumeResult = workflow.resume(suspendedState);
        
        // Assert - Workflow completed successfully
        assertTrue(resumeResult.isSuccess());
        assertTrue(resumeResult.getValue().isPresent());
        assertTrue(resumeResult.getValue().get().contains("suspend-test"));
        
        // Verify execution counts
        assertEquals(1, startNode.getProcessCount());
        assertEquals(2, middleNode.getProcessCount()); // Called during initial run and resume
        assertEquals(1, endNode.getProcessCount());
    }
    
    @Test
    @DisplayName("Should execute workflow asynchronously")
    void shouldExecuteWorkflowAsynchronously() throws Exception {
        // Arrange
        String input = "async-test";
        AtomicReference<String> resultValue = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        // Act
        CompletableFuture<WorkflowResult<String, WorkflowError>> future = workflow.startAsync(input);
        
        // Add completion handler
        future.thenAccept(result -> {
            if (result.isSuccess()) {
                resultValue.set(result.getValue().get());
            }
            latch.countDown();
        });
        
        // Wait for completion
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        
        // Assert
        assertTrue(completed, "Async workflow should complete within timeout");
        assertEquals("Output from workflow: async-test", resultValue.get());
    }
    
    @Test
    @DisplayName("Should validate workflow structure")
    void shouldValidateWorkflowStructure() {
        // Act
        ValidationResult validationResult = workflow.validate();
        
        // Assert
        assertTrue(validationResult.isValid());
        assertTrue(validationResult.getErrors().isEmpty());
        
        // Test an invalid workflow (no entry point)
        GraphWorkflowBuilder<String, String> invalidBuilder = GraphWorkflowImpl.<String, String>builder()
            .name("Invalid Workflow")
            .addNode(startNode)
            .addNode(endNode)
            .addEdge(startNode.getNodeId(), endNode.getNodeId());
            
        GraphWorkflow<String, String> invalidWorkflow = invalidBuilder.build();
        
        // Validate the invalid workflow
        ValidationResult invalidResult = invalidWorkflow.validate();
        
        // Should detect the missing entry point
        assertFalse(invalidResult.isValid());
        assertFalse(invalidResult.getErrors().isEmpty());
    }
    
    @Test
    @DisplayName("Should provide access to workflow structure")
    void shouldProvideAccessToWorkflowStructure() {
        // Act
        Map<NodeId, GraphWorkflowNode<String>> nodes = workflow.getNodes();
        Map<EdgeId, GraphEdge> edges = workflow.getEdges();
        Set<NodeId> entryPoints = workflow.getEntryPoints();
        
        // Assert
        assertEquals(3, nodes.size());
        assertEquals(2, edges.size());
        
        // For entry points, we might have one or more depending on implementation
        assertTrue(entryPoints.size() >= 1);
        assertTrue(entryPoints.contains(startNode.getNodeId()));
        
        // Check individual node access
        assertSame(startNode, workflow.getNode(startNode.getNodeId()));
        assertSame(middleNode, workflow.getNode(middleNode.getNodeId()));
        assertSame(endNode, workflow.getNode(endNode.getNodeId()));
        
        // Check edges from nodes
        Set<GraphEdge> startEdges = workflow.getEdgesFrom(startNode.getNodeId());
        assertTrue(startEdges.size() >= 1);
        boolean foundEdgeToMiddle = false;
        for (GraphEdge edge : startEdges) {
            if (edge.toNode().equals(middleNode.getNodeId())) {
                foundEdgeToMiddle = true;
                break;
            }
        }
        assertTrue(foundEdgeToMiddle, "Should find edge from start to middle node");
    }
    
    // Test node that counts the number of times it has been processed
    private static class CountingNode implements GraphWorkflowNode<String> {
        private final NodeId nodeId;
        private final String name;
        private NodeAction action;
        private String suspensionId = "suspended";
        private String suspensionReason = "Workflow suspended";
        private int processCount = 0;
        private java.util.function.Function<WorkflowContext, WorkflowContext> contextUpdater = ctx -> ctx;
        
        public CountingNode(String id, NodeAction action) {
            this.nodeId = NodeId.of(id);
            this.name = id + " Node";
            this.action = action;
        }
        
        @Override
        public WorkflowResult<GraphCommand<String>, WorkflowError> process(GraphWorkflowState<String> state) {
            processCount++;
            
            // Apply any context updates
            WorkflowContext updatedContext = contextUpdater.apply(state.context());
            
            // Perform the configured action
            switch (action) {
                case TRAVERSE_TO_NEXT:
                    NodeId nextNodeId;
                    if (nodeId.value().equals("start")) {
                        nextNodeId = NodeId.of("middle");
                    } else if (nodeId.value().equals("middle")) {
                        nextNodeId = NodeId.of("end");
                    } else {
                        nextNodeId = NodeId.of("next");
                    }
                    return WorkflowResult.success(
                        GraphCommandTraverse.toWithContext(nextNodeId, updatedContext)
                    );
                    
                case COMPLETE:
                    return WorkflowResult.success(
                        GraphCommandComplete.withResultAndContext("completed", updatedContext)
                    );
                    
                case SUSPEND:
                    // Create a suspend command with the suspension ID and reason
                    // Include context updates in the suspend command
                    GraphCommandSuspend<String> suspendCommand = GraphCommandSuspend.withId(suspensionId, suspensionReason);
                    // Since there's no direct way to add context, we'll still pass it and let the executor handle it
                    return WorkflowResult.success(suspendCommand);
                    
                default:
                    throw new IllegalStateException("Unknown action: " + action);
            }
        }
        
        @Override
        public WorkflowResult<GraphCommand<String>, WorkflowError> processWithLifecycle(GraphWorkflowState<String> state) {
            // For testing simplicity, just call the process method directly
            // This overrides the default implementation in GraphWorkflowNode
            return process(state);
        }
        
        @Override
        public NodeId getNodeId() {
            return nodeId;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getDescription() {
            return "A test node that counts processing calls";
        }
        
        public int getProcessCount() {
            return processCount;
        }
        
        public void setAction(NodeAction action) {
            this.action = action;
        }
        
        public void setSuspensionId(String suspensionId) {
            this.suspensionId = suspensionId;
        }
        
        public void setContextUpdater(java.util.function.Function<WorkflowContext, WorkflowContext> updater) {
            this.contextUpdater = updater;
        }
    }
    
    private enum NodeAction {
        TRAVERSE_TO_NEXT,
        COMPLETE,
        SUSPEND
    }
}