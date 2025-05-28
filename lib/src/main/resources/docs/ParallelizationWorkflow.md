# ParallelizationWorkflow

The **ParallelizationWorkflow** implements the Parallelization Workflow pattern for efficient concurrent processing of multiple LLM operations. This pattern enables parallel execution of LLM calls with automated output aggregation, significantly improving throughput for batch processing scenarios.

## Overview

The ParallelizationWorkflow pattern manifests in two key variations:

- **Sectioning**: Decomposes a complex task into independent subtasks that can be processed concurrently. For example, analyzing different sections of a document simultaneously.
- **Voting**: Executes identical prompts multiple times in parallel to gather diverse perspectives or implement majority voting mechanisms. This is particularly useful for validation or consensus-building tasks.

## Key Benefits

- **Improved throughput** through concurrent processing
- **Better resource utilization** of LLM API capacity
- **Reduced overall processing time** for batch operations
- **Enhanced result quality** through multiple perspectives (in voting scenarios)

## When to Use

- Processing large volumes of similar but independent items
- Tasks requiring multiple independent perspectives or validations
- Scenarios where processing time is critical and tasks are parallelizable
- Complex operations that can be decomposed into independent subtasks

## Implementation Considerations

- Ensure tasks are truly independent to avoid consistency issues
- Consider API rate limits when determining parallel execution capacity
- Monitor resource usage (memory, CPU) when scaling parallel operations
- Implement appropriate error handling for parallel task failures

## Basic Usage

### Simple Parallel Query

```java
import dev.agents4j.Agents4J;
import dev.langchain4j.model.chat.ChatModel;
import java.util.Arrays;
import java.util.List;

// Create a chat model (example with OpenAI)
ChatModel chatModel = OpenAiChatModel.builder()
    .apiKey("your-api-key")
    .modelName("gpt-3.5-turbo")
    .build();

// Process multiple inputs in parallel
List<String> inputs = Arrays.asList(
    "Hello World",
    "Good morning",
    "How are you?"
);

String prompt = "Translate the following text to French:";

// Execute with 3 worker threads
List<String> results = Agents4J.parallelQuery(
    chatModel,
    prompt,
    inputs,
    3
);

// Results will contain French translations in the same order as inputs
```

### Using ParallelizationWorkflow Directly

```java
import dev.agents4j.workflow.ParallelizationWorkflow;

// Create workflow
ParallelizationWorkflow workflow = ParallelizationWorkflow.builder()
    .name("TranslationWorkflow")
    .chatModel(chatModel)
    .build();

// Create input
ParallelizationWorkflow.ParallelInput input = 
    new ParallelizationWorkflow.ParallelInput(
        "Translate to Spanish:",
        Arrays.asList("Hello", "Goodbye", "Thank you"),
        2 // number of worker threads
    );

// Execute
List<String> results = workflow.execute(input);
```

### Asynchronous Processing

```java
import java.util.concurrent.CompletableFuture;

// Execute asynchronously
CompletableFuture<List<String>> future = workflow.executeAsync(input);

// Do other work while processing...
performOtherTasks();

// Get results when ready
List<String> results = future.get();
```

## Real-World Examples

### 1. Batch Translation

```java
List<String> textsToTranslate = Arrays.asList(
    "Hello World - translate to French",
    "Hello World - translate to Spanish", 
    "Hello World - translate to German"
);

String translationPrompt = "You are a professional translator. " +
    "Translate the text after the dash to the specified language. " +
    "Return only the translation without explanations.";

List<String> translations = Agents4J.parallelQuery(
    chatModel,
    translationPrompt,
    textsToTranslate,
    3
);
```

### 2. Document Analysis Sectioning

```java
List<String> documentSections = Arrays.asList(
    "Executive Summary: The quarterly report shows strong growth...",
    "Financial Data: Revenue increased by 15% compared to last quarter...",
    "Market Analysis: Consumer sentiment remains positive..."
);

String analysisPrompt = "Analyze the following document section and provide:\n" +
    "1. Key insights\n" +
    "2. Important metrics or data points\n" +
    "3. Actionable recommendations\n" +
    "Section to analyze:";

ParallelizationWorkflow workflow = Agents4J.createParallelizationWorkflow(
    "DocumentAnalysisWorkflow",
    chatModel
);

ParallelizationWorkflow.ParallelInput input = 
    new ParallelizationWorkflow.ParallelInput(
        analysisPrompt,
        documentSections,
        2
    );

List<String> analyses = workflow.execute(input);
```

### 3. Multi-Perspective Voting

```java
String question = "Should our company invest in renewable energy infrastructure?";

List<String> perspectivePrompts = Arrays.asList(
    "As a financial analyst, evaluate: " + question,
    "As an environmental expert, evaluate: " + question,
    "As a risk management specialist, evaluate: " + question
);

String votingPrompt = "You are an expert consultant. Provide a clear recommendation " +
    "(YES/NO) with 2-3 key supporting reasons. Be concise and decisive.";

List<String> perspectives = Agents4J.parallelQuery(
    chatModel,
    votingPrompt,
    perspectivePrompts,
    3
);
```

### 4. Content Generation Batch

