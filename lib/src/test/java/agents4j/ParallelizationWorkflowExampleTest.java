package agents4j;

import dev.agents4j.Agents4J;
import dev.agents4j.workflow.ParallelizationWorkflow;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Example tests demonstrating real-world usage patterns for ParallelizationWorkflow.
 * These examples show different scenarios where parallel processing can improve
 * performance and efficiency in AI agent workflows.
 */
class ParallelizationWorkflowExampleTest {

    private ChatModel mockModel;

    @BeforeEach
    void setUp() {
        mockModel = Mockito.mock(ChatModel.class);
        
        // Setup different mock responses for various scenarios
        setupMockResponses();
    }

    private void setupMockResponses() {
        // Translation responses
        AiMessage frenchResponse = Mockito.mock(AiMessage.class);
        when(frenchResponse.text()).thenReturn("Bonjour le monde");
        
        AiMessage spanishResponse = Mockito.mock(AiMessage.class);
        when(spanishResponse.text()).thenReturn("Hola mundo");
        
        AiMessage germanResponse = Mockito.mock(AiMessage.class);
        when(germanResponse.text()).thenReturn("Hallo Welt");

        // Analysis responses
        AiMessage sentimentResponse = Mockito.mock(AiMessage.class);
        when(sentimentResponse.text()).thenReturn("Positive sentiment with confidence 0.85");
        
        AiMessage summaryResponse = Mockito.mock(AiMessage.class);
        when(summaryResponse.text()).thenReturn("Summary: Key insights from the document include...");
        
        AiMessage keywordsResponse = Mockito.mock(AiMessage.class);
        when(keywordsResponse.text()).thenReturn("Keywords: technology, innovation, future, AI");

        // Setup chat responses
        ChatResponse[] responses = {
            createChatResponse(frenchResponse),
            createChatResponse(spanishResponse), 
            createChatResponse(germanResponse),
            createChatResponse(sentimentResponse),
            createChatResponse(summaryResponse),
            createChatResponse(keywordsResponse)
        };

        when(mockModel.chat(anyList()))
            .thenReturn(responses[0])
            .thenReturn(responses[1])
            .thenReturn(responses[2])
            .thenReturn(responses[3])
            .thenReturn(responses[4])
            .thenReturn(responses[5])
            .thenReturn(responses[0]) // Cycle back for additional calls
            .thenReturn(responses[1])
            .thenReturn(responses[2]);
    }

    private ChatResponse createChatResponse(AiMessage aiMessage) {
        ChatResponse response = Mockito.mock(ChatResponse.class);
        when(response.aiMessage()).thenReturn(aiMessage);
        return response;
    }

    @Test
    void exampleBatchTranslation() {
        /*
         * EXAMPLE 1: Batch Translation
         * Scenario: Translate multiple texts to different languages in parallel
         * Use Case: Internationalization, content localization
         */
        
        List<String> textsToTranslate = Arrays.asList(
            "Hello World - translate to French",
            "Hello World - translate to Spanish", 
            "Hello World - translate to German"
        );

        String translationPrompt = "You are a professional translator. " +
            "Translate the text after the dash to the specified language. " +
            "Return only the translation without explanations.";

        List<String> translations = Agents4J.parallelQuery(
            mockModel,
            translationPrompt,
            textsToTranslate,
            3 // Use 3 workers for 3 translations
        );

        assertNotNull(translations);
        assertEquals(3, translations.size());
        
        // Verify we got different translations
        assertTrue(translations.stream().anyMatch(t -> t.contains("Bonjour")));
        assertTrue(translations.stream().anyMatch(t -> t.contains("Hola")));
        assertTrue(translations.stream().anyMatch(t -> t.contains("Hallo")));
        
        System.out.println("=== Batch Translation Results ===");
        for (int i = 0; i < textsToTranslate.size(); i++) {
            System.out.println(textsToTranslate.get(i) + " -> " + translations.get(i));
        }
    }

    @Test
    void exampleDocumentAnalysisSectioning() {
        /*
         * EXAMPLE 2: Document Analysis Sectioning
         * Scenario: Analyze different sections of a large document in parallel
         * Use Case: Research papers, legal documents, technical manuals
         */
        
        List<String> documentSections = Arrays.asList(
            "Executive Summary: The quarterly report shows strong growth...",
            "Financial Data: Revenue increased by 15% compared to last quarter...",
            "Market Analysis: Consumer sentiment remains positive with emerging trends..."
        );

        String analysisPrompt = "Analyze the following document section and provide:\n" +
            "1. Key insights\n" +
            "2. Important metrics or data points\n" +
            "3. Actionable recommendations\n" +
            "Section to analyze:";

        ParallelizationWorkflow workflow = Agents4J.createParallelizationWorkflow(
            "DocumentAnalysisWorkflow",
            mockModel
        );

        ParallelizationWorkflow.ParallelInput input = new ParallelizationWorkflow.ParallelInput(
            analysisPrompt,
            documentSections,
            2 // Use 2 workers to avoid overwhelming the API
        );

        Map<String, Object> context = new HashMap<>();
        List<String> analyses = workflow.execute(input, context);

        assertNotNull(analyses);
        assertEquals(3, analyses.size());
        
        // Verify context was populated
        assertTrue(context.containsKey("workflow_name"));
        assertTrue(context.containsKey("num_inputs"));
        assertTrue(context.containsKey("results"));
        
        System.out.println("=== Document Analysis Results ===");
        for (int i = 0; i < documentSections.size(); i++) {
            System.out.println("Section " + (i + 1) + " Analysis:");
            System.out.println(analyses.get(i));
            System.out.println("---");
        }
    }

