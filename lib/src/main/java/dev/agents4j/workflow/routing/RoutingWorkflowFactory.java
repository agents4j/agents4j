package dev.agents4j.workflow.routing;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.routing.ContentRouter;
import dev.agents4j.api.routing.Route;
import dev.agents4j.api.strategy.WorkflowExecutionStrategy;
import dev.agents4j.impl.StringLangChain4JAgentNode;
import dev.agents4j.workflow.strategy.StrategyFactory;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Factory for creating routing workflows with common patterns and configurations.
 * 
 * <p>This factory provides convenient methods for constructing routing workflows
 * for common use cases such as customer support, content categorization, and
 * multi-language processing. It integrates with both LLM-based and rule-based
 * routers to provide flexible routing solutions.</p>
 * 
 * <p><b>Key Features:</b></p>
 * <ul>
 * <li>Pre-configured routing patterns for common use cases</li>
 * <li>Integration with Strategy Pattern for route execution</li>
 * <li>Support for both LLM-based and rule-based routing</li>
 * <li>Fluent API for complex routing workflow construction</li>
 * </ul>
 */
public class RoutingWorkflowFactory {

    /**
     * Creates a customer support routing workflow using LLM-based classification.
     *
     * @param name The workflow name
     * @param model The ChatModel to use for routing and processing
     * @param classificationPrompt The prompt for ticket classification
     * @return A new RoutingWorkflow for customer support
     */
    public static RoutingWorkflow<String, String> createCustomerSupportWorkflow(
        String name,
        ChatModel model,
        String classificationPrompt
    ) {
        // Create LLM router for intelligent classification
        LLMContentRouter<String> router = LLMContentRouter.<String>builder()
            .model(model)
            .classificationPrompt(classificationPrompt)
            .includeConfidenceScoring(true)
            .includeReasoning(true)
            .includeAlternatives(true)
            .build();

        // Create specialized routes for different support categories
        Route<String, String> technicalRoute = createTechnicalSupportRoute(model);
        Route<String, String> billingRoute = createBillingSupportRoute(model);
        Route<String, String> generalRoute = createGeneralSupportRoute(model);
        Route<String, String> escalationRoute = createEscalationRoute(model);

        return RoutingWorkflow.<String, String>builder()
            .name(name)
            .router(router)
            .addRoute(technicalRoute)
            .addRoute(billingRoute)
            .addRoute(generalRoute)
            .fallbackRoute(escalationRoute)
            .confidenceThreshold(0.7)
            .enableFallbackOnLowConfidence(true)
            .enableRouteAnalytics(true)
            .build();
    }

    /**
     * Creates a content categorization workflow using rule-based routing.
     *
     * @param name The workflow name
     * @param model The ChatModel to use for processing
     * @param categoryRules Map of category names to their keyword patterns
     * @return A new RoutingWorkflow for content categorization
     */
    public static RoutingWorkflow<String, String> createContentCategorizationWorkflow(
        String name,
        ChatModel model,
        Map<String, List<String>> categoryRules
    ) {
        // Create rule-based router for fast categorization
        RuleBasedContentRouter<String> router = RuleBasedContentRouter.<String>builder()
            .enableMultipleMatches(true)
            .maxAlternatives(3)
            .defaultRoute("general")
            .defaultConfidence(0.6)
            .build();

        // Add keyword rules for each category
        for (Map.Entry<String, List<String>> entry : categoryRules.entrySet()) {
            router = RuleBasedContentRouter.<String>builder()
                .addKeywordRule(entry.getKey(), entry.getValue(), 0.8)
                .build();
        }

        // Create routes for each category
        RoutingWorkflow.Builder<String, String> builder = RoutingWorkflow.<String, String>builder()
            .name(name)
            .router(router);

        for (String category : categoryRules.keySet()) {
            Route<String, String> route = createCategoryProcessingRoute(category, model);
            builder.addRoute(route);
        }

        // Add general route as fallback
        Route<String, String> generalRoute = createGeneralProcessingRoute(model);
        builder.fallbackRoute(generalRoute);

        return builder.build();
    }

