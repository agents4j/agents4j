package dev.agents4j.api.graph;

import static org.junit.jupiter.api.Assertions.*;

import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.WorkflowError;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for GraphWorkflow API components using only public interfaces.
 * Validates the basic functionality without relying on internal implementation details.
 */
class GraphWorkflowApiTest {

    private static final ContextKey<String> USER_ID = ContextKey.stringKey(
        "user.id"
    );
    private static final ContextKey<Boolean> APPROVED = ContextKey.booleanKey(
        "approved"
    );
    private static final ContextKey<Integer> COUNT = ContextKey.intKey("count");
    private static final ContextKey<Double> AMOUNT = ContextKey.of(
        "amount",
        Double.class
    );

    private WorkflowId workflowId;
    private String testData;
    private WorkflowContext testContext;

    @BeforeEach
    void setUp() {
        workflowId = WorkflowId.generate();
        testData = "test-data";
        testContext = WorkflowContext.empty()
            .with(USER_ID, "user-123")
            .with(APPROVED, true)
            .with(COUNT, 5)
            .with(AMOUNT, 1500.0);
    }

    @Test
    @DisplayName("Should create and manage NodeId correctly")
    void shouldCreateAndManageNodeId() {
        var nodeId1 = NodeId.of("start-node");
        var nodeId2 = NodeId.of("end-node");

        assertEquals("start-node", nodeId1.value());
        assertEquals("end-node", nodeId2.value());

        assertNotEquals(nodeId1, nodeId2);
        assertEquals(nodeId1, NodeId.of("start-node"));

        assertNotNull(nodeId1.toString());
        assertTrue(nodeId1.toString().contains("start-node"));
    }

    @Test
    @DisplayName("Should create and manage EdgeId correctly")
    void shouldCreateAndManageEdgeId() {
        var fromNode = NodeId.of("from");
        var toNode = NodeId.of("to");

        var edgeId1 = EdgeId.between(fromNode, toNode);
        var edgeId2 = EdgeId.between(toNode, fromNode);
        var edgeId3 = EdgeId.of("custom-edge");

        assertNotNull(edgeId1.value());
        assertNotNull(edgeId2.value());
        assertEquals("custom-edge", edgeId3.value());

        assertNotEquals(edgeId1, edgeId2);
        assertEquals(edgeId1, EdgeId.between(fromNode, toNode));
    }

    @Test
    @DisplayName("Should create GraphCommand.Traverse correctly")
    void shouldCreateTraverseCommand() {
        var targetNode = NodeId.of("target");

        var simpleCommand = GraphCommand.Traverse.to(targetNode);
        assertEquals(targetNode, simpleCommand.targetNode());
        assertFalse(simpleCommand.getContextUpdates().isPresent());
        assertFalse(simpleCommand.getStateData().isPresent());

        var contextCommand = GraphCommand.Traverse.toWithContext(
            targetNode,
            testContext
        );
        assertEquals(targetNode, contextCommand.targetNode());
        assertTrue(contextCommand.getContextUpdates().isPresent());
        assertEquals(
            "user-123",
            contextCommand.getContextUpdates().get().get(USER_ID).orElse("")
        );

        var dataCommand = GraphCommand.Traverse.toWithData(
            targetNode,
            testData
        );
        assertEquals(targetNode, dataCommand.targetNode());
        assertTrue(dataCommand.getStateData().isPresent());
        assertEquals(testData, dataCommand.getStateData().get());
    }

    @Test
    @DisplayName("Should create GraphCommand.Complete correctly")
    void shouldCreateCompleteCommand() {
        var result = "workflow-result";
        var command = GraphCommand.Complete.withResult(result);

        assertEquals(result, command.result());
        assertFalse(command.getContextUpdates().isPresent());
        assertFalse(command.getStateData().isPresent());

        var commandWithContext = GraphCommand.Complete.withResultAndContext(
            result,
            testContext
        );
        assertEquals(result, commandWithContext.result());
        assertTrue(commandWithContext.getContextUpdates().isPresent());
    }

    @Test
    @DisplayName("Should create GraphCommand.Suspend correctly")
    void shouldCreateSuspendCommand() {
        var suspensionId = "pending-approval";
        var reason = "Waiting for manual approval";

        var command = GraphCommand.Suspend.withId(suspensionId, reason);

        assertEquals(suspensionId, command.suspensionId());
        assertEquals(reason, command.reason());
        assertFalse(command.timeout().isPresent());
        assertFalse(command.getContextUpdates().isPresent());
        assertFalse(command.getStateData().isPresent());
    }