    @Test
    void exampleMultiPerspectiveVoting() {
        /*
         * EXAMPLE 3: Multi-Perspective Voting
         * Scenario: Get multiple AI perspectives on the same question for validation
         * Use Case: Decision making, consensus building, quality assurance
         */
        
        String question = "Should our company invest in renewable energy infrastructure?";
        
        // Create multiple prompts for different perspectives
        List<String> perspectivePrompts = Arrays.asList(
            "As a financial analyst, evaluate: " + question,
            "As an environmental expert, evaluate: " + question,
            "As a risk management specialist, evaluate: " + question
        );

        String votingPrompt = "You are an expert consultant. Provide a clear recommendation " +
            "(YES/NO) with 2-3 key supporting reasons. Be concise and decisive.";

        List<String> perspectives = Agents4J.parallelQuery(
            mockModel,
            votingPrompt,
            perspectivePrompts,
            3
        );

        assertNotNull(perspectives);
        assertEquals(3, perspectives.size());
        
        System.out.println("=== Multi-Perspective Analysis ===");
        String[] roles = {"Financial Analyst", "Environmental Expert", "Risk Management"};
        for (int i = 0; i < perspectives.size(); i++) {
            System.out.println(roles[i] + " Perspective:");
            System.out.println(perspectives.get(i));
            System.out.println("---");
        }
    }

    @Test
    void exampleContentGenerationBatch() {
        /*
         * EXAMPLE 4: Batch Content Generation
         * Scenario: Generate multiple pieces of content simultaneously
         * Use Case: Social media posts, product descriptions, marketing copy
         */
        
        List<String> contentTopics = Arrays.asList(
            "AI in Healthcare - Benefits and Challenges",
            "Remote Work Productivity Tips for 2024",
            "Sustainable Technology Trends",
            "Digital Marketing Strategies for Small Business",
            "Future of Electric Vehicles"
        );

        String contentPrompt = "Write a compelling 2-paragraph social media post about the following topic. " +
            "Make it engaging, informative, and include relevant hashtags. Topic:";

        // Use async execution for better performance
        ParallelizationWorkflow workflow = ParallelizationWorkflow.builder()
            .name("ContentGenerationWorkflow")
            .chatModel(mockModel)
            .build();

        ParallelizationWorkflow.ParallelInput input = new ParallelizationWorkflow.ParallelInput(
            contentPrompt,
            contentTopics,
            3 // Moderate parallelism to respect API limits
        );

        List<String> generatedContent = workflow.execute(input);

        assertNotNull(generatedContent);
        assertEquals(5, generatedContent.size());
        
        System.out.println("=== Generated Content ===");
        for (int i = 0; i < contentTopics.size(); i++) {
            System.out.println("Topic: " + contentTopics.get(i));
            System.out.println("Generated Content: " + generatedContent.get(i));
            System.out.println("---");
        }
    }

    @Test
    void exampleAsyncProcessing() throws Exception {
        /*
         * EXAMPLE 5: Asynchronous Processing
         * Scenario: Non-blocking parallel processing for better resource utilization
         * Use Case: Background processing, batch jobs, streaming data
         */
        
        List<String> tasks = Arrays.asList(
            "Analyze customer feedback sentiment",
            "Generate weekly sales summary", 
            "Create product recommendation list"
        );

        String taskPrompt = "You are a business analyst. Complete the following task efficiently:";

        ParallelizationWorkflow workflow = Agents4J.createParallelizationWorkflow(
            "AsyncProcessingWorkflow",
            mockModel
        );

        ParallelizationWorkflow.ParallelInput input = new ParallelizationWorkflow.ParallelInput(
            taskPrompt,
            tasks,
            2
        );

        // Execute asynchronously
        var future = workflow.executeAsync(input);
        
        // Do other work while processing...
        System.out.println("Processing started, doing other work...");
        Thread.sleep(100); // Simulate other work
        
        // Get results when ready
        List<String> results = future.get();
        
        assertNotNull(results);
        assertEquals(3, results.size());
        
        System.out.println("=== Async Processing Results ===");
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println("Task: " + tasks.get(i));
            System.out.println("Result: " + results.get(i));
            System.out.println("---");
        }
    }

    @Test
    void examplePerformanceComparison() {
        /*
         * EXAMPLE 6: Performance Comparison
         * Scenario: Demonstrate performance benefits of parallel vs sequential processing
         * Use Case: Performance optimization, capacity planning
         */
        
        List<String> testInputs = Arrays.asList(
            "Input 1", "Input 2", "Input 3", "Input 4", "Input 5"
        );
        String prompt = "Process the following input efficiently:";

        // Measure parallel processing time
        long parallelStart = System.currentTimeMillis();
        List<String> parallelResults = Agents4J.parallelQuery(
            mockModel, prompt, testInputs, 5
        );
        long parallelTime = System.currentTimeMillis() - parallelStart;

        // Simulate sequential processing time
        long sequentialStart = System.currentTimeMillis();
        for (String input : testInputs) {
            Agents4J.query(mockModel, prompt, input);
        }
        long sequentialTime = System.currentTimeMillis() - sequentialStart;

        System.out.println("=== Performance Comparison ===");
        System.out.println("Parallel Processing Time: " + parallelTime + "ms");
        System.out.println("Sequential Processing Time: " + sequentialTime + "ms");
        System.out.println("Parallel processed " + testInputs.size() + " inputs");
        
        assertNotNull(parallelResults);
        assertEquals(5, parallelResults.size());
    }
}