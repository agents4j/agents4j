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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeast;

/**
 * Integration test for the refactored StatefulOrchestratorWorkersWorkflow.
 * Tests the complete workflow execution using the StatefulWorkflow interface.
 */
class StatefulOrchestratorWorkersWorkflowIntegrationTest {

    private ChatModel mockModel;
    private OrchestratorWorkersWorkflow workflow;

    @BeforeEach
    void setUp() {
        // Setup mock model
        mockModel = Mockito.mock(ChatModel.class);

        // Setup mock responses for different phases
        setupMockResponses();

        // Create workflow using the StatefulWorkflow interface
        workflow = OrchestratorWorkersWorkflow.builder()
            .name("StatefulOrchestratorTest")
            .chatModel(mockModel)
            .addWorker("analyst", "Data analysis specialist", "You are a data analysis expert.")
            .addWorker("researcher", "Research specialist", "You are a research expert.")
            .addWorker("technical", "Technical specialist", "You are a technical expert.")
            .build();
    }

    private void setupMockResponses() {
        // Mock orchestrator response
        AiMessage orchestratorResponse = Mockito.mock(AiMessage.class);
        when(orchestratorResponse.text()).thenReturn(
            "Task Analysis:\n" +
            "This task requires technical analysis and research.\n" +
            "Subtask 1: Technical analysis of the system\n" +
            "Subtask 2: Research current best practices"
        );

        // Mock worker responses
        AiMessage workerResponse1 = Mockito.mock(AiMessage.class);
        when(workerResponse1.text()).thenReturn("Technical analysis complete: System is functioning optimally.");

        AiMessage workerResponse2 = Mockito.mock(AiMessage.class);
        when(workerResponse2.text()).thenReturn("Research complete: Found relevant best practices.");

        // Mock synthesizer response
        AiMessage synthesizerResponse = Mockito.mock(AiMessage.class);
        when(synthesizerResponse.text()).thenReturn(
            "Based on the technical analysis and research, the system is performing well and follows current best practices."
        );

        // Setup chat responses
        ChatResponse chatResponse1 = Mockito.mock(ChatResponse.class);
        ChatResponse chatResponse2 = Mockito.mock(ChatResponse.class);
        ChatResponse chatResponse3 = Mockito.mock(ChatResponse.class);
        ChatResponse chatResponse4 = Mockito.mock(ChatResponse.class);

        when(chatResponse1.aiMessage()).thenReturn(orchestratorResponse);
        when(chatResponse2.aiMessage()).thenReturn(workerResponse1);
        when(chatResponse3.aiMessage()).thenReturn(workerResponse2);
        when(chatResponse4.aiMessage()).thenReturn(synthesizerResponse);

        when(mockModel.chat(anyList()))
            .thenReturn(chatResponse1)
            .thenReturn(chatResponse2)
            .thenReturn(chatResponse3)
            .thenReturn(chatResponse4);
    }

    @Test
    void testStatefulWorkflowStart() throws WorkflowExecutionException {
        OrchestratorWorkersWorkflow.OrchestratorInput input = 
            new OrchestratorWorkersWorkflow.OrchestratorInput("Analyze system performance and recommend improvements");

        StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result = workflow.start(input);

        assertNotNull(result);
        assertTrue(result.isCompleted());
        assertFalse(result.isSuspended());
        assertFalse(result.isError());

        OrchestratorWorkersWorkflow.WorkerResponse output = result.getOutput().orElse(null);
        assertNotNull(output);
        assertTrue(output.isSuccessful());
        assertNotNull(output.getFinalResult());
        assertTrue(output.getFinalResult().contains("system is performing well"));

        // Verify state
        WorkflowState finalState = result.getState();
        assertNotNull(finalState);
        assertTrue(finalState.getWorkflowId().startsWith("StatefulOrchestratorTest-"));
        assertTrue(finalState.get("completed", false));

        // Verify model was called at least for orchestrator, workers, and synthesizer
        verify(mockModel, atLeast(3)).chat(anyList());
    }

    @Test
    void testStatefulWorkflowWithContext() throws WorkflowExecutionException {
        OrchestratorWorkersWorkflow.OrchestratorInput input = 
            new OrchestratorWorkersWorkflow.OrchestratorInput("Evaluate system architecture");

        Map<String, Object> context = new HashMap<>();
        context.put("user_id", "test_user");
        context.put("priority", "high");

        StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result = workflow.start(input, context);

        assertTrue(result.isCompleted());
        
        // Verify context was populated with workflow information
        assertEquals("StatefulOrchestratorTest", context.get("workflow_name"));
        assertEquals("Evaluate system architecture", context.get("task_description"));
        assertNotNull(context.get("available_workers"));
        assertNotNull(context.get("execution_time"));

        // Verify original context is preserved
        assertEquals("test_user", context.get("user_id"));
        assertEquals("high", context.get("priority"));

        verify(mockModel, atLeast(3)).chat(anyList());
    }

