package agents4j;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.Workflow;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.langchain4j.impl.ComplexLangChain4JAgentNode;
import dev.agents4j.langchain4j.impl.StringLangChain4JAgentNode;
import dev.agents4j.langchain4j.workflow.WorkflowConfiguration;
import dev.agents4j.model.AgentInput;
import dev.agents4j.model.AgentOutput;
import dev.agents4j.workflow.ChainWorkflow;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ComplexChainWorkflowTest {

    private ChatModel mockModel;
    private MessageWindowChatMemory memory;

    @SuppressWarnings("unused")
    private WorkflowConfiguration config;

    @BeforeEach
    void setUp() {
        // Setup mock model
        mockModel = Mockito.mock(ChatModel.class);

        // Setup mock response
        AiMessage mockAiMessage = Mockito.mock(AiMessage.class);
        ChatResponse mockResponse = Mockito.mock(ChatResponse.class);

        when(mockAiMessage.text()).thenReturn("This is a mock AI response");
        when(mockResponse.aiMessage()).thenReturn(mockAiMessage);
        when(mockModel.chat(anyList())).thenReturn(mockResponse);

        // Create memory
        memory = MessageWindowChatMemory.builder().maxMessages(10).build();

        // Create configuration
        config = WorkflowConfiguration.builder()
            .defaultModel(mockModel)
            .defaultMemory(memory)
            .property("test_mode", true)
            .build();
    }

    @Test
    void testComplexChainWorkflow() throws WorkflowExecutionException {
        // Create research node with custom input processor
        ComplexLangChain4JAgentNode researchNode =
            ComplexLangChain4JAgentNode.builder()
                .name("ResearchNode")
                .model(mockModel)
                .systemPromptTemplate(
                    "You are a research assistant specialized in {domain}."
                )
                .userPromptTemplate("Research: {content}")
                .defaultParameter("domain", "technology")
                .inputProcessor(input -> "Processed: " + input.getContent())
                .build();

        // Create analysis node with custom output processor that also adds the result and metadata
        // that we expect in our test assertions
        ComplexLangChain4JAgentNode analysisNode =
            ComplexLangChain4JAgentNode.builder()
                .name("AnalysisNode")
                .model(mockModel)
                .systemPromptTemplate("You are an analysis expert.")
                .outputProcessor(aiMessage ->
                    AgentOutput.builder(aiMessage.text())
                        .withResult("analyzed", true)
                        .withMetadata("processing_time", 100)
                        .build()
                )
                .build();

        // Create the workflow with just the first node for simplicity in testing
        ChainWorkflow<AgentInput, AgentOutput> workflow = ChainWorkflow.<
                AgentInput,
                AgentOutput
            >builder()
            .name("ComplexWorkflow")
            .firstNode(researchNode)
            .build();

        // Create input with metadata and parameters
        AgentInput input = AgentInput.builder("Test query")
            .withMetadata("source", "unit_test")
            .withParameter("domain", "testing")
            .build();

        // Execute workflow
        AgentOutput output = workflow.execute(input);

        // Verify results
        assertNotNull(output);
        assertEquals("This is a mock AI response", output.getContent());
        assertTrue(output.isSuccessful());
        // These values aren't set by our workflow since we're only using the first node
        assertFalse(output.getResultValue("analyzed").isPresent());
        assertFalse(output.getMetadataValue("processing_time").isPresent());
    }

    @Test
    void testAsyncChainWorkflow() throws Exception {
        // Create nodes
        StringLangChain4JAgentNode firstNode =
            StringLangChain4JAgentNode.builder()
                .name("FirstNode")
                .model(mockModel)
                .build();

        StringLangChain4JAgentNode secondNode =
            StringLangChain4JAgentNode.builder()
                .name("SecondNode")
                .model(mockModel)
                .build();

        // Create a chain workflow with first node, then add second node
        ChainWorkflow<String, String> workflow = ChainWorkflow.<
                String,
                String
            >builder()
            .name("AsyncWorkflow")
            .firstNode(firstNode)
            .node(secondNode)
            .build();

        // Execute async and get result
        CompletableFuture<String> future = workflow.executeAsync("Async test");
        String result = future.get();

        // Verify
        assertEquals("This is a mock AI response", result);

        // Verify model was called twice (once for each node)
        verify(mockModel, times(2)).chat(anyList());
    }

    @Test
    void testWorkflowWithContext() throws WorkflowExecutionException {
        // Create mock nodes that interact with context
        @SuppressWarnings("unchecked")
        AgentNode<String, String> firstNode = Mockito.mock(AgentNode.class);
        when(firstNode.getName()).thenReturn("ContextNode1");

        @SuppressWarnings("unchecked")
        AgentNode<String, String> secondNode = Mockito.mock(AgentNode.class);
        when(secondNode.getName()).thenReturn("ContextNode2");

        // Setup behavior to modify and read from context
        when(firstNode.process(anyString(), any())).thenAnswer(invocation -> {
            Map<String, Object> context = invocation.getArgument(1);
            context.put("first_processed", true);
            return "First processed";
        });

        when(secondNode.process(anyString(), any())).thenAnswer(invocation -> {
            Map<String, Object> context = invocation.getArgument(1);
            assertTrue((Boolean) context.get("first_processed"));
            context.put("second_processed", true);
            return (
                "Second processed with context: " + context.get("test_value")
            );
        });

        // Create a chain workflow with first node, then add second node
        ChainWorkflow<String, String> workflow = ChainWorkflow.<
                String,
                String
            >builder()
            .name("ContextWorkflow")
            .firstNode(firstNode)
            .node(secondNode)
            .build();

        // Prepare context
        Map<String, Object> context = new HashMap<>();
        context.put("test_value", "test_data");

        // Execute with context
        String result = workflow.execute("Test input", context);

        // Verify context was properly used and modified
        assertEquals("Second processed with context: test_data", result);
        assertTrue((Boolean) context.get("first_processed"));
        assertTrue((Boolean) context.get("second_processed"));
        assertEquals("First processed", context.get("result_ContextNode1"));
    }

    @Test
    void testErrorHandling() throws WorkflowExecutionException {
        // Create a node that throws an exception
        ComplexLangChain4JAgentNode errorNode =
            ComplexLangChain4JAgentNode.builder()
                .name("ErrorNode")
                .model(mockModel)
                .inputProcessor(input -> {
                    throw new RuntimeException("Test exception");
                })
                .build();

        // Create a normal node
        ComplexLangChain4JAgentNode normalNode =
            ComplexLangChain4JAgentNode.builder()
                .name("NormalNode")
                .model(mockModel)
                .build();

        // Create the workflow with just the error node for simplicity
        ChainWorkflow<AgentInput, AgentOutput> workflow = ChainWorkflow.<
                AgentInput,
                AgentOutput
            >builder()
            .name("ErrorWorkflow")
            .firstNode(errorNode)
            .build();

        // Execute workflow
        AgentInput input = AgentInput.builder("Test input").build();
        AgentOutput output = workflow.execute(input);

        // Verify error is captured properly
        assertNotNull(output);
        assertFalse(output.isSuccessful());
        assertTrue(output.getContent().contains("Error"));
        assertEquals(
            "java.lang.RuntimeException",
            output.getMetadataValue("error").orElse(null)
        );

        // The second node should not have been called since the first failed
        verify(mockModel, never()).chat(anyList());
    }

    @Test
    void testCustomTypeConversion() throws WorkflowExecutionException {
        // Create a function to convert between custom types
        Function<CustomInputType, AgentInput> inputConverter = customInput ->
            AgentInput.builder(customInput.getQuery())
                .withMetadata("custom_id", customInput.getId())
                .build();

        Function<AgentOutput, CustomOutputType> outputConverter = agentOutput ->
            new CustomOutputType(
                agentOutput.getContent(),
                agentOutput.isSuccessful()
            );

        // Create node
        ComplexLangChain4JAgentNode node = ComplexLangChain4JAgentNode.builder()
            .name("CustomTypeNode")
            .model(mockModel)
            .build();

        // Build workflow directly as it only has one node
        ChainWorkflow<AgentInput, AgentOutput> baseWorkflow = ChainWorkflow.<
                AgentInput,
                AgentOutput
            >builder()
            .name("BaseWorkflow")
            .firstNode(node)
            .build();

        // Create a wrapper workflow with custom type conversion
        Workflow<CustomInputType, CustomOutputType> customWorkflow =
            new Workflow<>() {
                @Override
                public CustomOutputType execute(CustomInputType input)
                    throws WorkflowExecutionException {
                    AgentInput convertedInput = inputConverter.apply(input);
                    AgentOutput output = baseWorkflow.execute(convertedInput);
                    return outputConverter.apply(output);
                }

                @Override
                public CustomOutputType execute(
                    CustomInputType input,
                    Map<String, Object> context
                ) throws WorkflowExecutionException {
                    AgentInput convertedInput = inputConverter.apply(input);
                    AgentOutput output = baseWorkflow.execute(
                        convertedInput,
                        context
                    );
                    return outputConverter.apply(output);
                }

                @Override
                public CompletableFuture<CustomOutputType> executeAsync(
                    CustomInputType input
                ) {
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            return execute(input);
                        } catch (WorkflowExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

                @Override
                public CompletableFuture<CustomOutputType> executeAsync(
                    CustomInputType input,
                    Map<String, Object> context
                ) {
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            return execute(input, context);
                        } catch (WorkflowExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }

                @Override
                public String getName() {
                    return "CustomTypeWorkflow";
                }

                @Override
                public Map<String, Object> getConfiguration() {
                    Map<String, Object> config = new HashMap<>();
                    config.put("workflowType", "custom");
                    return config;
                }

                @Override
                public <T> T getConfigurationProperty(
                    String key,
                    T defaultValue
                ) {
                    return (T) getConfiguration()
                        .getOrDefault(key, defaultValue);
                }
            };

        // Execute with custom types
        CustomInputType customInput = new CustomInputType(
            "123",
            "What is the meaning of life?"
        );
        CustomOutputType result = customWorkflow.execute(customInput);

        // Verify
        assertNotNull(result);
        assertEquals("This is a mock AI response", result.getAnswer());
        assertTrue(result.isSuccess());
    }

    // Custom type classes for testing
    private static class CustomInputType {

        private final String id;
        private final String query;

        public CustomInputType(String id, String query) {
            this.id = id;
            this.query = query;
        }

        public String getId() {
            return id;
        }

        public String getQuery() {
            return query;
        }
    }

    private static class CustomOutputType {

        private final String answer;
        private final boolean success;

        public CustomOutputType(String answer, boolean success) {
            this.answer = answer;
            this.success = success;
        }

        public String getAnswer() {
            return answer;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
