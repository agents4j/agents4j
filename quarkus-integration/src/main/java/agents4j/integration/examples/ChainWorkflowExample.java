package agents4j.integration.examples;

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

import java.util.HashMap;
import java.util.Map;
import agents4j.integration.examples.util.AgentOutputToInputAdapter;

/**
 * Comprehensive examples demonstrating ChainWorkflow usage patterns.
 * Shows various ways to create and configure sequential agent processing workflows.
 */
public class ChainWorkflowExample {

    private static final Logger LOG = Logger.getLogger(ChainWorkflowExample.class);

    private final ChatModel chatModel;

    public ChainWorkflowExample(ChatModel chatModel) {
        this.chatModel = chatModel;
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

            // Create an adapter node that converts AgentOutput to AgentInput for the analysis node
            // This is necessary because ComplexLangChain4JAgentNode expects AgentInput but the previous node outputs AgentOutput
            AgentOutputToInputAdapter outputToInputAdapter = AgentOutputToInputAdapter.create("ResearchToAnalysisAdapter");

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

            // Create another adapter for the summary node
            // Each ComplexLangChain4JAgentNode in the chain needs an adapter before it (except the first one)
            AgentOutputToInputAdapter outputToInputAdapter2 = AgentOutputToInputAdapter.create("AnalysisToSummaryAdapter");

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

            // Build the workflow manually with the complex nodes and adapters
            // Pattern: ComplexNode -> Adapter -> ComplexNode -> Adapter -> ComplexNode
            // The adapters handle the AgentOutput -> AgentInput conversion between ComplexLangChain4JAgentNode instances
            @SuppressWarnings({ "unchecked", "rawtypes" })
            ChainWorkflow<AgentInput, AgentOutput> workflow = ChainWorkflow.<
                    AgentInput,
                    AgentOutput
                >builder()
                .name("AdvancedWorkflow")
                .firstNode(researchNode)                    // AgentInput -> AgentOutput
                .node((AgentNode) outputToInputAdapter)     // AgentOutput -> AgentInput
                .node((AgentNode) analysisNode)             // AgentInput -> AgentOutput
                .node((AgentNode) outputToInputAdapter2)    // AgentOutput -> AgentInput
                .node((AgentNode) summaryNode)              // AgentInput -> AgentOutput
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