package dev.agents4j.api.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for GraphPosition class.
 * Tests all functionality including position tracking, cycle detection, and edge traversal.
 */
class GraphPositionTest {

    private NodeId nodeA;
    private NodeId nodeB;
    private NodeId nodeC;
    private EdgeId edgeAB;
    private EdgeId edgeBC;


    @BeforeEach
    void setUp() {
        nodeA = NodeId.of("nodeA");
        nodeB = NodeId.of("nodeB");
        nodeC = NodeId.of("nodeC");
        edgeAB = EdgeId.of("edgeAB");
        edgeBC = EdgeId.of("edgeBC");

    }

    @Nested
    @DisplayName("Constructor and Factory Methods")
    class ConstructorAndFactoryTests {

        @Test
        @DisplayName("Should create valid GraphPosition with all required fields")
        void shouldCreateValidGraphPosition() {
            var position = new GraphPosition(
                nodeA,
                Optional.empty(),
                List.of(nodeA),
                Map.of(),
                0
            );

            assertEquals(nodeA, position.currentNodeId());
            assertTrue(position.previousNodeId().isEmpty());
            assertEquals(List.of(nodeA), position.visitedNodes());
            assertTrue(position.edgeTraversalHistory().isEmpty());
            assertEquals(0, position.depth());
        }

        @Test
        @DisplayName("Should create initial position using at() factory method")
        void shouldCreateInitialPositionUsingFactory() {
            var position = GraphPosition.at(nodeA);

            assertEquals(nodeA, position.currentNodeId());
            assertTrue(position.previousNodeId().isEmpty());
            assertEquals(List.of(nodeA), position.visitedNodes());
            assertTrue(position.edgeTraversalHistory().isEmpty());
            assertEquals(0, position.depth());
        }

        @Test
        @DisplayName("Should throw exception when current node is null")
        void shouldThrowExceptionWhenCurrentNodeIsNull() {
            assertThrows(NullPointerException.class, () ->
                new GraphPosition(null, Optional.empty(), List.of(), Map.of(), 0)
            );
        }

        @Test
        @DisplayName("Should throw exception when previous node optional is null")
        void shouldThrowExceptionWhenPreviousNodeOptionalIsNull() {
            assertThrows(NullPointerException.class, () ->
                new GraphPosition(nodeA, null, List.of(), Map.of(), 0)
            );
        }

        @Test
        @DisplayName("Should throw exception when visited nodes list is null")
        void shouldThrowExceptionWhenVisitedNodesIsNull() {
            assertThrows(NullPointerException.class, () ->
                new GraphPosition(nodeA, Optional.empty(), null, Map.of(), 0)
            );
        }

        @Test
        @DisplayName("Should throw exception when edge traversal history is null")
        void shouldThrowExceptionWhenEdgeTraversalHistoryIsNull() {
            assertThrows(NullPointerException.class, () ->
                new GraphPosition(nodeA, Optional.empty(), List.of(), null, 0)
            );
        }