    /**
     * Creates a multi-language routing workflow.
     *
     * @param name The workflow name
     * @param model The ChatModel to use for processing
     * @param supportedLanguages List of supported language codes
     * @return A new RoutingWorkflow for multi-language processing
     */
    public static RoutingWorkflow<String, String> createMultiLanguageWorkflow(
        String name,
        ChatModel model,
        List<String> supportedLanguages
    ) {
        // Create language detection router
        RuleBasedContentRouter<String> router = RuleBasedContentRouter.<String>builder()
            .addRegexRule("english", Pattern.compile("\\b(the|and|or|but|is|are|was|were)\\b", Pattern.CASE_INSENSITIVE), 0.8)
            .addRegexRule("spanish", Pattern.compile("\\b(el|la|y|o|pero|es|son|fue|fueron)\\b", Pattern.CASE_INSENSITIVE), 0.8)
            .addRegexRule("french", Pattern.compile("\\b(le|la|et|ou|mais|est|sont|était|étaient)\\b", Pattern.CASE_INSENSITIVE), 0.8)
            .defaultRoute("translation")
            .defaultConfidence(0.5)
            .build();

        RoutingWorkflow.Builder<String, String> builder = RoutingWorkflow.<String, String>builder()
            .name(name)
            .router(router);

        // Create language-specific routes
        for (String language : supportedLanguages) {
            Route<String, String> languageRoute = createLanguageProcessingRoute(language, model);
            builder.addRoute(languageRoute);
        }

        // Add translation route as fallback
        Route<String, String> translationRoute = createTranslationRoute(model);
        builder.fallbackRoute(translationRoute);

        return builder.build();
    }

    /**
     * Creates a hybrid routing workflow using both LLM and rule-based routing.
     *
     * @param name The workflow name
     * @param model The ChatModel to use
     * @param primaryRouter The primary router to use
     * @param fallbackRouter The fallback router if primary fails
     * @param routes The routes to add to the workflow
     * @return A new RoutingWorkflow with hybrid routing
     */
    @SafeVarargs
    public static RoutingWorkflow<String, String> createHybridRoutingWorkflow(
        String name,
        ChatModel model,
        ContentRouter<String> primaryRouter,
        ContentRouter<String> fallbackRouter,
        Route<String, String>... routes
    ) {
        // For now, use primary router - hybrid logic can be enhanced later
        RoutingWorkflow.Builder<String, String> builder = RoutingWorkflow.<String, String>builder()
            .name(name)
            .router(primaryRouter);

        for (Route<String, String> route : routes) {
            builder.addRoute(route);
        }

        return builder.build();
    }

    /**
     * Creates a simple routing workflow with custom router and routes.
     *
     * @param <I> The input type
     * @param <O> The output type
     * @param name The workflow name
     * @param router The content router to use
     * @param routes The routes to add
     * @return A new RoutingWorkflow instance
     */
    @SafeVarargs
    public static <I, O> RoutingWorkflow<I, O> createSimpleRoutingWorkflow(
        String name,
        ContentRouter<I> router,
        Route<I, O>... routes
    ) {
        RoutingWorkflow.Builder<I, O> builder = RoutingWorkflow.<I, O>builder()
            .name(name)
            .router(router);

        for (Route<I, O> route : routes) {
            builder.addRoute(route);
        }

        return builder.build();
    }

    // Helper methods for creating specific routes

    /**
     * Creates a technical support route.
     */
    private static Route<String, String> createTechnicalSupportRoute(ChatModel model) {
        AgentNode<String, String> analyzerNode = createStringNode(
            "TechnicalAnalyzer",
            model,
            "You are a technical support specialist. Analyze the technical issue and provide detailed troubleshooting steps."
        );

        AgentNode<String, String> responseNode = createStringNode(
            "TechnicalResponder",
            model,
            "Based on the technical analysis, provide a clear, step-by-step solution to resolve the issue."
        );

        return Route.<String, String>builder()
            .id("technical")
            .description("Handles technical support requests with error analysis and troubleshooting")
            .addNode(analyzerNode)
            .addNode(responseNode)
            .strategy(StrategyFactory.sequential())
            .priority(10)
            .addTag("support")
            .addTag("technical")
            .confidenceThreshold(0.7)
            .build();
    }

    /**
     * Creates a billing support route.
     */
    private static Route<String, String> createBillingSupportRoute(ChatModel model) {
        AgentNode<String, String> billingNode = createStringNode(
            "BillingSpecialist",
            model,
            "You are a billing specialist. Handle billing inquiries, payment issues, and account questions with accuracy and empathy."
        );

        return Route.<String, String>builder()
            .id("billing")
            .description("Handles billing, payment, and account-related inquiries")
            .addNode(billingNode)
            .strategy(StrategyFactory.sequential())
            .priority(8)
            .addTag("support")
            .addTag("billing")
            .confidenceThreshold(0.6)
            .build();
    }

