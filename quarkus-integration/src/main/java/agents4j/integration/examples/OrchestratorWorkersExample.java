package agents4j.integration.examples;

import dev.agents4j.Agents4J;
import dev.agents4j.workflow.OrchestratorWorkersWorkflow;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Example demonstrating the Orchestrator-Workers workflow pattern in a Quarkus environment.
 * 
 * This example shows how to use the pattern for complex task decomposition
 * and specialized processing through coordinated workers.
 */
@ApplicationScoped
public class OrchestratorWorkersExample {

    @Inject
    ChatModel chatModel;

    @ConfigProperty(name = "agents4j.orchestrator.enabled", defaultValue = "true")
    boolean orchestratorEnabled;

    /**
     * Example 1: Simple orchestrated query using standard workers
     */
    public String simpleOrchestratedExample() {
        if (!orchestratorEnabled) {
            return "Orchestrator-Workers workflow is disabled";
        }

        System.out.println("=== Simple Orchestrated Query ===");
        
        String task = "Create a comprehensive analysis of renewable energy trends, " +
                     "including market data, future projections, and a summary report.";
        
        String result = Agents4J.orchestratedQuery(chatModel, task);
        
        System.out.println("Task: " + task);
        System.out.println("Result: " + result);
        
        return result;
    }

    /**
     * Example 2: Custom workers for specialized domains
     */
    public String customWorkersExample() {
        if (!orchestratorEnabled) {
            return "Orchestrator-Workers workflow is disabled";
        }

        System.out.println("\n=== Custom Workers Example ===");
        
        String task = "Develop a new mobile app concept, including technical specifications, " +
                     "user interface design, and marketing strategy.";
        
        String result = Agents4J.customOrchestratedQuery(
            chatModel,
            task,
            Agents4J.worker("technical_architect", 
                "Designs technical architecture and specifications",
                "You are a senior technical architect. Create detailed technical specifications, " +
                "architecture designs, and technology stack recommendations."),
            Agents4J.worker("ui_designer", 
                "Designs user interfaces and user experience",
                "You are a UI/UX designer. Create intuitive user interface designs, " +
                "user flows, and interaction patterns."),
            Agents4J.worker("marketing_strategist", 
                "Develops marketing and business strategies", 
                "You are a marketing strategist. Develop comprehensive marketing strategies, " +
                "target audience analysis, and go-to-market plans.")
        );
        
        System.out.println("Task: " + task);
        System.out.println("Result: " + result);
        
        return result;
    }

