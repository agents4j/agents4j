package agents4j;

import dev.agents4j.workflow.ParallelizationWorkflow;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ParallelizationWorkflowTest {

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

        // Create workflow
        workflow = ParallelizationWorkflow.builder()
            .name("TestParallelWorkflow")
            .chatModel(mockModel)
            .build();
    }

    @Test
    void testParallelizationWorkflowBuilder() {
        assertNotNull(workflow);
        assertEquals("TestParallelWorkflow", workflow.getName());
    }

    @Test
    void testParallelizationWorkflowBuilderWithDefaultName() {
        ParallelizationWorkflow defaultNameWorkflow = ParallelizationWorkflow.builder()
            .chatModel(mockModel)
            .build();
        
        assertNotNull(defaultNameWorkflow);
        assertTrue(defaultNameWorkflow.getName().startsWith("ParallelizationWorkflow-"));
    }

    @Test
    void testParallelizationWorkflowBuilderMissingModel() {
        assertThrows(IllegalStateException.class, () -> {
            ParallelizationWorkflow.builder()
                .name("TestWorkflow")
                .build();
        });
    }

    @Test
    void testParallelInput() {
        List<String> inputs = Arrays.asList("Hello", "World", "Test");
        ParallelizationWorkflow.ParallelInput parallelInput = 
            new ParallelizationWorkflow.ParallelInput("Translate:", inputs, 2);

        assertEquals("Translate:", parallelInput.getPrompt());
        assertEquals(inputs, parallelInput.getInputs());
        assertEquals(2, parallelInput.getNumWorkers());
    }

    @Test
    void testParallelInputValidation() {
        List<String> inputs = Arrays.asList("Hello", "World");

        // Test null prompt
        assertThrows(NullPointerException.class, () -> {
            new ParallelizationWorkflow.ParallelInput(null, inputs, 2);
        });

        // Test null inputs
        assertThrows(NullPointerException.class, () -> {
            new ParallelizationWorkflow.ParallelInput("Prompt", null, 2);
        });

        // Test empty inputs
        assertThrows(IllegalArgumentException.class, () -> {
            new ParallelizationWorkflow.ParallelInput("Prompt", Arrays.asList(), 2);
        });

        // Test invalid worker count
        assertThrows(IllegalArgumentException.class, () -> {
            new ParallelizationWorkflow.ParallelInput("Prompt", inputs, 0);
        });
    }

    @Test
    void testParallelMethod() {
        String prompt = "Translate the following to French:";
        List<String> inputs = Arrays.asList("Hello", "World", "Good morning");
        int numWorkers = 2;

        List<String> results = workflow.parallel(prompt, inputs, numWorkers);

        assertNotNull(results);
        assertEquals(3, results.size());
        
        // Verify that each result is the expected mock response
        for (String result : results) {
            assertEquals("Processed response", result);
        }

        // Verify that chat was called the expected number of times
        verify(mockModel, times(3)).chat(anyList());
    }

    @Test
    void testParallelMethodValidation() {
        List<String> inputs = Arrays.asList("Hello", "World");

        // Test null prompt
        assertThrows(IllegalArgumentException.class, () -> {
            workflow.parallel(null, inputs, 2);
        });

        // Test null inputs
        assertThrows(IllegalArgumentException.class, () -> {
            workflow.parallel("Prompt", null, 2);
        });

        // Test empty inputs
        assertThrows(IllegalArgumentException.class, () -> {
            workflow.parallel("Prompt", Arrays.asList(), 2);
        });

        // Test invalid worker count
        assertThrows(IllegalArgumentException.class, () -> {
            workflow.parallel("Prompt", inputs, 0);
        });
    }

    @Test
    void testExecuteMethod() {
        String prompt = "Analyze the following text:";
        List<String> inputs = Arrays.asList("Text 1", "Text 2");
        ParallelizationWorkflow.ParallelInput parallelInput = 
            new ParallelizationWorkflow.ParallelInput(prompt, inputs, 2);

        List<String> results = workflow.execute(parallelInput);

        assertNotNull(results);
        assertEquals(2, results.size());
        
        for (String result : results) {
            assertEquals("Processed response", result);
        }

        verify(mockModel, times(2)).chat(anyList());
    }

    @Test
    void testExecuteAsyncMethod() throws ExecutionException, InterruptedException {
        String prompt = "Summarize:";
        List<String> inputs = Arrays.asList("Document 1", "Document 2");
        ParallelizationWorkflow.ParallelInput parallelInput = 
            new ParallelizationWorkflow.ParallelInput(prompt, inputs, 2);

        CompletableFuture<List<String>> future = workflow.executeAsync(parallelInput);
        List<String> results = future.get();

        assertNotNull(results);
        assertEquals(2, results.size());
        
        for (String result : results) {
            assertEquals("Processed response", result);
        }

        verify(mockModel, times(2)).chat(anyList());
    }

    @Test
    void testErrorHandling() {
        // Setup mock to throw exception
        when(mockModel.chat(anyList())).thenThrow(new RuntimeException("API Error"));

        String prompt = "Process:";
        List<String> inputs = Arrays.asList("Input 1");

        assertThrows(RuntimeException.class, () -> {
            workflow.parallel(prompt, inputs, 1);
        });
    }

    @Test
    void testDifferentResponsesForDifferentInputs() {
        // Setup mock to return different responses based on input
        AiMessage response1 = Mockito.mock(AiMessage.class);
        AiMessage response2 = Mockito.mock(AiMessage.class);
        ChatResponse chatResponse1 = Mockito.mock(ChatResponse.class);
        ChatResponse chatResponse2 = Mockito.mock(ChatResponse.class);

        when(response1.text()).thenReturn("Response 1");
        when(response2.text()).thenReturn("Response 2");
        when(chatResponse1.aiMessage()).thenReturn(response1);
        when(chatResponse2.aiMessage()).thenReturn(response2);

        when(mockModel.chat(anyList()))
            .thenReturn(chatResponse1)
            .thenReturn(chatResponse2);

        String prompt = "Process:";
        List<String> inputs = Arrays.asList("Input 1", "Input 2");

        List<String> results = workflow.parallel(prompt, inputs, 2);

        assertEquals(2, results.size());
        assertTrue(results.contains("Response 1"));
        assertTrue(results.contains("Response 2"));
    }

    @Test
    void testLargeNumberOfInputs() {
        // Test with a larger number of inputs to verify parallel processing
        List<String> inputs = Arrays.asList(
            "Item 1", "Item 2", "Item 3", "Item 4", "Item 5",
            "Item 6", "Item 7", "Item 8", "Item 9", "Item 10"
        );

        String prompt = "Process item:";
        List<String> results = workflow.parallel(prompt, inputs, 3);

        assertNotNull(results);
        assertEquals(10, results.size());
        
        // Verify all results are present
        for (String result : results) {
            assertEquals("Processed response", result);
        }

        // Verify correct number of model calls
        verify(mockModel, times(10)).chat(anyList());
    }
}