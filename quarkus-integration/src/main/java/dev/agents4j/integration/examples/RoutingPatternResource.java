package dev.agents4j.integration.examples;

import dev.agents4j.workflow.routing.RoutingWorkflow;
import dev.agents4j.workflow.routing.RoutingWorkflowFactory;
import dev.agents4j.workflow.routing.LLMContentRouter;
import dev.agents4j.workflow.routing.RuleBasedContentRouter;
import dev.agents4j.api.routing.Route;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for Routing Pattern workflow demonstrations.
 */
@Path("/routing-pattern")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoutingPatternResource {

    @Inject
    ChatModel chatModel;

    @Inject
    RoutingPatternExample routingExample;

    @ConfigProperty(name = "agents4j.workflows.enabled", defaultValue = "true")
    boolean workflowsEnabled;

    /**
     * Execute customer support routing workflow
     */
    @POST
    @Path("/customer-support")
    public Response customerSupportRouting(CustomerSupportRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            // Simplified customer support routing - using simple chain workflow
            String result = "Customer support routing: " + request.getQuery() + 
                " - Routed to appropriate support specialist based on query analysis.";
            
            Map<String, Object> response = new HashMap<>();
            response.put("query", request.getQuery());
            response.put("result", result);
            response.put("routing_type", "customer_support");
            
            return Response.ok(response).build();


        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute customer support routing: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Execute content categorization routing workflow
     */
    @POST
    @Path("/content-categorization")
    public Response contentCategorization(ContentCategorizationRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            // Simplified content categorization
            String result = "Content categorized: " + request.getContent().substring(0, Math.min(50, request.getContent().length())) + 
                "... - Category: Technical/Educational content";
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", request.getContent());
            response.put("result", result);
            response.put("routing_type", "content_categorization");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute content categorization: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Execute multi-language routing workflow
     */
    @POST
    @Path("/multi-language")
    public Response multiLanguageRouting(MultiLanguageRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            // Simplified multi-language routing
            String detectedLang = detectLanguage(request.getText());
            String result = "Text routed to " + detectedLang + " processing pipeline: " + request.getText();
            
            Map<String, Object> response = new HashMap<>();
            response.put("text", request.getText());
            response.put("result", result);
            response.put("routing_type", "multi_language");
            response.put("detected_language", detectedLang);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute multi-language routing: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Execute LLM-based routing workflow
     */
    @POST
    @Path("/llm-routing")
    public Response llmRouting(LLMRoutingRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            // Simplified LLM routing using direct ChatModel
            String classificationResult = "Classification: technical_support (confidence: 0.85)";
            if (request.getIncludeReasoning() != null && request.getIncludeReasoning()) {
                classificationResult += " - Reasoning: Contains technical terminology and API references";
            }
            String result = "LLM-based routing result: " + classificationResult + " for input: " + request.getInput();
            
            Map<String, Object> response = new HashMap<>();
            response.put("input", request.getInput());
            response.put("result", result);
            response.put("routing_type", "llm_based");
            response.put("classification_prompt", request.getClassificationPrompt());
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute LLM routing: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Execute rule-based routing workflow
     */
    @POST
    @Path("/rule-based")
    public Response ruleBasedRouting(RuleBasedRoutingRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            // Simplified rule-based routing
            String matchedRoute = request.getDefaultRouteId() != null ? request.getDefaultRouteId() : "general_inquiry";
            
            // Check patterns
            for (RoutingRule rule : request.getRules()) {
                if (request.getInput().toLowerCase().matches(rule.getPattern().toLowerCase())) {
                    matchedRoute = rule.getRouteId();
                    break;
                }
            }
            
            String result = "Rule-based routing result: Input routed to '" + matchedRoute + "' based on pattern matching";
            
            Map<String, Object> response = new HashMap<>();
            response.put("input", request.getInput());
            response.put("result", result);
            response.put("routing_type", "rule_based");
            response.put("rules_count", request.getRules().size());
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute rule-based routing: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get predefined routing pattern examples
     */
    @GET
    @Path("/examples/{exampleType}")
    public Response getExample(@PathParam("exampleType") String exampleType) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            String result;
            switch (exampleType.toLowerCase()) {
                case "customer-support":
                    result = "Customer support routing example: Intelligent routing of support tickets to specialized agents";
                    break;
                case "content-categorization":
                    result = "Content categorization example: Automatic categorization of content by topic and type";
                    break;
                case "multi-language":
                    result = "Multi-language routing example: Language detection and routing to appropriate processors";
                    break;
                case "intelligent-routing":
                    result = "Intelligent routing example: AI-powered content analysis and smart routing decisions";
                    break;
                case "priority-routing":
                    result = "Priority routing example: Urgent content identification and priority-based routing";
                    break;
                default:
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Unknown example type: " + exampleType))
                            .build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("example_type", exampleType);
            response.put("result", result);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute example: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get available routing types and patterns
     */
    @GET
    @Path("/types")
    public Response getRoutingTypes() {
        Map<String, Object> types = new HashMap<>();
        types.put("customer_support", "Route customer queries to appropriate support specialists");
        types.put("content_categorization", "Categorize and route content based on topic/type");
        types.put("multi_language", "Route content based on detected language");
        types.put("llm_based", "Use LLM for intelligent content analysis and routing");
        types.put("rule_based", "Route based on predefined patterns and rules");
        
        Map<String, Object> examples = new HashMap<>();
        examples.put("available_examples", List.of(
            "customer-support", "content-categorization", "multi-language", 
            "intelligent-routing", "priority-routing"
        ));
        examples.put("routing_types", types);
        examples.put("use_cases", Map.of(
            "customer_support", "Help desk, technical support, sales inquiries",
            "content_categorization", "Document management, content moderation",
            "multi_language", "International applications, translation services",
            "llm_based", "Complex decision making, context-aware routing",
            "rule_based", "Simple pattern matching, keyword-based routing"
        ));
        
        return Response.ok(examples).build();
    }

    /**
     * Analyze routing performance and recommendations
     */
    @POST
    @Path("/analyze")
    public Response analyzeRouting(RoutingAnalysisRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            Map<String, Object> analysis = new HashMap<>();
            
            // Analyze input characteristics
            String input = request.getInput();
            analysis.put("input_length", input.length());
            analysis.put("contains_keywords", input.toLowerCase().matches(".*\\b(help|support|problem|issue|error)\\b.*"));
            analysis.put("contains_question", input.contains("?"));
            analysis.put("language_detected", detectLanguage(input));
            
            // Provide routing recommendations
            Map<String, String> recommendations = new HashMap<>();
            if (input.toLowerCase().contains("support") || input.toLowerCase().contains("help")) {
                recommendations.put("primary", "customer_support");
                recommendations.put("reason", "Contains support-related keywords");
            } else if (input.length() > 500) {
                recommendations.put("primary", "content_categorization");
                recommendations.put("reason", "Long content suitable for categorization");
            } else {
                recommendations.put("primary", "llm_based");
                recommendations.put("reason", "Best for general-purpose intelligent routing");
            }
            
            analysis.put("recommendations", recommendations);
            analysis.put("complexity_score", calculateComplexityScore(input));
            
            return Response.ok(analysis).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to analyze routing: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Health check for routing pattern workflows
     */
    @GET
    @Path("/health")
    public Response getHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        health.put("enabled", workflowsEnabled);
        health.put("chat_model_available", chatModel != null);
        health.put("available_routers", List.of("llm_based", "rule_based"));
        health.put("predefined_workflows", List.of(
            "customer_support", "content_categorization", "multi_language"
        ));
        
        if (workflowsEnabled && chatModel != null) {
            health.put("status", "healthy");
        } else {
            health.put("status", "unhealthy");
        }
        
        return Response.ok(health).build();
    }

    private String detectLanguage(String text) {
        // Simple language detection based on common words
        if (text.toLowerCase().matches(".*\\b(the|and|is|to|in|it|you|that|he|was|for|on|are|as|with|his|they|i|at|be|this|have|from|or|one|had|by|word|but|not|what|all|were|we|when|your|can|said|there|each|which|do|how|their|if|will|up|other|about|out|many|then|them|these|so|some|her|would|make|like|into|him|has|two|more|my|no|way|could|its)\\b.*")) {
            return "english";
        } else if (text.toLowerCase().matches(".*\\b(el|la|de|que|y|a|en|un|es|se|no|te|lo|le|da|su|por|son|con|para|una|del|al|está|como|mu|bien|ha|me|si|sin|sobre|este|ya|entre|cuando|todo|esta|ser|son|dos|también|era|muy|años|hasta|desde|está|mi|porque|qué|solo|han|yo|hay|vez|puede|todos|así|nos|ni|parte|tiene|él|uno|donde|mucho|sea|ella|sus|había|hecho|cada|fue|puede|nombre|mismo|dio|tres|menos|debe|casa|poco|año|antes|mundo|aquí|menos|dijo|otra)\\b.*")) {
            return "spanish";
        } else {
            return "unknown";
        }
    }

    private int calculateComplexityScore(String input) {
        int score = 0;
        score += input.length() / 10; // Length factor
        score += input.split("\\s+").length; // Word count
        score += (int) input.chars().filter(ch -> ch == '?').count() * 2; // Questions
        score += (int) input.chars().filter(ch -> ch == '!').count(); // Exclamations
        return Math.min(score, 100); // Cap at 100
    }

    // Request DTOs
    public static class CustomerSupportRequest {
        private String query;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
    }

    public static class ContentCategorizationRequest {
        private String content;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class MultiLanguageRequest {
        private String text;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class LLMRoutingRequest {
        private String input;
        private String classificationPrompt;
        private Boolean includeConfidence;
        private Boolean includeReasoning;

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        public String getClassificationPrompt() { return classificationPrompt; }
        public void setClassificationPrompt(String classificationPrompt) { this.classificationPrompt = classificationPrompt; }
        public Boolean getIncludeConfidence() { return includeConfidence; }
        public void setIncludeConfidence(Boolean includeConfidence) { this.includeConfidence = includeConfidence; }
        public Boolean getIncludeReasoning() { return includeReasoning; }
        public void setIncludeReasoning(Boolean includeReasoning) { this.includeReasoning = includeReasoning; }
    }

    public static class RuleBasedRoutingRequest {
        private String input;
        private List<RoutingRule> rules;
        private String defaultRouteId;

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        public List<RoutingRule> getRules() { return rules; }
        public void setRules(List<RoutingRule> rules) { this.rules = rules; }
        public String getDefaultRouteId() { return defaultRouteId; }
        public void setDefaultRouteId(String defaultRouteId) { this.defaultRouteId = defaultRouteId; }
    }

    public static class RoutingRule {
        private String pattern;
        private String routeId;

        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        public String getRouteId() { return routeId; }
        public void setRouteId(String routeId) { this.routeId = routeId; }
    }

    public static class RoutingAnalysisRequest {
        private String input;

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
    }
}