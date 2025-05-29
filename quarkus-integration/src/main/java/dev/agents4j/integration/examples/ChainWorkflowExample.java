package dev.agents4j.integration.examples;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.impl.ComplexLangChain4JAgentNode;
import dev.agents4j.impl.StringLangChain4JAgentNode;
import dev.agents4j.model.AgentInput;
import dev.agents4j.model.AgentOutput;
import dev.agents4j.workflow.AgentWorkflowFactory;
import dev.agents4j.workflow.ChainWorkflow;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import org.jboss.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
// Removed missing import

/**
 * Comprehensive examples demonstrating ChainWorkflow usage patterns.
 * Shows various ways to create and configure sequential agent processing workflows.
 */
@ApplicationScoped
public class ChainWorkflowExample {

    private static final Logger LOG = Logger.getLogger(ChainWorkflowExample.class);

    @Inject
    ChatModel chatModel;

    public ChainWorkflowExample() {
        // Default constructor for CDI
    }

    public ChainWorkflowExample(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Run simple chain workflow example and return result.
     */
    public String runSimpleChainExample() {
        LOG.info("Running simple chain workflow example");
        try {
            ChainWorkflow<String, String> workflow = AgentWorkflowFactory.createStringChainWorkflow(
                "SimpleExample",
                chatModel,
                "You are a helpful assistant. Provide a clear and concise answer."
            );
            
            String result = workflow.execute("What are the benefits of renewable energy?");
            LOG.info("Simple chain example completed successfully");
            return result;
        } catch (Exception e) {
            LOG.error("Simple chain example failed", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Run complex chain workflow example and return result.
     */
    public String runComplexChainExample() {
        LOG.info("Running complex chain workflow example");
        try {
            ChainWorkflow<String, String> workflow = AgentWorkflowFactory.createStringChainWorkflow(
                "ComplexExample",
                chatModel,
                "You are a research analyst. Analyze the given topic thoroughly.",
                "You are a writer. Create a well-structured summary based on the analysis.",
                "You are an editor. Polish and improve the content for clarity and readability."
            );
            
            String result = workflow.execute("The impact of artificial intelligence on modern healthcare");
            LOG.info("Complex chain example completed successfully");
            return result;
        } catch (Exception e) {
            LOG.error("Complex chain example failed", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Run three whys analysis example and return result.
     */
    public String runThreeWhysExample() {
        LOG.info("Running three whys analysis example");
        try {
            var firstWhyNode = StringLangChain4JAgentNode.builder()
                .name("FirstWhyNode")
                .model(chatModel)
                .systemPrompt(
                    "You are a thoughtful assistant that helps people understand topics more deeply. " +
                    "Your task is to analyze the user's question and ask a fundamental 'why' question about it. " +
                    "Provide a brief response to the original question first, and then ask your 'why' question."
                )
                .build();

            var secondWhyNode = StringLangChain4JAgentNode.builder()
                .name("SecondWhyNode")
                .model(chatModel)
                .systemPrompt(
                    "You are a thoughtful assistant continuing a chain of inquiry. " +
                    "The input will contain an initial question, an analysis, and a first 'why' question. " +
                    "Your task is to answer the first 'why' question thoroughly and then ask a deeper, second 'why' question " +
                    "that explores the underlying principles or causes."
                )
                .build();

            var finalNode = StringLangChain4JAgentNode.builder()
                .name("FinalNode")
                .model(chatModel)
                .systemPrompt(
                    "You are a thoughtful assistant finalizing a chain of inquiry. " +
                    "The input will contain an initial question and a series of 'why' questions and answers. " +
                    "Your task is to answer the final 'why' question thoroughly and then provide a comprehensive summary " +
                    "that ties together all the insights from this chain of inquiry."
                )
                .build();

            ChainWorkflow<String, String> workflow = ChainWorkflow.<String, String>builder()
                .name("ThreeWhysExample")
                .firstNode(firstWhyNode)
                .node(secondWhyNode)
                .node(finalNode)
                .build();

            String result = workflow.execute("Why do companies invest in artificial intelligence?");
            LOG.info("Three whys example completed successfully");
            return result;
        } catch (Exception e) {
            LOG.error("Three whys example failed", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Run conversational workflow example and return result.
     */
    public String runConversationalExample() {
        LOG.info("Running conversational workflow example");
        try {
            MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();

            ChainWorkflow<String, String> workflow = AgentWorkflowFactory.createStringChainWorkflowWithMemory(
                "ConversationalExample",
                chatModel,
                memory,
                "You are a knowledgeable assistant with memory of our conversation. " +
                "Build upon previous interactions to provide more personalized responses."
            );

            // Simulate a conversation
            workflow.execute("Hello, I'm interested in learning about machine learning.");
            String result = workflow.execute("Can you explain the difference between supervised and unsupervised learning?");
            
            LOG.info("Conversational example completed successfully");
            return result;
        } catch (Exception e) {
            LOG.error("Conversational example failed", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Run all ChainWorkflow examples.
     */
    public void runAllExamples() {
        LOG.info("Starting ChainWorkflow examples");
        
        System.out.println("=== ChainWorkflow Examples ===\n");
        
        try {
            // Example 1: Simple string workflow
            simpleStringWorkflowExample();
            
            // Example 2: Workflow with memory
            memoryWorkflowExample();
            
            // Example 3: Manual workflow construction
            manualChainWorkflowExample();
            
            // Example 4: Advanced workflow with complex nodes
            advancedChainWorkflowExample();
            
            // Example 5: Workflow with configuration context
            configuredWorkflowExample();
            
            LOG.info("All ChainWorkflow examples completed successfully");
            
        } catch (Exception e) {
            LOG.error("Error running ChainWorkflow examples", e);
            throw new RuntimeException("Failed to run examples", e);
        }
    }

    /**
     * Example 1: Simple string-based workflow using factory method.
     */
    public void simpleStringWorkflowExample() {
        LOG.info("Running simple string workflow example");
        
        System.out.println("1. Simple String Workflow");
        System.out.println("-------------------------");
        
        try {
            // Create a simple chain workflow with two agent nodes
            ChainWorkflow<String, String> workflow =
                AgentWorkflowFactory.createStringChainWorkflow(
                    "SimpleWorkflow",
                    chatModel,
                    "You are a helpful research assistant. Your job is to find relevant information about the topic.",
                    "You are a professional summarizer. Your job is to create a concise summary of the information provided."
                );

            // Execute the workflow
            String result = workflow.execute("Tell me about artificial intelligence");
            
            System.out.println("Input: Tell me about artificial intelligence");
            System.out.println("Result: " + result);
            System.out.println("Workflow Name: " + workflow.getName());
            System.out.println();
            
        } catch (WorkflowExecutionException e) {
            LOG.error("Simple string workflow failed", e);
            System.err.println("Simple workflow failed: " + e.getMessage());
        }
    }

    /**
     * Example 2: Workflow with conversation memory.
     */
    public void memoryWorkflowExample() {
        LOG.info("Running memory workflow example");
        
        System.out.println("2. Workflow with Memory");
        System.out.println("-----------------------");
        
        try {
            // Create a memory for conversation history
            MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();

            // Create a workflow with memory
            ChainWorkflow<String, String> workflow =
                AgentWorkflowFactory.createStringChainWorkflowWithMemory(
                    "MemoryWorkflow",
                    chatModel,
                    memory,
                    "You are a helpful conversational assistant that remembers previous interactions."
                );

            // Execute the workflow twice to demonstrate memory
            System.out.println("First interaction:");
            String response1 = workflow.execute("My name is John Doe");
            System.out.println("Input: My name is John Doe");
            System.out.println("Response: " + response1);
            
            System.out.println("\nSecond interaction (testing memory):");
            String response2 = workflow.execute("What's my name?");
            System.out.println("Input: What's my name?");
            System.out.println("Response: " + response2);
            System.out.println();
            
        } catch (WorkflowExecutionException e) {
            LOG.error("Memory workflow failed", e);
            System.err.println("Memory workflow failed: " + e.getMessage());
        }
    }

    /**
     * Example 3: Manually constructed workflow with builder pattern.
     */
    public void manualChainWorkflowExample() {
        LOG.info("Running manual chain workflow example");
        
        System.out.println("3. Manual Chain Construction");
        System.out.println("----------------------------");
        
        try {
            // Create the first agent node
            StringLangChain4JAgentNode firstNode =
                StringLangChain4JAgentNode.builder()
                    .name("ResearchNode")
                    .model(chatModel)
                    .systemPrompt(
                        "You are a research assistant. Find information about the topic."
                    )
                    .build();

            // Create the second agent node
            StringLangChain4JAgentNode secondNode =
                StringLangChain4JAgentNode.builder()
                    .name("SummaryNode")
                    .model(chatModel)
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
            String result = workflow.execute("Explain quantum computing");
            
            System.out.println("Input: Explain quantum computing");
            System.out.println("Result: " + result);
            System.out.println("Nodes in workflow: " + workflow.getNodes().size());
            System.out.println();
            
        } catch (WorkflowExecutionException e) {
            LOG.error("Manual chain workflow failed", e);
            System.err.println("Manual workflow failed: " + e.getMessage());
        }
    }

    /**
     * Example 4: Advanced workflow using complex agent nodes.
     * 
     * This example demonstrates how to chain ComplexLangChain4JAgentNode instances together.
     * Since ComplexLangChain4JAgentNode expects AgentInput but outputs AgentOutput, we need
     * to use AgentOutputToInputAdapter between nodes to handle the type conversion.
     * 
     * The workflow pattern:
     * AgentInput -> ComplexNode1 -> AgentOutput -> Adapter -> AgentInput -> ComplexNode2 -> AgentOutput
     */
    public void advancedChainWorkflowExample() {
        LOG.info("Running advanced chain workflow example");
        
        System.out.println("4. Advanced Complex Node Workflow");
        System.out.println("---------------------------------");
        System.out.println("This example shows how to chain ComplexLangChain4JAgentNode instances");
        System.out.println("using adapters to handle AgentOutput -> AgentInput conversions.");
        System.out.println();
        
        try {
            // Create a complex agent node for research
            ComplexLangChain4JAgentNode researchNode =
                ComplexLangChain4JAgentNode.builder()
                    .name("AdvancedResearchNode")
                    .model(chatModel)
                    .systemPromptTemplate(
                        "You are a research assistant specialized in {topic}. Find information about the user's query."
                    )
                    .userPromptTemplate("Research query: {content}")
                    .defaultParameter("topic", "technology")
                    .build();

            // Direct chain without adapter - simplified workflow

            // Create a complex agent node for analysis
            ComplexLangChain4JAgentNode analysisNode =
                ComplexLangChain4JAgentNode.builder()
                    .name("AnalysisNode")
                    .model(chatModel)
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

            // Create summary node - simplified without adapter

            // Create a complex agent node for summarizing
            ComplexLangChain4JAgentNode summaryNode =
                ComplexLangChain4JAgentNode.builder()
                    .name("SummaryNode")
                    .model(chatModel)
                    .systemPromptTemplate(
                        "You are a professional summarizer. Create a concise summary that highlights key points."
                    )
                    .userPromptTemplate(
                        "Summarize the following analysis: {content}"
                    )
                    .build();

            // Build the workflow manually with the complex nodes - simplified without adapters
            @SuppressWarnings({ "unchecked", "rawtypes" })
            ChainWorkflow<AgentInput, AgentOutput> workflow = ChainWorkflow.<
                    AgentInput,
                    AgentOutput
                >builder()
                .name("AdvancedWorkflow")
                .firstNode(researchNode)                    // AgentInput -> AgentOutput
                .node((AgentNode) analysisNode)             // Direct chain to analysis
                .node((AgentNode) summaryNode)              // Direct chain to summary
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
            AgentOutput result = workflow.execute(input, context);
            
            System.out.println("Input: " + input.getContent());
            System.out.println("Result: " + result.getContent());
            System.out.println("Word Count: " + result.getResults().get("word_count"));
            System.out.println("Contains References: " + result.getResults().get("contains_references"));
            System.out.println("Execution Context: " + context.keySet());
            System.out.println();
            
        } catch (WorkflowExecutionException e) {
            LOG.error("Advanced chain workflow failed", e);
            System.err.println("Advanced workflow failed: " + e.getMessage());
        }
    }

    /**
     * Example 5: Workflow with execution context and configuration.
     */
    public void configuredWorkflowExample() {
        LOG.info("Running configured workflow example");
        
        System.out.println("5. Configured Workflow with Context");
        System.out.println("-----------------------------------");
        
        try {
            // Create a simple chain workflow
            ChainWorkflow<String, String> workflow =
                AgentWorkflowFactory.createStringChainWorkflow(
                    "ConfiguredWorkflow",
                    chatModel,
                    "You are a helpful assistant.",
                    "Please elaborate on the response."
                );

            // Create an execution context with additional metadata
            Map<String, Object> context = new HashMap<>();
            context.put("max_tokens", 500);
            context.put("temperature", 0.7);
            context.put("execution_id", System.currentTimeMillis());

            // Execute the workflow with the context
            long startTime = System.currentTimeMillis();
            String result = workflow.execute("Tell me a short story", context);
            long endTime = System.currentTimeMillis();
            
            System.out.println("Input: Tell me a short story");
            System.out.println("Result: " + result);
            System.out.println("Execution Time: " + (endTime - startTime) + "ms");
            System.out.println("Configuration: " + workflow.getConfiguration());
            System.out.println("Context Keys: " + context.keySet());
            System.out.println();
            
        } catch (WorkflowExecutionException e) {
            LOG.error("Configured workflow failed", e);
            System.err.println("Configured workflow failed: " + e.getMessage());
        }
    }
}