        @Test
        @DisplayName("Should throw exception when depth is negative")
        void shouldThrowExceptionWhenDepthIsNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                new GraphPosition(nodeA, Optional.empty(), List.of(), Map.of(), -1)
            );
        }

        @Test
        @DisplayName("Should throw exception when start node is null in at() method")
        void shouldThrowExceptionWhenStartNodeIsNullInAtMethod() {
            assertThrows(NullPointerException.class, () ->
                GraphPosition.at(null)
            );
        }

        @Test
        @DisplayName("Should make collections immutable")
        void shouldMakeCollectionsImmutable() {
            var visitedList = new java.util.ArrayList<>(List.of(nodeA));
            var edgeMap = new java.util.HashMap<EdgeId, Instant>();
            
            var position = new GraphPosition(nodeA, Optional.empty(), visitedList, edgeMap, 0);

            // Modify original collections
            visitedList.add(nodeB);
            edgeMap.put(edgeAB, Instant.now());

            // Position should be unaffected
            assertEquals(1, position.visitedNodes().size());
            assertTrue(position.edgeTraversalHistory().isEmpty());
        }
    }

    @Nested
    @DisplayName("Movement Operations")
    class MovementOperationTests {

        @Test
        @DisplayName("Should move to new node correctly")
        void shouldMoveToNewNodeCorrectly() {
            var initialPosition = GraphPosition.at(nodeA);
            var newPosition = initialPosition.moveTo(nodeB);

            assertEquals(nodeB, newPosition.currentNodeId());
            assertEquals(Optional.of(nodeA), newPosition.previousNodeId());
            assertEquals(List.of(nodeA, nodeB), newPosition.visitedNodes());
            assertEquals(1, newPosition.depth());
            assertTrue(newPosition.edgeTraversalHistory().isEmpty());
        }

        @Test
        @DisplayName("Should throw exception when moving to null node")
        void shouldThrowExceptionWhenMovingToNullNode() {
            var position = GraphPosition.at(nodeA);
            
            assertThrows(NullPointerException.class, () ->
                position.moveTo(null)
            );
        }

        @Test
        @DisplayName("Should traverse edge correctly")
        void shouldTraverseEdgeCorrectly() {
            var initialPosition = GraphPosition.at(nodeA);
            var beforeTraversal = Instant.now();
            var newPosition = initialPosition.traverseEdge(edgeAB, nodeB);
            var afterTraversal = Instant.now();

            assertEquals(nodeB, newPosition.currentNodeId());
            assertEquals(Optional.of(nodeA), newPosition.previousNodeId());
            assertEquals(List.of(nodeA, nodeB), newPosition.visitedNodes());
            assertEquals(1, newPosition.depth());
            assertTrue(newPosition.hasTraversedEdge(edgeAB));
            
            var traversalTime = newPosition.getEdgeTraversalTime(edgeAB).orElseThrow();
            assertTrue(traversalTime.isAfter(beforeTraversal) || traversalTime.equals(beforeTraversal));
            assertTrue(traversalTime.isBefore(afterTraversal) || traversalTime.equals(afterTraversal));
        }

        @Test
        @DisplayName("Should throw exception when traversing with null edge")
        void shouldThrowExceptionWhenTraversingWithNullEdge() {
            var position = GraphPosition.at(nodeA);
            
            assertThrows(NullPointerException.class, () ->
                position.traverseEdge(null, nodeB)
            );
        }

        @Test
        @DisplayName("Should throw exception when traversing to null target node")
        void shouldThrowExceptionWhenTraversingToNullTargetNode() {
            var position = GraphPosition.at(nodeA);
            
            assertThrows(NullPointerException.class, () ->
                position.traverseEdge(edgeAB, null)
            );
        }

        @Test
        @DisplayName("Should reset to node correctly")
        void shouldResetToNodeCorrectly() {
            var position = GraphPosition.at(nodeA)
                .moveTo(nodeB)
                .moveTo(nodeC);
            
            var resetPosition = position.resetTo(nodeA);

            assertEquals(nodeA, resetPosition.currentNodeId());
            assertEquals(Optional.of(nodeC), resetPosition.previousNodeId());
            assertEquals(List.of(nodeA, nodeB, nodeC, nodeA), resetPosition.visitedNodes());
            assertEquals(3, resetPosition.depth());
        }

        @Test
        @DisplayName("Should throw exception when resetting to null node")
        void shouldThrowExceptionWhenResettingToNullNode() {
            var position = GraphPosition.at(nodeA);
            
            assertThrows(NullPointerException.class, () ->
                position.resetTo(null)
            );
        }
    }

    @Nested
    @DisplayName("Cycle Detection")
    class CycleDetectionTests {

        @Test
        @DisplayName("Should detect no cycle in initial position")
        void shouldDetectNoCycleInInitialPosition() {
            var position = GraphPosition.at(nodeA);
            
            assertFalse(position.hasCycle());
        }

        @Test
        @DisplayName("Should detect no cycle in linear path")
        void shouldDetectNoCycleInLinearPath() {
            var position = GraphPosition.at(nodeA)
                .moveTo(nodeB)
                .moveTo(nodeC);
            
            assertFalse(position.hasCycle());
        }

        @Test
        @DisplayName("Should detect cycle when returning to previously visited node")
        void shouldDetectCycleWhenReturningToPreviouslyVisitedNode() {
            var position = GraphPosition.at(nodeA)
                .moveTo(nodeB)
                .moveTo(nodeC)
                .moveTo(nodeA);
            
            assertTrue(position.hasCycle());
        }

        @Test
        @DisplayName("Should detect cycle in middle of path")
        void shouldDetectCycleInMiddleOfPath() {
            var position = GraphPosition.at(nodeA)
                .moveTo(nodeB)
                .moveTo(nodeC)
                .moveTo(nodeB);
            
            assertTrue(position.hasCycle());
        }
    }

    @Nested
    @DisplayName("Visit Tracking")
    class VisitTrackingTests {

        @Test
        @DisplayName("Should track visited nodes correctly")
        void shouldTrackVisitedNodesCorrectly() {
            var position = GraphPosition.at(nodeA)
                .moveTo(nodeB)
                .moveTo(nodeC);

            assertTrue(position.hasVisited(nodeA));
            assertTrue(position.hasVisited(nodeB));
            assertTrue(position.hasVisited(nodeC));
            assertFalse(position.hasVisited(NodeId.of("nodeD")));
        }

        @Test
        @DisplayName("Should throw exception when checking visit with null node")
        void shouldThrowExceptionWhenCheckingVisitWithNullNode() {
            var position = GraphPosition.at(nodeA);
            
            assertThrows(NullPointerException.class, () ->
                position.hasVisited(null)
            );
        }

        @Test
        @DisplayName("Should count visits correctly")
        void shouldCountVisitsCorrectly() {
            var position = GraphPosition.at(nodeA)
                .moveTo(nodeB)
                .moveTo(nodeA)
                .moveTo(nodeC)
                .moveTo(nodeA);

            assertEquals(3, position.getVisitCount(nodeA));
            assertEquals(1, position.getVisitCount(nodeB));
            assertEquals(1, position.getVisitCount(nodeC));
            assertEquals(0, position.getVisitCount(NodeId.of("nodeD")));
        }

        @Test
        @DisplayName("Should throw exception when counting visits with null node")
        void shouldThrowExceptionWhenCountingVisitsWithNullNode() {
            var position = GraphPosition.at(nodeA);
            
            assertThrows(NullPointerException.class, () ->
                position.getVisitCount(null)
            );
        }

        @Test
        @DisplayName("Should return correct path")
        void shouldReturnCorrectPath() {
            var position = GraphPosition.at(nodeA)
                .moveTo(nodeB)
                .moveTo(nodeC);

            assertEquals(List.of(nodeA, nodeB, nodeC), position.getPath());
        }

        @Test
        @DisplayName("Should return immutable path")
        void shouldReturnImmutablePath() {
            var position = GraphPosition.at(nodeA);
            var path = position.getPath();

            assertThrows(UnsupportedOperationException.class, () ->
                path.add(nodeB)
            );
        }
    }

    @Nested
    @DisplayName("Edge Traversal Tracking")
    class EdgeTraversalTrackingTests {

        @Test
        @DisplayName("Should track edge traversal correctly")
        void shouldTrackEdgeTraversalCorrectly() {
            var position = GraphPosition.at(nodeA)
                .traverseEdge(edgeAB, nodeB);

            assertTrue(position.hasTraversedEdge(edgeAB));
            assertFalse(position.hasTraversedEdge(edgeBC));
            assertTrue(position.getEdgeTraversalTime(edgeAB).isPresent());
            assertTrue(position.getEdgeTraversalTime(edgeBC).isEmpty());
        }

        @Test
        @DisplayName("Should throw exception when checking edge traversal with null edge")
        void shouldThrowExceptionWhenCheckingEdgeTraversalWithNullEdge() {
            var position = GraphPosition.at(nodeA);
            
            assertThrows(NullPointerException.class, () ->
                position.hasTraversedEdge(null)
            );
        }

        @Test
        @DisplayName("Should throw exception when getting edge traversal time with null edge")
        void shouldThrowExceptionWhenGettingEdgeTraversalTimeWithNullEdge() {
            var position = GraphPosition.at(nodeA);
            
            assertThrows(NullPointerException.class, () ->
                position.getEdgeTraversalTime(null)
            );
        }

        @Test
        @DisplayName("Should preserve edge traversal history during moves")
        void shouldPreserveEdgeTraversalHistoryDuringMoves() {
            var position = GraphPosition.at(nodeA)
                .traverseEdge(edgeAB, nodeB)
                .moveTo(nodeC);

            assertTrue(position.hasTraversedEdge(edgeAB));
            assertTrue(position.getEdgeTraversalTime(edgeAB).isPresent());
        }

        @Test
        @DisplayName("Should update edge traversal time on multiple traversals")
        void shouldUpdateEdgeTraversalTimeOnMultipleTraversals() {
            var position1 = GraphPosition.at(nodeA)
                .traverseEdge(edgeAB, nodeB);
            
            var firstTime = position1.getEdgeTraversalTime(edgeAB).orElseThrow();
            
            // Wait a bit to ensure different timestamp
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            var position2 = position1
                .moveTo(nodeA)
                .traverseEdge(edgeAB, nodeB);
            
            var secondTime = position2.getEdgeTraversalTime(edgeAB).orElseThrow();
            
            assertTrue(secondTime.isAfter(firstTime));
        }
    }

    @Nested
    @DisplayName("String Representation")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should generate correct path string")
        void shouldGenerateCorrectPathString() {
            var position = GraphPosition.at(nodeA)
                .moveTo(nodeB)
                .moveTo(nodeC);

            assertEquals("nodeA -> nodeB -> nodeC", position.getPathString());
        }

        @Test
        @DisplayName("Should handle single node path string")
        void shouldHandleSingleNodePathString() {
            var position = GraphPosition.at(nodeA);

            assertEquals("nodeA", position.getPathString());
        }

        @Test
        @DisplayName("Should generate correct toString representation")
        void shouldGenerateCorrectToStringRepresentation() {
            var position = GraphPosition.at(nodeA)
                .moveTo(nodeB);

            var expected = "GraphPosition{current=nodeB, depth=1, path=nodeA -> nodeB}";
            assertEquals(expected, position.toString());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle complex workflow navigation")
        void shouldHandleComplexWorkflowNavigation() {
            var position = GraphPosition.at(nodeA)
                .traverseEdge(edgeAB, nodeB)
                .traverseEdge(edgeBC, nodeC)
                .moveTo(nodeA)
                .resetTo(nodeB);

            assertEquals(nodeB, position.currentNodeId());
            assertEquals(Optional.of(nodeA), position.previousNodeId());
            assertEquals(4, position.depth());
            assertEquals(List.of(nodeA, nodeB, nodeC, nodeA, nodeB), position.visitedNodes());
            assertTrue(position.hasTraversedEdge(edgeAB));
            assertTrue(position.hasTraversedEdge(edgeBC));
            assertTrue(position.hasCycle());
            assertEquals(2, position.getVisitCount(nodeA));
            assertEquals(2, position.getVisitCount(nodeB));
            assertEquals(1, position.getVisitCount(nodeC));
        }

        @Test
        @DisplayName("Should maintain immutability throughout operations")
        void shouldMaintainImmutabilityThroughoutOperations() {
            var originalPosition = GraphPosition.at(nodeA);
            var newPosition = originalPosition.moveTo(nodeB);

            // Original position should be unchanged
            assertEquals(nodeA, originalPosition.currentNodeId());
            assertEquals(0, originalPosition.depth());
            assertEquals(List.of(nodeA), originalPosition.visitedNodes());

            // New position should have updates
            assertEquals(nodeB, newPosition.currentNodeId());
            assertEquals(1, newPosition.depth());
            assertEquals(List.of(nodeA, nodeB), newPosition.visitedNodes());
        }

        @Test
        @DisplayName("Should handle edge cases gracefully")
        void shouldHandleEdgeCasesGracefully() {
            // Test with same node multiple times
            var position = GraphPosition.at(nodeA)
                .moveTo(nodeA)
                .moveTo(nodeA);

            assertEquals(nodeA, position.currentNodeId());
            assertEquals(2, position.depth());
            assertEquals(3, position.getVisitCount(nodeA));
            assertTrue(position.hasCycle());
        }
    }
}