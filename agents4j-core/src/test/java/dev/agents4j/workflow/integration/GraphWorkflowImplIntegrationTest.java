package dev.agents4j.workflow.integration;

import static org.junit.jupiter.api.Assertions.*;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.ExecutionContext;
import dev.agents4j.api.graph.GraphCommand;
import dev.agents4j.api.graph.GraphCommandComplete;
import dev.agents4j.api.graph.GraphCommandTraverse;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.WorkflowError;
import dev.agents4j.workflow.GraphWorkflowImpl;
import dev.agents4j.workflow.builder.GraphWorkflowBuilder;
import dev.agents4j.workflow.output.OutputExtractor;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration test for GraphWorkflowImpl with a real 3-node workflow.
 * Tests the complete workflow execution path with context passing between nodes.
 */
class GraphWorkflowImplIntegrationTest {

    // Context keys for testing
    private static final ContextKey<String> STEP_1_KEY = ContextKey.stringKey(
        "step1.result"
    );
    private static final ContextKey<String> STEP_2_KEY = ContextKey.stringKey(
        "step2.result"
    );
    private static final ContextKey<String> STEP_3_KEY = ContextKey.stringKey(
        "step3.result"
    );

    private static final ContextKey<String> STEP_SUSPENDING_KEY =
        ContextKey.stringKey("stepSuspending.result");

    private static final ContextKey<Instant> STEP_1_TIME = ContextKey.of(
        "step1.time",
        Instant.class
    );
    private static final ContextKey<Instant> STEP_2_TIME = ContextKey.of(
        "step2.time",
        Instant.class
    );
    private static final ContextKey<Instant> STEP_3_TIME = ContextKey.of(
        "step3.time",
        Instant.class
    );
    private static final ContextKey<Instant> STEP_SUSPENDING_TIME =
        ContextKey.of("stepSuspending.time", Instant.class);

    // Node IDs
    private static final NodeId NODE_A = NodeId.of("nodeA");
    private static final NodeId NODE_B = NodeId.of("nodeB");
    private static final NodeId NODE_C = NodeId.of("nodeC");
    private static final NodeId NODE_SUSPENDING = NodeId.of("nodeSuspending");

    private GraphWorkflow<String, String> workflow;
    private TestNode nodeA;
    private TestNode nodeB;
    private TestNode nodeC;
    private SuspendingNode nodeSuspending;

    @BeforeEach
    void setUp() {
        // Create test nodes
        nodeA = new TestNode(
            NODE_A,
            "Processing in Node A",
            STEP_1_KEY,
            STEP_1_TIME,
            NODE_B
        );
        nodeB = new TestNode(
            NODE_B,
            "Processing in Node B",
            STEP_2_KEY,
            STEP_2_TIME,
            NODE_C
        );
        nodeC = new TestNode(
            NODE_C,
            "Processing in Node C",
            STEP_3_KEY,
            STEP_3_TIME,
            null
        );
        nodeSuspending = new SuspendingNode(
            NODE_SUSPENDING,
            "Suspending in Node",
            STEP_SUSPENDING_KEY,
            STEP_SUSPENDING_TIME,
            NODE_B
        );

        // Create workflow using builder
        workflow = GraphWorkflowBuilder.<String, String>create(String.class)
            .name("integration-test-workflow")
            .addNode(nodeA)
            .addNode(nodeB)
            .addNode(nodeC)
            .addEdge(NODE_A, NODE_B)
            .addEdge(NODE_B, NODE_C)
            .defaultEntryPoint(NODE_A)
            .outputExtractor(createOutputExtractor())
            .build();
    }

    @Nested
    @DisplayName("Synchronous Workflow Execution")
    class SynchronousExecutionTests {

        @Test
        @DisplayName("Should execute complete workflow through all 3 nodes")
        void shouldExecuteCompleteWorkflowThroughAllNodes() {
            var result = workflow.start("test-input");

            assertTrue(result.isSuccess());
            assertFalse(result.isFailure());
            assertNotNull(result.getValue());

            String output = result.getValue().orElse(null);
            assertEquals(
                "Final result: test-input -> Node A -> Node B -> Node C",
                output
            );
        }

