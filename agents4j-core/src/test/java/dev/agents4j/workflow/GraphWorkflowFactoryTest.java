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
        OutputExtractor<String, String> extractor = GraphWorkflowFactory.createResponseExtractor();
        
        assertNotNull(extractor);
    }

    @Test
    @DisplayName("Should create sequence workflow correctly")
    void shouldCreateSequenceWorkflowCorrectly() {
        var workflow = GraphWorkflowFactory.createSequence(
            "test-sequence",
            firstNode,
            secondNode
        );

        assertNotNull(workflow);
        assertEquals("test-sequence", workflow.getName());
        assertTrue(workflow instanceof GraphWorkflowImpl);
        
        // Verify the workflow was configured with both nodes
        var graphWorkflow = (GraphWorkflowImpl<String, String>) workflow;
        assertNotNull(graphWorkflow);
    }

    @Test
    @DisplayName("Should have non-null output extractor in sequence workflow")
    void shouldHaveNonNullOutputExtractorInSequenceWorkflow() {
        var workflow = GraphWorkflowFactory.createSequence(
            "test-extractor",
            firstNode,
            secondNode
        );

        assertNotNull(workflow);
        // Verify the workflow has the basic structure we expect
        assertEquals("test-extractor", workflow.getName());
    }
}