    /**
     * Example 3: Advanced workflow with custom configuration
     */
    public OrchestratorWorkersWorkflow.WorkerResponse advancedWorkflowExample() {
        if (!orchestratorEnabled) {
            System.out.println("Orchestrator-Workers workflow is disabled");
            return null;
        }

        System.out.println("\n=== Advanced Workflow Example ===");
        
        // Create workflow with custom orchestrator and synthesizer prompts
        OrchestratorWorkersWorkflow workflow = OrchestratorWorkersWorkflow.builder()
            .name("AdvancedBusinessAnalysis")
            .chatModel(chatModel)
            .addWorker("market_analyst", 
                "Analyzes market conditions and trends",
                "You are a market analyst specializing in business intelligence. " +
                "Provide detailed market analysis, competitive landscape assessment, " +
                "and trend identification.")
            .addWorker("financial_analyst", 
                "Analyzes financial data and projections",
                "You are a financial analyst. Create financial models, projections, " +
                "and investment recommendations based on the given information.")
            .addWorker("risk_assessor", 
                "Evaluates risks and mitigation strategies",
                "You are a risk management specialist. Identify potential risks, " +
                "assess their impact and probability, and recommend mitigation strategies.")
            .orchestratorPrompt(
                "You are a senior business consultant. Analyze complex business scenarios " +
                "and decompose them into specific analytical tasks. Assign each task to " +
                "the most appropriate specialist worker.")
            .synthesizerPrompt(
                "You are an executive consultant. Synthesize the analyses from multiple " +
                "specialists into a coherent, actionable business recommendation. " +
                "Provide clear next steps and strategic recommendations.")
            .build();

        try {
            String complexTask = "Our company is considering entering the electric vehicle market. " +
                               "We need a comprehensive analysis covering market opportunities, " +
                               "financial requirements, competitive landscape, and potential risks.";

            OrchestratorWorkersWorkflow.OrchestratorInput input = 
                new OrchestratorWorkersWorkflow.OrchestratorInput(complexTask);

            OrchestratorWorkersWorkflow.WorkerResponse response = workflow.execute(input);

            System.out.println("Complex Task: " + complexTask);
            System.out.println("\nSubtasks identified:");
            response.getSubtasks().forEach(subtask -> 
                System.out.println("- " + subtask.getWorkerType() + ": " + subtask.getInstructions())
            );

            System.out.println("\nWorker Results:");
            response.getSubtaskResults().forEach(result -> 
                System.out.println("- " + result.getWorkerType() + " (" + 
                    (result.isSuccessful() ? "SUCCESS" : "FAILED") + "): " + 
                    result.getResult().substring(0, Math.min(100, result.getResult().length())) + "...")
            );

            System.out.println("\nFinal Synthesized Result:");
            System.out.println(response.getFinalResult());

            return response;

        } catch (Exception e) {
            System.err.println("Workflow execution failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Example 4: Parallel processing with multiple perspectives
     */
    public String multiPerspectiveExample() {
        if (!orchestratorEnabled) {
            return "Orchestrator-Workers workflow is disabled";
        }

        System.out.println("\n=== Multi-Perspective Analysis ===");
        
        String result = Agents4J.customOrchestratedQuery(
            chatModel,
            "Evaluate the impact of artificial intelligence on the job market.",
            Agents4J.worker("economist", 
                "Economic perspective analysis",
                "You are an economist. Analyze the economic implications, market effects, " +
                "and macroeconomic trends related to the topic."),
            Agents4J.worker("sociologist", 
                "Social impact analysis", 
                "You are a sociologist. Examine the social implications, cultural changes, " +
                "and human behavioral aspects of the topic."),
            Agents4J.worker("technologist", 
                "Technical feasibility and innovation analysis",
                "You are a technology expert. Assess the technical aspects, innovation potential, " +
                "and technological feasibility of the topic.")
        );
        
        System.out.println("Multi-perspective analysis result: " + result);
        return result;
    }

    /**
     * Example 5: Content creation workflow
     */
    public String contentCreationExample() {
        if (!orchestratorEnabled) {
            return "Orchestrator-Workers workflow is disabled";
        }

        System.out.println("\n=== Content Creation Workflow ===");
        
        String result = Agents4J.customOrchestratedQuery(
            chatModel,
            "Create a complete blog post about sustainable technology trends, " +
            "including research, writing, editing, and SEO optimization.",
            Agents4J.worker("researcher", 
                "Research and fact-checking specialist",
                "You are a research specialist. Gather accurate, up-to-date information " +
                "and provide well-sourced facts and statistics."),
            Agents4J.worker("content_writer", 
                "Content writing specialist", 
                "You are a professional content writer. Create engaging, well-structured " +
                "articles with clear messaging and good flow."),
            Agents4J.worker("editor", 
                "Editing and proofreading specialist",
                "You are an editor. Review content for clarity, grammar, style, " +
                "and overall quality improvements."),
            Agents4J.worker("seo_specialist", 
                "SEO optimization specialist",
                "You are an SEO expert. Optimize content for search engines while " +
                "maintaining readability and user value.")
        );
        
        System.out.println("Content creation result: " + result);
        return result;
    }

    /**
     * Get workflow configuration information
     */
    public String getWorkflowInfo() {
        if (!orchestratorEnabled) {
            return "Orchestrator-Workers workflow is disabled";
        }

        OrchestratorWorkersWorkflow workflow = Agents4J.createOrchestratorWorkersWorkflow(
            "InfoWorkflow",
            chatModel
        );

        StringBuilder info = new StringBuilder();
        info.append("Workflow Configuration:\n");
        info.append("- Name: ").append(workflow.getName()).append("\n");
        info.append("- Type: ").append(workflow.getConfigurationProperty("workflowType", "unknown")).append("\n");
        info.append("- Available Workers: ").append(workflow.getConfigurationProperty("workerTypes", "none")).append("\n");
        info.append("- Max Parallel Workers: ").append(workflow.getConfigurationProperty("maxParallelWorkers", 0)).append("\n");

        return info.toString();
    }

    /**
     * Health check for the orchestrator workflow
     */
    public boolean isWorkflowHealthy() {
        if (!orchestratorEnabled) {
            return false;
        }

        try {
            OrchestratorWorkersWorkflow workflow = Agents4J.createOrchestratorWorkersWorkflow(
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
     * Demonstration method that runs all examples
     */
    public void runAllExamples() {
        System.out.println("Orchestrator-Workers Workflow Pattern Examples");
        System.out.println("==============================================");
        
        if (!isWorkflowHealthy()) {
            System.out.println("Workflow is not healthy. Check configuration.");
            return;
        }

        try {
            simpleOrchestratedExample();
            customWorkersExample();
            advancedWorkflowExample();
            multiPerspectiveExample();
            contentCreationExample();
            
            System.out.println("\n" + getWorkflowInfo());
        } catch (Exception e) {
            System.err.println("Error running examples: " + e.getMessage());
        }
    }
}