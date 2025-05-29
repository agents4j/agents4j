package dev.agents4j.workflow;

import dev.agents4j.Agents4J;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Test cases for OrchestratorWorkersWorkflow demonstrating the pattern usage.
 */
public class OrchestratorWorkersWorkflowTest {

    @Mock
    private ChatModel mockChatModel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        setupMockChatModel();
    }

    private void setupMockChatModel() {
        // Create mock AI messages
        AiMessage orchestratorMessage = AiMessage.from("Worker: analyst - Analyze market trends\nWorker: writer - Create summary report");
        AiMessage analystMessage = AiMessage.from("Market analysis shows positive trends");
        AiMessage writerMessage = AiMessage.from("Summary report completed");
        AiMessage synthesizerMessage = AiMessage.from("Combined analysis shows strong market position with positive outlook");

        // Create mock responses
        ChatResponse orchestratorResponse = ChatResponse.builder().aiMessage(orchestratorMessage).build();
        ChatResponse analystResponse = ChatResponse.builder().aiMessage(analystMessage).build();
        ChatResponse writerResponse = ChatResponse.builder().aiMessage(writerMessage).build();
        ChatResponse synthesizerResponse = ChatResponse.builder().aiMessage(synthesizerMessage).build();

        // Setup mock behavior
        when(mockChatModel.chat(anyList()))
            .thenReturn(orchestratorResponse)
            .thenReturn(analystResponse)
            .thenReturn(writerResponse)
            .thenReturn(synthesizerResponse);
    }

    @Test
    void testOrchestratorWorkersWorkflowCreation() {
        OrchestratorWorkersWorkflow workflow = OrchestratorWorkersWorkflow.builder()
            .name("TestWorkflow")
            .chatModel(mockChatModel)
            .addWorker("analyst", "Data analysis worker", "You are a data analyst.")
            .addWorker("writer", "Content creation worker", "You are a writer.")
            .build();

        assertNotNull(workflow);
        assertEquals("TestWorkflow", workflow.getName());
        assertEquals("orchestrator-workers", workflow.getConfigurationProperty("workflowType", ""));
    }

    @Test
    void testStandardOrchestratorWorkersWorkflow() {
        OrchestratorWorkersWorkflow workflow = Agents4J.createOrchestratorWorkersWorkflow(
            "StandardWorkflow",
            mockChatModel
        );

        assertNotNull(workflow);
        @SuppressWarnings("unchecked")
        Set<String> workerTypes = workflow.getConfigurationProperty("workerTypes", java.util.Set.of());
        assertTrue(workerTypes.contains("analyst"));
        assertTrue(workerTypes.contains("writer"));
    }

    @Test
    void testCustomOrchestratorWorkersWorkflow() {
        OrchestratorWorkersWorkflow workflow = Agents4J.createCustomOrchestratorWorkersWorkflow(
            "CustomWorkflow",
            mockChatModel,
            Agents4J.worker("translator", "Translates content", "You are a professional translator."),
            Agents4J.worker("editor", "Edits and proofreads", "You are an editor and proofreader.")
        );

        assertNotNull(workflow);
        @SuppressWarnings("unchecked")
        Set<String> workerTypes = workflow.getConfigurationProperty("workerTypes", java.util.Set.of());
        assertTrue(workerTypes.contains("translator"));
        assertTrue(workerTypes.contains("editor"));
    }

    @Test
    void testWorkflowExecution() throws Exception {
        OrchestratorWorkersWorkflow workflow = OrchestratorWorkersWorkflow.builder()
            .name("ExecutionTest")
            .chatModel(mockChatModel)
            .addWorker("analyst", "Analysis worker", "You are an analyst.")
            .addWorker("writer", "Writing worker", "You are a writer.")
            .build();

        OrchestratorWorkersWorkflow.OrchestratorInput input = 
            new OrchestratorWorkersWorkflow.OrchestratorInput("Analyze market trends and create a report");

        OrchestratorWorkersWorkflow.WorkerResponse response = workflow.execute(input);

        assertNotNull(response);
        assertTrue(response.isSuccessful());
        assertNotNull(response.getFinalResult());
        assertFalse(response.getSubtasks().isEmpty());
        assertFalse(response.getSubtaskResults().isEmpty());
    }

    @Test
    void testOrchestratedQuery() {
        String result = Agents4J.orchestratedQuery(
            mockChatModel,
            "Create a comprehensive business analysis report"
        );

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testCustomOrchestratedQuery() {
        String result = Agents4J.customOrchestratedQuery(
            mockChatModel,
            "Translate and localize marketing content for international markets",
            Agents4J.worker("translator", "Translates content", "You are a professional translator."),
            Agents4J.worker("localizer", "Localizes content", "You are a localization expert."),
            Agents4J.worker("marketer", "Marketing strategy", "You are a marketing strategist.")
        );

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testWorkflowWithContext() throws Exception {
        OrchestratorWorkersWorkflow workflow = OrchestratorWorkersWorkflow.builder()
            .name("ContextTest")
            .chatModel(mockChatModel)
            .addWorker("analyst", "Analysis worker", "You are an analyst.")
            .build();

        OrchestratorWorkersWorkflow.OrchestratorInput input = 
            new OrchestratorWorkersWorkflow.OrchestratorInput("Test task");

        Map<String, Object> context = new HashMap<>();
        OrchestratorWorkersWorkflow.WorkerResponse response = workflow.execute(input, context);

        assertNotNull(response);
        assertTrue(context.containsKey("workflow_name"));
        assertTrue(context.containsKey("execution_time"));
        assertEquals("ContextTest", context.get("workflow_name"));
    }

    @Test
    void testAsyncExecution() throws Exception {
        OrchestratorWorkersWorkflow workflow = OrchestratorWorkersWorkflow.builder()
            .name("AsyncTest")
            .chatModel(mockChatModel)
            .addWorker("analyst", "Analysis worker", "You are an analyst.")
            .build();

        OrchestratorWorkersWorkflow.OrchestratorInput input = 
            new OrchestratorWorkersWorkflow.OrchestratorInput("Async test task");

        CompletableFuture<OrchestratorWorkersWorkflow.WorkerResponse> future = 
            workflow.executeAsync(input);

        OrchestratorWorkersWorkflow.WorkerResponse response = future.get();
        assertNotNull(response);
        assertTrue(response.isSuccessful());
    }

    @Test
    void testBuilderValidation() {
        // Test that builder requires ChatModel
        assertThrows(IllegalStateException.class, () -> {
            OrchestratorWorkersWorkflow.builder()
                .name("TestWorkflow")
                .build();
        });

        // Test that builder requires at least one worker
        assertThrows(IllegalStateException.class, () -> {
            OrchestratorWorkersWorkflow.builder()
                .name("TestWorkflow")
                .chatModel(mockChatModel)
                .build();
        });
    }

    @Test
    void testInputValidation() {
        assertThrows(NullPointerException.class, () -> {
            new OrchestratorWorkersWorkflow.OrchestratorInput(null);
        });
    }

    @Test
    void testWorkerConfigValidation() {
        assertThrows(NullPointerException.class, () -> {
            new OrchestratorWorkersWorkflow.WorkerConfig(null, "description", "prompt");
        });

        assertThrows(NullPointerException.class, () -> {
            new OrchestratorWorkersWorkflow.WorkerConfig("type", null, "prompt");
        });

        assertThrows(NullPointerException.class, () -> {
            new OrchestratorWorkersWorkflow.WorkerConfig("type", "description", null);
        });
    }

    @Test
    void testCustomPromptsWorkflow() throws Exception {
        String customOrchestratorPrompt = "Custom orchestrator instructions.";
        String customSynthesizerPrompt = "Custom synthesizer instructions.";

        OrchestratorWorkersWorkflow workflow = OrchestratorWorkersWorkflow.builder()
            .name("CustomPromptsTest")
            .chatModel(mockChatModel)
            .addWorker("analyst", "Analysis worker", "You are an analyst.")
            .orchestratorPrompt(customOrchestratorPrompt)
            .synthesizerPrompt(customSynthesizerPrompt)
            .build();

        OrchestratorWorkersWorkflow.OrchestratorInput input = 
            new OrchestratorWorkersWorkflow.OrchestratorInput("Test task");

        OrchestratorWorkersWorkflow.WorkerResponse response = workflow.execute(input);
        assertNotNull(response);
        assertTrue(response.isSuccessful());
    }

    @Test
    void testSubtaskAndResultClasses() {
        OrchestratorWorkersWorkflow.Subtask subtask = 
            new OrchestratorWorkersWorkflow.Subtask("1", "analyst", "Analyze data");
        
        assertEquals("1", subtask.getId());
        assertEquals("analyst", subtask.getWorkerType());
        assertEquals("Analyze data", subtask.getInstructions());

        OrchestratorWorkersWorkflow.SubtaskResult result = 
            new OrchestratorWorkersWorkflow.SubtaskResult("1", "analyst", "Analysis complete", true);
        
        assertEquals("1", result.getSubtaskId());
        assertEquals("analyst", result.getWorkerType());
        assertEquals("Analysis complete", result.getResult());
        assertTrue(result.isSuccessful());
    }

    @Test
    void testFactoryMethodWithCustomWorkers() {
        OrchestratorWorkersWorkflow workflow = AgentWorkflowFactory.createCustomOrchestratorWorkersWorkflow(
            "FactoryTest",
            mockChatModel,
            AgentWorkflowFactory.worker("custom", "Custom worker", "You are a custom worker.")
        );

        assertNotNull(workflow);
        assertEquals("FactoryTest", workflow.getName());
    }

    @Test
    void testFactoryMethodValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            AgentWorkflowFactory.createCustomOrchestratorWorkersWorkflow(
                "Test",
                mockChatModel
                // No workers provided
            );
        });
    }
}