package dev.agents4j.integration.examples;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.langchain4j.workflow.GraphAgentFactory;
import dev.agents4j.workflow.history.ProcessingHistory;
import dev.agents4j.workflow.history.ProcessingHistoryUtils;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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

            // Create a sequence workflow using the enhanced GraphAgentFactory
            LOG.debug("Creating sequence workflow with proper node routing...");
            this.snarkyWorkflow = GraphAgentFactory.createLLMSequence(
                "snarky-workflow",
                QuestionRequest.class,
                GraphAgentFactory.llmNode(
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
                ),
                GraphAgentFactory.llmNode(
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
                )
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


}