        @Test
        @DisplayName("Should preserve context through all nodes")
        void shouldPreserveContextThroughAllNodes() {
            var result = workflow.start("context-test");

            assertTrue(result.isSuccess());
            var finalContext = result.getFinalContext().orElse(null);

            assertNotNull(finalContext);

            // Verify all nodes wrote to context
            assertTrue(finalContext.contains(STEP_1_KEY));
            assertTrue(finalContext.contains(STEP_2_KEY));
            assertTrue(finalContext.contains(STEP_3_KEY));

            // Verify context values
            assertEquals(
                "Processing in Node A",
                finalContext.get(STEP_1_KEY).orElse(null)
            );
            assertEquals(
                "Processing in Node B",
                finalContext.get(STEP_2_KEY).orElse(null)
            );
            assertEquals(
                "Processing in Node C",
                finalContext.get(STEP_3_KEY).orElse(null)
            );

            // Verify timestamps are present and sequential
            var time1 = finalContext.get(STEP_1_TIME).orElse(null);
            var time2 = finalContext.get(STEP_2_TIME).orElse(null);
            var time3 = finalContext.get(STEP_3_TIME).orElse(null);

            assertNotNull(time1);
            assertNotNull(time2);
            assertNotNull(time3);

            assertTrue(time1.isBefore(time2) || time1.equals(time2));
            assertTrue(time2.isBefore(time3) || time2.equals(time3));
        }

        @Test
        @DisplayName("Should track workflow path correctly")
        void shouldTrackWorkflowPathCorrectly() {
            var result = workflow.start("path-test");

            assertTrue(result.isSuccess());
            var finalContext = result.getFinalContext().orElse(null);
            assertNotNull(finalContext);

            // Verify that all three nodes were executed based on context
            assertTrue(finalContext.contains(STEP_1_KEY));
            assertTrue(finalContext.contains(STEP_2_KEY));
            assertTrue(finalContext.contains(STEP_3_KEY));
        }

        @Test
        @DisplayName("Should execute with custom initial context")
        void shouldExecuteWithCustomInitialContext() {
            var initialContext = ExecutionContext.empty()
                .with(
                    ContextKey.stringKey("custom.value"),
                    "initial-custom-value"
                )
                .with(ContextKey.intKey("custom.number"), 42);

            var result = workflow.start("custom-context-test", initialContext);

            assertTrue(result.isSuccess());
            var finalContext = result.getFinalContext().orElse(null);
            assertNotNull(finalContext);

            // Verify initial context is preserved
            assertEquals(
                "initial-custom-value",
                finalContext
                    .get(ContextKey.stringKey("custom.value"))
                    .orElse(null)
            );
            assertEquals(
                42,
                finalContext
                    .get(ContextKey.intKey("custom.number"))
                    .orElse(null)
            );

            // Verify workflow execution context is also present
            assertTrue(finalContext.contains(STEP_1_KEY));
            assertTrue(finalContext.contains(STEP_2_KEY));
            assertTrue(finalContext.contains(STEP_3_KEY));
        }
    }

    @Nested
    @DisplayName("Asynchronous Workflow Execution")
    class AsynchronousExecutionTests {

        @Test
        @DisplayName("Should execute workflow asynchronously")
        void shouldExecuteWorkflowAsynchronously()
            throws ExecutionException, InterruptedException {
            var future = workflow.startAsync("async-test");
            var result = future.get();

            assertTrue(result.isSuccess());
            assertEquals(
                "Final result: async-test -> Node A -> Node B -> Node C",
                result.getValue().orElse(null)
            );
        }

        @Test
        @DisplayName("Should execute workflow asynchronously with context")
        void shouldExecuteWorkflowAsynchronouslyWithContext()
            throws ExecutionException, InterruptedException {
            var initialContext = ExecutionContext.empty()
                .with(ContextKey.stringKey("async.marker"), "async-execution");

            var future = workflow.startAsync(
                "async-context-test",
                initialContext
            );
            var result = future.get();

            assertTrue(result.isSuccess());
            var finalContext = result.getFinalContext().orElse(null);
            assertNotNull(finalContext);

            assertEquals(
                "async-execution",
                finalContext
                    .get(ContextKey.stringKey("async.marker"))
                    .orElse(null)
            );
            assertTrue(finalContext.contains(STEP_1_KEY));
            assertTrue(finalContext.contains(STEP_2_KEY));
            assertTrue(finalContext.contains(STEP_3_KEY));
        }
    }

