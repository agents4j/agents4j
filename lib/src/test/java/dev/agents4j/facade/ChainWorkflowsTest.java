package dev.agents4j.facade;

import dev.agents4j.exception.AgentExecutionException;
import dev.agents4j.workflow.ChainWorkflow;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChainWorkflowsTest {

    @Mock
    private ChatModel mockChatModel;

    @BeforeEach
    void setUp() {
        // Setup mock behavior
    }

    @Test
    void shouldCreateChainWorkflow() {
        // Given
        String name = "TestWorkflow";
        String systemPrompt = "You are a helpful assistant";

        // When
        ChainWorkflow<String, String> workflow = ChainWorkflows.create(name, mockChatModel, systemPrompt);

        // Then
        assertNotNull(workflow);
        assertEquals(name, workflow.getName());
    }

    @Test
    void shouldThrowExceptionForNullName() {
        // Given
        String systemPrompt = "You are a helpful assistant";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.create(null, mockChatModel, systemPrompt));
    }

    @Test
    void shouldThrowExceptionForEmptyName() {
        // Given
        String systemPrompt = "You are a helpful assistant";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.create("", mockChatModel, systemPrompt));
    }

    @Test
    void shouldThrowExceptionForNullModel() {
        // Given
        String name = "TestWorkflow";
        String systemPrompt = "You are a helpful assistant";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.create(name, null, systemPrompt));
    }

    @Test
    void shouldThrowExceptionForNoSystemPrompts() {
        // Given
        String name = "TestWorkflow";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.create(name, mockChatModel));
    }

    @Test
    void shouldThrowExceptionForNullSystemPrompt() {
        // Given
        String name = "TestWorkflow";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.create(name, mockChatModel, (String) null));
    }

    @Test
    void shouldCreateConversationalWorkflow() {
        // Given
        String name = "ConversationalWorkflow";
        int maxMessages = 10;
        String systemPrompt = "You are a helpful assistant";

        // When
        ChainWorkflow<String, String> workflow = ChainWorkflows.createConversational(
            name, mockChatModel, maxMessages, systemPrompt);

        // Then
        assertNotNull(workflow);
        assertEquals(name, workflow.getName());
    }

    @Test
    void shouldThrowExceptionForNegativeMaxMessages() {
        // Given
        String name = "ConversationalWorkflow";
        int maxMessages = -1;
        String systemPrompt = "You are a helpful assistant";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.createConversational(name, mockChatModel, maxMessages, systemPrompt));
    }

    @Test
    void shouldThrowExceptionForZeroMaxMessages() {
        // Given
        String name = "ConversationalWorkflow";
        int maxMessages = 0;
        String systemPrompt = "You are a helpful assistant";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.createConversational(name, mockChatModel, maxMessages, systemPrompt));
    }

    @Test
    void shouldValidateExecuteSimpleParameters() {
        // Given
        String systemPrompt = "You are a helpful assistant";
        String query = "Test query";

        // When & Then - should not throw for valid parameters
        assertDoesNotThrow(() -> 
            ChainWorkflows.executeSimple(mockChatModel, systemPrompt, query));
    }

    @Test
    void shouldThrowExceptionForNullModelInExecuteSimple() {
        // Given
        String systemPrompt = "You are a helpful assistant";
        String query = "Test query";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.executeSimple(null, systemPrompt, query));
    }

    @Test
    void shouldThrowExceptionForNullSystemPromptInExecuteSimple() {
        // Given
        String query = "Test query";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.executeSimple(mockChatModel, null, query));
    }

    @Test
    void shouldThrowExceptionForEmptySystemPromptInExecuteSimple() {
        // Given
        String query = "Test query";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.executeSimple(mockChatModel, "", query));
    }

    @Test
    void shouldThrowExceptionForNullQueryInExecuteSimple() {
        // Given
        String systemPrompt = "You are a helpful assistant";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.executeSimple(mockChatModel, systemPrompt, null));
    }

    @Test
    void shouldThrowExceptionForEmptyQueryInExecuteSimple() {
        // Given
        String systemPrompt = "You are a helpful assistant";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.executeSimple(mockChatModel, systemPrompt, ""));
    }

    @Test
    void shouldValidateExecuteComplexParameters() {
        // Given
        String query = "Test query";
        String[] systemPrompts = {"Prompt 1", "Prompt 2"};

        // When & Then - should not throw for valid parameters
        assertDoesNotThrow(() -> 
            ChainWorkflows.executeComplex(mockChatModel, query, systemPrompts));
    }

    @Test
    void shouldThrowExceptionForNullModelInExecuteComplex() {
        // Given
        String query = "Test query";
        String[] systemPrompts = {"Prompt 1", "Prompt 2"};

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.executeComplex(null, query, systemPrompts));
    }

    @Test
    void shouldThrowExceptionForNullQueryInExecuteComplex() {
        // Given
        String[] systemPrompts = {"Prompt 1", "Prompt 2"};

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.executeComplex(mockChatModel, null, systemPrompts));
    }

    @Test
    void shouldThrowExceptionForEmptyQueryInExecuteComplex() {
        // Given
        String[] systemPrompts = {"Prompt 1", "Prompt 2"};

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.executeComplex(mockChatModel, "", systemPrompts));
    }

    @Test
    void shouldThrowExceptionForNoSystemPromptsInExecuteComplex() {
        // Given
        String query = "Test query";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.executeComplex(mockChatModel, query));
    }

    @Test
    void shouldValidateExecuteConversationalParameters() {
        // Given
        String query = "Test query";
        int maxMessages = 5;
        String[] systemPrompts = {"Prompt 1", "Prompt 2"};

        // When & Then - should not throw for valid parameters
        assertDoesNotThrow(() -> 
            ChainWorkflows.executeConversational(mockChatModel, query, maxMessages, systemPrompts));
    }

    @Test
    void shouldThrowExceptionForNegativeMaxMessagesInExecuteConversational() {
        // Given
        String query = "Test query";
        int maxMessages = -1;
        String[] systemPrompts = {"Prompt 1", "Prompt 2"};

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.executeConversational(mockChatModel, query, maxMessages, systemPrompts));
    }

    @Test
    void shouldThrowExceptionForZeroMaxMessagesInExecuteConversational() {
        // Given
        String query = "Test query";
        int maxMessages = 0;
        String[] systemPrompts = {"Prompt 1", "Prompt 2"};

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.executeConversational(mockChatModel, query, maxMessages, systemPrompts));
    }

    @Test
    void shouldValidateMultipleSystemPrompts() {
        // Given
        String name = "MultiAgentWorkflow";
        String[] systemPrompts = {
            "You are a researcher",
            "You are an analyst", 
            "You are a writer"
        };

        // When
        ChainWorkflow<String, String> workflow = ChainWorkflows.create(name, mockChatModel, systemPrompts);

        // Then
        assertNotNull(workflow);
        assertEquals(name, workflow.getName());
    }

    @Test
    void shouldHandleNullInSystemPromptsArray() {
        // Given
        String name = "TestWorkflow";
        String[] systemPrompts = {"Valid prompt", null, "Another valid prompt"};

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.create(name, mockChatModel, systemPrompts));
    }

    @Test
    void shouldHandleEmptyStringInSystemPromptsArray() {
        // Given
        String name = "TestWorkflow";
        String[] systemPrompts = {"Valid prompt", "", "Another valid prompt"};

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.create(name, mockChatModel, systemPrompts));
    }

    @Test
    void shouldHandleWhitespaceOnlyStringInSystemPromptsArray() {
        // Given
        String name = "TestWorkflow";
        String[] systemPrompts = {"Valid prompt", "   ", "Another valid prompt"};

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.create(name, mockChatModel, systemPrompts));
    }

    @Test
    void shouldTrimWorkflowName() {
        // Given
        String name = "  TestWorkflow  ";
        String systemPrompt = "You are a helpful assistant";

        // When & Then - should not throw for name with whitespace
        assertDoesNotThrow(() -> 
            ChainWorkflows.create(name, mockChatModel, systemPrompt));
    }

    @Test
    void shouldRejectWhitespaceOnlyWorkflowName() {
        // Given
        String name = "   ";
        String systemPrompt = "You are a helpful assistant";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            ChainWorkflows.create(name, mockChatModel, systemPrompt));
    }
}