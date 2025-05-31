/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.workflow;

import dev.agents4j.api.StatefulWorkflow;
import dev.agents4j.api.StatefulAgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.api.workflow.WorkflowRoute;
import dev.agents4j.api.workflow.WorkflowCommand;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Implements the Orchestrator-Workers Workflow pattern for complex task decomposition
 * and specialized processing. This pattern demonstrates how to implement more complex
 * agent-like behavior while maintaining control through a central orchestrator that
 * coordinates task decomposition and specialized workers that handle specific subtasks.
 * 
 * <p>The pattern consists of three main phases:</p>
 * 
 * <ul>
 * <li><b>Orchestration</b>: A central LLM analyzes the input task and determines what
 * subtasks need to be performed, how they should be executed, and which workers should
 * handle them.</li>
 * <li><b>Execution</b>: Specialized workers process subtasks in parallel, each optimized
 * for specific types of operations or domains.</li>
 * <li><b>Synthesis</b>: Results from all workers are collected and combined into a
 * coherent final response.</li>
 * </ul>
 *
 * <p><b>Key Benefits:</b></p>
 * <ul>
 * <li>Dynamic task decomposition based on content analysis</li>
 * <li>Specialized processing through domain-specific workers</li>
 * <li>Parallel execution of independent subtasks</li>
 * <li>Clear boundaries that maintain system reliability</li>
 * <li>Adaptive problem-solving capabilities</li>
 * <li>Stateful execution with suspend/resume capabilities</li>
 * </ul>
 *
 * <p><b>When to Use:</b></p>
 * <ul>
 * <li>Complex tasks where subtasks can't be predicted upfront</li>
 * <li>Tasks requiring different approaches or perspectives</li>
 * <li>Situations needing adaptive problem-solving</li>
 * <li>Multi-domain problems requiring specialized expertise</li>
 * <li>Tasks where the processing strategy depends on the input content</li>
 * </ul>
 *
 * <p><b>Implementation Considerations:</b></p>
 * <ul>
 * <li>Ensure workers are truly specialized and independent</li>
 * <li>Design clear interfaces between orchestrator and workers</li>
 * <li>Consider the overhead of orchestration for simple tasks</li>
 * <li>Implement robust error handling for failed subtasks</li>
 * <li>Monitor resource usage with parallel worker execution</li>
 * </ul>
 *
 * @see dev.langchain4j.model.chat.ChatModel
 * @see dev.agents4j.workflow.ParallelizationWorkflow
 * @see <a href="https://docs.langchain4j.dev/">LangChain4J Documentation</a>
 * @see <a href="https://www.anthropic.com/research/building-effective-agents">Building Effective Agents</a>
 */
public class OrchestratorWorkersWorkflow implements StatefulWorkflow<OrchestratorWorkersWorkflow.OrchestratorInput, OrchestratorWorkersWorkflow.WorkerResponse> {

    private static final String ORCHESTRATOR_NODE = "orchestrator";
    private static final String WORKERS_NODE = "workers";
    private static final String SYNTHESIZER_NODE = "synthesizer";

    private final String name;
    private final ChatModel chatModel;
    private final Map<String, WorkerConfig> workerConfigs;
    private final String orchestratorPrompt;
    private final String synthesizerPrompt;
    private final List<StatefulAgentNode<OrchestratorInput>> nodes;
    private final List<WorkflowRoute<OrchestratorInput>> routes;

