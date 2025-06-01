package dev.agents4j.api.graph;

import static org.junit.jupiter.api.Assertions.*;

import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.ExecutionContext;
import dev.agents4j.api.context.WorkflowContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for GraphWorkflowState class.
 * Tests all functionality including state management, context operations, and graph navigation.
 */
class GraphWorkflowStateTest {

    private WorkflowId workflowId;
    private NodeId nodeA;
    private NodeId nodeB;
    private NodeId nodeC;
    private EdgeId edgeAB;
    private EdgeId edgeBC;
    private ContextKey<String> userKey;
    private ContextKey<Integer> countKey;
    private ContextKey<Boolean> flagKey;
    private String initialData;

    @BeforeEach
    void setUp() {
        workflowId = WorkflowId.of("test-workflow");
        nodeA = NodeId.of("nodeA");
        nodeB = NodeId.of("nodeB");
        nodeC = NodeId.of("nodeC");
        edgeAB = EdgeId.of("edgeAB");
        edgeBC = EdgeId.of("edgeBC");
        userKey = ContextKey.stringKey("user");
        countKey = ContextKey.intKey("count");
        flagKey = ContextKey.booleanKey("flag");
        initialData = "initial-data";
    }

    @Nested
    @DisplayName("Constructor and Factory Methods")
    class ConstructorAndFactoryTests {

        @Test
        @DisplayName("Should create valid GraphWorkflowState with all required fields")
        void shouldCreateValidGraphWorkflowState() {
            var context = ExecutionContext.empty();
            var position = GraphPosition.at(nodeA);
            var metadata = StateMetadata.initial();

            var state = new GraphWorkflowState<>(
                workflowId,
                initialData,
                context,
                Optional.of(nodeA),
                position,
                metadata
            );

            assertEquals(workflowId, state.workflowId());
            assertEquals(initialData, state.data());
            assertEquals(context, state.context());
            assertEquals(Optional.of(nodeA), state.currentNode());
            assertEquals(position, state.position());
            assertEquals(metadata, state.metadata());
        }

        @Test
        @DisplayName("Should create initial state using create() factory method")
        void shouldCreateInitialStateUsingFactory() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);

