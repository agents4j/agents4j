package dev.agents4j.workflow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.workflow.output.OutputExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test to verify that GraphWorkflowFactory works correctly in the core module.
 */
class GraphWorkflowFactoryTest {

    @Mock
    private GraphWorkflowNode<String> firstNode;

    @Mock
    private GraphWorkflowNode<String> secondNode;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(firstNode.getNodeId()).thenReturn(NodeId.of("first"));
        when(secondNode.getNodeId()).thenReturn(NodeId.of("second"));
    }

    @Test
    @DisplayName("Should create response extractor correctly")
    void shouldCreateResponseExtractorCorrectly() {
        OutputExtractor<String, String> extractor =
            GraphWorkflowFactory.createResponseExtractor();

        assertNotNull(extractor);
    }

    @Test
    @DisplayName(
        "Should create type-safe sequence workflow with explicit input type"
    )
    void shouldCreateTypeSafeSequenceWorkflow() {
        var workflow = GraphWorkflowFactory.createSequence(
            "type-safe-sequence",
            String.class,
            firstNode,
            secondNode
        );

        assertNotNull(workflow);
        assertEquals("type-safe-sequence", workflow.getName());
        assertEquals("1.0.0", workflow.getVersion());

        // Verify type safety is maintained
        assertTrue(workflow instanceof GraphWorkflowImpl);
        var typedWorkflow = (GraphWorkflowImpl<String, String>) workflow;
        assertNotNull(typedWorkflow.getStateSerializer());
    }

    @Test
    @DisplayName("Should maintain backward compatibility with legacy method")
    void shouldMaintainBackwardCompatibilityWithLegacyMethod() {
        @SuppressWarnings("deprecation")
        var workflow = GraphWorkflowFactory.createSequence(
            "legacy-sequence",
            String.class,
            firstNode,
            secondNode
        );

        assertNotNull(workflow);
        assertEquals("legacy-sequence", workflow.getName());
        assertTrue(workflow instanceof GraphWorkflowImpl);
    }

    @Test
    @DisplayName("Should create sequence workflow correctly")
    void shouldCreateSequenceWorkflowCorrectly() {
        var workflow = GraphWorkflowFactory.createSequence(
            "test-sequence",
            String.class,
            firstNode,
            secondNode
        );

        assertNotNull(workflow);
        assertEquals("test-sequence", workflow.getName());
        assertEquals("1.0.0", workflow.getVersion());
        assertTrue(workflow instanceof GraphWorkflowImpl);

        // Verify the workflow was configured with both nodes
        var graphWorkflow = (GraphWorkflowImpl<String, String>) workflow;
        assertNotNull(graphWorkflow);
        assertNotNull(graphWorkflow.getNode(NodeId.of("first")));
        assertNotNull(graphWorkflow.getNode(NodeId.of("second")));
    }

    @Test
    @DisplayName("Should have non-null output extractor in sequence workflow")
    void shouldHaveNonNullOutputExtractorInSequenceWorkflow() {
        var workflow = GraphWorkflowFactory.createSequence(
            "test-extractor",
            String.class,
            firstNode,
            secondNode
        );

        assertNotNull(workflow);
        assertEquals("test-extractor", workflow.getName());
        assertEquals("1.0.0", workflow.getVersion());
        assertNotNull(workflow.getStateSerializer());
    }
}
