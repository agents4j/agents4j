![Agents4J Logo](./docs/logo.png)

# Agents4J

A modern Java framework for building AI agent-based workflows with comprehensive LangChain4J integration. Agents4J provides a flexible, type-safe, and scalable foundation for creating complex AI workflows using directed graph execution models.

## Overview

Agents4J is designed to make it easy for Java applications to create sophisticated AI agent workflows by leveraging the power of LangChain4J and Large Language Models. The framework provides a clean separation between API contracts and implementations, ensuring maximum flexibility and extensibility.

### Key Features

- **Graph-based Workflows** - Flexible directed graph execution model with support for complex routing patterns
- **Stateful Execution** - Persistent workflow state with suspend/resume capabilities
- **LangChain4J Integration** - Seamless integration with LangChain4J chat models and AI services
- **Type Safety** - Full generic type support throughout the framework with compile-time safety
- **Async-First Design** - Built-in support for non-blocking workflow execution
- **Content Routing** - Intelligent content-based routing for dynamic workflow paths
- **Context Management** - Type-safe context sharing between workflow nodes
- **Observability** - Comprehensive metrics, tracing, and logging abstractions
- **Validation Framework** - Extensible validation system for workflows and configurations
- **REST API** - Complete HTTP API for workflow execution and management

## Architecture

Agents4J follows a modular architecture with clear separation of concerns:

### Core Modules

- **agents4j-api** - Core API interfaces and contracts (zero dependencies)
- **agents4j-core** - Core implementations of the API interfaces
- **agents4j-langchain4j** - LangChain4J integration and specialized implementations
- **quarkus-integration** - Quarkus REST API service with HTTP endpoints

## Quick Start

### Prerequisites

- JDK 17 or later
- Gradle 7.4 or later
- A LangChain4J-compatible LLM provider (OpenAI, Anthropic, etc.)

### Installation

Add the dependencies to your `build.gradle`:

```gradle
dependencies {
    implementation 'dev.agents4j:agents4j-core:0.2.3-SNAPSHOT'
    implementation 'dev.agents4j:agents4j-langchain4j:0.2.3-SNAPSHOT'
}
```

### Basic Usage

#### 1. Simple Graph Workflow

```java
import dev.agents4j.api.*;
import dev.agents4j.core.*;

// Create workflow nodes
GraphWorkflowNode<String> startNode = new SimpleNode("start", input -> {
    return GraphCommand.traverse("process", "Processed: " + input);
});

GraphWorkflowNode<String> processNode = new SimpleNode("process", input -> {
    return GraphCommand.complete("Final result: " + input);
});

// Build the workflow
GraphWorkflow<String, String> workflow = GraphWorkflowBuilder.create("MyWorkflow")
    .withNode(startNode)
    .withNode(processNode)
    .withEdge(startNode.getId(), processNode.getId())
    .withEntryPoint(startNode.getId())
    .build();

// Execute the workflow
WorkflowResult<String, WorkflowError> result = workflow.start("Hello World");

// Handle results
switch (result) {
    case WorkflowResult.Success<String, WorkflowError> success ->
        System.out.println("Result: " + success.value());
    case WorkflowResult.Failure<String, WorkflowError> failure ->
        System.err.println("Error: " + failure.error().getMessage());
    case WorkflowResult.Suspended<String, WorkflowError> suspended ->
        System.out.println("Workflow suspended: " + suspended.suspensionId());
}
```

#### 2. LangChain4J Integration

```java
import dev.agents4j.langchain4j.*;
import dev.langchain4j.model.openai.OpenAiChatModel;

// Define AI service interface
interface ChatAgent {
    String chat(String message);
}

// Create chat model
ChatModel chatModel = OpenAiChatModel.builder()
    .apiKey("your-api-key")
    .modelName("gpt-3.5-turbo")
    .build();

// Create agent node with memory
AgentNode<String, String> agentNode = LangChain4JAgentIntegration
    .builder(ChatAgent.class)
    .withChatModel(chatModel)
    .withDefaultMemory()
    .build();

// Use in workflow
GraphWorkflow<String, String> workflow = GraphWorkflowBuilder.create("ChatWorkflow")
    .withNode(agentNode)
    .withEntryPoint(agentNode.getId())
    .build();
```

#### 3. Content Routing

```java
public class DocumentRouter implements ContentRouter<Document> {
    @Override
    public RoutingDecision route(Document content, Set<NodeId> availableRoutes, WorkflowContext context) {
        String contentType = analyzeContent(content);
        NodeId selectedRoute = mapContentTypeToRoute(contentType, availableRoutes);

        return new RoutingDecision(
            selectedRoute,
            0.85,
            "Document classified as " + contentType,
            calculateAlternatives(content, availableRoutes),
            Map.of("contentType", contentType)
        );
    }

    // Implementation details...
}
```

## Project Structure

```
agents4j/
├── agents4j-api/          # Core API interfaces and contracts
├── agents4j-core/         # Core implementations
├── agents4j-langchain4j/  # LangChain4J integration
├── quarkus-integration/   # Example REST API service
├── docs/                  # Documentation and assets
├── build.gradle          # Main build configuration
├── settings.gradle       # Project settings
└── VERSION               # Current version
```

## Building the Project

### Build All Modules

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

### Clean Build

```bash
./gradlew clean build
```

