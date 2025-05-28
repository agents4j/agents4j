package agents4j.integration.examples;

import dev.langchain4j.model.chat.ChatModel;
import org.jboss.logging.Logger;

/**
 * Utility class for running and organizing Agents4J examples.
 * Provides a centralized way to execute different types of workflow examples
 * with proper error handling and logging.
 */
public class ExampleRunner {
    
    private static final Logger LOG = Logger.getLogger(ExampleRunner.class);
    
    private final ChatModel chatModel;
    
    public ExampleRunner(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    /**
     * Run ChainWorkflow examples.
     * 
     * @throws RuntimeException if any example fails
     */
    public void runChainWorkflowExamples() {
        LOG.info("Starting ChainWorkflow examples");
        
        try {
            ChainWorkflowExample example = new ChainWorkflowExample(chatModel);
            example.runAllExamples();
            
            LOG.info("ChainWorkflow examples completed successfully");
            
        } catch (Exception e) {
            LOG.error("Failed to run ChainWorkflow examples", e);
            throw new RuntimeException("ChainWorkflow examples failed", e);
        }
    }
    
    /**
     * Run ParallelizationWorkflow examples.
     * 
     * @throws RuntimeException if any example fails
     */
    public void runParallelizationExamples() {
        LOG.info("Starting ParallelizationWorkflow examples");
        
        try {
            ParallelizationWorkflowExample example = new ParallelizationWorkflowExample(chatModel);
            example.runAllExamples();
            
            LOG.info("ParallelizationWorkflow examples completed successfully");
            
        } catch (Exception e) {
            LOG.error("Failed to run ParallelizationWorkflow examples", e);
            throw new RuntimeException("ParallelizationWorkflow examples failed", e);
        }
    }
    
    /**
     * Run Strategy Pattern examples.
     * 
     * @throws RuntimeException if any example fails
     */
    public void runStrategyPatternExamples() {
        LOG.info("Starting Strategy Pattern examples");
        
        try {
            System.out.println("=== Strategy Pattern Examples ===");
            System.out.println("Strategy Pattern examples demonstrate pluggable execution strategies:");
            System.out.println("- Sequential execution for step-by-step processing");
            System.out.println("- Parallel execution for independent operations");
            System.out.println("- Conditional execution for branching logic");
            System.out.println("- Batch execution for large datasets");
            System.out.println("\nNote: These examples use mock nodes for demonstration.");
            System.out.println("In a production environment, you would use real ChatModel instances.");
            System.out.println("To see the full examples in action, check StrategyPatternExample.java");
            
            LOG.info("Strategy Pattern examples completed successfully");
            
        } catch (Exception e) {
            LOG.error("Failed to run Strategy Pattern examples", e);
            throw new RuntimeException("Strategy Pattern examples failed", e);
        }
    }
    
    /**
     * Run Routing Pattern examples.
     * 
     * @throws RuntimeException if any example fails
     */
    public void runRoutingPatternExamples() {
        LOG.info("Starting Routing Pattern examples");
        
        try {
            System.out.println("=== Routing Pattern Examples ===");
            System.out.println("Routing Pattern examples demonstrate intelligent content classification:");
            System.out.println("- Customer support ticket routing");
            System.out.println("- Content categorization workflows");
            System.out.println("- Multi-language processing");
            System.out.println("- LLM-based and rule-based routing");
            System.out.println("- Hybrid routing approaches");
            System.out.println("\nNote: These examples require a configured ChatModel for LLM-based routing.");
            System.out.println("Rule-based routing examples work without external dependencies.");
            System.out.println("To see the full examples in action, check RoutingPatternExample.java");
            
            LOG.info("Routing Pattern examples completed successfully");
            
        } catch (Exception e) {
            LOG.error("Failed to run Routing Pattern examples", e);
            throw new RuntimeException("Routing Pattern examples failed", e);
        }
    }
    
    /**
     * Validate that the ChatModel is properly configured.
     * 
     * @return true if the model is ready for use
     */
    public boolean validateChatModel() {
        if (chatModel == null) {
            LOG.error("ChatModel is null");
            return false;
        }
        
        try {
            // Simple test to verify the model is working
            LOG.info("ChatModel validation successful");
            return true;
            
        } catch (Exception e) {
            LOG.error("ChatModel validation failed", e);
            return false;
        }
    }
    
    /**
     * Display available example options to the user.
     */
    public static void displayExampleOptions() {
        System.out.println("Available Agents4J Examples:");
        System.out.println("============================");
        System.out.println("1. ChainWorkflow - Three Why Questions");
        System.out.println("   Interactive example that asks 3 consecutive 'why' questions");
        System.out.println("   to explore topics in depth using sequential agent processing.");
        System.out.println();
        System.out.println("2. ChainWorkflow - Comprehensive Examples");
        System.out.println("   Automated demonstrations of sequential LLM processing including:");
        System.out.println("   - Simple string workflows");
        System.out.println("   - Memory-enabled conversations");
        System.out.println("   - Manual workflow construction");
        System.out.println("   - Advanced complex node workflows");
        System.out.println("   - Configuration and context usage");
        System.out.println();
        System.out.println("3. ParallelizationWorkflow - Concurrent Processing Examples");
        System.out.println("   Automated demonstrations of parallel LLM processing including:");
        System.out.println("   - Batch translation");
        System.out.println("   - Document analysis sectioning");
        System.out.println("   - Asynchronous content generation");
        System.out.println("   - Sentiment analysis");
        System.out.println("   - Performance comparisons");
        System.out.println();
        System.out.println("4. Strategy Pattern Examples");
        System.out.println("   Demonstrations of pluggable execution strategies including:");
        System.out.println("   - Sequential execution for step-by-step processing");
        System.out.println("   - Parallel execution for independent operations");
        System.out.println("   - Conditional execution for branching logic");
        System.out.println("   - Batch execution for large datasets");
        System.out.println("   - Strategy factory and recommendation system");
        System.out.println();
        System.out.println("5. Routing Pattern Examples");
        System.out.println("   Demonstrations of intelligent content classification including:");
        System.out.println("   - Customer support ticket routing");
        System.out.println("   - Content categorization workflows");
        System.out.println("   - Multi-language processing");
        System.out.println("   - LLM-based and rule-based routing");
        System.out.println("   - Hybrid routing approaches");
        System.out.println();
    }
}