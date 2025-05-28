package agents4j.integration;

import agents4j.integration.examples.ExampleRunner;
import agents4j.integration.examples.ParallelizationWorkflowExample;
import dev.agents4j.impl.StringLangChain4JAgentNode;
import dev.agents4j.workflow.ChainWorkflow;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import java.util.Scanner;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Main application class for the Agents4J Quarkus integration.
 * This application demonstrates CLI examples that use the Agents4J library including:
 * - ChainWorkflow for asking 3 consecutive "why" questions
 * - ParallelizationWorkflow for concurrent LLM processing
 */
@QuarkusMain
public class Application implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(Application.class);

    @ConfigProperty(name = "OPENAI_API_KEY")
    String apiKey;

    public static void main(String... args) {
        Quarkus.run(Application.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        LOG.info("Agents4J Quarkus Integration Application started");

        if (apiKey == null || apiKey.isEmpty()) {
            LOG.error("API key not found in configuration");
            return 1;
        }

        // Initialize OpenAI chat model
        ChatModel chatModel = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-3.5-turbo")
            .temperature(0.7)
            .build();

        LOG.info("OpenAI chat model initialized");

        // Validate the chat model
        ExampleRunner exampleRunner = new ExampleRunner(chatModel);
        if (!exampleRunner.validateChatModel()) {
            LOG.error("ChatModel validation failed");
            return 1;
        }

        // Get user choice for which example to run
        Scanner scanner = new Scanner(System.in);
        ExampleRunner.displayExampleOptions();
        System.out.print("Enter your choice (1 or 2): ");
        
        String choice = scanner.nextLine().trim();
        
        try {
            switch (choice) {
                case "1":
                    return runChainWorkflowExample(chatModel, scanner);
                case "2":
                    exampleRunner.runParallelizationExamples();
                    return 0;
                default:
                    System.out.println("Invalid choice. Please enter 1 or 2.");
                    return 1;
            }
        } catch (Exception e) {
            LOG.error("Error running example", e);
            System.err.println("Example execution failed: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Run the ChainWorkflow example for three why questions.
     *
     * @param chatModel The ChatModel to use
     * @param scanner Scanner for user input
     * @return Exit code
     */
    private int runChainWorkflowExample(ChatModel chatModel, Scanner scanner) {
        LOG.info("Running ChainWorkflow example");
        
        // Create the workflow for 3 consecutive why questions
        ChainWorkflow<String, String> workflow = createWhyWorkflow(chatModel);

        // Get input from user
        System.out.println("\nEnter your initial question:");
        String initialQuestion = scanner.nextLine();

        if (initialQuestion.isEmpty()) {
            LOG.error("No question provided");
            return 1;
        }

        LOG.info("Processing initial question: " + initialQuestion);
        System.out.println(
            "\nProcessing your question through 3 levels of 'why'...\n"
        );

        // Execute the workflow
        String result = workflow.execute(initialQuestion);

        // Print the result
        System.out.println("\n--- Final Result ---");
        System.out.println(result);

        return 0;
    }



    /**
     * Creates a ChainWorkflow that asks 3 consecutive "why" questions.
     *
     * @param chatModel The ChatModel to use
     * @return A ChainWorkflow that processes the input through three consecutive "why" questions
     */
    private ChainWorkflow<String, String> createWhyWorkflow(
        ChatModel chatModel
    ) {
        // First node: Initial analysis and first "why" question
        StringLangChain4JAgentNode firstWhyNode =
            StringLangChain4JAgentNode.builder()
                .name("FirstWhyNode")
                .model(chatModel)
                .systemPrompt(
                    "You are a thoughtful assistant that helps people understand topics more deeply. " +
                    "Your task is to analyze the user's question and ask a fundamental 'why' question about it. " +
                    "Provide a brief response to the original question first, and then ask your 'why' question."
                )
                .build();

        // Second node: Process the first why and ask second why
        StringLangChain4JAgentNode secondWhyNode =
            StringLangChain4JAgentNode.builder()
                .name("SecondWhyNode")
                .model(chatModel)
                .systemPrompt(
                    "You are a thoughtful assistant continuing a chain of inquiry. " +
                    "The input will contain an initial question, an analysis, and a first 'why' question. " +
                    "Your task is to answer the first 'why' question thoroughly and then ask a deeper, second 'why' question " +
                    "that explores the underlying principles or causes."
                )
                .build();

        // Third node: Process the second why and ask final why
        StringLangChain4JAgentNode thirdWhyNode =
            StringLangChain4JAgentNode.builder()
                .name("ThirdWhyNode")
                .model(chatModel)
                .systemPrompt(
                    "You are a thoughtful assistant continuing a chain of inquiry. " +
                    "The input will contain an initial question, previous analyses, and a second 'why' question. " +
                    "Your task is to answer the second 'why' question thoroughly and then ask a third, even deeper 'why' question " +
                    "that explores the most fundamental aspects of the topic."
                )
                .build();

        // Final node: Process the third why and provide a comprehensive answer
        StringLangChain4JAgentNode finalNode =
            StringLangChain4JAgentNode.builder()
                .name("FinalNode")
                .model(chatModel)
                .systemPrompt(
                    "You are a thoughtful assistant finalizing a chain of inquiry. " +
                    "The input will contain an initial question and a series of 'why' questions and answers. " +
                    "Your task is to answer the third 'why' question thoroughly and then provide a comprehensive summary " +
                    "that ties together all the insights from this chain of inquiry. " +
                    "Include the original question and how this deep analysis helps us understand it better."
                )
                .build();

        // Build the workflow
        return ChainWorkflow.<String, String>builder()
            .name("ThreeWhysWorkflow")
            .firstNode(firstWhyNode)
            .node(secondWhyNode)
            .node(thirdWhyNode)
            .node(finalNode)
            .build();
    }
}
