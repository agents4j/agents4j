# Agents4J Quarkus REST API Integration

A comprehensive REST API service for demonstrating and using Agents4J workflow patterns. This service provides HTTP endpoints for all major agent workflow patterns including chain workflows, parallelization, strategy patterns, routing, and orchestrator-workers.

## Features

- **Complete REST API** for all Agents4J workflow patterns
- **Interactive Swagger UI** for API exploration and testing
- **Health checks and metrics** for monitoring
- **Configurable workflows** with environment-based settings
- **Production-ready** with proper logging, CORS, and error handling

## Workflow Patterns Available

### 1. Chain Workflow (`/chain-workflow`)
Sequential processing through multiple agents
- Simple single-agent queries
- Complex multi-agent chains
- Three-whys deep analysis
- Conversational workflows with memory

### 2. Parallelization (`/parallelization`)
Concurrent processing for improved throughput
- Parallel processing of multiple inputs
- Content sectioning
- Voting mechanisms for consensus
- Batch processing optimization

### 3. Strategy Pattern (`/strategy-pattern`)
Flexible execution strategies
- Sequential vs parallel execution
- Conditional execution based on criteria
- Batch processing strategies
- Strategy comparison and analysis

### 4. Routing Pattern (`/routing-pattern`)
Intelligent content routing and classification
- Customer support routing
- Content categorization
- Multi-language routing
- LLM-based intelligent routing
- Rule-based pattern matching

### 5. Orchestrator-Workers (`/orchestrator-workers`)
Complex task decomposition and orchestration
- Automatic task decomposition
- Specialized worker coordination
- Parallel subtask execution
- Result synthesis

## Quick Start

### Prerequisites
- Java 17 or later
- Maven or Gradle
- OpenAI API key (or compatible LLM provider)

### Running the Service

1. **Set your API key:**
```bash
export OPENAI_API_KEY=your-api-key-here
```

2. **Start in development mode:**
```bash
./gradlew quarkusDev
```

3. **Access the API:**
- API Base URL: http://localhost:8080
- Swagger UI: http://localhost:8080/q/swagger-ui/
- Health Check: http://localhost:8080/q/health
- API Overview: http://localhost:8080/api/overview

### First API Call

Try a simple chain workflow:
```bash
curl -X POST http://localhost:8080/chain-workflow/simple \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is artificial intelligence?",
    "systemPrompt": "You are a helpful AI assistant."
  }'
```

## API Documentation

### API Overview Endpoint
`GET /api/overview` - Complete API documentation and endpoint discovery

### Quick Start Guide
`GET /api/quickstart` - Step-by-step guide for getting started

### Health and Status
- `GET /api/health` - System health and component status
- `GET /api/stats` - API statistics and capabilities
- `GET /q/health` - Quarkus health check
- `GET /q/metrics` - Application metrics

### Search Endpoints
`GET /api/search?q=keyword` - Search available endpoints by keyword

## Example API Calls

### Chain Workflow - Three Whys Analysis
```bash
curl -X POST http://localhost:8080/chain-workflow/three-whys \
  -H "Content-Type: application/json" \
  -d '{"question": "Why do companies invest in AI?"}'
```

### Parallelization - Batch Translation
```bash
curl -X POST http://localhost:8080/parallelization/parallel-simple \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Translate to Spanish:",
    "inputs": ["Hello", "Good morning", "Thank you"]
  }'
```

### Orchestrator-Workers - Complex Task
```bash
curl -X POST http://localhost:8080/orchestrator-workers/query \
  -H "Content-Type: application/json" \
  -d '{
    "task": "Create a comprehensive business analysis for entering the electric vehicle market"
  }'
```

### Strategy Pattern - Compare Approaches
```bash
curl -X POST http://localhost:8080/strategy-pattern/compare \
  -H "Content-Type: application/json" \
  -d '{
    "input": "Analyze market trends",
    "systemPrompts": [
      "You are a market analyst",
      "You are a financial expert",
      "You are a trend forecaster"
    ]
  }'
```

### Routing Pattern - Intelligent Classification
```bash
curl -X POST http://localhost:8080/routing-pattern/llm-routing \
  -H "Content-Type: application/json" \
  -d '{
    "input": "I need help with my order",
    "classificationPrompt": "Classify this as technical, billing, or general support",
    "includeConfidence": true
  }'
```

## Configuration

