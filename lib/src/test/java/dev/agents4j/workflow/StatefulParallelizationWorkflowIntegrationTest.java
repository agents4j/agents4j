package dev.agents4j.workflow;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowState;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Integration test for the refactored StatefulParallelizationWorkflow.
 * Tests the complete workflow execution using the StatefulWorkflow interface.
 */
class StatefulParallelizationWorkflowIntegrationTest {

    private ChatModel mockModel;
    private ParallelizationWorkflow workflow;

    @BeforeEach
    void setUp() {
        // Setup mock model
        mockModel = Mockito.mock(ChatModel.class);

        // Setup mock response
        AiMessage mockAiMessage = Mockito.mock(AiMessage.class);
        ChatResponse mockResponse = Mockito.mock(ChatResponse.class);

        when(mockAiMessage.text()).thenReturn("Processed response");
        when(mockResponse.aiMessage()).thenReturn(mockAiMessage);
        when(mockModel.chat(anyList())).thenReturn(mockResponse);

        // Create workflow using the StatefulWorkflow interface
        workflow = ParallelizationWorkflow.builder()
            .name("StatefulParallelizationTest")
            .chatModel(mockModel)
            .build();
    }

    @Test
    void testStatefulWorkflowStart() throws WorkflowExecutionException {
        String prompt = "Translate the following to French:";
        List<String> inputs = Arrays.asList("Hello", "World", "Good morning");
        ParallelizationWorkflow.ParallelInput parallelInput = 
            new ParallelizationWorkflow.ParallelInput(prompt, inputs, 2);

        StatefulWorkflowResult<List<String>> result = workflow.start(parallelInput);

        assertNotNull(result);
        assertTrue(result.isCompleted());
        assertFalse(result.isSuspended());
        assertFalse(result.isError());

        List<String> output = result.getOutput().orElse(null);
        assertNotNull(output);
        assertEquals(3, output.size());

        // Verify all results are the expected mock response
        for (String response : output) {
            assertEquals("Processed response", response);
        }

        // Verify state
        WorkflowState finalState = result.getState();
        assertNotNull(finalState);
        assertTrue(finalState.getWorkflowId().startsWith("StatefulParallelizationTest-"));
        assertTrue(finalState.get("completed", false));
        assertTrue(finalState.get("results").isPresent());

        // Verify model was called correct number of times
        verify(mockModel, times(3)).chat(anyList());
    }

    @Test
    void testStatefulWorkflowWithContext() throws WorkflowExecutionException {
        String prompt = "Analyze the following:";
        List<String> inputs = Arrays.asList("Data 1", "Data 2");
        ParallelizationWorkflow.ParallelInput parallelInput = 
            new ParallelizationWorkflow.ParallelInput(prompt, inputs, 2);

        Map<String, Object> context = new HashMap<>();
        context.put("user_id", "test_user");
        context.put("session_id", "test_session");

        StatefulWorkflowResult<List<String>> result = workflow.start(parallelInput, context);

        assertTrue(result.isCompleted());
        
        // Verify context was populated with workflow information
        assertEquals("StatefulParallelizationTest", context.get("workflow_name"));
        assertEquals(2, context.get("num_inputs"));
        assertEquals(2, context.get("num_workers"));
        assertNotNull(context.get("execution_time"));
        assertNotNull(context.get("results"));

        // Verify original context is preserved
        assertEquals("test_user", context.get("user_id"));
        assertEquals("test_session", context.get("session_id"));
    }

    @Test
    void testStatefulWorkflowAsync() throws Exception {
        String prompt = "Process:";
        List<String> inputs = Arrays.asList("Item 1", "Item 2", "Item 3");
        ParallelizationWorkflow.ParallelInput parallelInput = 
            new ParallelizationWorkflow.ParallelInput(prompt, inputs, 2);

        CompletableFuture<StatefulWorkflowResult<List<String>>> future = workflow.startAsync(parallelInput);
        StatefulWorkflowResult<List<String>> result = future.get();

        assertTrue(result.isCompleted());
        List<String> output = result.getOutput().orElse(null);
        assertNotNull(output);
        assertEquals(3, output.size());

        verify(mockModel, times(3)).chat(anyList());
    }