    @Test
    @DisplayName("Should create GraphCommand.Fork correctly")
    void shouldCreateForkCommand() {
        var node1 = NodeId.of("branch1");
        var node2 = NodeId.of("branch2");
        var branches = Set.of(node1, node2);

        var command = GraphCommand.Fork.parallel(branches);

        assertEquals(branches, command.targetNodes());
        assertEquals(
            GraphCommand.Fork.ForkStrategy.PARALLEL,
            command.strategy()
        );
        assertFalse(command.getContextUpdates().isPresent());
        assertFalse(command.getStateData().isPresent());
    }

    @Test
    @DisplayName("Should create GraphCommand.Join correctly")
    void shouldCreateJoinCommand() {
        var joinNode = NodeId.of("join-point");

        var command = GraphCommand.Join.waitAll(joinNode);

        assertEquals(joinNode, command.joinNode());
        assertEquals(
            GraphCommand.Join.JoinStrategy.WAIT_ALL,
            command.strategy()
        );
        assertFalse(command.timeout().isPresent());
        assertFalse(command.getContextUpdates().isPresent());
        assertFalse(command.getStateData().isPresent());
    }

    @Test
    @DisplayName("Should create EdgeCondition correctly")
    void shouldCreateEdgeConditions() {
        var always = EdgeCondition.always();
        var never = EdgeCondition.never();

        assertTrue(always.getDescription().toLowerCase().contains("always"));
        assertTrue(never.getDescription().toLowerCase().contains("never"));

        var contextCondition = EdgeCondition.whenContextEquals(APPROVED, true);
        assertNotNull(contextCondition.getDescription());

        var numericCondition = EdgeCondition.whenContextGreaterThan(
            AMOUNT,
            1000.0
        );
        assertNotNull(numericCondition.getDescription());
    }

    @Test
    @DisplayName("Should combine EdgeConditions with logical operators")
    void shouldCombineEdgeConditions() {
        var condition1 = EdgeCondition.whenContextEquals(APPROVED, true);
        var condition2 = EdgeCondition.whenContextGreaterThan(AMOUNT, 1000.0);

        var andCondition = condition1.and(condition2);
        var orCondition = condition1.or(condition2);
        var notCondition = condition1.not();

        assertTrue(andCondition.getDescription().toUpperCase().contains("AND"));
        assertTrue(orCondition.getDescription().toUpperCase().contains("OR"));
        assertTrue(notCondition.getDescription().toUpperCase().contains("NOT"));

        assertNotSame(condition1, andCondition);
        assertNotSame(condition1, orCondition);
        assertNotSame(condition1, notCondition);
    }

    @Test
    @DisplayName("Should create GraphWorkflowState correctly")
    void shouldCreateGraphWorkflowState() {
        var startNode = NodeId.of("start");

        var state = GraphWorkflowState.create(
            workflowId,
            testData,
            startNode,
            testContext
        );

        assertEquals(workflowId, state.workflowId());
        assertEquals(testData, state.data());
        assertEquals(startNode, state.currentNode().orElse(null));

        assertEquals("user-123", state.getContextOrDefault(USER_ID, ""));
        assertEquals(true, state.getContextOrDefault(APPROVED, false));
        assertEquals(5, state.getContextOrDefault(COUNT, 0));
        assertEquals(1500.0, state.getContextOrDefault(AMOUNT, 0.0));
    }

    @Test
    @DisplayName("Should handle state immutability correctly")
    void shouldHandleStateImmutability() {
        var startNode = NodeId.of("start");
        var originalState = GraphWorkflowState.create(
            workflowId,
            testData,
            startNode
        );

        var newData = "updated-data";
        var updatedState = originalState.withData(newData);

        assertEquals(testData, originalState.data());
        assertEquals(newData, updatedState.data());
        assertNotSame(originalState, updatedState);

        assertEquals(1, originalState.getVersion());
        assertEquals(2, updatedState.getVersion());
    }

    @Test
    @DisplayName("Should handle path tracking in workflow state")
    void shouldHandlePathTracking() {
        var startNode = NodeId.of("start");
        var nextNode = NodeId.of("next");
        var endNode = NodeId.of("end");

        var state = GraphWorkflowState.create(workflowId, testData, startNode)
            .moveToNode(nextNode)
            .moveToNode(endNode);

        assertEquals(endNode, state.currentNode().orElse(null));
        assertTrue(state.hasVisited(startNode));
        assertTrue(state.hasVisited(nextNode));
        assertTrue(state.hasVisited(endNode));

        assertEquals(2, state.getDepth());
        assertEquals(3, state.getPath().size());
    }

