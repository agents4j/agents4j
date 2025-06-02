package dev.agents4j.integration.examples;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.GraphCommand;
import dev.agents4j.api.graph.GraphCommandComplete;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.ExecutionError;
import dev.agents4j.api.result.error.WorkflowError;
import dev.agents4j.workflow.GraphWorkflowFactory;
import dev.agents4j.workflow.history.NodeInteraction;
import dev.agents4j.workflow.history.ProcessingHistory;
import dev.agents4j.workflow.history.ProcessingHistoryUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.logging.Level;

import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * A REST resource that demonstrates using GraphWorkflowFactory.createSequence
 * to create a workflow that responds to questions with snarky answers.
 */
@Path("/api/snarky")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SnarkyResponseResource {

    private static final Logger LOG = Logger.getLogger(
        SnarkyResponseResource.class
    );

    private final GraphWorkflow<QuestionRequest, String> snarkyWorkflow;

    @Inject
    public SnarkyResponseResource(ChatModel chatModel) {
        LOG.info("=== SnarkyResponseResource Constructor START ===");

        try {
            LOG.info(
                "Initializing SnarkyResponseResource with ChatModel: " +
                (chatModel != null
                        ? chatModel.getClass().getSimpleName()
                        : "NULL")
            );

            if (chatModel == null) {
                LOG.error(
                    "ChatModel is null - this will cause workflow failures"
                );
                throw new IllegalStateException("ChatModel cannot be null");
            }

            // Create the first node that processes the question with a snarky system prompt
            LOG.debug("Creating snarky-responder node...");
            GraphWorkflowNode<QuestionRequest> snarkyNode = new SequenceLLMNode<>(
                "snarky-responder",
                chatModel,
                "You are a snarky assistant. Reply with something snarky, witty, and slightly condescending, " +
                "but still ultimately helpful. Keep responses brief and punchy.",
                state -> {
                    String question = state.data().question();
                    LOG.debug(
                        "Snarky node processing question: '" +
                        question +
                        "'"
                    );
                    return question;
                }
            );
            LOG.debug("Snarky-responder node created successfully");

            // Create a follow-up node that adds a disclaimer
            LOG.debug("Creating disclaimer-adder node...");
            GraphWorkflowNode<QuestionRequest> disclaimerNode = new SequenceLLMNode<>(
                "disclaimer-adder",
                chatModel,
                "Add a brief, humorous disclaimer to the end of this snarky response. " +
                "Keep it short and don't change the original response.",
                state -> {
                    String previousOutput =
                        ProcessingHistoryUtils.getLatestOutputFromNode(
                            state,
                            "snarky-responder"
                        ).orElse("");
                    LOG.debug(
                        "Disclaimer node processing previous output: '" +
                        previousOutput +
                        "'"
                    );
                    return previousOutput;
                }
            );
            LOG.debug("Disclaimer-adder node created successfully");

            // Create a sequence workflow with the two nodes
            LOG.debug("Creating sequence workflow...");
            this.snarkyWorkflow = GraphWorkflowFactory.createSequence(
                "snarky-workflow",
                snarkyNode,
                disclaimerNode,
                QuestionRequest.class
            );
            LOG.info("Snarky workflow created successfully");
        } catch (Exception e) {
            LOG.error("Failed to initialize SnarkyResponseResource", e);
            throw new RuntimeException(
                "SnarkyResponseResource initialization failed",
                e
            );
        }

        LOG.info("=== SnarkyResponseResource Constructor SUCCESS ===");
    }

    /**
     * Processes a question and returns a snarky response.
     *
     * @param request The question request
     * @param headers HTTP headers for debugging
     * @param uriInfo URI information for debugging
     * @return A CompletionStage with the snarky response
     */
    @POST
    @Blocking
    public CompletionStage<Response> processQuestion(
        QuestionRequest request,
        @Context HttpHeaders headers,
        @Context UriInfo uriInfo
    ) {
        // Enhanced debugging information
        LOG.info("=== SnarkyResponseResource.processQuestion START ===");
        LOG.info("Request URI: " + uriInfo.getRequestUri());
        LOG.info("Request Method: POST");
        LOG.info("Content-Type: " + headers.getHeaderString("Content-Type"));
        LOG.info("Accept: " + headers.getHeaderString("Accept"));
        LOG.info("User-Agent: " + headers.getHeaderString("User-Agent"));

        // Log all headers for debugging
        LOG.debug("All request headers:");
        for (Map.Entry<String, List<String>> header : headers
            .getRequestHeaders()
            .entrySet()) {
            LOG.debug("  " + header.getKey() + ": " + header.getValue());
        }

        // Validate and log request
        if (request == null) {
            LOG.error("Request is null");
            return CompletableFuture.completedFuture(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(
                        new ErrorResponse(
                            "Invalid request",
                            "Request body is null or empty"
                        )
                    )
                    .build()
            );
        }

        if (request.question() == null) {
            LOG.error("Question field is null in request");
            return CompletableFuture.completedFuture(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(
                        new ErrorResponse(
                            "Invalid request",
                            "Question field is required and cannot be null"
                        )
                    )
                    .build()
            );
        }

        if (request.question().trim().isEmpty()) {
            LOG.error("Question field is empty in request");
            return CompletableFuture.completedFuture(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(
                        new ErrorResponse(
                            "Invalid request",
                            "Question field cannot be empty"
                        )
                    )
                    .build()
            );
        }

        LOG.info("Received valid question: '" + request.question() + "'");
        LOG.info(
            "Question length: " + request.question().length() + " characters"
        );

        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Starting workflow execution...");
                LOG.debug(
                    "Workflow instance: " +
                    snarkyWorkflow.getClass().getSimpleName()
                );

                // Execute the workflow with the question as input
                var workflowResult = snarkyWorkflow.start(request);

                LOG.info("Workflow execution completed");
                LOG.debug(
                    "Workflow result success: " + !workflowResult.isFailure()
                );

                LOG.debug(workflowResult);

                if (workflowResult.isFailure()) {
                    var error = workflowResult.getError().orElse(null);
                    LOG.error("Workflow execution failed");
                    if (error != null) {
                        LOG.error("Error message: " + error.message());
                        LOG.error(
                            "Error type: " + error.getClass().getSimpleName()
                        );
                        LOG.error(error);
                    }

                    return Response.status(
                        Response.Status.INTERNAL_SERVER_ERROR
                    )
                        .entity(
                            new ErrorResponse(
                                "Workflow execution failed",
                                workflowResult
                                    .getError()
                                    .map(error2 -> error2.message())
                                    .orElse("Unknown error")
                            )
                        )
                        .build();
                }

                String snarkyResponse = workflowResult.getOrElse(
                    "No response generated"
                );
                LOG.info("Generated snarky response: '" + snarkyResponse + "'");

                // Extract processing history from the final workflow context
                List<NodeInteractionResponse> interactions = workflowResult
                    .getFinalContext()
                    .flatMap(context ->
                        context.get(ProcessingHistory.HISTORY_KEY)
                    )
                    .map(history -> {
                        LOG.debug(
                            "Processing history contains " +
                            history.getAllInteractions().size() +
                            " interactions"
                        );
                        return history
                            .getAllInteractions()
                            .stream()
                            .map(interaction -> {
                                LOG.debug(
                                    "Node interaction: " +
                                    interaction.nodeId().value() +
                                    " -> " +
                                    interaction.output()
                                );
                                return new NodeInteractionResponse(
                                    interaction.nodeId().value(),
                                    interaction.nodeName(),
                                    interaction.input(),
                                    interaction.output(),
                                    interaction.timestamp().toString()
                                );
                            })
                            .collect(Collectors.toList());
                    })
                    .orElse(List.of());

                LOG.debug(
                    "Extracted " + interactions.size() + " node interactions"
                );

                EnhancedSnarkyResponse response = new EnhancedSnarkyResponse(
                    snarkyResponse,
                    request.question(),
                    interactions
                );

                LOG.info(
                    "=== SnarkyResponseResource.processQuestion SUCCESS ==="
                );
                return Response.ok(response).build();
            } catch (Exception e) {
                LOG.error(
                    "=== SnarkyResponseResource.processQuestion ERROR ==="
                );
                LOG.error("Exception type: " + e.getClass().getSimpleName());
                LOG.error("Exception message: " + e.getMessage());
                LOG.error("Exception stack trace:", e);

                // Log the root cause if available
                Throwable rootCause = e;
                while (rootCause.getCause() != null) {
                    rootCause = rootCause.getCause();
                }
                if (rootCause != e) {
                    LOG.error(
                        "Root cause type: " +
                        rootCause.getClass().getSimpleName()
                    );
                    LOG.error("Root cause message: " + rootCause.getMessage());
                }

                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(
                        new ErrorResponse(
                            "Failed to generate snarky response",
                            e.getMessage() +
                            (rootCause != e
                                    ? " (Root cause: " +
                                    rootCause.getMessage() +
                                    ")"
                                    : "")
                        )
                    )
                    .build();
            }
        });
    }

    /**
     * Record representing a question request.
     */
    public record QuestionRequest(String question) {}

    /**
     * Record representing a node interaction for the response.
     */
    public record NodeInteractionResponse(
        String nodeId,
        String nodeName,
        String input,
        String output,
        String timestamp
    ) {}

    /**
     * Record representing an enhanced snarky response with processing history.
     */
    public record EnhancedSnarkyResponse(
        String response,
        String originalQuestion,
        List<NodeInteractionResponse> processingHistory
    ) {}

    /**
     * Record representing an error response.
     */
    public record ErrorResponse(String error, String details) {}

    /**
     * A specialized LLM node for sequence workflows that completes instead of traversing,
     * letting the workflow engine handle the sequencing through edges.
     */
    private static class SequenceLLMNode<T> implements GraphWorkflowNode<T> {
        private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(SequenceLLMNode.class.getName());
        
        private final NodeId id;
        private final ChatModel model;
        private final String systemPrompt;
        private final Function<GraphWorkflowState<T>, String> userMessageExtractor;
        private final String name;

        public SequenceLLMNode(
            String nodeId,
            ChatModel model,
            String systemPrompt,
            Function<GraphWorkflowState<T>, String> userMessageExtractor
        ) {
            this.id = NodeId.of(nodeId);
            this.model = model;
            this.systemPrompt = systemPrompt;
            this.userMessageExtractor = userMessageExtractor;
            this.name = "SequenceLLM-" + nodeId;
        }

        @Override
        public WorkflowResult<GraphCommand<T>, WorkflowError> process(GraphWorkflowState<T> state) {
            LOGGER.info(() -> "Processing in sequence LLM node: " + id.value());
            
            try {
                // Extract the user message from the state using the provided extractor
                String userMessage = userMessageExtractor.apply(state);
                LOGGER.fine(() -> "Extracted user message: " + userMessage);

                // Create chat messages
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from(systemPrompt));
                messages.add(UserMessage.from(userMessage));

                LOGGER.info(() -> "Sending request to LLM with " + messages.size() + " messages");
                
                // Get LLM response
                long startTime = System.currentTimeMillis();
                AiMessage response = model.chat(messages).aiMessage();
                long duration = System.currentTimeMillis() - startTime;
                
                String responseText = response.text();
                LOGGER.info(() -> "Received LLM response in " + duration + "ms");
                LOGGER.fine(() -> "Response content: " + responseText);

                // Get or create the processing history
                ProcessingHistory history = ProcessingHistoryUtils.getOrCreateHistory(state);

                // Add this interaction to the history
                NodeInteraction interaction = new NodeInteraction(
                    id,
                    getName(),
                    userMessage,
                    responseText,
                    Instant.now()
                );
                history.addInteraction(interaction);
                LOGGER.fine(() -> "Added interaction to history. Total interactions: " + 
                             history.getAllInteractions().size());

                // Create updated context with the response and history
                WorkflowContext updatedContext = state
                    .context()
                    // Keep the response key for backward compatibility
                    .with(ContextKey.of("response", Object.class), responseText)
                    // Store the processing history
                    .with(ProcessingHistory.HISTORY_KEY, history);

                LOGGER.info(() -> "LLM processing complete, letting workflow handle sequencing");
                
                // Use GraphCommandComplete to let the workflow engine handle sequencing
                return WorkflowResult.success(
                    GraphCommandComplete.withResultAndContext(responseText, updatedContext)
                );
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing with LLM: " + e.getMessage(), e);
                
                return WorkflowResult.failure(
                    ExecutionError.withCause(
                        "llm-processing-error",
                        "Error processing with LLM: " + e.getMessage(),
                        id.value(),
                        e
                    )
                );
            }
        }

        @Override
        public NodeId getNodeId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return "Sequence LLM Node: " + systemPrompt.substring(0, Math.min(50, systemPrompt.length())) + "...";
        }
    }
}