    @Test
    void testWorkflowStructure() {
        // Verify the workflow has proper stateful structure
        assertDoesNotThrow(() -> workflow.validate());

        // Verify nodes exist
        assertFalse(workflow.getNodes().isEmpty());
        assertEquals(2, workflow.getNodes().size());

        // Verify specific nodes
        assertTrue(workflow.getNode("parallel-processor").isPresent());
        assertTrue(workflow.getNode("aggregator").isPresent());

        // Verify routes exist
        assertFalse(workflow.getRoutes().isEmpty());
        assertEquals(1, workflow.getRoutes().size());

        // Verify entry points
        assertFalse(workflow.getEntryPoints().isEmpty());
        assertEquals(1, workflow.getEntryPoints().size());
        assertEquals("parallel-processor", workflow.getEntryPoints().get(0).getNodeId());

        // Verify routes from processor node
        List<dev.agents4j.api.workflow.WorkflowRoute<ParallelizationWorkflow.ParallelInput>> routesFromProcessor = 
            workflow.getRoutesFrom("parallel-processor");
        assertEquals(1, routesFromProcessor.size());
        assertEquals("aggregator", routesFromProcessor.get(0).getToNodeId());
    }

    @Test
    void testErrorHandling() throws WorkflowExecutionException {
        // Setup mock to throw exception
        when(mockModel.chat(anyList())).thenThrow(new RuntimeException("API Error"));

        String prompt = "Process:";
        List<String> inputs = Arrays.asList("Input 1");
        ParallelizationWorkflow.ParallelInput parallelInput = 
            new ParallelizationWorkflow.ParallelInput(prompt, inputs, 1);

        StatefulWorkflowResult<List<String>> result = workflow.start(parallelInput);

        assertTrue(result.isError());
        assertFalse(result.isCompleted());
        assertFalse(result.isSuspended());
        assertTrue(result.getErrorMessage().isPresent());
        assertFalse(result.getOutput().isPresent());

        // Verify error state
        WorkflowState errorState = result.getState();
        assertNotNull(errorState);
    }

    @Test
    void testWorkflowMetadata() throws WorkflowExecutionException {
        String prompt = "Test:";
        List<String> inputs = Arrays.asList("A", "B");
        ParallelizationWorkflow.ParallelInput parallelInput = 
            new ParallelizationWorkflow.ParallelInput(prompt, inputs, 1);

        StatefulWorkflowResult<List<String>> result = workflow.start(parallelInput);

        assertTrue(result.isCompleted());

        // Verify metadata
        Map<String, Object> metadata = result.getMetadata();
        assertNotNull(metadata);
        assertTrue(metadata.containsKey("execution_time"));
        assertTrue(metadata.containsKey("num_results"));
        assertEquals(2, metadata.get("num_results"));
        assertTrue((Long) metadata.get("execution_time") >= 0);
    }

    @Test
    void testWorkflowStateProgression() throws WorkflowExecutionException {
        String prompt = "Transform:";
        List<String> inputs = Arrays.asList("Input");
        ParallelizationWorkflow.ParallelInput parallelInput = 
            new ParallelizationWorkflow.ParallelInput(prompt, inputs, 1);

        StatefulWorkflowResult<List<String>> result = workflow.start(parallelInput);

        // Verify final state contains expected data
        WorkflowState finalState = result.getState();
        assertTrue(finalState.get("prompt").isPresent());
        assertTrue(finalState.get("inputs").isPresent());
        assertTrue(finalState.get("numWorkers").isPresent());
        assertTrue(finalState.get("results").isPresent());
        assertTrue(finalState.get("finalResults").isPresent());
        assertTrue(finalState.get("completed", false));

        // Verify state version progression
        assertTrue(finalState.getVersion() > 1);
    }

    @Test
    void testWorkflowName() {
        assertEquals("StatefulParallelizationTest", workflow.getName());
    }

    @Test
    void testBackwardCompatibilityMethod() {
        // Test that the old parallel method still works
        String prompt = "Translate:";
        List<String> inputs = Arrays.asList("Hello", "World");
        
        List<String> results = workflow.parallel(prompt, inputs, 2);
        
        assertNotNull(results);
        assertEquals(2, results.size());
        
        for (String result : results) {
            assertEquals("Processed response", result);
        }
        
        verify(mockModel, times(2)).chat(anyList());
    }
}