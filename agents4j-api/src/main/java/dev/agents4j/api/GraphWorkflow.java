package dev.agents4j.api;

import dev.agents4j.api.graph.GraphWorkflowState;

public interface GraphWorkflow<S, O>
    extends StatefulWorkflow<GraphWorkflowState<S>, O> {}
