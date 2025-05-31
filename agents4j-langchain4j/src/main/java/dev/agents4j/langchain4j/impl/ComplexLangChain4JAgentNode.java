package dev.agents4j.langchain4j.impl;

import dev.agents4j.langchain4j.LangChain4JAgentNode;
import dev.agents4j.model.AgentInput;
import dev.agents4j.model.AgentOutput;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A more complex implementation of LangChain4JAgentNode that works with structured
 * input and output types and offers more customization options.
 *
 * This agent node supports:
 * - Custom input preprocessing
 * - Output post-processing and validation
 * - Template-based system and user prompts
 * - Structured data extraction from AI responses
 */
public class ComplexLangChain4JAgentNode
    implements LangChain4JAgentNode<AgentInput, AgentOutput> {

    private final String name;
    private final ChatModel model;
    private final ChatMemory memory;
    private final String systemPromptTemplate;
    private final String userPromptTemplate;
    private final Function<AgentInput, String> inputProcessor;
    private final Function<AiMessage, AgentOutput> outputProcessor;
    private final Map<String, Object> defaultParameters = new HashMap<>();

    /**
     * Creates a new ComplexLangChain4JAgentNode with the provided configuration.
     *
     * @param name The name of the agent node
     * @param model The ChatModel to use
     * @param memory The ChatMemory to use, can be null if no memory is needed
     * @param systemPromptTemplate The system prompt template, can be null if no system prompt is needed
     * @param userPromptTemplate The user prompt template, can be null to use the input content directly
     * @param inputProcessor Function to process the input before sending to the model, can be null
     * @param outputProcessor Function to process the AI message before returning, can be null
     * @param defaultParameters Default parameters to use for each request
     */
    private ComplexLangChain4JAgentNode(
        String name,
        ChatModel model,
        ChatMemory memory,
        String systemPromptTemplate,
        String userPromptTemplate,
        Function<AgentInput, String> inputProcessor,
        Function<AiMessage, AgentOutput> outputProcessor,
        Map<String, Object> defaultParameters
    ) {
        this.name = Objects.requireNonNull(name, "Agent name cannot be null");
        this.model = Objects.requireNonNull(
            model,
            "ChatLanguageModel cannot be null"
        );
        this.memory = memory; // Memory can be null
        this.systemPromptTemplate = systemPromptTemplate; // Can be null
        this.userPromptTemplate = userPromptTemplate; // Can be null
        this.inputProcessor = inputProcessor != null
            ? inputProcessor
            : AgentInput::getContent;
        this.outputProcessor = outputProcessor != null
            ? outputProcessor
            : aiMessage -> AgentOutput.builder(aiMessage.text()).build();

        if (defaultParameters != null) {
            this.defaultParameters.putAll(defaultParameters);
        }

        // If memory and system prompt are both provided, add the system message to memory
        if (
            memory != null &&
            systemPromptTemplate != null &&
            !systemPromptTemplate.isEmpty()
        ) {
            memory.add(new SystemMessage(systemPromptTemplate));
        }
    }

    @Override
    public ChatModel getModel() {
        return model;
    }

    @Override
    public Optional<ChatMemory> getMemory() {
        return Optional.ofNullable(memory);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AgentOutput process(AgentInput input, Map<String, Object> context) {
        try {
            // Process the input
            String processedInput = inputProcessor.apply(input);

            // Create the user message
            UserMessage userMessage = createUserMessage(input, processedInput);

            // Get the AI response
            AiMessage aiMessage = getAiResponse(userMessage, context);

            // Process the output
            AgentOutput output = outputProcessor.apply(aiMessage);

            // Add context information to the output metadata
            Map<String, Object> metadata = new HashMap<>(output.getMetadata());
            metadata.put("agent_name", name);
            metadata.put("timestamp", System.currentTimeMillis());

            // Return the output with additional metadata
            return AgentOutput.builder(output.getContent())
                .successful(output.isSuccessful())
                .withAllMetadata(metadata)
                .withAllResults(output.getResults())
                .build();
        } catch (Exception e) {
            // Create an error output
            return AgentOutput.builder(
                "Error processing input: " + e.getMessage()
            )
                .successful(false)
                .withMetadata("error", e.getClass().getName())
                .withMetadata("agent_name", name)
                .build();
        }
    }

    @Override
    public CompletableFuture<AgentOutput> processAsync(
        AgentInput input,
        Map<String, Object> context
    ) {
        return CompletableFuture.supplyAsync(() -> process(input, context));
    }

    /**
     * Create a UserMessage from the input using templates if available.
     *
     * @param input The original input
     * @param processedContent The processed input content
     * @return A UserMessage representing the input
     */
    protected UserMessage createUserMessage(
        AgentInput input,
        String processedContent
    ) {
        String content = processedContent;

        // Apply the user prompt template if available
        if (userPromptTemplate != null && !userPromptTemplate.isEmpty()) {
            content = applyTemplate(userPromptTemplate, input);
        }

        return UserMessage.from(content);
    }

    /**
     * Get the AI response for the given UserMessage.
     *
     * @param userMessage The user message to process
     * @param context Additional context information
     * @return The AiMessage response
     */
    protected AiMessage getAiResponse(
        UserMessage userMessage,
        Map<String, Object> context
    ) {
        // Add the user message to memory if available
        if (memory != null) {
            memory.add(userMessage);
        }

        // Get all messages from memory or create a new message list with the system prompt
        List<dev.langchain4j.data.message.ChatMessage> messages =
            new ArrayList<>();

        if (memory != null) {
            memory.messages().forEach(messages::add);
        } else {
            if (
                systemPromptTemplate != null && !systemPromptTemplate.isEmpty()
            ) {
                messages.add(new SystemMessage(systemPromptTemplate));
            }
            messages.add(userMessage);
        }

        // Get the response from the model
        AiMessage aiMessage = model.chat(messages).aiMessage();

        // Add the AI message to memory if available
        if (memory != null) {
            memory.add(aiMessage);
        }

        return aiMessage;
    }

    /**
     * Apply a template string using the input's content and parameters.
     *
     * @param template The template string with {placeholders}
     * @param input The input containing values for the placeholders
     * @return The template with placeholders replaced by values
     */
    protected String applyTemplate(String template, AgentInput input) {
        String result = template;

        // Replace {content} with the input content
        result = result.replace("{content}", input.getContent());

        // Replace parameters from the input
        for (Map.Entry<String, Object> entry : input
            .getParameters()
            .entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(
                    placeholder,
                    String.valueOf(entry.getValue())
                );
            }
        }

        // Replace with default parameters if not already replaced
        for (Map.Entry<String, Object> entry : defaultParameters.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(
                    placeholder,
                    String.valueOf(entry.getValue())
                );
            }
        }

        return result;
    }

    /**
     * Builder for creating ComplexLangChain4JAgentNode instances.
     */
    public static class Builder {

        private String name;
        private ChatModel model;
        private ChatMemory memory;
        private String systemPromptTemplate;
        private String userPromptTemplate;
        private Function<AgentInput, String> inputProcessor;
        private Function<AiMessage, AgentOutput> outputProcessor;
        private final Map<String, Object> defaultParameters = new HashMap<>();

        /**
         * Set the name of the agent node.
         *
         * @param name The name of the agent node
         * @return This builder instance for method chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the ChatModel to use.
         *
         * @param model The ChatModel to use
         * @return This builder instance for method chaining
         */
        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        /**
         * Set the ChatMemory to use.
         *
         * @param memory The ChatMemory to use
         * @return This builder instance for method chaining
         */
        public Builder memory(ChatMemory memory) {
            this.memory = memory;
            return this;
        }

        /**
         * Set the system prompt template.
         *
         * @param systemPromptTemplate The system prompt template
         * @return This builder instance for method chaining
         */
        public Builder systemPromptTemplate(String systemPromptTemplate) {
            this.systemPromptTemplate = systemPromptTemplate;
            return this;
        }

        /**
         * Set the user prompt template.
         *
         * @param userPromptTemplate The user prompt template
         * @return This builder instance for method chaining
         */
        public Builder userPromptTemplate(String userPromptTemplate) {
            this.userPromptTemplate = userPromptTemplate;
            return this;
        }

        /**
         * Set the input processor function.
         *
         * @param inputProcessor The input processor function
         * @return This builder instance for method chaining
         */
        public Builder inputProcessor(
            Function<AgentInput, String> inputProcessor
        ) {
            this.inputProcessor = inputProcessor;
            return this;
        }

        /**
         * Set the output processor function.
         *
         * @param outputProcessor The output processor function
         * @return This builder instance for method chaining
         */
        public Builder outputProcessor(
            Function<AiMessage, AgentOutput> outputProcessor
        ) {
            this.outputProcessor = outputProcessor;
            return this;
        }

        /**
         * Add a default parameter.
         *
         * @param key The parameter key
         * @param value The parameter value
         * @return This builder instance for method chaining
         */
        public Builder defaultParameter(String key, Object value) {
            this.defaultParameters.put(key, value);
            return this;
        }

        /**
         * Add multiple default parameters.
         *
         * @param parameters Map of parameter keys and values
         * @return This builder instance for method chaining
         */
        public Builder defaultParameters(Map<String, Object> parameters) {
            if (parameters != null) {
                this.defaultParameters.putAll(parameters);
            }
            return this;
        }

        /**
         * Build the ComplexLangChain4JAgentNode instance.
         *
         * @return A new ComplexLangChain4JAgentNode instance
         */
        public ComplexLangChain4JAgentNode build() {
            return new ComplexLangChain4JAgentNode(
                name,
                model,
                memory,
                systemPromptTemplate,
                userPromptTemplate,
                inputProcessor,
                outputProcessor,
                defaultParameters
            );
        }
    }

    /**
     * Create a new Builder to construct a ComplexLangChain4JAgentNode.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
