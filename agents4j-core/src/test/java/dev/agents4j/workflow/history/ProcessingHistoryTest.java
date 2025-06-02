package dev.agents4j.workflow.history;

import static org.junit.jupiter.api.Assertions.*;

import dev.agents4j.api.graph.NodeId;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test to verify that processing history classes work correctly in the core module.
 */
class ProcessingHistoryTest {

    private ProcessingHistory history;
    private NodeId nodeA;
    private NodeId nodeB;

    @BeforeEach
    void setUp() {
        history = new ProcessingHistory();
        nodeA = NodeId.of("nodeA");
        nodeB = NodeId.of("nodeB");
    }

    @Test
    @DisplayName("Should add and retrieve interactions correctly")
    void shouldAddAndRetrieveInteractionsCorrectly() {
        var interaction1 = new NodeInteraction(nodeA, "Node A", "input1", "output1", Instant.now());
        var interaction2 = new NodeInteraction(nodeB, "Node B", "input2", "output2", Instant.now());

        history.addInteraction(interaction1);
        history.addInteraction(interaction2);

        var interactions = history.getAllInteractions();
        assertEquals(2, interactions.size());
        assertEquals(interaction1, interactions.get(0));
        assertEquals(interaction2, interactions.get(1));
    }

    @Test
    @DisplayName("Should find latest interaction from specific node")
    void shouldFindLatestInteractionFromSpecificNode() {
        var interaction1 = new NodeInteraction(nodeA, "Node A", "input1", "output1", Instant.now());
        var interaction2 = new NodeInteraction(nodeB, "Node B", "input2", "output2", Instant.now());
        var interaction3 = new NodeInteraction(nodeA, "Node A", "input3", "output3", Instant.now());

        history.addInteraction(interaction1);
        history.addInteraction(interaction2);
        history.addInteraction(interaction3);

        var latestA = history.getLatestFromNode(nodeA);
        var latestB = history.getLatestFromNode(nodeB);

        assertTrue(latestA.isPresent());
        assertTrue(latestB.isPresent());
        assertEquals(interaction3, latestA.get());
        assertEquals(interaction2, latestB.get());
    }

    @Test
    @DisplayName("Should return empty for non-existent node")
    void shouldReturnEmptyForNonExistentNode() {
        var interaction = new NodeInteraction(nodeA, "Node A", "input", "output", Instant.now());
        history.addInteraction(interaction);

        var result = history.getLatestFromNode(NodeId.of("nonExistent"));
        assertTrue(result.isEmpty());
    }
}