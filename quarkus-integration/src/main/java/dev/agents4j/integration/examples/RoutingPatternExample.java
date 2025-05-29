package dev.agents4j.integration.examples;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.routing.Route;
import dev.agents4j.api.routing.RoutingDecision;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.workflow.AgentWorkflowFactory;
import dev.agents4j.workflow.routing.LLMContentRouter;
import dev.agents4j.workflow.routing.RuleBasedContentRouter;
import dev.agents4j.workflow.routing.RoutingWorkflow;
import dev.agents4j.workflow.routing.RoutingWorkflowFactory;
import dev.agents4j.workflow.strategy.StrategyFactory;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Comprehensive example demonstrating the Routing Pattern implementation.
 * 
 * This example shows how to use intelligent content routing to direct different
 * types of inputs to specialized handlers, enabling:
 * - Separation of concerns with specialized processing paths
 * - Improved accuracy through route-specific optimization
 * - Scalable architecture for diverse content types
 * - Resource optimization through appropriate routing
 */
@ApplicationScoped
public class RoutingPatternExample {

    @Inject
    ChatModel chatModel;

    public RoutingPatternExample() {
        // Default constructor for CDI
    }

    public static void main(String[] args) {
        try {
            // Create a mock ChatModel (replace with real model in production)
            ChatModel model = createMockChatModel();
            
            System.out.println("=== Routing Pattern Demonstration ===\n");
            
            // Demonstrate different routing approaches
            demonstrateCustomerSupportRouting(model);
            demonstrateContentCategorization(model);
            demonstrateMultiLanguageRouting(model);
            demonstrateLLMBasedRouting(model);
            demonstrateRuleBasedRouting(model);
            demonstrateHybridRouting(model);
            
        } catch (Exception e) {
            System.err.println("Error running routing examples: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Demonstrates customer support ticket routing using LLM classification.
     */
    private static void demonstrateCustomerSupportRouting(ChatModel model) throws WorkflowExecutionException {
        System.out.println("--- Customer Support Routing ---");
        
        String classificationPrompt = 
            "You are a customer support ticket classifier. Analyze the ticket and classify it into one of these categories: " +
            "technical, billing, general, escalation. Consider the urgency, complexity, and type of issue.";
        
        RoutingWorkflow<String, String> supportWorkflow = 
            AgentWorkflowFactory.createLLMRoutingWorkflow(
                "CustomerSupportRouting", 
                model, 
                classificationPrompt,
                AgentWorkflowFactory.createStringRoute("technical", "Technical Support", "You are a technical support specialist."),
                AgentWorkflowFactory.createStringRoute("billing", "Billing Support", "You are a billing specialist."),
                AgentWorkflowFactory.createStringRoute("general", "General Support", "You are a general support representative."),
                AgentWorkflowFactory.createStringRoute("escalation", "Escalation Support", "You are a senior support specialist.")
            );
        
        // Test different types of support tickets
        String[] testTickets = {
            "My application keeps crashing with error code 500 when I try to save data",
            "I was charged twice for my subscription and need a refund",
            "How do I change my password?",
            "URGENT: Our entire production system is down and we're losing money"
        };
        
        for (String ticket : testTickets) {
            System.out.println("Ticket: " + ticket);
            
            Map<String, Object> context = new HashMap<>();
            StatefulWorkflowResult<String> result = supportWorkflow.start(ticket, context);
            
            if (result.isCompleted()) {
                System.out.println("Response: " + result.getOutput().orElse("No output"));
                System.out.println("Routed to: " + result.getMetadata().get("selected_route_id"));
                System.out.println("Confidence: " + result.getMetadata().get("routing_confidence"));
            } else {
                System.out.println("Workflow did not complete successfully");
            }
            System.out.println();
        }
    }

    /**
     * Demonstrates content categorization using rule-based routing.
     */
    private static void demonstrateContentCategorization(ChatModel model) throws WorkflowExecutionException {
        System.out.println("--- Content Categorization ---");
        
        Map<String, List<String>> categoryRules = Map.of(
            "technology", List.of("software", "hardware", "programming", "AI", "machine learning", "database"),
            "business", List.of("management", "strategy", "marketing", "sales", "finance", "revenue"),
            "science", List.of("research", "experiment", "theory", "discovery", "analysis", "study"),
            "entertainment", List.of("movie", "music", "game", "celebrity", "sports", "festival")
        );
        
        String classificationPrompt = 
            "You are a content classifier. Analyze the content and classify it into one of these categories: " +
            String.join(", ", categoryRules.keySet());
        
        RoutingWorkflow<String, String> categorizationWorkflow = 
            AgentWorkflowFactory.createLLMRoutingWorkflow(
                "ContentCategorization",
                model,
                classificationPrompt,
                AgentWorkflowFactory.createStringRoute("technology", "Technology Handler", "You are a technology specialist."),
                AgentWorkflowFactory.createStringRoute("business", "Business Handler", "You are a business specialist."),
                AgentWorkflowFactory.createStringRoute("science", "Science Handler", "You are a science specialist."),
                AgentWorkflowFactory.createStringRoute("entertainment", "Entertainment Handler", "You are an entertainment specialist.")
            );
        
        String[] testContent = {
            "Latest breakthrough in machine learning algorithms shows 40% improvement in accuracy",
            "Company announces new marketing strategy to increase revenue by 25%",
            "Scientists discover new species in deep ocean research expedition",
            "Popular music festival announces lineup with top celebrities"
        };
        
        for (String content : testContent) {
            System.out.println("Content: " + content);
            
            Map<String, Object> context = new HashMap<>();
            StatefulWorkflowResult<String> result = categorizationWorkflow.start(content, context);
            
            if (result.isCompleted()) {
                System.out.println("Category: " + result.getMetadata().get("selected_route_id"));
                System.out.println("Processed: " + result.getOutput().orElse("No output"));
            } else {
                System.out.println("Workflow did not complete successfully");
            }
            System.out.println();
        }
    }

    /**
     * Demonstrates multi-language content routing.
     */
    private static void demonstrateMultiLanguageRouting(ChatModel model) throws WorkflowExecutionException {
        System.out.println("--- Multi-Language Routing ---");
        
        List<String> supportedLanguages = List.of("english", "spanish", "french");
        
        String languageClassificationPrompt = 
            "You are a language detector. Detect the language of the input and classify it as one of: " +
            String.join(", ", supportedLanguages);
        
        RoutingWorkflow<String, String> multiLangWorkflow = 
            AgentWorkflowFactory.createLLMRoutingWorkflow(
                "MultiLanguageProcessor",
                model,
                languageClassificationPrompt,
                AgentWorkflowFactory.createStringRoute("english", "English Handler", "You are an English language specialist."),
                AgentWorkflowFactory.createStringRoute("spanish", "Spanish Handler", "You are a Spanish language specialist."),
                AgentWorkflowFactory.createStringRoute("french", "French Handler", "You are a French language specialist.")
            );
        
        String[] testTexts = {
            "Hello, I need help with my account settings and password reset",
            "Hola, necesito ayuda con la configuración de mi cuenta",
            "Bonjour, j'ai besoin d'aide avec les paramètres de mon compte",
            "こんにちは、アカウント設定のヘルプが必要です" // Japanese - should go to translation
        };
        
        for (String text : testTexts) {
            System.out.println("Text: " + text);
            
            Map<String, Object> context = new HashMap<>();
            StatefulWorkflowResult<String> result = multiLangWorkflow.start(text, context);
            
            if (result.isCompleted()) {
                System.out.println("Detected Language Route: " + result.getMetadata().get("selected_route_id"));
                System.out.println("Processed: " + result.getOutput().orElse("No output"));
            } else {
                System.out.println("Workflow did not complete successfully");
            }
            System.out.println();
        }
    }

    /**
     * Demonstrates LLM-based routing with custom routes.
     */
    private static void demonstrateLLMBasedRouting(ChatModel model) throws WorkflowExecutionException {
        System.out.println("--- LLM-Based Routing ---");
        
        // Create custom routes
        Route<String, String> analysisRoute = AgentWorkflowFactory.createStringRoute(
            "analysis",
            "Performs detailed analysis and research",
            model,
            "You are a data analyst. Analyze the input thoroughly and provide insights.",
            "Summarize your analysis with key findings and recommendations."
        );
        
        Route<String, String> creativeRoute = AgentWorkflowFactory.createStringRoute(
            "creative",
            "Handles creative and artistic content",
            model,
            "You are a creative writer. Transform the input into engaging, creative content."
        );
        
        Route<String, String> factualRoute = AgentWorkflowFactory.createStringRoute(
            "factual",
            "Processes factual and informational content",
            model,
            "You are a fact-checker. Verify and present information accurately and clearly."
        );
        
        String classificationPrompt = 
            "Classify the input as requiring: 'analysis' (for data/research), " +
            "'creative' (for artistic/imaginative content), or 'factual' (for information/facts).";
        
        RoutingWorkflow<String, String> llmWorkflow = AgentWorkflowFactory.createLLMRoutingWorkflow(
            "LLMBasedRouting",
            model,
            classificationPrompt,
            analysisRoute,
            creativeRoute,
            factualRoute
        );
        
        String[] testInputs = {
            "Analyze the sales data trends for Q3 and predict Q4 performance",
            "Write a creative story about a robot learning to paint",
            "What are the key features of renewable energy sources?"
        };
        
        for (String input : testInputs) {
            System.out.println("Input: " + input);
            
            Map<String, Object> context = new HashMap<>();
            StatefulWorkflowResult<String> result = llmWorkflow.start(input, context);
            
            if (result.isCompleted()) {
                RoutingDecision decision = (RoutingDecision) result.getMetadata().get("routing_decision");
                System.out.println("Route: " + decision.getSelectedRoute());
                System.out.println("Confidence: " + decision.getConfidence());
                System.out.println("Reasoning: " + decision.getReasoning());
                System.out.println("Result: " + result.getOutput().orElse("No output"));
            } else {
                System.out.println("Workflow did not complete successfully");
            }
            System.out.println();
        }
    }

    /**
     * Demonstrates rule-based routing with regex and keyword patterns.
     */
    private static void demonstrateRuleBasedRouting(ChatModel model) throws WorkflowExecutionException {
        System.out.println("--- Rule-Based Routing ---");
        
        // Create custom routes
        Route<String, String> urgentRoute = AgentWorkflowFactory.createStringRouteWithStrategy(
            "urgent",
            "Handles urgent issues with high priority",
            model,
            StrategyFactory.parallel(), // Use parallel for faster processing
            "URGENT ALERT: Process this high-priority issue immediately.",
            "Provide immediate action steps and escalation if needed."
        );
        
        Route<String, String> errorRoute = AgentWorkflowFactory.createStringRoute(
            "error",
            "Handles error reports and debugging",
            model,
            "You are a debugging specialist. Analyze this error and provide solutions."
        );
        
        Route<String, String> questionRoute = AgentWorkflowFactory.createStringRoute(
            "question",
            "Handles questions and help requests",
            model,
            "You are a helpful assistant. Answer this question clearly and thoroughly."
        );
        
        // Create rule-based router
        RuleBasedContentRouter<String> ruleRouter = RuleBasedContentRouter.<String>builder()
            .addRegexRule("urgent", Pattern.compile("\\b(urgent|emergency|critical|asap)\\b", Pattern.CASE_INSENSITIVE), 0.95, 10, "Urgent keywords detected")
            .addRegexRule("error", Pattern.compile("\\b(error|exception|bug|crash|fail)\\b", Pattern.CASE_INSENSITIVE), 0.85, 8, "Error-related keywords")
            .addKeywordRule("question", List.of("how", "what", "why", "when", "where", "help"), 0.7, 5, "Question indicators")
            .defaultRoute("question")
            .enableMultipleMatches(true)
            .build();
        
        RoutingWorkflow<String, String> ruleWorkflow = RoutingWorkflow.<String, String>builder()
            .name("RuleBasedRouting")
            .router(ruleRouter)
            .addRoute(urgentRoute)
            .addRoute(errorRoute)
            .addRoute(questionRoute)
            .build();
        
        String[] testInputs = {
            "URGENT: Production server is down and customers cannot access the system!",
            "I'm getting a NullPointerException error when running the application",
            "How do I configure the database connection settings?",
            "The website crashed again with a 500 error - this is critical!"
        };
        
        for (String input : testInputs) {
            System.out.println("Input: " + input);
            
            Map<String, Object> context = new HashMap<>();
            StatefulWorkflowResult<String> result = ruleWorkflow.start(input, context);
            
            if (result.isCompleted()) {
                RoutingDecision decision = (RoutingDecision) result.getMetadata().get("routing_decision");
                System.out.println("Route: " + decision.getSelectedRoute());
                System.out.println("Confidence: " + decision.getConfidence());
                System.out.println("Reasoning: " + decision.getReasoning());
                System.out.println("Result: " + result.getOutput().orElse("No output"));
            } else {
                System.out.println("Workflow did not complete successfully");
            }
            System.out.println();
        }
    }

    /**
     * Demonstrates hybrid routing combining LLM and rule-based approaches.
     */
    private static void demonstrateHybridRouting(ChatModel model) throws WorkflowExecutionException {
        System.out.println("--- Hybrid Routing (LLM + Rules) ---");
        
        // Create a specialized security route
        Route<String, String> securityRoute = AgentWorkflowFactory.createStringRoute(
            "security",
            "Handles security-related issues with specialized protocols",
            model,
            "SECURITY ALERT: Analyze this security issue and provide immediate response protocols.",
            "Document the security incident and provide remediation steps."
        );
        
        // First check with rules for security keywords (fast, deterministic)
        RuleBasedContentRouter<String> securityRouter = RuleBasedContentRouter.<String>builder()
            .addRegexRule("security", Pattern.compile("\\b(security|breach|hack|attack|vulnerability|malware)\\b", Pattern.CASE_INSENSITIVE), 0.9)
            .build();
        
        // For non-security issues, use LLM for intelligent classification
        LLMContentRouter<String> llmRouter = LLMContentRouter.<String>builder()
            .model(model)
            .classificationPrompt("Classify this input for general support routing")
            .build();
        
        // Create hybrid workflow (simplified - using primary router)
        RoutingWorkflow<String, String> hybridWorkflow = RoutingWorkflow.<String, String>builder()
            .name("HybridRouting")
            .router(securityRouter) // Primary: fast rule-based security detection
            .addRoute(securityRoute)
            .build();
        
        String[] testInputs = {
            "We detected a potential security breach in our database",
            "Someone is trying to hack into our system with multiple login attempts",
            "Regular customer inquiry about account balance"
        };
        
        for (String input : testInputs) {
            System.out.println("Input: " + input);
            
            Map<String, Object> context = new HashMap<>();
            
            // Try security router first
            try {
                StatefulWorkflowResult<String> result = hybridWorkflow.start(input, context);
                if (result.isCompleted()) {
                    System.out.println("Security Route Result: " + result.getOutput().orElse("No output"));
                } else {
                    System.out.println("Security workflow did not complete successfully");
                }
            } catch (Exception e) {
                System.out.println("No security match - would route to LLM classifier");
            }
            
            System.out.println();
        }
    }

    /**
     * Creates a mock ChatModel for demonstration purposes.
     */
    private static ChatModel createMockChatModel() {
        // In a real implementation, you would create an actual ChatModel
        // For this example, we'll return null since our demo nodes don't use it
        return null;
    }

    /**
     * Mock agent node for demonstration.
     */
    private static class MockAgentNode implements AgentNode<String, String> {
        private final String name;
        private final String responseTemplate;

        public MockAgentNode(String name, String responseTemplate) {
            this.name = name;
            this.responseTemplate = responseTemplate;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String process(String input, Map<String, Object> context) {
            return responseTemplate + " [Processed: " + input.substring(0, Math.min(50, input.length())) + "...]";
        }
    }
}