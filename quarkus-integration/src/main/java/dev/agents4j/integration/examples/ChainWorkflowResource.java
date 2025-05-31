package dev.agents4j.integration.examples;

import dev.agents4j.langchain4j.facade.ChainWorkflows;
import dev.agents4j.langchain4j.impl.StringLangChain4JAgentNode;
import dev.agents4j.workflow.ChainWorkflow;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * REST endpoints for Chain Workflow pattern demonstrations.
 */
@Path("/chain-workflow")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChainWorkflowResource {

    @Inject
    ChatModel chatModel;

    @Inject
    ChainWorkflowExample chainWorkflowExample;

    @ConfigProperty(name = "agents4j.workflows.enabled", defaultValue = "true")
    boolean workflowsEnabled;

    /**
     * Simple chain workflow query
     */
    @POST
    @Path("/simple")
    public Response simpleChainQuery(SimpleChainRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Map.of("error", "Workflows are disabled"))
                .build();
        }

        try {
            String result = ChainWorkflows.executeSimple(
                chatModel,
                request.getSystemPrompt(),
                request.getQuery()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("query", request.getQuery());
            response.put("result", result);
            response.put("workflow_type", "simple_chain");

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    Map.of(
                        "error",
                        "Failed to execute simple chain query: " +
                        e.getMessage()
                    )
                )
                .build();
        }
    }

    /**
     * Complex chain workflow with multiple agents
     */
    @POST
    @Path("/complex")
    public Response complexChainQuery(ComplexChainRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Map.of("error", "Workflows are disabled"))
                .build();
        }

        try {
            String result = ChainWorkflows.executeComplex(
                chatModel,
                request.getQuery(),
                request.getSystemPrompts().toArray(new String[0])
            );

            Map<String, Object> response = new HashMap<>();
            response.put("query", request.getQuery());
            response.put("result", result);
            response.put("workflow_type", "complex_chain");
            response.put("agents_count", request.getSystemPrompts().size());

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    Map.of(
                        "error",
                        "Failed to execute complex chain query: " +
                        e.getMessage()
                    )
                )
                .build();
        }
    }

    /**
     * Three whys analysis workflow
     */
    @POST
    @Path("/three-whys")
    public Response threeWhysAnalysis(ThreeWhysRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Map.of("error", "Workflows are disabled"))
                .build();
        }

        try {
            ChainWorkflow<String, String> workflow = createThreeWhysWorkflow();
            String result = workflow.execute(request.getQuestion());

            Map<String, Object> response = new HashMap<>();
            response.put("question", request.getQuestion());
            response.put("result", result);
            response.put("workflow_type", "three_whys");
            response.put("analysis_depth", 3);

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    Map.of(
                        "error",
                        "Failed to execute three whys analysis: " +
                        e.getMessage()
                    )
                )
                .build();
        }
    }

    /**
     * Conversational workflow with memory
     */
    @POST
    @Path("/conversational")
    public Response conversationalQuery(ConversationalRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Map.of("error", "Workflows are disabled"))
                .build();
        }

        try {
            ChainWorkflow<String, String> workflow =
                ChainWorkflows.createConversational(
                    "ConversationalWorkflow",
                    chatModel,
                    request.getMaxMessages() != null
                        ? request.getMaxMessages()
                        : 10,
                    request.getSystemPrompts().toArray(new String[0])
                );

            String result = workflow.execute(request.getQuery());

            Map<String, Object> response = new HashMap<>();
            response.put("query", request.getQuery());
            response.put("result", result);
            response.put("workflow_type", "conversational");
            response.put(
                "max_messages",
                request.getMaxMessages() != null ? request.getMaxMessages() : 10
            );

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    Map.of(
                        "error",
                        "Failed to execute conversational query: " +
                        e.getMessage()
                    )
                )
                .build();
        }
    }

    /**
     * Get predefined chain workflow examples
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
                case "simple":
                    result = chainWorkflowExample.runSimpleChainExample();
                    break;
                case "complex":
                    result = chainWorkflowExample.runComplexChainExample();
                    break;
                case "three-whys":
                    result = chainWorkflowExample.runThreeWhysExample();
                    break;
                case "conversational":
                    result = chainWorkflowExample.runConversationalExample();
                    break;
                default:
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(
                            Map.of(
                                "error",
                                "Unknown example type: " + exampleType
                            )
                        )
                        .build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("example_type", exampleType);
            response.put("result", result);

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    Map.of(
                        "error",
                        "Failed to execute example: " + e.getMessage()
                    )
                )
                .build();
        }
    }

    /**
     * Get available chain workflow types
     */
    @GET
    @Path("/types")
    public Response getChainWorkflowTypes() {
        Map<String, Object> types = new HashMap<>();
        types.put("simple_chain", "Single agent processing");
        types.put("complex_chain", "Multiple agents in sequence");
        types.put("three_whys", "Deep analysis through three why questions");
        types.put("conversational", "Chain with conversation memory");

        Map<String, Object> examples = new HashMap<>();
        examples.put(
            "available_examples",
            List.of("simple", "complex", "three-whys", "conversational")
        );
        examples.put("workflow_types", types);

        return Response.ok(examples).build();
    }

    /**
     * Health check for chain workflows
     */
    @GET
    @Path("/health")
    public Response getHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        health.put("enabled", workflowsEnabled);
        health.put("chat_model_available", chatModel != null);

        if (workflowsEnabled && chatModel != null) {
            health.put("status", "healthy");
        } else {
            health.put("status", "unhealthy");
        }

        return Response.ok(health).build();
    }

    private ChainWorkflow<String, String> createThreeWhysWorkflow() {
        var firstWhyNode = StringLangChain4JAgentNode.builder()
            .name("FirstWhyNode")
            .model(chatModel)
            .systemPrompt(
                "You are a thoughtful assistant that helps people understand topics more deeply. " +
                "Your task is to analyze the user's question and ask a fundamental 'why' question about it. " +
                "Provide a brief response to the original question first, and then ask your 'why' question."
            )
            .build();

        var secondWhyNode = StringLangChain4JAgentNode.builder()
            .name("SecondWhyNode")
            .model(chatModel)
            .systemPrompt(
                "You are a thoughtful assistant continuing a chain of inquiry. " +
                "The input will contain an initial question, an analysis, and a first 'why' question. " +
                "Your task is to answer the first 'why' question thoroughly and then ask a deeper, second 'why' question " +
                "that explores the underlying principles or causes."
            )
            .build();

        var thirdWhyNode = StringLangChain4JAgentNode.builder()
            .name("ThirdWhyNode")
            .model(chatModel)
            .systemPrompt(
                "You are a thoughtful assistant continuing a chain of inquiry. " +
                "The input will contain an initial question, previous analyses, and a second 'why' question. " +
                "Your task is to answer the second 'why' question thoroughly and then ask a third, even deeper 'why' question " +
                "that explores the most fundamental aspects of the topic."
            )
            .build();

        var finalNode = StringLangChain4JAgentNode.builder()
            .name("FinalNode")
            .model(chatModel)
            .systemPrompt(
                "You are a thoughtful assistant finalizing a chain of inquiry. " +
                "The input will contain an initial question and a series of 'why' questions and answers. " +
                "Your task is to answer the third 'why' question thoroughly and then provide a comprehensive summary " +
                "that ties together all the insights from this chain of inquiry. " +
                "Include the original question and how this deep analysis helps us understand it better."
            )
            .build();

        return ChainWorkflow.<String, String>builder()
            .name("ThreeWhysWorkflow")
            .firstNode(firstWhyNode)
            .node(secondWhyNode)
            .node(thirdWhyNode)
            .node(finalNode)
            .build();
    }

    // Request DTOs
    public static class SimpleChainRequest {

        private String query;
        private String systemPrompt;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }
    }

    public static class ComplexChainRequest {

        private String query;
        private List<String> systemPrompts;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public List<String> getSystemPrompts() {
            return systemPrompts;
        }

        public void setSystemPrompts(List<String> systemPrompts) {
            this.systemPrompts = systemPrompts;
        }
    }

    public static class ThreeWhysRequest {

        private String question;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }

    public static class ConversationalRequest {

        private String query;
        private List<String> systemPrompts;
        private Integer maxMessages;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public List<String> getSystemPrompts() {
            return systemPrompts;
        }

        public void setSystemPrompts(List<String> systemPrompts) {
            this.systemPrompts = systemPrompts;
        }

        public Integer getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(Integer maxMessages) {
            this.maxMessages = maxMessages;
        }
    }
}