            assertEquals(workflowId, state.workflowId());
            assertEquals(initialData, state.data());
            assertEquals(Optional.of(nodeA), state.currentNode());
            assertEquals(nodeA, state.position().currentNodeId());
            assertEquals(0, state.getDepth());
            assertEquals(1, state.getVersion());
            assertNotNull(state.getCreatedAt());
            assertNotNull(state.getLastModified());
        }

        @Test
        @DisplayName("Should create initial state with context using create() factory method")
        void shouldCreateInitialStateWithContextUsingFactory() {
            var initialContext = ExecutionContext.empty().with(userKey, "testUser");
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA, initialContext);

            assertEquals(workflowId, state.workflowId());
            assertEquals(initialData, state.data());
            assertEquals(initialContext, state.context());
            assertEquals(Optional.of(nodeA), state.currentNode());
            assertEquals("testUser", state.getContext(userKey).orElse(null));
        }

        @Test
        @DisplayName("Should throw exception when workflow ID is null")
        void shouldThrowExceptionWhenWorkflowIdIsNull() {
            assertThrows(NullPointerException.class, () ->
                new GraphWorkflowState<>(
                    null,
                    initialData,
                    ExecutionContext.empty(),
                    Optional.of(nodeA),
                    GraphPosition.at(nodeA),
                    StateMetadata.initial()
                )
            );
        }

        @Test
        @DisplayName("Should throw exception when context is null")
        void shouldThrowExceptionWhenContextIsNull() {
            assertThrows(NullPointerException.class, () ->
                new GraphWorkflowState<>(
                    workflowId,
                    initialData,
                    null,
                    Optional.of(nodeA),
                    GraphPosition.at(nodeA),
                    StateMetadata.initial()
                )
            );
        }

        @Test
        @DisplayName("Should throw exception when current node optional is null")
        void shouldThrowExceptionWhenCurrentNodeOptionalIsNull() {
            assertThrows(NullPointerException.class, () ->
                new GraphWorkflowState<>(
                    workflowId,
                    initialData,
                    ExecutionContext.empty(),
                    null,
                    GraphPosition.at(nodeA),
                    StateMetadata.initial()
                )
            );
        }

        @Test
        @DisplayName("Should throw exception when position is null")
        void shouldThrowExceptionWhenPositionIsNull() {
            assertThrows(NullPointerException.class, () ->
                new GraphWorkflowState<>(
                    workflowId,
                    initialData,
                    ExecutionContext.empty(),
                    Optional.of(nodeA),
                    null,
                    StateMetadata.initial()
                )
            );
        }

        @Test
        @DisplayName("Should throw exception when metadata is null")
        void shouldThrowExceptionWhenMetadataIsNull() {
            assertThrows(NullPointerException.class, () ->
                new GraphWorkflowState<>(
                    workflowId,
                    initialData,
                    ExecutionContext.empty(),
                    Optional.of(nodeA),
                    GraphPosition.at(nodeA),
                    null
                )
            );
        }

        @Test
        @DisplayName("Should throw exception when start node is null in create() method")
        void shouldThrowExceptionWhenStartNodeIsNullInCreateMethod() {
            assertThrows(NullPointerException.class, () ->
                GraphWorkflowState.create(workflowId, initialData, null)
            );
        }

        @Test
        @DisplayName("Should throw exception when initial context is null in create() method")
        void shouldThrowExceptionWhenInitialContextIsNullInCreateMethod() {
            assertThrows(NullPointerException.class, () ->
                GraphWorkflowState.create(workflowId, initialData, nodeA, null)
            );
        }
    }

    @Nested
    @DisplayName("Data Operations")
    class DataOperationTests {

        @Test
        @DisplayName("Should update data correctly")
        void shouldUpdateDataCorrectly() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            var newData = "updated-data";
            var updatedState = state.withData(newData);

            assertEquals(newData, updatedState.data());
            assertEquals(initialData, state.data()); // Original unchanged
            assertEquals(state.getVersion() + 1, updatedState.getVersion());
            assertTrue(updatedState.getLastModified().isAfter(state.getLastModified()) ||
                      updatedState.getLastModified().equals(state.getLastModified()));
        }

        @Test
        @DisplayName("Should allow null data")
        void shouldAllowNullData() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            var updatedState = state.withData(null);

            assertNull(updatedState.data());
            assertEquals(state.getVersion() + 1, updatedState.getVersion());
        }
    }

    @Nested
    @DisplayName("Context Operations")
    class ContextOperationTests {

        @Test
        @DisplayName("Should add context value correctly")
        void shouldAddContextValueCorrectly() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            var updatedState = state.withContext(userKey, "testUser");

            assertEquals("testUser", updatedState.getContext(userKey).orElse(null));
            assertTrue(updatedState.getContext(userKey).isPresent());
            assertTrue(updatedState.hasContext(userKey));
            assertFalse(state.hasContext(userKey)); // Original unchanged
            assertEquals(state.getVersion() + 1, updatedState.getVersion());
        }

        @Test
        @DisplayName("Should replace entire context correctly")
        void shouldReplaceEntireContextCorrectly() {
            var initialState = GraphWorkflowState.create(workflowId, initialData, nodeA)
                .withContext(userKey, "oldUser");
            
            var newContext = ExecutionContext.empty()
                .with(userKey, "newUser")
                .with(countKey, 42);
            
            var updatedState = initialState.withContext(newContext);

            assertEquals("newUser", updatedState.getContext(userKey).orElse(null));
            assertEquals(42, updatedState.getContext(countKey).orElse(null));
            assertEquals(newContext, updatedState.context());
            assertEquals(initialState.getVersion() + 1, updatedState.getVersion());
        }

        @Test
        @DisplayName("Should throw exception when replacing context with null")
        void shouldThrowExceptionWhenReplacingContextWithNull() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            
            assertThrows(NullPointerException.class, () ->
                state.withContext((WorkflowContext) null)
            );
        }

        @Test
        @DisplayName("Should get context value with default")
        void shouldGetContextValueWithDefault() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA)
                .withContext(userKey, "testUser");

            assertEquals("testUser", state.getContextOrDefault(userKey, "default"));
            assertEquals(123, state.getContextOrDefault(countKey, 123));
        }

        @Test
        @DisplayName("Should check context key existence correctly")
        void shouldCheckContextKeyExistenceCorrectly() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA)
                .withContext(userKey, "testUser");

            assertTrue(state.hasContext(userKey));
            assertFalse(state.hasContext(countKey));
        }

        @Test
        @DisplayName("Should update data, context, and node atomically")
        void shouldUpdateDataContextAndNodeAtomically() {
            var initialState = GraphWorkflowState.create(workflowId, initialData, nodeA);
            var newData = "new-data";
            var contextUpdates = ExecutionContext.empty()
                .with(userKey, "newUser")
                .with(countKey, 100);

            var updatedState = initialState.withDataContextAndNode(newData, contextUpdates, nodeB);

            assertEquals(newData, updatedState.data());
            assertEquals("newUser", updatedState.getContext(userKey).orElse(null));
            assertEquals(100, updatedState.getContext(countKey).orElse(null));
            assertEquals(Optional.of(nodeB), updatedState.currentNode());
            assertEquals(nodeB, updatedState.position().currentNodeId());
            assertEquals(1, updatedState.getDepth());
            assertEquals(initialState.getVersion() + 1, updatedState.getVersion());
        }

        @Test
        @DisplayName("Should throw exception when updating with null node")
        void shouldThrowExceptionWhenUpdatingWithNullNode() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            var contextUpdates = ExecutionContext.empty();

            assertThrows(NullPointerException.class, () ->
                state.withDataContextAndNode("new-data", contextUpdates, null)
            );
        }

        @Test
        @DisplayName("Should throw exception when updating with null context updates")
        void shouldThrowExceptionWhenUpdatingWithNullContextUpdates() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);

            assertThrows(NullPointerException.class, () ->
                state.withDataContextAndNode("new-data", null, nodeB)
            );
        }
    }

    @Nested
    @DisplayName("Graph Navigation")
    class GraphNavigationTests {

        @Test
        @DisplayName("Should move to node correctly")
        void shouldMoveToNodeCorrectly() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            var movedState = state.moveToNode(nodeB);

            assertEquals(Optional.of(nodeB), movedState.currentNode());
            assertEquals(nodeB, movedState.position().currentNodeId());
            assertEquals(Optional.of(nodeA), movedState.getPreviousNode());
            assertEquals(1, movedState.getDepth());
            assertEquals(List.of(nodeA, nodeB), movedState.getPath());
            assertEquals(state.getVersion() + 1, movedState.getVersion());
        }

        @Test
        @DisplayName("Should throw exception when moving to null node")
        void shouldThrowExceptionWhenMovingToNullNode() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);

            assertThrows(NullPointerException.class, () ->
                state.moveToNode(null)
            );
        }

        @Test
        @DisplayName("Should traverse edge correctly")
        void shouldTraverseEdgeCorrectly() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            var traversedState = state.traverseEdge(edgeAB, nodeB);

            assertEquals(Optional.of(nodeB), traversedState.currentNode());
            assertEquals(nodeB, traversedState.position().currentNodeId());
            assertEquals(Optional.of(nodeA), traversedState.getPreviousNode());
            assertEquals(1, traversedState.getDepth());
            assertEquals(List.of(nodeA, nodeB), traversedState.getPath());
            assertTrue(traversedState.position().hasTraversedEdge(edgeAB));
            assertEquals(state.getVersion() + 1, traversedState.getVersion());
        }

        @Test
        @DisplayName("Should throw exception when traversing with null edge")
        void shouldThrowExceptionWhenTraversingWithNullEdge() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);

            assertThrows(NullPointerException.class, () ->
                state.traverseEdge(null, nodeB)
            );
        }

        @Test
        @DisplayName("Should throw exception when traversing to null target node")
        void shouldThrowExceptionWhenTraversingToNullTargetNode() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);

            assertThrows(NullPointerException.class, () ->
                state.traverseEdge(edgeAB, null)
            );
        }

        @Test
        @DisplayName("Should reset to node correctly")
        void shouldResetToNodeCorrectly() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA)
                .moveToNode(nodeB)
                .moveToNode(nodeC);
            
            var resetState = state.resetToNode(nodeA);

            assertEquals(Optional.of(nodeA), resetState.currentNode());
            assertEquals(nodeA, resetState.position().currentNodeId());
            assertEquals(Optional.of(nodeC), resetState.getPreviousNode());
            assertEquals(3, resetState.getDepth());
            assertEquals(List.of(nodeA, nodeB, nodeC, nodeA), resetState.getPath());
            assertEquals(state.getVersion() + 1, resetState.getVersion());
        }

        @Test
        @DisplayName("Should throw exception when resetting to null node")
        void shouldThrowExceptionWhenResettingToNullNode() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);

            assertThrows(NullPointerException.class, () ->
                state.resetToNode(null)
            );
        }
    }

    @Nested
    @DisplayName("Cycle Detection and Path Tracking")
    class CycleDetectionAndPathTrackingTests {

        @Test
        @DisplayName("Should detect no cycle in initial state")
        void shouldDetectNoCycleInInitialState() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);

            assertFalse(state.hasCycle());
        }

        @Test
        @DisplayName("Should detect cycle when returning to previously visited node")
        void shouldDetectCycleWhenReturningToPreviouslyVisitedNode() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA)
                .moveToNode(nodeB)
                .moveToNode(nodeC)
                .moveToNode(nodeA);

            assertTrue(state.hasCycle());
        }

        @Test
        @DisplayName("Should track visited nodes correctly")
        void shouldTrackVisitedNodesCorrectly() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA)
                .moveToNode(nodeB)
                .moveToNode(nodeC);

            assertTrue(state.hasVisited(nodeA));
            assertTrue(state.hasVisited(nodeB));
            assertTrue(state.hasVisited(nodeC));
            assertFalse(state.hasVisited(NodeId.of("nodeD")));
        }

        @Test
        @DisplayName("Should return correct path")
        void shouldReturnCorrectPath() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA)
                .moveToNode(nodeB)
                .moveToNode(nodeC);

            assertEquals(List.of(nodeA, nodeB, nodeC), state.getPath());
        }

        @Test
        @DisplayName("Should return correct path string")
        void shouldReturnCorrectPathString() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA)
                .moveToNode(nodeB)
                .moveToNode(nodeC);

            assertEquals("nodeA -> nodeB -> nodeC", state.getPathString());
        }

        @Test
        @DisplayName("Should track depth correctly")
        void shouldTrackDepthCorrectly() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            assertEquals(0, state.getDepth());

            state = state.moveToNode(nodeB);
            assertEquals(1, state.getDepth());

            state = state.moveToNode(nodeC);
            assertEquals(2, state.getDepth());
        }

        @Test
        @DisplayName("Should track previous node correctly")
        void shouldTrackPreviousNodeCorrectly() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            assertTrue(state.getPreviousNode().isEmpty());

            state = state.moveToNode(nodeB);
            assertEquals(Optional.of(nodeA), state.getPreviousNode());

            state = state.moveToNode(nodeC);
            assertEquals(Optional.of(nodeB), state.getPreviousNode());
        }
    }

    @Nested
    @DisplayName("Metadata and Timestamps")
    class MetadataAndTimestampTests {

        @Test
        @DisplayName("Should track version correctly")
        void shouldTrackVersionCorrectly() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            assertEquals(1, state.getVersion());

            state = state.withData("new-data");
            assertEquals(2, state.getVersion());

            state = state.moveToNode(nodeB);
            assertEquals(3, state.getVersion());

            state = state.withContext(userKey, "user");
            assertEquals(4, state.getVersion());
        }

        @Test
        @DisplayName("Should track timestamps correctly")
        void shouldTrackTimestampsCorrectly() {
            var beforeCreation = Instant.now();
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            var afterCreation = Instant.now();

            assertTrue(state.getCreatedAt().isAfter(beforeCreation) || 
                      state.getCreatedAt().equals(beforeCreation));
            assertTrue(state.getCreatedAt().isBefore(afterCreation) || 
                      state.getCreatedAt().equals(afterCreation));
            assertEquals(state.getCreatedAt(), state.getLastModified());

            // Wait a bit to ensure different timestamp
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            var beforeUpdate = Instant.now();
            var updatedState = state.withData("new-data");
            var afterUpdate = Instant.now();

            assertEquals(state.getCreatedAt(), updatedState.getCreatedAt());
            assertTrue(updatedState.getLastModified().isAfter(state.getLastModified()));
            assertTrue(updatedState.getLastModified().isAfter(beforeUpdate) || 
                      updatedState.getLastModified().equals(beforeUpdate));
            assertTrue(updatedState.getLastModified().isBefore(afterUpdate) || 
                      updatedState.getLastModified().equals(afterUpdate));
        }

        @Test
        @DisplayName("Should calculate age correctly")
        void shouldCalculateAgeCorrectly() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            var age = state.getAge();

            assertTrue(age.toMillis() >= 0);
            assertTrue(age.toSeconds() < 1); // Should be very recent
        }

        @Test
        @DisplayName("Should calculate time since last modified correctly")
        void shouldCalculateTimeSinceLastModifiedCorrectly() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            var timeSinceModified = state.getTimeSinceLastModified();

            assertTrue(timeSinceModified.toMillis() >= 0);
            assertTrue(timeSinceModified.toSeconds() < 1); // Should be very recent
        }
    }

    @Nested
    @DisplayName("String Representation")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should generate correct toString representation")
        void shouldGenerateCorrectToStringRepresentation() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA)
                .withContext(userKey, "testUser")
                .moveToNode(nodeB);

            var expected = String.format(
                "GraphWorkflowState{id=%s, currentNode=%s, depth=%d, version=%d, contextSize=%d}",
                workflowId.value(),
                nodeB.value(),
                1,
                3,
                1
            );

            assertEquals(expected, state.toString());
        }

        @Test
        @DisplayName("Should handle empty current node in toString")
        void shouldHandleEmptyCurrentNodeInToString() {
            var state = new GraphWorkflowState<>(
                workflowId,
                initialData,
                ExecutionContext.empty(),
                Optional.empty(),
                GraphPosition.at(nodeA),
                StateMetadata.initial()
            );

            assertTrue(state.toString().contains("currentNode=none"));
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Should maintain immutability throughout operations")
        void shouldMaintainImmutabilityThroughoutOperations() {
            var originalState = GraphWorkflowState.create(workflowId, initialData, nodeA);
            var modifiedState = originalState
                .withData("new-data")
                .withContext(userKey, "user")
                .moveToNode(nodeB);

            // Original state should be unchanged
            assertEquals(initialData, originalState.data());
            assertEquals(Optional.of(nodeA), originalState.currentNode());
            assertEquals(0, originalState.getDepth());
            assertEquals(1, originalState.getVersion());
            assertFalse(originalState.hasContext(userKey));

            // Modified state should have updates
            assertEquals("new-data", modifiedState.data());
            assertEquals(Optional.of(nodeB), modifiedState.currentNode());
            assertEquals(1, modifiedState.getDepth());
            assertEquals(4, modifiedState.getVersion());
            assertTrue(modifiedState.hasContext(userKey));
        }

        @Test
        @DisplayName("Should return immutable path")
        void shouldReturnImmutablePath() {
            var state = GraphWorkflowState.create(workflowId, initialData, nodeA);
            var path = state.getPath();

            assertThrows(UnsupportedOperationException.class, () ->
                path.add(nodeB)
            );
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complex workflow scenario")
        void shouldHandleComplexWorkflowScenario() {
            var state = GraphWorkflowState.create(workflowId, "start", nodeA)
                .withContext(userKey, "alice")
                .withContext(countKey, 0)
                .traverseEdge(edgeAB, nodeB)
                .withData("processing")
                .withContext(countKey, 1)
                .traverseEdge(edgeBC, nodeC)
                .withData("completed")
                .withContext(flagKey, true)
                .resetToNode(nodeA)
                .withData("reset");

            assertEquals("reset", state.data());
            assertEquals("alice", state.getContext(userKey).orElse(null));
            assertEquals(1, state.getContext(countKey).orElse(null));
            assertEquals(true, state.getContext(flagKey).orElse(null));
            assertEquals(Optional.of(nodeA), state.currentNode());
            assertEquals(3, state.getDepth());
            assertEquals(List.of(nodeA, nodeB, nodeC, nodeA), state.getPath());
            assertTrue(state.hasCycle());
            assertEquals(11, state.getVersion());
        }

        @Test
        @DisplayName("Should handle edge case scenarios gracefully")
        void shouldHandleEdgeCaseScenarios() {
            // Test with null data
            var stateWithNullData = GraphWorkflowState.create(workflowId, null, nodeA);
            assertNull(stateWithNullData.data());

            // Test with same node multiple times
            var cyclicState = stateWithNullData
                .moveToNode(nodeA)
                .moveToNode(nodeA);
            assertTrue(cyclicState.hasCycle());
            assertEquals(2, cyclicState.getDepth());

            // Test with empty context
            assertEquals(0, stateWithNullData.context().size());
            assertFalse(stateWithNullData.hasContext(userKey));
        }
    }
}