```java
List<String> contentTopics = Arrays.asList(
    "AI in Healthcare - Benefits and Challenges",
    "Remote Work Productivity Tips for 2024",
    "Sustainable Technology Trends"
);

String contentPrompt = "Write a compelling 2-paragraph social media post about the following topic. " +
    "Make it engaging, informative, and include relevant hashtags. Topic:";

List<String> generatedContent = Agents4J.parallelQuery(
    chatModel,
    contentPrompt,
    contentTopics,
    2
);
```

## Factory Methods

The `AgentWorkflowFactory` provides convenient methods for creating ParallelizationWorkflow instances:

```java
import dev.agents4j.workflow.AgentWorkflowFactory;

// Create with specific name
ParallelizationWorkflow workflow = AgentWorkflowFactory
    .createParallelizationWorkflow("MyWorkflow", chatModel);

// Create with default name
ParallelizationWorkflow workflow = AgentWorkflowFactory
    .createParallelizationWorkflow(chatModel);
```

## Integration with Agents4J

The ParallelizationWorkflow integrates seamlessly with the Agents4J library and provides convenient static methods:

```java
// Simple parallel query with default 4 workers
List<String> results = Agents4J.parallelQuery(
    chatModel,
    "Summarize the following text:",
    textList
);

// Parallel query with custom worker count
List<String> results = Agents4J.parallelQuery(
    chatModel,
    "Analyze sentiment:",
    textList,
    6 // 6 worker threads
);

// Create workflow for reuse
ParallelizationWorkflow workflow = Agents4J.createParallelizationWorkflow(
    "ReusableWorkflow",
    chatModel
);
```

## Error Handling

The ParallelizationWorkflow provides robust error handling:

```java
try {
    List<String> results = workflow.parallel(prompt, inputs, workers);
} catch (IllegalArgumentException e) {
    // Handle validation errors (null/empty inputs, invalid worker count)
    System.err.println("Invalid input: " + e.getMessage());
} catch (RuntimeException e) {
    // Handle processing errors from individual tasks
    System.err.println("Processing failed: " + e.getMessage());
    Throwable cause = e.getCause();
    // Examine the specific failure cause
}
```

## Performance Considerations

### Worker Thread Configuration

- **Too few workers**: Underutilizes available resources
- **Too many workers**: May overwhelm API rate limits and increase overhead
- **Recommended**: Start with 2-4 workers and adjust based on API limits and performance testing

### API Rate Limits

Consider your LLM provider's rate limits when setting worker count:

```java
// For OpenAI GPT-3.5-turbo (60 RPM limit)
int maxWorkers = Math.min(4, inputs.size());

// For higher tier plans, you can use more workers
int maxWorkers = Math.min(10, inputs.size());
```

### Memory Usage

Monitor memory usage when processing large batches:

```java
// Process large datasets in chunks
List<String> largeDataset = // ... thousands of items
int chunkSize = 100;
List<String> allResults = new ArrayList<>();

for (int i = 0; i < largeDataset.size(); i += chunkSize) {
    List<String> chunk = largeDataset.subList(
        i, 
        Math.min(i + chunkSize, largeDataset.size())
    );
    
    List<String> chunkResults = Agents4J.parallelQuery(
        chatModel, prompt, chunk, 4
    );
    
    allResults.addAll(chunkResults);
}
```

## Best Practices

1. **Validate Independence**: Ensure your tasks are truly independent before parallelizing
2. **Monitor Performance**: Measure actual performance gains vs sequential processing
3. **Handle Failures Gracefully**: Implement retry logic for transient failures
4. **Respect Rate Limits**: Configure worker count based on your API tier
5. **Use Context**: Store execution metadata using the context parameter for debugging

```java
Map<String, Object> context = new HashMap<>();
List<String> results = workflow.execute(input, context);

// Access execution metadata
System.out.println("Workflow: " + context.get("workflow_name"));
System.out.println("Processed: " + context.get("num_inputs") + " items");
System.out.println("Execution time: " + context.get("execution_time"));
```

## API Reference

### ParallelizationWorkflow

| Method | Description |
|--------|-------------|
| `execute(ParallelInput)` | Execute the workflow synchronously |
| `execute(ParallelInput, Map<String, Object>)` | Execute with context |
| `executeAsync(ParallelInput)` | Execute asynchronously |
| `parallel(String, List<String>, int)` | Direct parallel processing method |
| `getName()` | Get workflow name |

### ParallelInput

| Constructor Parameter | Type | Description |
|----------------------|------|-------------|
| `prompt` | String | Prompt template to apply to each input |
| `inputs` | List<String> | List of inputs to process |
| `numWorkers` | int | Number of worker threads |

### Builder Methods

| Method | Description |
|--------|-------------|
| `builder()` | Create new builder instance |
| `name(String)` | Set workflow name |
| `chatModel(ChatModel)` | Set the ChatModel to use |
| `build()` | Build the workflow instance |

## See Also

- [ChainWorkflow Documentation](ChainWorkflow.md) - For sequential agent processing
- [Agent Node API](AgentNode.md) - For understanding individual agent components
- [LangChain4J Documentation](https://docs.langchain4j.dev/) - For ChatModel configuration