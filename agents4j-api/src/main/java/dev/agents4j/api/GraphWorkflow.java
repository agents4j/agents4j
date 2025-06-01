package dev.agents4j.api;

import dev.agents4j.api.workflow.GraphWorkflowState;

public interface GraphWorkflow<S, O>
    extends StatefulWorkflow<GraphWorkflowState<S>, O> {}
