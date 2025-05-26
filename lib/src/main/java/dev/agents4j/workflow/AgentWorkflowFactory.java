package dev.agents4j.workflow;

import dev.agents4j.api.AgentNode;
import dev.agents4j.impl.StringLangChain4JAgentNode;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating various types of agent workflows.
 * This factory provides convenient methods for constructing
 * common workflow patterns.
 */
public class AgentWorkflowFactory {

    /**
     * Creates a chain workflow with the given agent nodes.
     *
     * @param <I> The input type for the first node in the chain
     * @param <O> The output type of the last node in the chain
     * @param name The name of the workflow
     * @param firstNode The first node in the chain
     * @param restNodes The remaining nodes in the chain
     * @return A new ChainWorkflow instance
     */
    @SafeVarargs
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <I, O> ChainWorkflow<I, O> createChainWorkflow(
        String name,
        AgentNode<I, ?> firstNode,
        AgentNode<?, ?>... restNodes
    ) {
        // Start building the chain with the first node
        ChainWorkflow.Builder<I, ?> builder = ChainWorkflow.<I, Object>builder()
            .name(name)
            .firstNode(firstNode);

        // Use type erasure to add the remaining nodes
        for (AgentNode<?, ?> node : restNodes) {
            // This is safe because at runtime, the generic type information is erased
            builder = (ChainWorkflow.Builder<
                    I,
                    ?
                >) ((ChainWorkflow.Builder) builder).node(node);
        }

        // Cast the final result to the expected type
        return (ChainWorkflow<I, O>) builder.build();
    }

    /**
     * Creates a chain workflow of string-processing LangChain4J agents.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use for all agents
     * @param systemPrompts The system prompts for each agent in the chain
     * @return A new ChainWorkflow instance
     */
    public static ChainWorkflow<String, String> createStringChainWorkflow(
        String name,
        ChatModel model,
        String... systemPrompts
    ) {
        if (systemPrompts.length == 0) {
            throw new IllegalArgumentException(
                "At least one system prompt is required"
            );
        }

        List<AgentNode<String, String>> nodes = new ArrayList<>();

        for (int i = 0; i < systemPrompts.length; i++) {
            String nodeName = name + "-Node" + (i + 1);
            StringLangChain4JAgentNode node =
                StringLangChain4JAgentNode.builder()
                    .name(nodeName)
                    .model(model)
                    .systemPrompt(systemPrompts[i])
                    .build();
            nodes.add(node);
        }

        return createChainWorkflow(
            name,
            nodes.get(0),
            nodes.subList(1, nodes.size()).toArray(new AgentNode[0])
        );
    }

    /**
     * Creates a chain workflow of string-processing LangChain4J agents with memory.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use for all agents
     * @param memory The ChatMemory to use for all agents
     * @param systemPrompts The system prompts for each agent in the chain
     * @return A new ChainWorkflow instance
     */
    public static ChainWorkflow<
        String,
        String
    > createStringChainWorkflowWithMemory(
        String name,
        ChatModel model,
        ChatMemory memory,
        String... systemPrompts
    ) {
        if (systemPrompts.length == 0) {
            throw new IllegalArgumentException(
                "At least one system prompt is required"
            );
        }

        List<AgentNode<String, String>> nodes = new ArrayList<>();

        for (int i = 0; i < systemPrompts.length; i++) {
            String nodeName = name + "-Node" + (i + 1);
            StringLangChain4JAgentNode node =
                StringLangChain4JAgentNode.builder()
                    .name(nodeName)
                    .model(model)
                    .memory(memory)
                    .systemPrompt(systemPrompts[i])
                    .build();
            nodes.add(node);
        }

        return createChainWorkflow(
            name,
            nodes.get(0),
            nodes.subList(1, nodes.size()).toArray(new AgentNode[0])
        );
    }
}
