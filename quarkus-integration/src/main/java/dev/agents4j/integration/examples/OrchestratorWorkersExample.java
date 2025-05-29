package dev.agents4j.integration.examples;

import dev.agents4j.workflow.AgentWorkflowFactory;
import dev.agents4j.workflow.OrchestratorWorkersWorkflow;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;

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

    public OrchestratorWorkersExample() {
        // Default constructor for CDI
    }

    /**
     * Example 1: Simple orchestrated query
     */
    public String simpleOrchestrationExample() {
        if (!orchestratorEnabled || chatModel == null) {
            return "Orchestrator workflow is disabled or ChatModel not available";
        }

        System.out.println("\n=== Simple Orchestration Example ===");
        
        String task = "Analyze the potential impact of renewable energy adoption on the economy";
        
        try {
            OrchestratorWorkersWorkflow workflow = AgentWorkflowFactory.createStandardOrchestratorWorkersWorkflow(
                "SimpleOrchestrator", 
                chatModel
            );
            
            OrchestratorWorkersWorkflow.OrchestratorInput input = 
                new OrchestratorWorkersWorkflow.OrchestratorInput(task, Map.of());
            
            StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result = workflow.start(input);
            String output = result.isCompleted() ? "Analysis completed successfully" : "Analysis failed";
            
            System.out.println("Task: " + task);
            System.out.println("Result: " + output);
            
            return output;
            
        } catch (Exception e) {
            System.err.println("Simple orchestration failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Example 2: Business analysis workflow
     */
    public String businessAnalysisExample() {
        if (!orchestratorEnabled) {
            return "Orchestrator workflow is disabled";
        }

        System.out.println("\n=== Business Analysis Workflow ===");
        
        try {
            OrchestratorWorkersWorkflow workflow = AgentWorkflowFactory.createStandardOrchestratorWorkersWorkflow(
                "BusinessAnalysis",
                chatModel
            );
            
            String businessQuery = "Evaluate market opportunities for electric vehicle charging infrastructure";
            OrchestratorWorkersWorkflow.OrchestratorInput input = 
                new OrchestratorWorkersWorkflow.OrchestratorInput(businessQuery, Map.of());

            StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result = workflow.start(input);
            String analysis = result.isCompleted() ? "Business analysis completed" : "Analysis incomplete";

            System.out.println("Business Query: " + businessQuery);
            System.out.println("Analysis: " + analysis);
            
            return analysis;
            
        } catch (Exception e) {
            System.err.println("Business analysis failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Example 3: Advanced workflow with custom configuration
     */
    public String advancedWorkflowExample() {
        if (!orchestratorEnabled) {
            System.out.println("Orchestrator-Workers workflow is disabled");
            return "Failed to process";
        }

        System.out.println("\n=== Advanced Workflow Example ===");

        try {
            OrchestratorWorkersWorkflow workflow = AgentWorkflowFactory.createStandardOrchestratorWorkersWorkflow(
                "AdvancedOrchestrator",
                chatModel
            );

            String complexTask = "Our company is considering entering the electric vehicle market. " +
                               "We need a comprehensive analysis covering market opportunities, " +
                               "financial requirements, competitive landscape, and potential risks.";

            OrchestratorWorkersWorkflow.OrchestratorInput input = 
                new OrchestratorWorkersWorkflow.OrchestratorInput(complexTask, Map.of());

            StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result = workflow.start(input);
            String response = result.isCompleted() ? "Comprehensive analysis completed" : "Analysis failed";

            System.out.println("Complex Task: " + complexTask);
            System.out.println("Result: " + response);

            return response;

        } catch (Exception e) {
            System.err.println("Workflow execution failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Example 4: Multi-perspective analysis
     */
    public String multiPerspectiveExample() {
        if (!orchestratorEnabled) {
            return "Orchestrator-Workers workflow is disabled";
        }

        System.out.println("\n=== Multi-Perspective Analysis ===");
        
        try {
            OrchestratorWorkersWorkflow workflow = AgentWorkflowFactory.createStandardOrchestratorWorkersWorkflow(
                "MultiPerspective",
                chatModel
            );
            
            String topic = "Evaluate the impact of artificial intelligence on the job market";
            OrchestratorWorkersWorkflow.OrchestratorInput input = 
                new OrchestratorWorkersWorkflow.OrchestratorInput(topic, Map.of());
                
            StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result = workflow.start(input);
            String analysis = result.isCompleted() ? "Multi-perspective analysis completed" : "Analysis incomplete";
            
            System.out.println("Multi-perspective analysis result: " + analysis);
            return analysis;
            
        } catch (Exception e) {
            System.err.println("Multi-perspective analysis failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Example 5: Content creation workflow
     */
    public String contentCreationExample() {
        if (!orchestratorEnabled) {
            return "Orchestrator-Workers workflow is disabled";
        }

        System.out.println("\n=== Content Creation Workflow ===");
        
        try {
            OrchestratorWorkersWorkflow workflow = AgentWorkflowFactory.createStandardOrchestratorWorkersWorkflow(
                "ContentCreation",
                chatModel
            );
            
            String contentRequest = "Create a complete blog post about sustainable technology trends, " +
                                  "including research, writing, editing, and SEO optimization.";
            OrchestratorWorkersWorkflow.OrchestratorInput input = 
                new OrchestratorWorkersWorkflow.OrchestratorInput(contentRequest, Map.of());
                
            StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result = workflow.start(input);
            String content = result.isCompleted() ? "Content creation completed" : "Creation incomplete";
            
            System.out.println("Content creation result: " + content);
            return content;
            
        } catch (Exception e) {
            System.err.println("Content creation failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get workflow configuration information
     */
    public String getWorkflowInfo() {
        if (!orchestratorEnabled) {
            return "Orchestrator-Workers workflow is disabled";
        }

        try {
            OrchestratorWorkersWorkflow workflow = AgentWorkflowFactory.createStandardOrchestratorWorkersWorkflow(
                "InfoWorkflow",
                chatModel
            );

            StringBuilder info = new StringBuilder();
            info.append("Workflow Configuration:\n");
            info.append("- Name: ").append(workflow.getName()).append("\n");
            info.append("- Type: OrchestratorWorkersWorkflow\n");
            info.append("- Available Workers: Standard workers\n");
            info.append("- Max Parallel Workers: 4\n");

            return info.toString();
            
        } catch (Exception e) {
            return "Error getting workflow info: " + e.getMessage();
        }
    }

    /**
     * Health check for the orchestrator workflow
     */
    public boolean isWorkflowHealthy() {
        if (!orchestratorEnabled) {
            return false;
        }

        try {
            OrchestratorWorkersWorkflow workflow = AgentWorkflowFactory.createStandardOrchestratorWorkersWorkflow(
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
            System.out.println("Workflow is not healthy, skipping examples");
            return;
        }

        simpleOrchestrationExample();
        businessAnalysisExample();
        advancedWorkflowExample();
        multiPerspectiveExample();
        contentCreationExample();
        
        System.out.println("\nWorkflow Information:");
        System.out.println(getWorkflowInfo());
        
        System.out.println("\nAll orchestrator-workers examples completed!");
    }
}