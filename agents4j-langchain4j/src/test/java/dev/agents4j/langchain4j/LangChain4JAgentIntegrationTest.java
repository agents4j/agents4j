package dev.agents4j.langchain4j;

import dev.agents4j.api.AgentNode;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LangChain4JAgentIntegrationTest {

    @Mock
    private ChatModel mockChatModel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    interface TestAiService {
        String chat(String message);
    }

    @Test
    void testCreateAgentNode() {
        // When
        AgentNode<String, String> agentNode = LangChain4JAgentIntegration.createAgentNode(
            TestAiService.class, 
            mockChatModel
        );

        // Then
        assertNotNull(agentNode);
        assertEquals("LangChain4J-TestAiService", agentNode.getName());
        assertTrue(agentNode instanceof LangChain4JAgentNodeAdapter);
    }

    @Test
    void testCreateAgentNodeWithMemory() {
        // Given
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(5);

        // When
        AgentNode<String, String> agentNode = LangChain4JAgentIntegration.createAgentNode(
            TestAiService.class, 
            mockChatModel,
            memory
        );

        // Then
        assertNotNull(agentNode);
        assertEquals("LangChain4J-TestAiService", agentNode.getName());
        assertTrue(agentNode instanceof LangChain4JAgentNodeAdapter);
        
        LangChain4JAgentNodeAdapter<TestAiService> adapter = 
            (LangChain4JAgentNodeAdapter<TestAiService>) agentNode;
        assertTrue(adapter.getMemory().isPresent());
        assertEquals(memory, adapter.getMemory().get());
    }

    @Test
    void testBuilderPattern() {
        // When
        AgentNode<String, String> agentNode = LangChain4JAgentIntegration
            .builder(TestAiService.class)
            .withChatModel(mockChatModel)
            .build();

        // Then
        assertNotNull(agentNode);
        assertEquals("LangChain4J-TestAiService", agentNode.getName());
    }

    @Test
    void testBuilderWithMemory() {
        // Given
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);

        // When
        AgentNode<String, String> agentNode = LangChain4JAgentIntegration
            .builder(TestAiService.class)
            .withChatModel(mockChatModel)
            .withMemory(memory)
            .build();

        // Then
        assertNotNull(agentNode);
        assertEquals("LangChain4J-TestAiService", agentNode.getName());
        
        LangChain4JAgentNodeAdapter<TestAiService> adapter = 
            (LangChain4JAgentNodeAdapter<TestAiService>) agentNode;
        assertTrue(adapter.getMemory().isPresent());
        assertEquals(memory, adapter.getMemory().get());
    }

    @Test
    void testBuilderWithDefaultMemory() {
        // When
        AgentNode<String, String> agentNode = LangChain4JAgentIntegration
            .builder(TestAiService.class)
            .withChatModel(mockChatModel)
            .withDefaultMemory()
            .build();

        // Then
        assertNotNull(agentNode);
        assertEquals("LangChain4J-TestAiService", agentNode.getName());
        
        LangChain4JAgentNodeAdapter<TestAiService> adapter = 
            (LangChain4JAgentNodeAdapter<TestAiService>) agentNode;
        assertTrue(adapter.getMemory().isPresent());
        assertTrue(adapter.getMemory().get() instanceof MessageWindowChatMemory);
    }

    @Test
    void testBuilderRequiresChatModel() {
        // When & Then
        assertThrows(IllegalStateException.class, () -> 
            LangChain4JAgentIntegration
                .builder(TestAiService.class)
                .build()
        );
    }

    @Test
    void testAgentNodeAdapter() {
        // Given
        TestAiService mockService = message -> "Response: " + message;
        LangChain4JAgentNodeAdapter<TestAiService> adapter = 
            new LangChain4JAgentNodeAdapter<>(mockService, TestAiService.class, mockChatModel, null);

        // When
        Map<String, Object> context = new HashMap<>();
        String result = adapter.process("Hello", context);

        // Then
        assertEquals("Response: Hello", result);
        assertEquals("LangChain4J-TestAiService", adapter.getName());
        assertEquals(mockChatModel, adapter.getModel());
        assertFalse(adapter.getMemory().isPresent());
    }

    @Test
    void testAgentNodeAdapterWithMemory() {
        // Given
        TestAiService mockService = message -> "Response: " + message;
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(5);
        LangChain4JAgentNodeAdapter<TestAiService> adapter = 
            new LangChain4JAgentNodeAdapter<>(mockService, TestAiService.class, mockChatModel, memory);

        // When
        Map<String, Object> context = new HashMap<>();
        String result = adapter.process("Hello", context);

        // Then
        assertEquals("Response: Hello", result);
        assertEquals("LangChain4J-TestAiService", adapter.getName());
        assertEquals(mockChatModel, adapter.getModel());
        assertTrue(adapter.getMemory().isPresent());
        assertEquals(memory, adapter.getMemory().get());
    }

    interface InvalidAiService {
        void doSomething();
    }

    @Test
    void testInvalidServiceInterface() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            new LangChain4JAgentNodeAdapter<>(null, InvalidAiService.class, mockChatModel, null)
        );
    }
}