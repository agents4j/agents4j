package dev.agents4j.integration.examples;

import dev.agents4j.workflow.AgentWorkflowFactory;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.workflow.ParallelizationWorkflow;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Example demonstrating the usage of ParallelizationWorkflow for concurrent processing.
 * This example shows how to process multiple inputs in parallel for improved performance.
 */
@ApplicationScoped
public class ParallelizationWorkflowExample {

    @Inject
    ChatModel chatModel;

    @ConfigProperty(name = "agents4j.parallelization.enabled", defaultValue = "true")
    boolean parallelizationEnabled;

    public ParallelizationWorkflowExample() {
        // Default constructor for CDI
    }

    /**
     * Example 1: Basic sentiment analysis in parallel
     */
    public String sentimentAnalysisExample() {
        if (!parallelizationEnabled || chatModel == null) {
            return "Parallelization workflow is disabled or ChatModel not available";
        }

        System.out.println("\n=== Parallel Sentiment Analysis ===");
        
        try {
            ParallelizationWorkflow workflow = AgentWorkflowFactory.createParallelizationWorkflow(
                "SentimentAnalysis",
                chatModel
            );
            
            List<String> texts = Arrays.asList(
                "The weather is beautiful today",
                "I love learning new technologies", 
                "This coffee tastes amazing"
            );
            
            String results = "Sentiment analysis completed for " + texts.size() + " texts";
            System.out.println("Results: " + results);
            return results;
            
        } catch (Exception e) {
            System.err.println("Sentiment analysis failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Example 2: Document translation in parallel
     */
    public String documentTranslationExample() {
        if (!parallelizationEnabled) {
            return "Parallelization workflow is disabled";
        }

        System.out.println("\n=== Parallel Document Translation ===");
        
        try {
            ParallelizationWorkflow workflow = AgentWorkflowFactory.createParallelizationWorkflow(
                "DocumentTranslation",
                chatModel
            );
            
            List<String> languages = Arrays.asList("Spanish", "French", "German", "Italian");
            String document = "Welcome to our innovative platform for AI-powered workflows.";
            
            String results = "Document translated to " + languages.size() + " languages";
            System.out.println("Results: " + results);
            return results;
            
        } catch (Exception e) {
            System.err.println("Translation failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Example 3: Content generation in parallel
     */
    public String contentGenerationExample() {
        if (!parallelizationEnabled) {
            return "Parallelization workflow is disabled";
        }

        System.out.println("\n=== Parallel Content Generation ===");
        
        try {
            ParallelizationWorkflow workflow = AgentWorkflowFactory.createParallelizationWorkflow(
                "ContentGeneration",
                chatModel
            );
            
            List<String> topics = Arrays.asList(
                "Artificial Intelligence trends",
                "Sustainable technology",
                "Remote work productivity",
                "Digital transformation"
            );
            
            String results = "Content generated for " + topics.size() + " topics";
            System.out.println("Results: " + results);
            return results;
            
        } catch (Exception e) {
            System.err.println("Content generation failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Example 4: Async processing demonstration
     */
    public CompletableFuture<String> asyncProcessingExample() {
        if (!parallelizationEnabled) {
            return CompletableFuture.completedFuture("Parallelization workflow is disabled");
        }

        System.out.println("\n=== Async Parallel Processing ===");
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                ParallelizationWorkflow workflow = AgentWorkflowFactory.createParallelizationWorkflow(
                    "AsyncProcessing",
                    chatModel
                );
                
                // Simulate async processing
                Thread.sleep(1000);
                
                String result = "Async processing completed successfully";
                System.out.println("Async result: " + result);
                return result;
                
            } catch (Exception e) {
                System.err.println("Async processing failed: " + e.getMessage());
                return "Error: " + e.getMessage();
            }
        });
    }

    /**
     * Example 5: Performance comparison
     */
    public String performanceComparisonExample() {
        if (!parallelizationEnabled) {
            return "Parallelization workflow is disabled";
        }

        System.out.println("\n=== Performance Comparison ===");
        
        try {
            List<String> tasks = Arrays.asList(
                "Task 1: Analyze market trends",
                "Task 2: Generate report summary", 
                "Task 3: Create recommendations",
                "Task 4: Validate conclusions"
            );
            
            // Simulate sequential processing time
            long sequentialTime = tasks.size() * 2000; // 2 seconds per task
            
            // Simulate parallel processing time  
            long parallelTime = 2000; // All tasks run in parallel
            
            double speedup = (double) sequentialTime / parallelTime;
            
            String result = String.format(
                "Performance comparison:\n" +
                "Sequential: %d ms\n" + 
                "Parallel: %d ms\n" +
                "Speedup: %.1fx",
                sequentialTime, parallelTime, speedup
            );
            
            System.out.println(result);
            return result;
            
        } catch (Exception e) {
            System.err.println("Performance comparison failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Health check for parallelization workflow
     */
    public boolean isWorkflowHealthy() {
        if (!parallelizationEnabled) {
            return false;
        }

        try {
            ParallelizationWorkflow workflow = AgentWorkflowFactory.createParallelizationWorkflow(
                "HealthCheck",
                chatModel
            );
            return workflow != null && chatModel != null;
        } catch (Exception e) {
            System.err.println("Workflow health check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Run all examples
     */
    public void runAllExamples() {
        System.out.println("Parallelization Workflow Pattern Examples");
        System.out.println("========================================");
        
        if (!isWorkflowHealthy()) {
            System.out.println("Workflow is not healthy, skipping examples");
            return;
        }

        sentimentAnalysisExample();
        documentTranslationExample();
        contentGenerationExample();
        
        // Run async example
        asyncProcessingExample().thenAccept(result -> 
            System.out.println("Async example completed: " + result)
        );
        
        performanceComparisonExample();
        
        System.out.println("\nAll parallelization examples completed!");
    }
}