### Environment Variables
```bash
# Required
OPENAI_API_KEY=your-openai-api-key

# Optional
AGENTS4J_WORKFLOWS_ENABLED=true
AGENTS4J_ORCHESTRATOR_ENABLED=true
AGENTS4J_MAX_WORKERS=8
PORT=8080
```

### Application Configuration (application.yml)
```yaml
agents4j:
  workflows:
    enabled: true
  orchestrator:
    enabled: true
  execution:
    max-parallel-workers: 8
    default-timeout: 300s

openai:
  api-key: ${OPENAI_API_KEY}
  model-name: gpt-3.5-turbo
  temperature: 0.7
```

## Deployment

### Docker
```dockerfile
FROM openjdk:17-jre-slim
COPY target/quarkus-app/ /app/
WORKDIR /app
CMD ["java", "-jar", "quarkus-run.jar"]
```

### Native Build
```bash
./gradlew build -Dquarkus.package.type=native
```

### Cloud Deployment
The service is ready for deployment to:
- Kubernetes/OpenShift
- AWS ECS/Fargate
- Google Cloud Run
- Azure Container Instances
- Heroku

## Monitoring and Observability

### Health Checks
- `GET /q/health` - Overall application health
- `GET /q/health/live` - Liveness probe
- `GET /q/health/ready` - Readiness probe

### Metrics
- `GET /q/metrics` - Prometheus-compatible metrics
- Application metrics for request counts, durations, errors
- JVM metrics for memory, threads, garbage collection

### Logging
- Structured JSON logging in production
- Configurable log levels per component
- Request/response logging for debugging

## Development

### Project Structure
```
src/main/java/agents4j/integration/
├── Application.java                    # Main application class
└── examples/
    ├── Agents4JApiResource.java       # Main API overview
    ├── ChainWorkflowResource.java     # Chain workflow endpoints
    ├── ParallelizationWorkflowResource.java # Parallelization endpoints
    ├── StrategyPatternResource.java   # Strategy pattern endpoints
    ├── RoutingPatternResource.java    # Routing pattern endpoints
    ├── OrchestratorWorkersResource.java # Orchestrator-workers endpoints
    └── OrchestratorWorkersExample.java # Business logic examples
```

### Running Tests
```bash
./gradlew test
```

### Development Mode
```bash
./gradlew quarkusDev
```
Enables hot reload for rapid development.

## API Response Format

All endpoints return JSON responses with consistent structure:

### Success Response
```json
{
  "input": "user input",
  "result": "processed result",
  "workflow_type": "pattern_name",
  "metadata": {}
}
```

### Error Response
```json
{
  "error": "Error description",
  "timestamp": "2024-01-01T12:00:00Z",
  "path": "/api/endpoint"
}
```

## Integration Examples

### JavaScript/Node.js
```javascript
const axios = require('axios');

const response = await axios.post('http://localhost:8080/chain-workflow/simple', {
  query: 'Explain quantum computing',
  systemPrompt: 'You are a physics professor.'
});

console.log(response.data.result);
```

### Python
```python
import requests

response = requests.post(
    'http://localhost:8080/orchestrator-workers/query',
    json={'task': 'Create a marketing strategy for a new product'}
)

print(response.json()['result'])
```

### Java
```java
WebClient client = WebClient.create("http://localhost:8080");

Mono<String> result = client.post()
    .uri("/parallelization/parallel-simple")
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(Map.of(
        "prompt", "Summarize in one sentence:",
        "inputs", List.of("Article 1", "Article 2", "Article 3")
    ))
    .retrieve()
    .bodyToMono(String.class);
```

## Troubleshooting

### Common Issues

1. **API Key Not Set**
   - Error: "ChatModel validation failed"
   - Solution: Set `OPENAI_API_KEY` environment variable

2. **Workflows Disabled**
   - Error: "Workflows are disabled"
   - Solution: Set `agents4j.workflows.enabled=true`

3. **Rate Limiting**
   - Error: HTTP 429 responses
   - Solution: Implement backoff or upgrade API plan

4. **Memory Issues**
   - Error: OutOfMemoryError
   - Solution: Increase JVM heap size or reduce parallel workers

### Debug Mode
```bash
./gradlew quarkusDev -Dquarkus.log.level=DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new endpoints
4. Update documentation
5. Submit a pull request

## License

This project is part of the Agents4J library and follows the same license terms.