    /**
     * Creates a general support route.
     */
    private static Route<String, String> createGeneralSupportRoute(ChatModel model) {
        AgentNode<String, String> generalNode = createStringNode(
            "GeneralSupport",
            model,
            "You are a helpful customer support representative. Provide friendly, professional assistance for general inquiries."
        );

        return Route.<String, String>builder()
            .id("general")
            .description("Handles general customer inquiries and support requests")
            .addNode(generalNode)
            .strategy(StrategyFactory.sequential())
            .priority(5)
            .addTag("support")
            .addTag("general")
            .confidenceThreshold(0.4)
            .build();
    }

    /**
     * Creates an escalation route for complex issues.
     */
    private static Route<String, String> createEscalationRoute(ChatModel model) {
        AgentNode<String, String> escalationNode = createStringNode(
            "EscalationHandler",
            model,
            "This inquiry requires escalation to a human agent. Provide a professional acknowledgment and next steps."
        );

        return Route.<String, String>builder()
            .id("escalation")
            .description("Handles complex issues that require human escalation")
            .addNode(escalationNode)
            .strategy(StrategyFactory.sequential())
            .priority(1)
            .addTag("escalation")
            .confidenceThreshold(0.0)
            .build();
    }

    /**
     * Creates a category processing route.
     */
    private static Route<String, String> createCategoryProcessingRoute(String category, ChatModel model) {
        AgentNode<String, String> processorNode = createStringNode(
            category + "Processor",
            model,
            "You are a specialist in " + category + " content. Process and categorize the content appropriately."
        );

        return Route.<String, String>builder()
            .id(category)
            .description("Processes " + category + " content with specialized handling")
            .addNode(processorNode)
            .strategy(StrategyFactory.sequential())
            .priority(5)
            .addTag("category")
            .addTag(category)
            .build();
    }

    /**
     * Creates a general processing route.
     */
    private static Route<String, String> createGeneralProcessingRoute(ChatModel model) {
        AgentNode<String, String> processorNode = createStringNode(
            "GeneralProcessor",
            model,
            "Process this content using general guidelines and best practices."
        );

        return Route.<String, String>builder()
            .id("general")
            .description("General content processing for uncategorized content")
            .addNode(processorNode)
            .strategy(StrategyFactory.sequential())
            .priority(1)
            .addTag("general")
            .build();
    }

    /**
     * Creates a language-specific processing route.
     */
    private static Route<String, String> createLanguageProcessingRoute(String language, ChatModel model) {
        AgentNode<String, String> languageNode = createStringNode(
            language + "Processor",
            model,
            "Process this " + language + " content with appropriate cultural and linguistic considerations."
        );

        return Route.<String, String>builder()
            .id(language)
            .description("Processes content in " + language + " with cultural awareness")
            .addNode(languageNode)
            .strategy(StrategyFactory.sequential())
            .priority(8)
            .addTag("language")
            .addTag(language)
            .build();
    }

    /**
     * Creates a translation route.
     */
    private static Route<String, String> createTranslationRoute(ChatModel model) {
        AgentNode<String, String> translatorNode = createStringNode(
            "Translator",
            model,
            "Translate this content to English and then process it appropriately."
        );

        return Route.<String, String>builder()
            .id("translation")
            .description("Translates content to English before processing")
            .addNode(translatorNode)
            .strategy(StrategyFactory.sequential())
            .priority(3)
            .addTag("translation")
            .build();
    }

    /**
     * Creates a string processing agent node.
     */
    private static AgentNode<String, String> createStringNode(String name, ChatModel model, String systemPrompt) {
        return StringLangChain4JAgentNode.builder()
            .name(name)
            .model(model)
            .systemPrompt(systemPrompt)
            .build();
    }

    /**
     * Creates a string processing agent node with memory.
     */
    private static AgentNode<String, String> createStringNodeWithMemory(String name, ChatModel model, 
                                                                        String systemPrompt, ChatMemory memory) {
        return StringLangChain4JAgentNode.builder()
            .name(name)
            .model(model)
            .systemPrompt(systemPrompt)
            .memory(memory)
            .build();
    }

    // Private constructor to prevent instantiation
    private RoutingWorkflowFactory() {
        throw new UnsupportedOperationException("RoutingWorkflowFactory is a utility class and should not be instantiated");
    }
}