## REST API Service

The Quarkus integration module provides a complete REST API for workflow execution:

### Start the API Server

1. Set your OpenAI API key:
   ```bash
   export OPENAI_API_KEY=your-api-key-here
   ```

2. Build and run:
```bash
./gradlew :quarkus-integration:quarkusDev
```
or
```bash
make run
```

Swagger UI:
```
http://localhost:8080/swagger-ui/
```

### Example

Request -
```bash
curl -X 'POST' \
  'http://localhost:8080/api/snarky' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "question": "why is the sky blue"
}'
```

Response -
```json
{
  "response": "Oh, look at you asking the age-old question. The sky appears blue because of Rayleigh scattering. Short answer: science. \n\nDisclaimer: No blue skies were harmed in the making of this response.",
  "originalQuestion": "why is the sky blue",
  "processingHistory": [
    {
      "nodeId": "snarky-responder",
      "nodeName": "LLM Node",
      "input": "why is the sky blue",
      "output": "Oh, look at you asking the age-old question. The sky appears blue because of Rayleigh scattering. Short answer: science.",
      "timestamp": "2025-06-03T20:17:51.773890Z"
    },
    {
      "nodeId": "disclaimer-adder",
      "nodeName": "CompletingLLM-disclaimer-adder",
      "input": "Oh, look at you asking the age-old question. The sky appears blue because of Rayleigh scattering. Short answer: science.",
      "output": "Oh, look at you asking the age-old question. The sky appears blue because of Rayleigh scattering. Short answer: science. \n\nDisclaimer: No blue skies were harmed in the making of this response.",
      "timestamp": "2025-06-03T20:17:52.544234Z"
    }
  ]
}
```




## Key Concepts

### Graph Workflows

Workflows are represented as directed graphs where nodes perform processing and edges define possible transitions. This model supports:

- **Sequential Processing** - Linear chains of operations
- **Parallel Execution** - Concurrent node execution with join points
- **Conditional Routing** - Dynamic path selection based on content or context
- **Loops and Cycles** - Iterative processing patterns

### Workflow State Management

Agents4J supports stateful workflows with:

- **Persistent State** - Workflows can be suspended and resumed
- **Context Isolation** - Each execution maintains isolated context
- **State Transitions** - Clear semantics for state changes
- **Error Recovery** - Comprehensive error handling and recovery

### Result Types

The framework uses a comprehensive result system:

- `WorkflowResult.Success<T>` - Successful completion with result
- `WorkflowResult.Failure<E>` - Execution failure with error details
- `WorkflowResult.Suspended` - Workflow suspended pending external input

### Validation System

Built-in validation ensures workflow correctness:

- **Structural Validation** - Graph connectivity and node configuration
- **Type Safety** - Input/output type compatibility
- **Business Rules** - Custom validation rules and constraints
- **Runtime Checks** - Dynamic validation during execution

## Advanced Features

### Observability

Comprehensive monitoring and debugging capabilities:

```java
// Metrics collection
MetricsCollector metrics = new DefaultMetricsCollector();
workflow.withMetrics(metrics);

// Distributed tracing
WorkflowTracer tracer = new JaegerTracer();
workflow.withTracer(tracer);
```

### Custom Node Types

Extend the framework with custom node implementations:

```java
public class CustomProcessingNode implements GraphWorkflowNode<MyData> {
    @Override
    public WorkflowResult<GraphCommand<MyData>, WorkflowError> process(GraphWorkflowState<MyData> state) {
        // Custom processing logic
        MyData result = processData(state.getData());
        return WorkflowResult.success(GraphCommand.complete(result));
    }

    // Required interface methods...
}
```

### Async Execution

Full support for asynchronous processing:

```java
CompletableFuture<WorkflowResult<String, WorkflowError>> future =
    workflow.startAsync("input data");

future.thenAccept(result -> {
    // Handle async result
});
```

## Configuration

### Environment Variables

- `OPENAI_API_KEY` - OpenAI API key for LangChain4J integration
- `AGENTS4J_LOG_LEVEL` - Logging level (DEBUG, INFO, WARN, ERROR)
- `AGENTS4J_METRICS_ENABLED` - Enable metrics collection (true/false)

### System Properties

- `agents4j.execution.timeout` - Default execution timeout in seconds
- `agents4j.validation.strict` - Enable strict validation mode
- `agents4j.async.pool.size` - Async execution thread pool size

## Contributing

We welcome contributions! Please see our contributing guidelines and:

1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Submit a pull request

### Development Setup

1. Clone the repository
2. Install Java 17+
3. Run `./gradlew build` to verify setup
4. Import into your IDE as a Gradle project


## Documentation

- [API Documentation](./agents4j-api/README.md) - Core interfaces and contracts
- [LangChain4J Integration](./agents4j-langchain4j/README.md) - Integration guide and examples

## Version History

Current version: **0.2.3-SNAPSHOT**

The project follows semantic versioning:
- **Major versions**: Breaking API changes
- **Minor versions**: Backward-compatible additions
- **Patch versions**: Bug fixes and improvements

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

- 📖 Documentation: Check module-specific README files
- 🐛 Issues: Report bugs via GitHub Issues
- 💬 Discussions: Join our GitHub Discussions
- 📧 Contact: Create an issue for questions

---

Built with ❤️ for the Java AI community