    /**
     * Creates a new OrchestratorWorkersWorkflow.
     *
     * @param name The name of the workflow
     * @param chatModel The ChatModel to use for orchestration and workers
     * @param workerConfigs Configuration for available workers
     * @param orchestratorPrompt Custom prompt for the orchestrator (optional)
     * @param synthesizerPrompt Custom prompt for result synthesis (optional)
     */
    public OrchestratorWorkersWorkflow(
        String name,
        ChatModel chatModel,
        Map<String, WorkerConfig> workerConfigs,
        String orchestratorPrompt,
        String synthesizerPrompt
    ) {
        this.name = Objects.requireNonNull(name, "Workflow name cannot be null");
        this.chatModel = Objects.requireNonNull(chatModel, "ChatModel cannot be null");
        this.workerConfigs = new HashMap<>(Objects.requireNonNull(workerConfigs, "Worker configs cannot be null"));
        this.orchestratorPrompt = orchestratorPrompt != null ? orchestratorPrompt : getDefaultOrchestratorPrompt();
        this.synthesizerPrompt = synthesizerPrompt != null ? synthesizerPrompt : getDefaultSynthesizerPrompt();
        
        if (workerConfigs.isEmpty()) {
            throw new IllegalArgumentException("At least one worker configuration must be provided");
        }

        this.nodes = createNodes();
        this.routes = createRoutes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<WorkerResponse> start(OrchestratorInput input) throws WorkflowExecutionException {
        return start(input, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<WorkerResponse> start(OrchestratorInput input, Map<String, Object> context) 
            throws WorkflowExecutionException {
        WorkflowState initialState = WorkflowState.create(name + "-" + System.currentTimeMillis());
        return start(input, initialState, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<WorkerResponse> start(OrchestratorInput input, WorkflowState initialState, 
            Map<String, Object> context) throws WorkflowExecutionException {
        try {
            // Store execution context
            context.put("workflow_name", name);
            context.put("task_description", input.getTaskDescription());
            context.put("available_workers", workerConfigs.keySet());
            
            long startTime = System.currentTimeMillis();
            
            // Initialize state with input data
            Map<String, Object> stateData = new HashMap<>();
            stateData.put("input", input);
            stateData.put("startTime", startTime);
            stateData.put("workerConfigs", workerConfigs);
            stateData.put("orchestratorPrompt", orchestratorPrompt);
            stateData.put("synthesizerPrompt", synthesizerPrompt);
            
            WorkflowState state = initialState.withUpdatesAndCurrentNode(stateData, ORCHESTRATOR_NODE);
            
            // Execute orchestration logic
            StatefulAgentNode<OrchestratorInput> orchestratorNode = getNode(ORCHESTRATOR_NODE)
                    .orElseThrow(() -> new WorkflowExecutionException(name, "Orchestrator node not found"));
            
            WorkflowCommand<OrchestratorInput> command = orchestratorNode.process(input, state, context);
            
            // Handle the command
            return handleCommand(command, input, state, context);
            
        } catch (Exception e) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("error_message", e.getMessage());
            throw new WorkflowExecutionException(name, "Orchestrator-Workers workflow execution failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<WorkerResponse> resume(OrchestratorInput input, WorkflowState state) 
            throws WorkflowExecutionException {
        return resume(input, state, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<WorkerResponse> resume(OrchestratorInput input, WorkflowState state, 
            Map<String, Object> context) throws WorkflowExecutionException {
        try {
            String currentNodeId = state.getCurrentNodeId()
                    .orElseThrow(() -> new WorkflowExecutionException(name, "Cannot resume: no current node in state"));
            
            StatefulAgentNode<OrchestratorInput> currentNode = getNode(currentNodeId)
                    .orElseThrow(() -> new WorkflowExecutionException(name, "Current node not found: " + currentNodeId));
            
            WorkflowCommand<OrchestratorInput> command = currentNode.process(input, state, context);
            
            return handleCommand(command, input, state, context);
            
        } catch (Exception e) {
            throw new WorkflowExecutionException(name, "Failed to resume workflow", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<StatefulWorkflowResult<WorkerResponse>> startAsync(OrchestratorInput input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return start(input);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<StatefulWorkflowResult<WorkerResponse>> startAsync(OrchestratorInput input, 
            Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return start(input, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<StatefulWorkflowResult<WorkerResponse>> resumeAsync(OrchestratorInput input, 
            WorkflowState state) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return resume(input, state);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<StatefulWorkflowResult<WorkerResponse>> resumeAsync(OrchestratorInput input, 
            WorkflowState state, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return resume(input, state, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StatefulAgentNode<OrchestratorInput>> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WorkflowRoute<OrchestratorInput>> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<StatefulAgentNode<OrchestratorInput>> getNode(String nodeId) {
        return nodes.stream()
                .filter(node -> node.getNodeId().equals(nodeId))
                .findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WorkflowRoute<OrchestratorInput>> getRoutesFrom(String fromNodeId) {
        return routes.stream()
                .filter(route -> route.getFromNodeId().equals(fromNodeId))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StatefulAgentNode<OrchestratorInput>> getEntryPoints() {
        return nodes.stream()
                .filter(StatefulAgentNode::canBeEntryPoint)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws IllegalStateException {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Workflow must have at least one node");
        }
        
        if (getEntryPoints().isEmpty()) {
            throw new IllegalStateException("Workflow must have at least one entry point");
        }
        
        // Validate all routes reference existing nodes
        for (WorkflowRoute<OrchestratorInput> route : routes) {
            if (getNode(route.getFromNodeId()).isEmpty()) {
                throw new IllegalStateException("Route references non-existent from node: " + route.getFromNodeId());
            }
            if (getNode(route.getToNodeId()).isEmpty()) {
                throw new IllegalStateException("Route references non-existent to node: " + route.getToNodeId());
            }
        }
    }

    private List<StatefulAgentNode<OrchestratorInput>> createNodes() {
        List<StatefulAgentNode<OrchestratorInput>> nodeList = new ArrayList<>();
        
        // Orchestrator node
        nodeList.add(new OrchestratorNode(ORCHESTRATOR_NODE, chatModel));
        
        // Workers node
        nodeList.add(new WorkersNode(WORKERS_NODE, chatModel));
        
        // Synthesizer node
        nodeList.add(new SynthesizerNode(SYNTHESIZER_NODE, chatModel));
        
        return nodeList;
    }

    private List<WorkflowRoute<OrchestratorInput>> createRoutes() {
        List<WorkflowRoute<OrchestratorInput>> routeList = new ArrayList<>();
        
        // Route from orchestrator to workers
        routeList.add(WorkflowRoute.<OrchestratorInput>builder()
                .id("orchestrator-to-workers")
                .from(ORCHESTRATOR_NODE)
                .to(WORKERS_NODE)
                .description("Route from orchestrator to workers")
                .build());
        
        // Route from workers to synthesizer
        routeList.add(WorkflowRoute.<OrchestratorInput>builder()
                .id("workers-to-synthesizer")
                .from(WORKERS_NODE)
                .to(SYNTHESIZER_NODE)
                .description("Route from workers to synthesizer")
                .build());
        
        return routeList;
    }

    private StatefulWorkflowResult<WorkerResponse> handleCommand(WorkflowCommand<OrchestratorInput> command, 
            OrchestratorInput input, WorkflowState state, Map<String, Object> context) {
        
        // Apply state updates
        WorkflowState newState = state.withUpdates(command.getStateUpdates());
        
        switch (command.getType()) {
            case COMPLETE:
                @SuppressWarnings("unchecked")
                WorkerResponse result = (WorkerResponse) newState.get("result").orElse(null);
                
                // Add execution metadata
                long endTime = System.currentTimeMillis();
                long startTime = newState.get("startTime", 0L);
                
                // Update context
                context.put("subtasks_count", result != null ? result.getSubtasks().size() : 0);
                context.put("execution_time", endTime - startTime);
                context.put("successful_subtasks", result != null ? 
                    result.getSubtaskResults().stream().mapToLong(r -> r.isSuccessful() ? 1 : 0).sum() : 0);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("execution_time", endTime - startTime);
                metadata.put("subtasks_count", result != null ? result.getSubtasks().size() : 0);
                
                return StatefulWorkflowResult.withMetadata(
                        StatefulWorkflowResult.Status.COMPLETED, 
                        result, 
                        newState, 
                        null, 
                        metadata);
                
            case CONTINUE:
                // Find next node via routes
                String currentNodeId = newState.getCurrentNodeId().orElse("");
                List<WorkflowRoute<OrchestratorInput>> availableRoutes = getRoutesFrom(currentNodeId);
                
                if (availableRoutes.isEmpty()) {
                    return StatefulWorkflowResult.error("No routes available from node: " + currentNodeId, newState);
                }
                
                // Take the first matching route
                WorkflowRoute<OrchestratorInput> route = availableRoutes.get(0);
                String nextNodeId = route.getToNodeId();
                
                StatefulAgentNode<OrchestratorInput> nextNode = getNode(nextNodeId)
                        .orElseThrow(() -> new RuntimeException("Next node not found: " + nextNodeId));
                
                WorkflowState nextState = newState.withCurrentNode(nextNodeId);
                OrchestratorInput nextInput = command.getNextInput().orElse(input);
                
                WorkflowCommand<OrchestratorInput> nextCommand = nextNode.process(nextInput, nextState, context);
                return handleCommand(nextCommand, nextInput, nextState, context);
                
            case GOTO:
                String targetNodeId = command.getTargetNodeId()
                        .orElseThrow(() -> new RuntimeException("GOTO command missing target node"));
                
                StatefulAgentNode<OrchestratorInput> targetNode = getNode(targetNodeId)
                        .orElseThrow(() -> new RuntimeException("Target node not found: " + targetNodeId));
                
                WorkflowState gotoState = newState.withCurrentNode(targetNodeId);
                OrchestratorInput gotoInput = command.getNextInput().orElse(input);
                
                WorkflowCommand<OrchestratorInput> gotoCommand = targetNode.process(gotoInput, gotoState, context);
                return handleCommand(gotoCommand, gotoInput, gotoState, context);
                
            case SUSPEND:
                return StatefulWorkflowResult.suspended(newState);
                
            case ERROR:
                String errorMessage = command.getErrorMessage().orElse("Unknown error occurred");
                return StatefulWorkflowResult.error(errorMessage, newState);
                
            default:
                return StatefulWorkflowResult.error("Unknown command type: " + command.getType(), newState);
        }
    }

    private String getDefaultOrchestratorPrompt() {
        return "You are an intelligent task orchestrator. Analyze the given task and break it down into specific subtasks that can be handled by specialized workers. For each subtask, specify the worker type and detailed instructions.";
    }

    private String getDefaultSynthesizerPrompt() {
        return "You are a synthesis specialist. Combine the results from different workers into a coherent, comprehensive final response that addresses the original task.";
    }

    /**
     * Node that handles task orchestration and decomposition.
     */
    private static class OrchestratorNode implements StatefulAgentNode<OrchestratorInput> {
        private final String nodeId;
        private final ChatModel chatModel;

        public OrchestratorNode(String nodeId, ChatModel chatModel) {
            this.nodeId = nodeId;
            this.chatModel = chatModel;
        }

        @Override
        public WorkflowCommand<OrchestratorInput> process(OrchestratorInput input, WorkflowState state, 
                Map<String, Object> context) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, WorkerConfig> workerConfigs = (Map<String, WorkerConfig>) state.get("workerConfigs")
                        .orElseThrow(() -> new RuntimeException("Worker configs not found in state"));
                
                String orchestratorPrompt = state.get("orchestratorPrompt")
                        .map(obj -> (String) obj)
                        .orElseThrow(() -> new RuntimeException("Orchestrator prompt not found in state"));

                // Orchestrate task decomposition
                OrchestratorResponse orchestratorResponse = orchestrate(input.getTaskDescription(), 
                        orchestratorPrompt, workerConfigs, chatModel);
                
                return WorkflowCommand.<OrchestratorInput>continueWith()
                        .updateState("orchestratorResponse", orchestratorResponse)
                        .updateState("subtasks", orchestratorResponse.getSubtasks())
                        .build();
                        
            } catch (Exception e) {
                return WorkflowCommand.<OrchestratorInput>error("Failed to orchestrate task: " + e.getMessage())
                        .build();
            }
        }

        private OrchestratorResponse orchestrate(String taskDescription, String orchestratorPrompt, 
                Map<String, WorkerConfig> workerConfigs, ChatModel chatModel) {
            List<ChatMessage> messages = new ArrayList<>();
            
            String fullPrompt = orchestratorPrompt + "\n\nAvailable workers: " + 
                workerConfigs.entrySet().stream()
                    .map(entry -> entry.getKey() + " - " + entry.getValue().getDescription())
                    .collect(Collectors.joining(", ")) +
                "\n\nTask: " + taskDescription;
            
            messages.add(SystemMessage.from(fullPrompt));
            messages.add(UserMessage.from("Please analyze this task and break it down into subtasks. For each subtask, specify the worker type and detailed instructions."));
            
            AiMessage response = chatModel.chat(messages).aiMessage();
            
            return parseOrchestratorResponse(response.text());
        }

        private OrchestratorResponse parseOrchestratorResponse(String response) {
            List<Subtask> subtasks = new ArrayList<>();
            
            // Simple parsing logic - in practice, this would be more sophisticated
            String[] lines = response.split("\n");
            String analysisText = "";
            int subtaskCounter = 1;
            
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                
                if (line.toLowerCase().contains("subtask") || line.toLowerCase().contains("task")) {
                    String workerType = extractWorkerType(line);
                    String instructions = extractInstructions(line);
                    
                    if (workerType != null && instructions != null) {
                        subtasks.add(new Subtask("subtask-" + subtaskCounter++, workerType, instructions));
                    }
                } else {
                    analysisText += line + "\n";
                }
            }
            
            return new OrchestratorResponse(analysisText.trim(), subtasks);
        }

        private String extractWorkerType(String line) {
            // Simple extraction logic
            if (line.toLowerCase().contains("technical")) return "technical";
            if (line.toLowerCase().contains("analyst")) return "analyst";
            if (line.toLowerCase().contains("researcher")) return "researcher";
            return "general";
        }

        private String extractInstructions(String line) {
            return line.trim();
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return "Orchestrator";
        }

        @Override
        public boolean canBeEntryPoint() {
            return true;
        }
    }

    /**
     * Node that executes subtasks using specialized workers.
     */
    private static class WorkersNode implements StatefulAgentNode<OrchestratorInput> {
        private final String nodeId;
        private final ChatModel chatModel;

        public WorkersNode(String nodeId, ChatModel chatModel) {
            this.nodeId = nodeId;
            this.chatModel = chatModel;
        }

        @Override
        public WorkflowCommand<OrchestratorInput> process(OrchestratorInput input, WorkflowState state, 
                Map<String, Object> context) {
            try {
                @SuppressWarnings("unchecked")
                List<Subtask> subtasks = (List<Subtask>) state.get("subtasks")
                        .orElseThrow(() -> new RuntimeException("Subtasks not found in state"));
                
                @SuppressWarnings("unchecked")
                Map<String, WorkerConfig> workerConfigs = (Map<String, WorkerConfig>) state.get("workerConfigs")
                        .orElseThrow(() -> new RuntimeException("Worker configs not found in state"));

                // Execute subtasks in parallel
                List<SubtaskResult> subtaskResults = executeSubtasks(subtasks, workerConfigs, chatModel);
                
                return WorkflowCommand.<OrchestratorInput>continueWith()
                        .updateState("subtaskResults", subtaskResults)
                        .build();
                        
            } catch (Exception e) {
                return WorkflowCommand.<OrchestratorInput>error("Failed to execute subtasks: " + e.getMessage())
                        .build();
            }
        }

        private List<SubtaskResult> executeSubtasks(List<Subtask> subtasks, 
                Map<String, WorkerConfig> workerConfigs, ChatModel chatModel) {
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(subtasks.size(), 4));
            try {
                List<CompletableFuture<SubtaskResult>> futures = subtasks.stream()
                        .map(subtask -> CompletableFuture.supplyAsync(() -> 
                            executeSubtask(subtask, workerConfigs, chatModel), executor))
                        .collect(Collectors.toList());

                return futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList());
            } finally {
                executor.shutdown();
            }
        }

        private SubtaskResult executeSubtask(Subtask subtask, Map<String, WorkerConfig> workerConfigs, 
                ChatModel chatModel) {
            try {
                WorkerConfig config = workerConfigs.get(subtask.getWorkerType());
                if (config == null) {
                    config = workerConfigs.values().iterator().next(); // Use first available worker
                }

                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from(config.getSystemPrompt()));
                messages.add(UserMessage.from(subtask.getInstructions()));

                AiMessage response = chatModel.chat(messages).aiMessage();
                
                return new SubtaskResult(subtask.getId(), subtask.getWorkerType(), response.text(), true);
            } catch (Exception e) {
                return new SubtaskResult(subtask.getId(), subtask.getWorkerType(), 
                        "Error executing subtask: " + e.getMessage(), false);
            }
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return "Workers";
        }
    }

    /**
     * Node that synthesizes results from all workers.
     */
    private static class SynthesizerNode implements StatefulAgentNode<OrchestratorInput> {
        private final String nodeId;
        private final ChatModel chatModel;

        public SynthesizerNode(String nodeId, ChatModel chatModel) {
            this.nodeId = nodeId;
            this.chatModel = chatModel;
        }

        @Override
        public WorkflowCommand<OrchestratorInput> process(OrchestratorInput input, WorkflowState state, 
                Map<String, Object> context) {
            try {
                @SuppressWarnings("unchecked")
                List<SubtaskResult> subtaskResults = (List<SubtaskResult>) state.get("subtaskResults")
                        .orElseThrow(() -> new RuntimeException("Subtask results not found in state"));
                
                @SuppressWarnings("unchecked")
                List<Subtask> subtasks = (List<Subtask>) state.get("subtasks")
                        .orElseThrow(() -> new RuntimeException("Subtasks not found in state"));
                
                String synthesizerPrompt = state.get("synthesizerPrompt")
                        .map(obj -> (String) obj)
                        .orElseThrow(() -> new RuntimeException("Synthesizer prompt not found in state"));

                // Synthesize results
                String finalResult = synthesizeResults(input.getTaskDescription(), subtaskResults, 
                        synthesizerPrompt, chatModel);
                
                // Create final response
                WorkerResponse workerResponse = new WorkerResponse(finalResult, subtasks, subtaskResults, true);
                
                return WorkflowCommand.<OrchestratorInput>complete()
                        .updateState("result", workerResponse)
                        .updateState("finalResult", finalResult)
                        .updateState("completed", true)
                        .build();
                        
            } catch (Exception e) {
                return WorkflowCommand.<OrchestratorInput>error("Failed to synthesize results: " + e.getMessage())
                        .build();
            }
        }

        private String synthesizeResults(String originalTask, List<SubtaskResult> subtaskResults, 
                String synthesizerPrompt, ChatModel chatModel) {
            List<ChatMessage> messages = new ArrayList<>();
            
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("Original task: ").append(originalTask).append("\n\n");
            contextBuilder.append("Subtask results:\n");
            
            for (SubtaskResult result : subtaskResults) {
                contextBuilder.append("- ").append(result.getWorkerType()).append(": ");
                contextBuilder.append(result.getResult()).append("\n");
            }
            
            messages.add(SystemMessage.from(synthesizerPrompt));
            messages.add(UserMessage.from(contextBuilder.toString()));
            
            AiMessage response = chatModel.chat(messages).aiMessage();
            return response.text();
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return "Synthesizer";
        }
    }

    /**
     * Input container for OrchestratorWorkersWorkflow.
     */
    public static class OrchestratorInput {
        private final String taskDescription;
        private final Map<String, Object> context;

        public OrchestratorInput(String taskDescription) {
            this(taskDescription, new HashMap<>());
        }

        public OrchestratorInput(String taskDescription, Map<String, Object> context) {
            this.taskDescription = Objects.requireNonNull(taskDescription, "Task description cannot be null");
            this.context = context != null ? new HashMap<>(context) : new HashMap<>();
        }

        public String getTaskDescription() {
            return taskDescription;
        }

        public Map<String, Object> getContext() {
            return context;
        }
    }

    /**
     * Response from the orchestrator containing analysis and subtasks.
     */
    public static class OrchestratorResponse {
        private final String analysisText;
        private final List<Subtask> subtasks;

        public OrchestratorResponse(String analysisText, List<Subtask> subtasks) {
            this.analysisText = Objects.requireNonNull(analysisText, "Analysis text cannot be null");
            this.subtasks = new ArrayList<>(Objects.requireNonNull(subtasks, "Subtasks cannot be null"));
        }

        public String getAnalysisText() {
            return analysisText;
        }

        public List<Subtask> getSubtasks() {
            return new ArrayList<>(subtasks);
        }
    }

    /**
     * Represents a subtask to be executed by a worker.
     */
    public static class Subtask {
        private final String id;
        private final String workerType;
        private final String instructions;

        public Subtask(String id, String workerType, String instructions) {
            this.id = Objects.requireNonNull(id, "Subtask ID cannot be null");
            this.workerType = Objects.requireNonNull(workerType, "Worker type cannot be null");
            this.instructions = Objects.requireNonNull(instructions, "Instructions cannot be null");
        }

        public String getId() {
            return id;
        }

        public String getWorkerType() {
            return workerType;
        }

        public String getInstructions() {
            return instructions;
        }
    }

    /**
     * Result from executing a subtask.
     */
    public static class SubtaskResult {
        private final String subtaskId;
        private final String workerType;
        private final String result;
        private final boolean successful;

        public SubtaskResult(String subtaskId, String workerType, String result, boolean successful) {
            this.subtaskId = Objects.requireNonNull(subtaskId, "Subtask ID cannot be null");
            this.workerType = Objects.requireNonNull(workerType, "Worker type cannot be null");
            this.result = Objects.requireNonNull(result, "Result cannot be null");
            this.successful = successful;
        }

        public String getSubtaskId() {
            return subtaskId;
        }

        public String getWorkerType() {
            return workerType;
        }

        public String getResult() {
            return result;
        }

        public boolean isSuccessful() {
            return successful;
        }
    }

    /**
     * Final response from the workflow containing synthesized results.
     */
    public static class WorkerResponse {
        private final String finalResult;
        private final List<Subtask> subtasks;
        private final List<SubtaskResult> subtaskResults;
        private final boolean successful;

        public WorkerResponse(
            String finalResult,
            List<Subtask> subtasks,
            List<SubtaskResult> subtaskResults,
            boolean successful
        ) {
            this.finalResult = Objects.requireNonNull(finalResult, "Final result cannot be null");
            this.subtasks = new ArrayList<>(Objects.requireNonNull(subtasks, "Subtasks cannot be null"));
            this.subtaskResults = new ArrayList<>(Objects.requireNonNull(subtaskResults, "Subtask results cannot be null"));
            this.successful = successful;
        }

        public String getFinalResult() {
            return finalResult;
        }

        public List<Subtask> getSubtasks() {
            return new ArrayList<>(subtasks);
        }

        public List<SubtaskResult> getSubtaskResults() {
            return new ArrayList<>(subtaskResults);
        }

        public boolean isSuccessful() {
            return successful;
        }
    }

    /**
     * Configuration for a worker type.
     */
    public static class WorkerConfig {
        private final String workerType;
        private final String description;
        private final String systemPrompt;

        public WorkerConfig(String workerType, String description, String systemPrompt) {
            this.workerType = Objects.requireNonNull(workerType, "Worker type cannot be null");
            this.description = Objects.requireNonNull(description, "Description cannot be null");
            this.systemPrompt = Objects.requireNonNull(systemPrompt, "System prompt cannot be null");
        }

        public String getWorkerType() {
            return workerType;
        }

        public String getDescription() {
            return description;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }
    }

    /**
     * Builder for creating OrchestratorWorkersWorkflow instances.
     */
    public static class Builder {
        private String name;
        private ChatModel chatModel;
        private final Map<String, WorkerConfig> workerConfigs = new HashMap<>();
        private String orchestratorPrompt;
        private String synthesizerPrompt;

        /**
         * Sets the name of the workflow.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the ChatModel to use.
         */
        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        /**
         * Adds a worker configuration.
         */
        public Builder addWorker(String workerType, String description, String systemPrompt) {
            workerConfigs.put(workerType, new WorkerConfig(workerType, description, systemPrompt));
            return this;
        }

        /**
         * Adds a worker configuration.
         */
        public Builder addWorker(WorkerConfig workerConfig) {
            workerConfigs.put(workerConfig.getWorkerType(), workerConfig);
            return this;
        }

        /**
         * Sets a custom orchestrator prompt.
         */
        public Builder orchestratorPrompt(String orchestratorPrompt) {
            this.orchestratorPrompt = orchestratorPrompt;
            return this;
        }

        /**
         * Sets a custom synthesizer prompt.
         */
        public Builder synthesizerPrompt(String synthesizerPrompt) {
            this.synthesizerPrompt = synthesizerPrompt;
            return this;
        }

        /**
         * Builds the OrchestratorWorkersWorkflow instance.
         */
        public OrchestratorWorkersWorkflow build() {
            if (name == null) {
                name = "OrchestratorWorkersWorkflow-" + System.currentTimeMillis();
            }
            if (chatModel == null) {
                throw new IllegalStateException("ChatModel must be set");
            }
            if (workerConfigs.isEmpty()) {
                throw new IllegalStateException("At least one worker configuration must be added");
            }
            
            return new OrchestratorWorkersWorkflow(name, chatModel, workerConfigs, orchestratorPrompt, synthesizerPrompt);
        }
    }

    /**
     * Creates a new Builder for constructing OrchestratorWorkersWorkflow instances.
     */
    public static Builder builder() {
        return new Builder();
    }
}