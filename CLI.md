# Agents4J Command Line Interface

The Agents4J CLI provides an easy way to explore and experiment with the framework's capabilities through interactive examples and demonstrations.

## Installation & Setup

### Prerequisites
- Java 17 or later
- OpenAI API key

### Quick Setup
1. Set your OpenAI API key:
   ```bash
   export OPENAI_API_KEY=your-api-key-here
   ```

2. Build the application:
   ```bash
   ./gradlew :quarkus-integration:build
   ```

## Usage

### Using the CLI Script (Recommended)
The project includes a convenient shell script that handles building and running:

```bash
# Show help
./quarkus-integration/agents4j-cli.sh --help

# Run examples
./quarkus-integration/agents4j-cli.sh interactive-why
./quarkus-integration/agents4j-cli.sh chain-workflow
./quarkus-integration/agents4j-cli.sh parallelization
./quarkus-integration/agents4j-cli.sh strategy-pattern
./quarkus-integration/agents4j-cli.sh routing-pattern
```

### Using Java Directly
```bash
java -jar quarkus-integration/build/quarkus-app/quarkus-run.jar [command] [options]
```

## Available Commands

### `interactive-why`
Interactive example that explores topics through three consecutive "why" questions.

**Usage:**
```bash
# Interactive mode (prompts for question)
agents4j-cli.sh interactive-why

# Provide question directly
agents4j-cli.sh interactive-why -q "Why do leaves change color in fall?"
```

**Options:**
- `-q, --question <text>` - Initial question to analyze

**Example Output:**
```
Processing your question through 3 levels of 'why'...

--- Final Result ---
[Comprehensive analysis exploring chemical processes, evolutionary advantages, 
and ecological significance through progressively deeper questioning]
```

### `chain-workflow`
Demonstrates sequential processing through multiple LLM agents.

**Usage:**
```bash
agents4j-cli.sh chain-workflow
```

**Examples Shown:**
- Simple string workflows using factory methods
- Memory-enabled conversational workflows
- Manual workflow construction with builder pattern
- Advanced workflows with complex agent nodes
- Configuration and context usage patterns

### `parallelization`
Shows concurrent processing capabilities for performance optimization.

**Usage:**
```bash
agents4j-cli.sh parallelization
```

**Examples Shown:**
- Batch translation to multiple languages
- Document analysis sectioning
- Asynchronous content generation
- Sentiment analysis of multiple texts
- Performance comparisons between parallel and sequential processing

### `strategy-pattern`
Illustrates pluggable execution strategies for different workflow patterns.

**Usage:**
```bash
agents4j-cli.sh strategy-pattern
```

**Strategies Demonstrated:**
- **Sequential**: Step-by-step processing
- **Parallel**: Independent operations executed concurrently
- **Conditional**: Branching logic based on conditions
- **Batch**: Optimized processing for large datasets

### `routing-pattern`
Demonstrates intelligent content classification and routing workflows.

**Usage:**
```bash
agents4j-cli.sh routing-pattern
```

**Routing Types Shown:**
- Customer support ticket routing
- Content categorization workflows
- Multi-language processing
- LLM-based and rule-based routing
- Hybrid routing approaches

## CLI Script Options

The `agents4j-cli.sh` script supports several options:

```bash
Usage: agents4j-cli.sh [options] [command] [command-options]

Options:
  -h, --help          Show script help
  --no-build          Don't build if JAR is missing
  --jar-path PATH     Use custom JAR path

Commands:
  help                Show CLI application help
  [any command]       Run the specified CLI command
```

## Error Handling

The CLI includes robust error handling:

- **Missing API Key**: Clear instructions for setting `OPENAI_API_KEY`
- **Build Issues**: Automatic building with `--no-build` option to skip
- **Connectivity**: ChatModel validation before running examples
- **Invalid Commands**: Helpful error messages with suggestions

## Development Mode

For development, use Quarkus dev mode:

```bash
./gradlew :quarkus-integration:quarkusDev -Dquarkus.args="--help"
```

In dev mode, you can:
- Edit code and see changes automatically
- Use the interactive development interface
- Access additional debugging features

## Example Workflows

### Exploring a Complex Topic
```bash
# Start with a broad question
agents4j-cli.sh interactive-why -q "Why is artificial intelligence important?"

# The system will progressively ask:
# 1. Why do we need AI assistance in daily tasks?
# 2. Why do current computing paradigms fall short?
# 3. Why is the human-machine collaboration model emerging?
```

### Comparing Processing Approaches
```bash
# See sequential processing
agents4j-cli.sh chain-workflow

# Compare with parallel processing
agents4j-cli.sh parallelization
```

### Understanding Design Patterns
```bash
# Learn about execution strategies
agents4j-cli.sh strategy-pattern

# Explore content routing
agents4j-cli.sh routing-pattern
```

## Configuration

### Environment Variables
- `OPENAI_API_KEY` - Your OpenAI API key (required)

### Model Configuration
The examples use OpenAI's `gpt-3.5-turbo` model with:
- Temperature: 0.7 (balanced creativity and consistency)
- Standard timeout and retry settings
- Automatic error handling and fallbacks

## Troubleshooting

### Common Issues

**"OPENAI_API_KEY not found"**
```bash
export OPENAI_API_KEY=your-actual-api-key
```

**"JAR file not found"**
```bash
./gradlew :quarkus-integration:build
```

**"ChatModel validation failed"**
- Check your API key is valid
- Verify internet connectivity
- Ensure OpenAI service is accessible

### Getting Help
```bash
# Script help
./quarkus-integration/agents4j-cli.sh --help

# Application help
./quarkus-integration/agents4j-cli.sh help

# Command-specific help
./quarkus-integration/agents4j-cli.sh interactive-why --help
```

## Integration Examples

The CLI serves as both a demonstration tool and a reference for integrating Agents4J into your own applications. Each example shows:

- How to configure ChatModels
- Workflow construction patterns
- Error handling approaches
- Performance optimization techniques
- Best practices for agent coordination

## Performance Considerations

- **Parallel workflows** are ideal for independent tasks
- **Chain workflows** work best for dependent processing steps
- **Strategy patterns** allow runtime optimization decisions
- **Routing patterns** enable intelligent content distribution

## Next Steps

After exploring the CLI examples:

1. **Review the source code** in `quarkus-integration/src/main/java/agents4j/integration/examples/`
2. **Examine the core library** in `lib/src/main/java/dev/agents4j/`
3. **Build your own workflows** using the patterns demonstrated
4. **Contribute examples** by following the established patterns

The CLI provides a solid foundation for understanding how to build sophisticated AI agent workflows with Agents4J.