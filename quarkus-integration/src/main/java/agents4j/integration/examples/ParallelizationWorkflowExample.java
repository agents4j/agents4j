package agents4j.integration.examples;

import dev.agents4j.Agents4J;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.workflow.ParallelizationWorkflow;
import dev.langchain4j.model.chat.ChatModel;
import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Example demonstrating the usage of ParallelizationWorkflow for concurrent LLM processing.
 * 
 * This example shows various use cases including:
 * - Batch translation
 * - Document analysis
 * - Content generation
 * - Asynchronous processing
 */
public class ParallelizationWorkflowExample {

    private static final Logger LOG = Logger.getLogger(ParallelizationWorkflowExample.class);

    private final ChatModel chatModel;

    public ParallelizationWorkflowExample(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Run all ParallelizationWorkflow examples
     */
    public void runAllExamples() {
        LOG.info("Starting ParallelizationWorkflow examples");
        
        System.out.println("=== ParallelizationWorkflow Examples ===\n");
        
        try {
            // Example 1: Simple parallel query
            simpleParallelQuery();
            
            // Example 2: Batch translation
            batchTranslation();
            
            // Example 3: Document analysis
            documentAnalysis();
            
            // Example 4: Asynchronous processing
            asynchronousProcessing();
            
            // Example 5: Custom workflow
            customWorkflow();
            
            LOG.info("All ParallelizationWorkflow examples completed successfully");
            
        } catch (Exception e) {
            LOG.error("Error running ParallelizationWorkflow examples", e);
            throw new RuntimeException("Failed to run examples", e);
        }
    }

    /**
     * Example 1: Simple parallel query using convenience method
     */
    public void simpleParallelQuery() {
        LOG.info("Running simple parallel query example");
        
        System.out.println("1. Simple Parallel Query");
        System.out.println("------------------------");
        
        List<String> inputs = Arrays.asList(
            "The weather is beautiful today",
            "I love learning new technologies",
            "This coffee tastes amazing"
        );
        
        String prompt = "Analyze the sentiment of the following text and respond with POSITIVE, NEGATIVE, or NEUTRAL:";
        
        List<String> results = Agents4J.parallelQuery(
            chatModel,
            prompt,
            inputs,
            3 // Use 3 worker threads
        );
        
        System.out.println("Sentiment Analysis Results:");
        for (int i = 0; i < inputs.size(); i++) {
            System.out.println("Text: " + inputs.get(i));
            System.out.println("Sentiment: " + results.get(i));
            System.out.println();
        }
    }

    /**
     * Example 2: Batch translation to multiple languages
     */
    public void batchTranslation() {
        LOG.info("Running batch translation example");
        
        System.out.println("2. Batch Translation");
        System.out.println("-------------------");
        
        List<String> translationRequests = Arrays.asList(
            "Hello, how are you? -> French",
            "Thank you very much -> Spanish",
            "Good morning everyone -> German",
            "Have a great day -> Italian"
        );
        
        String prompt = "Translate the text before the arrow to the language after the arrow. " +
                       "Respond only with the translation:";
        
        List<String> translations = Agents4J.parallelQuery(
            chatModel,
            prompt,
            translationRequests,
            2 // Use 2 workers to respect API limits
        );
        
        System.out.println("Translation Results:");
        for (int i = 0; i < translationRequests.size(); i++) {
            System.out.println("Request: " + translationRequests.get(i));
            System.out.println("Translation: " + translations.get(i));
            System.out.println();
        }
    }

    /**
     * Example 3: Document analysis sectioning
     */
    public void documentAnalysis() {
        LOG.info("Running document analysis example");
        
        System.out.println("3. Document Analysis");
        System.out.println("-------------------");
        
        List<String> documentSections = Arrays.asList(
            "Executive Summary: Our company achieved record growth this quarter with revenues up 25%. " +
            "Key drivers include strong product adoption and expansion into new markets.",
            
            "Financial Performance: Q3 revenue reached $50M, representing a 25% increase year-over-year. " +
            "Operating margins improved to 15% due to operational efficiencies.",
            
            "Market Outlook: Industry trends favor continued growth with increasing demand for our solutions. " +
            "Competitive positioning remains strong with our technology advantage."
        );
        
        String analysisPrompt = "Analyze this business document section and provide:\n" +
                               "1. Key insight (one sentence)\n" +
                               "2. Important metric or number\n" +
                               "3. One actionable recommendation\n\n" +
                               "Section:";
        
        try {
            ParallelizationWorkflow workflow = Agents4J.createParallelizationWorkflow(
                "DocumentAnalysisWorkflow",
                chatModel
            );
            
            ParallelizationWorkflow.ParallelInput input = new ParallelizationWorkflow.ParallelInput(
                analysisPrompt,
                documentSections,
                2
            );
            
            List<String> analyses = workflow.execute(input);
            
            System.out.println("Document Analysis Results:");
            String[] sectionNames = {"Executive Summary", "Financial Performance", "Market Outlook"};
            for (int i = 0; i < analyses.size(); i++) {
                System.out.println("Section: " + sectionNames[i]);
                System.out.println("Analysis: " + analyses.get(i));
                System.out.println();
            }
        } catch (WorkflowExecutionException e) {
            LOG.error("Document analysis failed", e);
            System.err.println("Document analysis failed: " + e.getMessage());
        }
    }

    /**
     * Example 4: Asynchronous processing
     */
    public void asynchronousProcessing() {
        LOG.info("Running asynchronous processing example");
        
        System.out.println("4. Asynchronous Processing");
        System.out.println("-------------------------");
        
        List<String> tasks = Arrays.asList(
            "Generate a creative product name for a smart water bottle",
            "Write a tagline for an eco-friendly cleaning product",
            "Create a social media hashtag for a fitness app"
        );
        
        String prompt = "Complete this creative task with a single, concise response:";
        
        try {
            ParallelizationWorkflow workflow = Agents4J.createParallelizationWorkflow(
                "AsyncWorkflow",
                chatModel
            );
            
            ParallelizationWorkflow.ParallelInput input = new ParallelizationWorkflow.ParallelInput(
                prompt,
                tasks,
                3
            );
            
            // Start async processing
            CompletableFuture<List<String>> future = workflow.executeAsync(input);
            
            // Simulate doing other work
            System.out.println("Started async processing, doing other work...");
            Thread.sleep(100); // Simulate other work
            System.out.println("Other work completed, waiting for results...");
            
            // Get results when ready
            List<String> results = future.get();
            
            System.out.println("Async Processing Results:");
            for (int i = 0; i < tasks.size(); i++) {
                System.out.println("Task: " + tasks.get(i));
                System.out.println("Result: " + results.get(i));
                System.out.println();
            }
            
        } catch (Exception e) {
            LOG.error("Error in asynchronous processing example", e);
            throw new RuntimeException("Async processing failed", e);
        }
    }

    /**
     * Example 5: Custom workflow with builder pattern
     */
    public void customWorkflow() {
        LOG.info("Running custom workflow example");
        
        System.out.println("5. Custom Workflow");
        System.out.println("-----------------");
        
        try {
            // Build a custom workflow
            ParallelizationWorkflow workflow = ParallelizationWorkflow.builder()
                .name("ContentGenerationWorkflow")
                .chatModel(chatModel)
                .build();
            
            List<String> contentTopics = Arrays.asList(
                "Benefits of remote work for productivity",
                "Impact of AI on customer service",
                "Sustainable technology trends in 2024"
            );
            
            String contentPrompt = "Write a compelling 50-word social media post about this topic. " +
                                  "Include relevant hashtags:";
            
            // Create input with specific worker configuration
            ParallelizationWorkflow.ParallelInput input = new ParallelizationWorkflow.ParallelInput(
                contentPrompt,
                contentTopics,
                2 // Conservative worker count for content generation
            );
            
            // Execute and measure performance
            long startTime = System.currentTimeMillis();
            List<String> posts = workflow.execute(input);
            long endTime = System.currentTimeMillis();
            
            System.out.println("Generated Social Media Posts:");
            for (int i = 0; i < contentTopics.size(); i++) {
                System.out.println("Topic: " + contentTopics.get(i));
                System.out.println("Post: " + posts.get(i));
                System.out.println();
            }
            
            System.out.println("Processing completed in " + (endTime - startTime) + "ms");
            System.out.println("Workflow name: " + workflow.getName());
        } catch (WorkflowExecutionException e) {
            LOG.error("Custom workflow failed", e);
            System.err.println("Custom workflow failed: " + e.getMessage());
        }
    }

    /**
     * Performance comparison between parallel and sequential processing
     */
    public void performanceComparison() {
        LOG.info("Running performance comparison example");
        
        System.out.println("6. Performance Comparison");
        System.out.println("------------------------");
        
        List<String> testInputs = Arrays.asList(
            "Summarize machine learning",
            "Explain cloud computing",
            "Describe blockchain technology",
            "Define artificial intelligence",
            "Overview of cybersecurity"
        );
        
        String prompt = "Provide a one-sentence explanation of:";
        
        // Parallel processing
        long parallelStart = System.currentTimeMillis();
        List<String> parallelResults = Agents4J.parallelQuery(
            chatModel,
            prompt,
            testInputs,
            3
        );
        long parallelTime = System.currentTimeMillis() - parallelStart;
        
        // Sequential processing simulation
        long sequentialStart = System.currentTimeMillis();
        for (String input : testInputs) {
            Agents4J.query(chatModel, prompt, input);
        }
        long sequentialTime = System.currentTimeMillis() - sequentialStart;
        
        System.out.println("Performance Results:");
        System.out.println("Parallel Processing: " + parallelTime + "ms");
        System.out.println("Sequential Processing: " + sequentialTime + "ms");
        System.out.println("Speed improvement: " + 
            String.format("%.1fx", (double) sequentialTime / parallelTime));
        System.out.println("Processed " + testInputs.size() + " inputs");
    }
}