    @Test
    void testStatefulWorkflowAsync() throws Exception {
        OrchestratorWorkersWorkflow.OrchestratorInput input = 
            new OrchestratorWorkersWorkflow.OrchestratorInput("Perform comprehensive system audit");

        CompletableFuture<StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse>> future = 
            workflow.startAsync(input);
        StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result = future.get();

        assertTrue(result.isCompleted());
        OrchestratorWorkersWorkflow.WorkerResponse output = result.getOutput().orElse(null);
        assertNotNull(output);
        assertTrue(output.isSuccessful());

        verify(mockModel, atLeast(3)).chat(anyList());
    }

    @Test
    void testWorkflowStructure() {
        // Verify the workflow has proper stateful structure
        assertDoesNotThrow(() -> workflow.validate());

        // Verify nodes exist
        assertFalse(workflow.getNodes().isEmpty());
        assertEquals(3, workflow.getNodes().size());

        // Verify specific nodes
        assertTrue(workflow.getNode("orchestrator").isPresent());
        assertTrue(workflow.getNode("workers").isPresent());
        assertTrue(workflow.getNode("synthesizer").isPresent());

        // Verify routes exist
        assertFalse(workflow.getRoutes().isEmpty());
        assertEquals(2, workflow.getRoutes().size());

        // Verify entry points
        assertFalse(workflow.getEntryPoints().isEmpty());
        assertEquals(1, workflow.getEntryPoints().size());
        assertEquals("orchestrator", workflow.getEntryPoints().get(0).getNodeId());

        // Verify routes from orchestrator node
        List<dev.agents4j.api.workflow.WorkflowRoute<OrchestratorWorkersWorkflow.OrchestratorInput>> routesFromOrchestrator = 
            workflow.getRoutesFrom("orchestrator");
        assertEquals(1, routesFromOrchestrator.size());
        assertEquals("workers", routesFromOrchestrator.get(0).getToNodeId());
    }

    @Test
    void testErrorHandling() throws WorkflowExecutionException {
        // Setup mock to throw exception during orchestration
        when(mockModel.chat(anyList())).thenThrow(new RuntimeException("Orchestration error"));

        OrchestratorWorkersWorkflow.OrchestratorInput input = 
            new OrchestratorWorkersWorkflow.OrchestratorInput("Task that will fail");

        StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result = workflow.start(input);

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
        OrchestratorWorkersWorkflow.OrchestratorInput input = 
            new OrchestratorWorkersWorkflow.OrchestratorInput("Test task for metadata");

        StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result = workflow.start(input);

        assertTrue(result.isCompleted());

        // Check metadata
        Map<String, Object> metadata = result.getMetadata();
        assertNotNull(metadata);
        assertTrue(metadata.containsKey("execution_time"));
        assertTrue(metadata.containsKey("subtasks_count"));
        assertTrue((Long) metadata.get("execution_time") >= 0);
    }

    @Test
    void testWorkflowName() {
        assertEquals("StatefulOrchestratorTest", workflow.getName());
    }

    @Test
    void testSubtaskExecution() throws WorkflowExecutionException {
        OrchestratorWorkersWorkflow.OrchestratorInput input = 
            new OrchestratorWorkersWorkflow.OrchestratorInput("Complex multi-step analysis");

        StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result = workflow.start(input);

        assertTrue(result.isCompleted());
        OrchestratorWorkersWorkflow.WorkerResponse output = result.getOutput().orElse(null);
        assertNotNull(output);

        // Verify subtasks were created and executed
        assertFalse(output.getSubtasks().isEmpty());
        assertFalse(output.getSubtaskResults().isEmpty());
        assertEquals(output.getSubtasks().size(), output.getSubtaskResults().size());

        // Verify all subtasks were successful
        assertTrue(output.getSubtaskResults().stream().allMatch(
            OrchestratorWorkersWorkflow.SubtaskResult::isSuccessful));
    }

    @Test
    void testStateProgression() throws WorkflowExecutionException {
        OrchestratorWorkersWorkflow.OrchestratorInput input = 
            new OrchestratorWorkersWorkflow.OrchestratorInput("Test state progression");

        StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result = workflow.start(input);

        // Verify final state contains expected data
        WorkflowState finalState = result.getState();
        assertTrue(finalState.get("input").isPresent());
        assertTrue(finalState.get("workerConfigs").isPresent());
        assertTrue(finalState.get("result").isPresent());
        assertTrue(finalState.get("completed", false));

        // Verify state version progression
        assertTrue(finalState.getVersion() > 1);
    }
}