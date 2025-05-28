package dev.agents4j.examples;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.impl.ComplexLangChain4JAgentNode;
import dev.agents4j.impl.StringLangChain4JAgentNode;
import dev.agents4j.model.AgentInput;
import dev.agents4j.model.AgentOutput;
import dev.agents4j.workflow.AgentWorkflowFactory;
import dev.agents4j.workflow.ChainWorkflow;
import dev.agents4j.workflow.WorkflowConfiguration;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import java.util.HashMap;
import java.util.Map;

/**
 * Example usage of the Chain Workflow.
 * This class demonstrates various ways to create and use agent workflows.
 */
public class ChainWorkflowExample {

    /**
     * A simple example using string-based agents.
     *
     * @param model The ChatModel to use
     * @return The result of executing the workflow
     */
    public static String simpleStringWorkflowExample(ChatModel model) throws WorkflowExecutionException {
        // Create a simple chain workflow with two agent nodes
        ChainWorkflow<String, String> workflow =
            AgentWorkflowFactory.createStringChainWorkflow(
                "SimpleWorkflow",
                model,
                "You are a helpful research assistant. Your job is to find relevant information about the topic.",
                "You are a professional summarizer. Your job is to create a concise summary of the information provided."
            );

        // Execute the workflow
        return workflow.execute("Tell me about artificial intelligence");
    }

    /**
     * An example using string-based agents with memory.
     *
     * @param model The ChatModel to use
     * @return The result of executing the workflow twice
     */
    public static String memoryWorkflowExample(ChatModel model) throws WorkflowExecutionException {
        // Create a memory for conversation history
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
            .maxMessages(10)
            .build();

        // Create a workflow with memory
        ChainWorkflow<String, String> workflow =
            AgentWorkflowFactory.createStringChainWorkflowWithMemory(
                "MemoryWorkflow",
                model,
                memory,
                "You are a helpful conversational assistant that remembers previous interactions."
            );

        // Execute the workflow twice to demonstrate memory
        workflow.execute("My name is John Doe");
        return workflow.execute("What's my name?");
    }

    /**
     * An example using manually built agent nodes.
     *
     * @param model The ChatModel to use
     * @return The result of executing the workflow
     */
    public static String manualChainWorkflowExample(ChatModel model) throws WorkflowExecutionException {
        // Create the first agent node
        StringLangChain4JAgentNode firstNode =
            StringLangChain4JAgentNode.builder()
                .name("ResearchNode")
                .model(model)
                .systemPrompt(
                    "You are a research assistant. Find information about the topic."
                )
                .build();

        // Create the second agent node
        StringLangChain4JAgentNode secondNode =
            StringLangChain4JAgentNode.builder()
                .name("SummaryNode")
                .model(model)
                .systemPrompt("You are a summarizer. Create a concise summary.")
                .build();

        // Build the workflow manually
        ChainWorkflow<String, String> workflow = ChainWorkflow.<
                String,
                String
            >builder()
            .name("ManualWorkflow")
            .firstNode(firstNode)
            .node(secondNode)
            .build();

        // Execute the workflow
        return workflow.execute("Explain quantum computing");
    }

    /**
     * An advanced example using complex agent nodes with custom input/output.
     *
     * @param model The ChatModel to use
     * @return The result of executing the workflow
     */
    public static AgentOutput advancedChainWorkflowExample(ChatModel model) throws WorkflowExecutionException {
        // Create a complex agent node for research
        ComplexLangChain4JAgentNode researchNode =
            ComplexLangChain4JAgentNode.builder()
                .name("AdvancedResearchNode")
                .model(model)
                .systemPromptTemplate(
                    "You are a research assistant specialized in {topic}. Find information about the user's query."
                )
                .userPromptTemplate("Research query: {content}")
                .defaultParameter("topic", "technology")
                .build();

        // Create a complex agent node for analysis
        ComplexLangChain4JAgentNode analysisNode =
            ComplexLangChain4JAgentNode.builder()
                .name("AnalysisNode")
                .model(model)
                .systemPromptTemplate(
                    "You are an analyst specialized in interpreting research findings."
                )
                .userPromptTemplate("Analyze the following research: {content}")
                .outputProcessor(aiMessage -> {
                    // Process the AI message to extract structured data
                    String content = aiMessage.text();
                    return AgentOutput.builder(content)
                        .withResult("word_count", content.split("\\s+").length)
                        .withResult(
                            "contains_references",
                            content.contains("Reference") ||
                            content.contains("reference")
                        )
                        .build();
                })
                .build();

        // Create a complex agent node for summarizing
        ComplexLangChain4JAgentNode summaryNode =
            ComplexLangChain4JAgentNode.builder()
                .name("SummaryNode")
                .model(model)
                .systemPromptTemplate(
                    "You are a professional summarizer. Create a concise summary that highlights key points."
                )
                .userPromptTemplate(
                    "Summarize the following analysis: {content}"
                )
                .build();

        // Build the workflow manually with the complex nodes
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ChainWorkflow<AgentInput, AgentOutput> workflow = ChainWorkflow.<
                AgentInput,
                AgentOutput
            >builder()
            .name("AdvancedWorkflow")
            .firstNode(researchNode)
            .node((AgentNode) analysisNode)
            .node((AgentNode) summaryNode)
            .build();

        // Create an input with metadata and parameters
        AgentInput input = AgentInput.builder(
            "Explain the impact of artificial intelligence on healthcare"
        )
            .withMetadata("source", "user_query")
            .withParameter("max_length", 500)
            .build();

        // Create a context map for the execution
        Map<String, Object> context = new HashMap<>();
        context.put("execution_id", "12345");
        context.put("timestamp", System.currentTimeMillis());

        // Execute the workflow with context
        return workflow.execute(input, context);
    }

    /**
     * Example using workflow configuration.
     *
     * @param model The ChatModel to use
     * @return The result of executing the workflow
     */
    public static String configuredWorkflowExample(ChatModel model) throws WorkflowExecutionException {
        // Create a simple chain workflow
        ChainWorkflow<String, String> workflow =
            AgentWorkflowFactory.createStringChainWorkflow(
                "ConfiguredWorkflow",
                model,
                "You are a helpful assistant.",
                "Please elaborate on the response."
            );

        // Create an execution context with additional metadata
        Map<String, Object> context = new HashMap<>();
        context.put("max_tokens", 500);
        context.put("temperature", 0.7);
        context.put("execution_id", System.currentTimeMillis());

        // Execute the workflow with the context
        return workflow.execute("Tell me a short story", context);
    }
}
