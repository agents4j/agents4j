# Agents4J Quarkus Integration

This project demonstrates the Agents4J framework using Quarkus with a modern CLI interface powered by Picocli.

## Prerequisites

- Java 17 or later
- OpenAI API key

## Setup

1. Set your OpenAI API key as an environment variable:
   ```bash
   export OPENAI_API_KEY=your-api-key-here
   ```

2. Build the application:
   ```bash
   ./gradlew :quarkus-integration:build
   ```

## Usage

The application provides a command-line interface with multiple example commands:

### Available Commands

Run the application without arguments to see available options:
```bash
java -jar build/quarkus-app/quarkus-run.jar
```

#### Interactive Why Questions
Ask three consecutive "why" questions to explore a topic in depth:
```bash
# Interactive mode (will prompt for question)
java -jar build/quarkus-app/quarkus-run.jar interactive-why

# With question provided directly
java -jar build/quarkus-app/quarkus-run.jar interactive-why -q "Why is the sky blue?"
```

#### Chain Workflow Examples
Run comprehensive chain workflow demonstrations:
```bash
java -jar build/quarkus-app/quarkus-run.jar chain-workflow
```

#### Parallelization Examples
Demonstrate concurrent LLM processing:
```bash
java -jar build/quarkus-app/quarkus-run.jar parallelization
```

#### Strategy Pattern Examples
Show pluggable execution strategies:
```bash
java -jar build/quarkus-app/quarkus-run.jar strategy-pattern
```

#### Routing Pattern Examples
Demonstrate intelligent content classification and routing:
```bash
java -jar build/quarkus-app/quarkus-run.jar routing-pattern
```

### Help

Get help for any command:
```bash
java -jar build/quarkus-app/quarkus-run.jar --help
java -jar build/quarkus-app/quarkus-run.jar interactive-why --help
```

## Development Mode

For development, you can use Quarkus dev mode:
```bash
./gradlew :quarkus-integration:quarkusDev -Dquarkus.args="--help"
```

## Examples Overview

### Chain Workflow
Demonstrates sequential processing through multiple LLM agents:
- Simple string workflows
- Memory-enabled conversations
- Manual workflow construction
- Advanced complex node workflows

### Parallelization Workflow
Shows concurrent processing capabilities:
- Batch translation
- Document analysis sectioning
- Asynchronous content generation
- Sentiment analysis
- Performance comparisons

### Strategy Pattern
Illustrates pluggable execution strategies:
- Sequential execution for step-by-step processing
- Parallel execution for independent operations
- Conditional execution for branching logic
- Batch execution for large datasets

### Routing Pattern
Demonstrates intelligent content classification:
- Customer support ticket routing
- Content categorization workflows
- Multi-language processing
- LLM-based and rule-based routing
- Hybrid routing approaches

## Configuration

The application uses the following configuration properties:
- `OPENAI_API_KEY`: Your OpenAI API key (required)

## Architecture

The CLI is built using:
- **Quarkus**: Modern Java framework for cloud-native applications
- **Picocli**: Modern framework for building command line applications
- **Agents4J**: The core workflow framework
- **LangChain4J**: Integration with language models

## Error Handling

The application includes comprehensive error handling:
- API key validation
- ChatModel connectivity checks
- Graceful failure modes with meaningful error messages
- Proper exit codes for scripting