    @Nested
    @DisplayName("Workflow Resume Tests")
    class WorkflowResumeTests {

        @Test
        @DisplayName("Should resume workflow from intermediate state")
        void shouldResumeWorkflowFromIntermediateState() {
            // Create a state positioned at node B
            var workflow = GraphWorkflowImpl.<String, String>builder()
                .name("resume-test")
                .addNode(nodeSuspending)
                .addNode(nodeB)
                .addNode(nodeC)
                .addEdge(NODE_B, NODE_C)
                .defaultEntryPoint(NODE_SUSPENDING)
                .outputExtractor(createOutputExtractor())
                .build();

            var suspendedResult = workflow.start("resume-test");

            assertTrue(suspendedResult.isSuspended());
            var suspension = suspendedResult.getSuspension().orElseThrow();

            @SuppressWarnings("unchecked")
            var suspendedState = (GraphWorkflowState<String>) suspension.suspensionState();
            var result = workflow.resume(suspendedState);

            assertTrue(result.isSuccess());
            assertEquals(
                "Final result: resume-test -> Node B -> Node C",
                result.getValue().orElse(null)
            );
        }

        @Test
        @DisplayName("Should resume workflow asynchronously")
        void shouldResumeWorkflowAsynchronously()
            throws ExecutionException, InterruptedException {
            // Create a state positioned at node C
            var finalNodeWorkflow = GraphWorkflowBuilder.<String, String>create(String.class)
                .name("async-resume-test")
                .addNode(nodeC)
                .defaultEntryPoint(NODE_C)
                .outputExtractor(createOutputExtractor())
                .build();

            var future = finalNodeWorkflow.startAsync("async-resume-test");
            var result = future.get();

            assertTrue(result.isSuccess());
            assertEquals(
                "Final result: async-resume-test -> Node C",
                result.getValue().orElse(null)
            );
        }
    }

    @Nested
    @DisplayName("Workflow Metadata Tests")
    class WorkflowMetadataTests {

        @Test
        @DisplayName("Should provide correct workflow metadata")
        void shouldProvideCorrectWorkflowMetadata() {
            assertEquals("integration-test-workflow", workflow.getName());
            assertEquals(3, workflow.getNodes().size());
            assertEquals(2, workflow.getEdges().size());

            // Only NODE_A should be an entry point since it has no incoming edges
            // NODE_B and NODE_C have incoming edges, so they shouldn't be entry points
            assertEquals(1, workflow.getEntryPoints().size());
            assertTrue(workflow.getEntryPoints().contains(NODE_A));
        }

        @Test
        @DisplayName("Should validate workflow structure")
        void shouldValidateWorkflowStructure() {
            var validationResult = workflow.validate();
            assertTrue(validationResult.isValid());
            assertTrue(validationResult.getErrors().isEmpty());
        }

        @Test
        @DisplayName("Should retrieve nodes and edges correctly")
        void shouldRetrieveNodesAndEdgesCorrectly() {
            assertEquals(nodeA, workflow.getNode(NODE_A));
            assertEquals(nodeB, workflow.getNode(NODE_B));
            assertEquals(nodeC, workflow.getNode(NODE_C));

            var edgesFromA = workflow.getEdgesFrom(NODE_A);
            assertEquals(1, edgesFromA.size());

            var edgesFromB = workflow.getEdgesFrom(NODE_B);
            assertEquals(1, edgesFromB.size());

            var edgesFromC = workflow.getEdgesFrom(NODE_C);
            assertEquals(0, edgesFromC.size());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle node processing errors gracefully")
        void shouldHandleNodeProcessingErrorsGracefully() {
            var errorNode = new ErrorTestNode(NodeId.of("errorNode"));

            var errorWorkflow = GraphWorkflowBuilder.<String, String>create(String.class)
                .name("error-test-workflow")
                .addNode(errorNode)
                .defaultEntryPoint(NodeId.of("errorNode"))
                .outputExtractor(createOutputExtractor())
                .build();

            var result = errorWorkflow.start("error-test");

            assertTrue(result.isFailure());
            assertNotNull(result.getError());
            assertEquals("TEST_ERROR", result.getError().orElse(null).code());
        }
    }

