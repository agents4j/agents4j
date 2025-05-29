package dev.agents4j.integration.examples;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;

import dev.agents4j.workflow.StrategyWorkflow;
import dev.agents4j.workflow.strategy.ConditionalExecutionStrategy;
import dev.agents4j.workflow.strategy.StrategyFactory;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Example demonstrating the Strategy Pattern implementation for workflow execution.
 * 
 * This example shows how different execution strategies can be applied to the same
 * set of nodes to achieve different behaviors:
 * - Sequential execution for step-by-step processing
 * - Parallel execution for independent operations
 * - Conditional execution for branching logic
 * - Batch execution for large datasets
 */
@ApplicationScoped
public class StrategyPatternExample {

    @Inject
    ChatModel chatModel;

    public StrategyPatternExample() {
        // Default constructor for CDI
    }


    
    public static void main(String[] args) {
        try {
            // Create a mock ChatModel (replace with real model in production)
            ChatModel model = createMockChatModel();
            
            System.out.println("=== Strategy Pattern Demonstration ===\n");
            
            // Create some example nodes
            AgentNode<String, String> analyzerNode = createAnalyzerNode(model);
            AgentNode<String, String> summarizerNode = createSummarizerNode(model);
            AgentNode<String, String> validatorNode = createValidatorNode(model);
            
            String input = "This is a sample text that needs to be processed through multiple agents.";
            
            // Demonstrate different strategies
            demonstrateSequentialStrategy(input, analyzerNode, summarizerNode, validatorNode);
            demonstrateParallelStrategy(input, analyzerNode, summarizerNode, validatorNode);
            demonstrateConditionalStrategy(input, analyzerNode, summarizerNode, validatorNode);
            demonstrateBatchStrategy(model);
            demonstrateStrategyFactory();
            
        } catch (Exception e) {
            System.err.println("Error running example: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Demonstrate sequential execution strategy.
     */
    @SafeVarargs
    private static void demonstrateSequentialStrategy(String input, AgentNode<String, String>... nodes)
            throws WorkflowExecutionException {
        System.out.println("--- Sequential Strategy ---");
        
        StrategyWorkflow<String, String> workflow = StrategyWorkflow.<String, String>builder()
            .name("SequentialWorkflow")
            .strategy(StrategyFactory.sequential())
            .addNodes(List.of(nodes))
            .defaultContext("storeIntermediateResults", true)
            .build();
        
        Map<String, Object> context = new HashMap<>();
        String result = workflow.execute(input, context);
        
        System.out.println("Input: " + input);
        System.out.println("Final Result: " + result);
        System.out.println("Strategy Used: " + context.get("strategy_used"));
        System.out.println("Total Steps: " + context.get("total_steps"));
        System.out.println("Execution Time: " + context.get("total_execution_time_ms") + "ms");
        System.out.println();
    }

    /**
     * Demonstrate parallel execution strategy.
     */
    @SafeVarargs
    private static void demonstrateParallelStrategy(String input, AgentNode<String, String>... nodes)
            throws WorkflowExecutionException {
        System.out.println("--- Parallel Strategy ---");
        
        StrategyWorkflow<String, List<String>> workflow = StrategyWorkflow.<String, List<String>>builder()
            .name("ParallelWorkflow")
            .strategy(StrategyFactory.parallel())
            .addNodes(List.of(nodes))
            .defaultContext("maxConcurrency", 3)
            .defaultContext("aggregationStrategy", "list")
            .build();
        
        Map<String, Object> context = new HashMap<>();
        List<String> results = workflow.execute(input, context);
        
        System.out.println("Input: " + input);
        System.out.println("All Results: " + results);
        System.out.println("Strategy Used: " + context.get("strategy_used"));
        System.out.println("Successful Nodes: " + context.get("successful_nodes"));
        System.out.println("Execution Time: " + context.get("execution_time_ms") + "ms");
        System.out.println();
    }

    /**
     * Demonstrate conditional execution strategy.
     */
    @SafeVarargs
    private static void demonstrateConditionalStrategy(String input, AgentNode<String, String>... nodes)
            throws WorkflowExecutionException {
        System.out.println("--- Conditional Strategy ---");
        
        // Set up conditions for each node
        Map<String, BiPredicate<Object, Map<String, Object>>> nodeConditions = new HashMap<>();
        
        // Always execute analyzer
        nodeConditions.put("TextAnalyzer", (inp, ctx) -> true);
        
        // Execute summarizer only if input is long
        nodeConditions.put("TextSummarizer", (inp, ctx) -> {
            String text = inp.toString();
            return text.length() > 50;
        });
        
        // Execute validator only if we have intermediate results
        nodeConditions.put("TextValidator", (inp, ctx) -> ctx.containsKey("result_TextAnalyzer"));
        
        Map<String, Object> conditionalConfig = ConditionalExecutionStrategy.<String, String>builder()
            .addCondition("TextAnalyzer", nodeConditions.get("TextAnalyzer"))
            .addCondition("TextSummarizer", nodeConditions.get("TextSummarizer"))
            .addCondition("TextValidator", nodeConditions.get("TextValidator"))
            .shortCircuit(false)
            .requireAtLeastOne(true)
            .buildContext();
        
        StrategyWorkflow<String, String> workflow = StrategyWorkflow.<String, String>builder()
            .name("ConditionalWorkflow")
            .strategy(StrategyFactory.conditional())
            .addNodes(List.of(nodes))
            .defaultContext(conditionalConfig)
            .build();
        
        Map<String, Object> context = new HashMap<>();
        String result = workflow.execute(input, context);
        
        System.out.println("Input: " + input);
        System.out.println("Final Result: " + result);
        System.out.println("Strategy Used: " + context.get("strategy_used"));
        System.out.println("Executed Nodes: " + context.get("executed_nodes"));
        System.out.println("Skipped Nodes: " + context.get("skipped_nodes"));
        System.out.println();
    }

    /**
     * Demonstrate batch execution strategy.
     */
    private static void demonstrateBatchStrategy(ChatModel model) throws WorkflowExecutionException {
        System.out.println("--- Batch Strategy ---");
        
        // Create a simple processing node
        AgentNode<List<String>, List<String>> batchProcessor = new AgentNode<List<String>, List<String>>() {
            @Override
            public String getName() {
                return "BatchProcessor";
            }
            
            @Override
            public List<String> process(List<String> input, Map<String, Object> context) {
                return input.stream()
                    .map(item -> "Processed: " + item)
                    .toList();
            }
        };
        
        StrategyWorkflow<List<String>, List<Object>> workflow = StrategyWorkflow.<List<String>, List<Object>>builder()
            .name("BatchWorkflow")
            .strategy(StrategyFactory.batch())
            .addNode(batchProcessor)
            .defaultContext("batchSize", 2)
            .defaultContext("parallelBatches", false)
            .build();
        
        List<String> inputList = List.of("item1", "item2", "item3", "item4", "item5");
        Map<String, Object> context = new HashMap<>();
        
        List<Object> results = workflow.execute(inputList, context);
        
        System.out.println("Input List: " + inputList);
        System.out.println("Batch Results: " + results);
        System.out.println("Strategy Used: " + context.get("strategy_used"));
        System.out.println("Total Batches: " + context.get("total_batches"));
        System.out.println("Processed Batches: " + context.get("processed_batches"));
        System.out.println();
    }

    /**
     * Demonstrate StrategyFactory capabilities.
     */
    private static void demonstrateStrategyFactory() {
        System.out.println("--- Strategy Factory ---");
        
        System.out.println("Available Strategies: " + StrategyFactory.getAvailableStrategies());
        
        // Get strategy configurations
        for (String strategyName : StrategyFactory.getAvailableStrategies()) {
            Map<String, Object> config = StrategyFactory.getStrategyConfiguration(strategyName);
            System.out.println(strategyName + " configuration: " + config);
        }
        
        // Test strategy recommendation
        Map<String, Object> characteristics = new HashMap<>();
        characteristics.put("nodeCount", 3);
        characteristics.put("independentNodes", true);
        
        String recommended = StrategyFactory.getRecommendedStrategy(characteristics);
        System.out.println("Recommended strategy for independent nodes: " + recommended);
        
        characteristics.put("requiresBranching", true);
        recommended = StrategyFactory.getRecommendedStrategy(characteristics);
        System.out.println("Recommended strategy for branching: " + recommended);
        
        characteristics.clear();
        characteristics.put("dataSize", 2000);
        recommended = StrategyFactory.getRecommendedStrategy(characteristics);
        System.out.println("Recommended strategy for large dataset: " + recommended);
        
        System.out.println();
    }

    /**
     * Create a text analyzer node.
     */
    private static AgentNode<String, String> createAnalyzerNode(ChatModel model) {
        return new AgentNode<String, String>() {
            @Override
            public String getName() {
                return "TextAnalyzer";
            }
            
            @Override
            public String process(String input, Map<String, Object> context) {
                // Simulate text analysis
                return "ANALYZED: " + input + " [Length: " + input.length() + " characters]";
            }
        };
    }

    /**
     * Create a text summarizer node.
     */
    private static AgentNode<String, String> createSummarizerNode(ChatModel model) {
        return new AgentNode<String, String>() {
            @Override
            public String getName() {
                return "TextSummarizer";
            }
            
            @Override
            public String process(String input, Map<String, Object> context) {
                // Simulate text summarization
                String summary = input.length() > 50 ? 
                    input.substring(0, Math.min(30, input.length())) + "..." :
                    input;
                return "SUMMARY: " + summary;
            }
        };
    }

    /**
     * Create a text validator node.
     */
    private static AgentNode<String, String> createValidatorNode(ChatModel model) {
        return new AgentNode<String, String>() {
            @Override
            public String getName() {
                return "TextValidator";
            }
            
            @Override
            public String process(String input, Map<String, Object> context) {
                // Simulate validation
                boolean isValid = input != null && !input.trim().isEmpty();
                return "VALIDATED: " + input + " [Status: " + (isValid ? "VALID" : "INVALID") + "]";
            }
        };
    }

    /**
     * Create a mock ChatModel for demonstration purposes.
     */
    private static ChatModel createMockChatModel() {
        // In a real implementation, you would create an actual ChatModel
        // For this example, we'll return null since our demo nodes don't use it
        return null;
    }
}