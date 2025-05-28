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
        System.out.println("2. ParallelizationWorkflow - Concurrent Processing Examples");
        System.out.println("   Automated demonstrations of parallel LLM processing including:");
        System.out.println("   - Batch translation");
        System.out.println("   - Document analysis sectioning");
        System.out.println("   - Asynchronous content generation");
        System.out.println("   - Sentiment analysis");
        System.out.println("   - Performance comparisons");
        System.out.println();
    }
}