    /**
     * Creates an output extractor that builds a result from the context.
     */
    private OutputExtractor<String, String> createOutputExtractor() {
        return state -> {
            var input = state.data();
            var step1 = state
                .getContext(STEP_1_KEY)
                .map(s -> " -> Node A")
                .orElse("");
            var step2 = state
                .getContext(STEP_2_KEY)
                .map(s -> " -> Node B")
                .orElse("");
            var step3 = state
                .getContext(STEP_3_KEY)
                .map(s -> " -> Node C")
                .orElse("");

            return "Final result: " + input + step1 + step2 + step3;
        };
    }

    /**
     * Test node implementation that writes to context and optionally traverses to next node.
     */
    private static class TestNode implements GraphWorkflowNode<String> {

        private final NodeId nodeId;
        private final String message;
        private final ContextKey<String> contextKey;
        private final ContextKey<Instant> timeKey;
        private final NodeId nextNode;

        public TestNode(
            NodeId nodeId,
            String message,
            ContextKey<String> contextKey,
            ContextKey<Instant> timeKey,
            NodeId nextNode
        ) {
            this.nodeId = nodeId;
            this.message = message;
            this.contextKey = contextKey;
            this.timeKey = timeKey;
            this.nextNode = nextNode;
        }

        @Override
        public NodeId getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return nodeId.value();
        }

        @Override
        public boolean isEntryPoint() {
            // Only nodeA should be an entry point (no incoming edges)
            return nodeId.equals(NODE_A);
        }

        @Override
        public WorkflowResult<GraphCommand<String>, WorkflowError> process(
            GraphWorkflowState<String> state
        ) {
            try {
                // Write to context
                var updatedContext = state
                    .context()
                    .with(contextKey, message)
                    .with(timeKey, Instant.now());

                // Either traverse to next node or complete
                if (nextNode != null) {
                    return WorkflowResult.success(
                        GraphCommandTraverse.<String>toWithContext(
                            nextNode,
                            updatedContext
                        ),
                        updatedContext
                    );
                } else {
                    return WorkflowResult.success(
                        GraphCommandComplete.<String>withResultAndContext(
                            message,
                            updatedContext
                        ),
                        updatedContext
                    );
                }
            } catch (Exception e) {
                return WorkflowResult.failure(
                    dev.agents4j.api.result.error.ExecutionError.of(
                        "TEST_ERROR",
                        e.getMessage(),
                        nodeId.value()
                    )
                );
            }
        }
    }

    /**
     * Test node implementation that suspends the process
     */
    private static class SuspendingNode implements GraphWorkflowNode<String> {

        private final NodeId nodeId;
        private final String message;
        private final ContextKey<String> contextKey;
        private final ContextKey<Instant> timeKey;
        private final NodeId nextNode;

        public SuspendingNode(
            NodeId nodeId,
            String message,
            ContextKey<String> contextKey,
            ContextKey<Instant> timeKey,
            NodeId nextNode
        ) {
            this.nodeId = nodeId;
            this.message = message;
            this.contextKey = contextKey;
            this.timeKey = timeKey;
            this.nextNode = nextNode;
        }

        @Override
        public NodeId getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return nodeId.value();
        }

        @Override
        public boolean isEntryPoint() {
            // Only nodeA should be an entry point (no incoming edges)
            return nodeId.equals(NODE_A);
        }

        @Override
        public WorkflowResult<GraphCommand<String>, WorkflowError> process(
            GraphWorkflowState<String> state
        ) {
            return WorkflowResult.suspended(
                "suspended-id",
                state,
                "for testing suspension"
            );
        }
    }

    /**
     * Test node that always fails for error handling tests.
     */
    private static class ErrorTestNode implements GraphWorkflowNode<String> {

        private final NodeId nodeId;

        public ErrorTestNode(NodeId nodeId) {
            this.nodeId = nodeId;
        }

        @Override
        public NodeId getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return nodeId.value();
        }

        @Override
        public boolean isEntryPoint() {
            // Error nodes are not entry points
            return false;
        }

        @Override
        public WorkflowResult<GraphCommand<String>, WorkflowError> process(
            GraphWorkflowState<String> state
        ) {
            return WorkflowResult.failure(
                dev.agents4j.api.result.error.ExecutionError.of(
                    "TEST_ERROR",
                    "Test error from node",
                    nodeId.value()
                )
            );
        }
    }
}