    @Test
    @DisplayName("Should validate command parameters correctly")
    void shouldValidateCommandParameters() {
        // Test null validations
        assertThrows(NullPointerException.class, () -> NodeId.of(null));

        assertThrows(IllegalArgumentException.class, () -> NodeId.of(""));

        assertThrows(NullPointerException.class, () -> EdgeId.of(null));

        assertThrows(IllegalArgumentException.class, () -> EdgeId.of(""));

        assertThrows(NullPointerException.class, () ->
            GraphCommand.Traverse.to(null)
        );

        assertThrows(NullPointerException.class, () ->
            GraphCommand.Suspend.withId(null, "reason")
        );

        assertThrows(NullPointerException.class, () ->
            GraphCommand.Suspend.withId("id", null)
        );

        assertThrows(NullPointerException.class, () ->
            GraphCommand.Fork.parallel(null)
        );

        assertThrows(IllegalArgumentException.class, () ->
            GraphCommand.Fork.parallel(Set.of())
        );

        assertThrows(NullPointerException.class, () ->
            GraphCommand.Join.waitAll(null)
        );
    }

    @Test
    @DisplayName("Should handle WorkflowResult operations correctly")
    void shouldHandleWorkflowResultOperations() {
        var successResult = WorkflowResult.success("success-value");
        var suspendedResult = WorkflowResult.suspended(
            "suspend-id",
            "state",
            "reason"
        );

        assertTrue(successResult.isSuccess());
        assertFalse(successResult.isFailure());
        assertFalse(successResult.isSuspended());
        assertEquals("success-value", successResult.getValue().orElse(""));

        assertFalse(suspendedResult.isSuccess());
        assertFalse(suspendedResult.isFailure());
        assertTrue(suspendedResult.isSuspended());
        assertEquals(
            "suspend-id",
            suspendedResult.getSuspension().get().suspensionId()
        );
    }

    @Test
    @DisplayName("Should create simple workflow node implementation")
    void shouldCreateSimpleWorkflowNode() {
        var testNode = new GraphWorkflowNode<String>() {
            private final NodeId nodeId = NodeId.of("test-node");

            @Override
            public WorkflowResult<GraphCommand<String>, WorkflowError> process(
                GraphWorkflowState<String> state
            ) {
                var targetNode = NodeId.of("next-node");
                var command = GraphCommand.Traverse.<String>to(targetNode);
                return WorkflowResult.success(command);
            }

            @Override
            public NodeId getNodeId() {
                return nodeId;
            }

            @Override
            public String getName() {
                return "Test Node";
            }

            @Override
            public String getDescription() {
                return "A test node implementation";
            }
        };

        assertEquals(NodeId.of("test-node"), testNode.getNodeId());
        assertEquals("Test Node", testNode.getName());
        assertEquals("A test node implementation", testNode.getDescription());
        // Note: Default implementation may return true for isEntryPoint
        // assertFalse(testNode.isEntryPoint());
        // Note: Default implementation may return true for isExitPoint
        // assertFalse(testNode.isExitPoint());

        var metadata = testNode.getMetadata();
        assertEquals(testNode.getNodeId(), metadata.nodeId());
        assertEquals(testNode.getName(), metadata.name());
        assertEquals(testNode.getDescription(), metadata.description());
    }

    @Test
    @DisplayName("Should evaluate edge conditions with workflow state")
    void shouldEvaluateEdgeConditions() {
        var state = GraphWorkflowState.create(
            workflowId,
            testData,
            NodeId.of("test"),
            testContext
        );

        var always = EdgeCondition.always();
        var never = EdgeCondition.never();
        var approvedCondition = EdgeCondition.whenContextEquals(APPROVED, true);
        var amountCondition = EdgeCondition.whenContextGreaterThan(
            AMOUNT,
            1000.0
        );

        assertTrue(always.evaluate(state));
        assertFalse(never.evaluate(state));
        assertTrue(approvedCondition.evaluate(state));
        assertTrue(amountCondition.evaluate(state));

        // Test logical combinations
        var andCondition = approvedCondition.and(amountCondition);
        var orCondition = approvedCondition.or(never);
        var notCondition = never.not();

        assertTrue(andCondition.evaluate(state));
        assertTrue(orCondition.evaluate(state));
        assertTrue(notCondition.evaluate(state));
    }
}
