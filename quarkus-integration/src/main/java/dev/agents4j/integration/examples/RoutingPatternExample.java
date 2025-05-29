package dev.agents4j.integration.examples;


import dev.langchain4j.model.chat.ChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;



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

    @ConfigProperty(name = "agents4j.routing.enabled", defaultValue = "true")
    boolean routingEnabled;

    public RoutingPatternExample() {
        // Default constructor for CDI
    }

    /**
     * Example 1: Customer support ticket routing simulation
     */
    public String customerSupportRoutingExample() {
        if (!routingEnabled || chatModel == null) {
            return "Routing workflow is disabled or ChatModel not available";
        }

        System.out.println("\n=== Customer Support Routing ===");
        
        try {
            String[] testTickets = {
                "My application keeps crashing with error code 500 when I try to save data",
                "I was charged twice for my subscription and need a refund",
                "How do I change my password?",
                "URGENT: Our entire production system is down and we're losing money"
            };
            
            StringBuilder results = new StringBuilder();
            for (String ticket : testTickets) {
                String route = classifyTicket(ticket);
                String response = processTicket(ticket, route);
                
                results.append("Ticket: ").append(ticket).append("\n");
                results.append("Route: ").append(route).append("\n");
                results.append("Response: ").append(response).append("\n\n");
                
                System.out.println("Ticket: " + ticket);
                System.out.println("Route: " + route);
                System.out.println("Response: " + response);
                System.out.println();
            }
            
            return results.toString();
            
        } catch (Exception e) {
            System.err.println("Customer support routing failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Example 2: Content categorization simulation
     */
    public String contentCategorizationExample() {
        if (!routingEnabled) {
            return "Routing workflow is disabled";
        }

        System.out.println("\n=== Content Categorization ===");
        
        try {
            String[] testContent = {
                "Latest breakthrough in machine learning algorithms shows 40% improvement in accuracy",
                "Company announces new marketing strategy to increase revenue by 25%",
                "Scientists discover new species in deep ocean research expedition",
                "Popular music festival announces lineup with top celebrities"
            };
            
            StringBuilder results = new StringBuilder();
            for (String content : testContent) {
                String category = categorizeContent(content);
                String processed = processContent(content, category);
                
                results.append("Content: ").append(content).append("\n");
                results.append("Category: ").append(category).append("\n");
                results.append("Processed: ").append(processed).append("\n\n");
                
                System.out.println("Content: " + content);
                System.out.println("Category: " + category);
                System.out.println("Processed: " + processed);
                System.out.println();
            }
            
            return results.toString();
            
        } catch (Exception e) {
            System.err.println("Content categorization failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Example 3: Multi-language routing simulation
     */
    public String multiLanguageRoutingExample() {
        if (!routingEnabled) {
            return "Routing workflow is disabled";
        }

        System.out.println("\n=== Multi-Language Routing ===");
        
        try {
            String[] testTexts = {
                "Hello, I need help with my account settings and password reset",
                "Hola, necesito ayuda con la configuración de mi cuenta",
                "Bonjour, j'ai besoin d'aide avec les paramètres de mon compte",
                "こんにちは、アカウント設定のヘルプが必要です"
            };
            
            StringBuilder results = new StringBuilder();
            for (String text : testTexts) {
                String language = detectLanguage(text);
                String response = processLanguageSpecific(text, language);
                
                results.append("Text: ").append(text).append("\n");
                results.append("Language: ").append(language).append("\n");
                results.append("Response: ").append(response).append("\n\n");
                
                System.out.println("Text: " + text);
                System.out.println("Language: " + language);
                System.out.println("Response: " + response);
                System.out.println();
            }
            
            return results.toString();
            
        } catch (Exception e) {
            System.err.println("Multi-language routing failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Example 4: Rule-based routing simulation
     */
    public String ruleBasedRoutingExample() {
        if (!routingEnabled) {
            return "Routing workflow is disabled";
        }

        System.out.println("\n=== Rule-Based Routing ===");
        
        try {
            String[] testInputs = {
                "URGENT: Production server is down and customers cannot access the system!",
                "I'm getting a NullPointerException error when running the application",
                "How do I configure the database connection settings?",
                "The website crashed again with a 500 error - this is critical!"
            };
            
            StringBuilder results = new StringBuilder();
            for (String input : testInputs) {
                String route = classifyByRules(input);
                String response = processRuleBased(input, route);
                
                results.append("Input: ").append(input).append("\n");
                results.append("Route: ").append(route).append("\n");
                results.append("Response: ").append(response).append("\n\n");
                
                System.out.println("Input: " + input);
                System.out.println("Route: " + route);
                System.out.println("Response: " + response);
                System.out.println();
            }
            
            return results.toString();
            
        } catch (Exception e) {
            System.err.println("Rule-based routing failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Example 5: Hybrid routing simulation (rules + LLM)
     */
    public String hybridRoutingExample() {
        if (!routingEnabled) {
            return "Routing workflow is disabled";
        }

        System.out.println("\n=== Hybrid Routing ===");
        
        try {
            String[] testInputs = {
                "We detected a potential security breach in our database",
                "Someone is trying to hack into our system with multiple login attempts",
                "Regular customer inquiry about account balance"
            };
            
            StringBuilder results = new StringBuilder();
            for (String input : testInputs) {
                String route = hybridClassify(input);
                String response = processHybrid(input, route);
                
                results.append("Input: ").append(input).append("\n");
                results.append("Route: ").append(route).append("\n");
                results.append("Response: ").append(response).append("\n\n");
                
                System.out.println("Input: " + input);
                System.out.println("Route: " + route);
                System.out.println("Response: " + response);
                System.out.println();
            }
            
            return results.toString();
            
        } catch (Exception e) {
            System.err.println("Hybrid routing failed: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Health check for routing workflows
     */
    public boolean isWorkflowHealthy() {
        return routingEnabled && chatModel != null;
    }

    /**
     * Run all routing examples
     */
    public void runAllExamples() {
        System.out.println("Routing Pattern Examples");
        System.out.println("========================");
        
        if (!isWorkflowHealthy()) {
            System.out.println("Workflow is not healthy, skipping examples");
            return;
        }

        customerSupportRoutingExample();
        contentCategorizationExample();
        multiLanguageRoutingExample();
        ruleBasedRoutingExample();
        hybridRoutingExample();
        
        System.out.println("\nAll routing examples completed!");
    }

    // Helper methods for simulation

    private String classifyTicket(String ticket) {
        if (ticket.toLowerCase().contains("error") || ticket.toLowerCase().contains("crash")) {
            return "technical";
        } else if (ticket.toLowerCase().contains("charge") || ticket.toLowerCase().contains("billing")) {
            return "billing";
        } else if (ticket.toLowerCase().contains("urgent") || ticket.toLowerCase().contains("down")) {
            return "escalation";
        } else {
            return "general";
        }
    }

    private String processTicket(String ticket, String route) {
        switch (route) {
            case "technical":
                return "Technical specialist will investigate the error and provide a solution.";
            case "billing":
                return "Billing department will review your charges and process any refunds.";
            case "escalation":
                return "Escalated to senior support team for immediate attention.";
            default:
                return "General support team will assist you with your inquiry.";
        }
    }

    private String categorizeContent(String content) {
        String lower = content.toLowerCase();
        if (lower.contains("machine learning") || lower.contains("algorithm") || lower.contains("technology")) {
            return "technology";
        } else if (lower.contains("marketing") || lower.contains("revenue") || lower.contains("business")) {
            return "business";
        } else if (lower.contains("research") || lower.contains("scientist") || lower.contains("discovery")) {
            return "science";
        } else if (lower.contains("music") || lower.contains("festival") || lower.contains("celebrity")) {
            return "entertainment";
        } else {
            return "general";
        }
    }

    private String processContent(String content, String category) {
        return "Content processed by " + category + " specialist: Analysis completed";
    }

    private String detectLanguage(String text) {
        if (text.contains("Hola") || text.contains("necesito")) {
            return "spanish";
        } else if (text.contains("Bonjour") || text.contains("j'ai besoin")) {
            return "french";
        } else if (text.contains("こんにちは")) {
            return "japanese";
        } else {
            return "english";
        }
    }

    private String processLanguageSpecific(String text, String language) {
        switch (language) {
            case "spanish":
                return "Procesado por especialista en español";
            case "french":
                return "Traité par un spécialiste français";
            case "japanese":
                return "日本語スペシャリストが処理しました";
            default:
                return "Processed by English language specialist";
        }
    }

    private String classifyByRules(String input) {
        String lower = input.toLowerCase();
        if (lower.contains("urgent") || lower.contains("critical") || lower.contains("down")) {
            return "urgent";
        } else if (lower.contains("error") || lower.contains("exception") || lower.contains("crash")) {
            return "error";
        } else {
            return "question";
        }
    }

    private String processRuleBased(String input, String route) {
        switch (route) {
            case "urgent":
                return "URGENT: Escalated to priority support team for immediate action";
            case "error":
                return "Error analysis: Technical team will investigate and provide solution";
            default:
                return "Standard support: Question answered by support team";
        }
    }

    private String hybridClassify(String input) {
        // First check security rules (fast)
        String lower = input.toLowerCase();
        if (lower.contains("security") || lower.contains("breach") || lower.contains("hack")) {
            return "security";
        }
        // Fallback to general classification
        return "general";
    }

    private String processHybrid(String input, String route) {
        if ("security".equals(route)) {
            return "SECURITY ALERT: Processed by security team with immediate protocols";
        } else {
            return "Processed by general support team";
        }
    }


}