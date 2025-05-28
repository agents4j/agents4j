package agents4j.integration;

import agents4j.integration.examples.ExampleRunner;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import jakarta.inject.Inject;
import java.util.Scanner;

/**
 * Main application class for the Agents4J Quarkus integration.
 * This application demonstrates CLI examples that use the Agents4J library using Picocli.
 */
@QuarkusMain
public class Application implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(Application.class);

    @Inject
    CommandLine.IFactory factory;

    public static void main(String... args) {
        Quarkus.run(Application.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(new MainCommand(), factory).execute(args);
    }

    @TopCommand
    @CommandLine.Command(
        name = "agents4j",
        description = "Agents4J CLI - Demonstrations of agent workflows",
        subcommands = {
            InteractiveWhyCommand.class,
            ChainWorkflowCommand.class,
            ParallelizationCommand.class,
            StrategyPatternCommand.class,
            RoutingPatternCommand.class
        },
        mixinStandardHelpOptions = true
    )
    public static class MainCommand implements Runnable {

        @Override
        public void run() {
            System.out.println("Agents4J CLI - Use --help to see available commands");
            System.out.println();
            System.out.println("Available commands:");
            System.out.println("  interactive-why    - Interactive three why questions example");
            System.out.println("  chain-workflow     - Chain workflow examples");
            System.out.println("  parallelization    - Parallelization workflow examples");
            System.out.println("  strategy-pattern   - Strategy pattern examples");
            System.out.println("  routing-pattern    - Routing pattern examples");
            System.out.println();
            System.out.println("Use 'agents4j <command> --help' for more information on a command.");
        }
    }

    @CommandLine.Command(
        name = "interactive-why",
        description = "Interactive example that asks 3 consecutive 'why' questions",
        mixinStandardHelpOptions = true
    )
    public static class InteractiveWhyCommand implements Runnable {

        @ConfigProperty(name = "OPENAI_API_KEY")
        String apiKey;

        @CommandLine.Option(
            names = {"-q", "--question"},
            description = "Initial question to analyze (if not provided, will prompt for input)"
        )
        String initialQuestion;

        @Override
        public void run() {
            LOG.info("Running interactive why example");

            if (apiKey == null || apiKey.isEmpty()) {
                System.err.println("Error: OPENAI_API_KEY not found in configuration");
                System.exit(1);
            }

            ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-3.5-turbo")
                .temperature(0.7)
                .build();

            ExampleRunner exampleRunner = new ExampleRunner(chatModel);
            if (!exampleRunner.validateChatModel()) {
                System.err.println("Error: ChatModel validation failed");
                System.exit(1);
            }

            String question = initialQuestion;
            if (question == null || question.trim().isEmpty()) {
                Scanner scanner = new Scanner(System.in);
                System.out.println("Enter your initial question:");
                question = scanner.nextLine();
                scanner.close();
            }

            if (question.trim().isEmpty()) {
                System.err.println("Error: No question provided");
                System.exit(1);
            }

            try {
                MainApplication mainApp = new MainApplication();
                mainApp.runChainWorkflowExample(chatModel, question);
            } catch (Exception e) {
                LOG.error("Error running interactive why example", e);
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @CommandLine.Command(
        name = "chain-workflow",
        description = "Comprehensive chain workflow examples",
        mixinStandardHelpOptions = true
    )
    public static class ChainWorkflowCommand implements Runnable {

        @ConfigProperty(name = "OPENAI_API_KEY")
        String apiKey;

        @Override
        public void run() {
            LOG.info("Running chain workflow examples");

            if (apiKey == null || apiKey.isEmpty()) {
                System.err.println("Error: OPENAI_API_KEY not found in configuration");
                System.exit(1);
            }

            ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-3.5-turbo")
                .temperature(0.7)
                .build();

            try {
                ExampleRunner exampleRunner = new ExampleRunner(chatModel);
                if (!exampleRunner.validateChatModel()) {
                    System.err.println("Error: ChatModel validation failed");
                    System.exit(1);
                }
                exampleRunner.runChainWorkflowExamples();
            } catch (Exception e) {
                LOG.error("Error running chain workflow examples", e);
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @CommandLine.Command(
        name = "parallelization",
        description = "Parallelization workflow examples",
        mixinStandardHelpOptions = true
    )
    public static class ParallelizationCommand implements Runnable {

        @ConfigProperty(name = "OPENAI_API_KEY")
        String apiKey;

        @Override
        public void run() {
            LOG.info("Running parallelization examples");

            if (apiKey == null || apiKey.isEmpty()) {
                System.err.println("Error: OPENAI_API_KEY not found in configuration");
                System.exit(1);
            }

            ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-3.5-turbo")
                .temperature(0.7)
                .build();

            try {
                ExampleRunner exampleRunner = new ExampleRunner(chatModel);
                if (!exampleRunner.validateChatModel()) {
                    System.err.println("Error: ChatModel validation failed");
                    System.exit(1);
                }
                exampleRunner.runParallelizationExamples();
            } catch (Exception e) {
                LOG.error("Error running parallelization examples", e);
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @CommandLine.Command(
        name = "strategy-pattern",
        description = "Strategy pattern examples",
        mixinStandardHelpOptions = true
    )
    public static class StrategyPatternCommand implements Runnable {

        @ConfigProperty(name = "OPENAI_API_KEY")
        String apiKey;

        @Override
        public void run() {
            LOG.info("Running strategy pattern examples");

            if (apiKey == null || apiKey.isEmpty()) {
                System.err.println("Error: OPENAI_API_KEY not found in configuration");
                System.exit(1);
            }

            ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-3.5-turbo")
                .temperature(0.7)
                .build();

            try {
                ExampleRunner exampleRunner = new ExampleRunner(chatModel);
                if (!exampleRunner.validateChatModel()) {
                    System.err.println("Error: ChatModel validation failed");
                    System.exit(1);
                }
                exampleRunner.runStrategyPatternExamples();
            } catch (Exception e) {
                LOG.error("Error running strategy pattern examples", e);
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @CommandLine.Command(
        name = "routing-pattern",
        description = "Routing pattern examples",
        mixinStandardHelpOptions = true
    )
    public static class RoutingPatternCommand implements Runnable {

        @ConfigProperty(name = "OPENAI_API_KEY")
        String apiKey;

        @Override
        public void run() {
            LOG.info("Running routing pattern examples");

            if (apiKey == null || apiKey.isEmpty()) {
                System.err.println("Error: OPENAI_API_KEY not found in configuration");
                System.exit(1);
            }

            ChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-3.5-turbo")
                .temperature(0.7)
                .build();

            try {
                ExampleRunner exampleRunner = new ExampleRunner(chatModel);
                if (!exampleRunner.validateChatModel()) {
                    System.err.println("Error: ChatModel validation failed");
                    System.exit(1);
                }
                exampleRunner.runRoutingPatternExamples();
            } catch (Exception e) {
                LOG.error("Error running routing pattern examples", e);
                System.err.println("Error: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    /**
     * Helper class containing the original application logic for the interactive why example
     */
    public static class MainApplication {

        private static final Logger LOG = Logger.getLogger(MainApplication.class);

        public void runChainWorkflowExample(ChatModel chatModel, String initialQuestion) {
            LOG.info("Processing initial question: " + initialQuestion);
            System.out.println("\nProcessing your question through 3 levels of 'why'...\n");

            try {
                var workflow = createWhyWorkflow(chatModel);
                String result = workflow.execute(initialQuestion);

                System.out.println("\n--- Final Result ---");
                System.out.println(result);

            } catch (Exception e) {
                LOG.error("Workflow execution failed", e);
                throw new RuntimeException("Error processing your question: " + e.getMessage(), e);
            }
        }

        private dev.agents4j.workflow.ChainWorkflow<String, String> createWhyWorkflow(ChatModel chatModel) {
            var firstWhyNode = dev.agents4j.impl.StringLangChain4JAgentNode.builder()
                .name("FirstWhyNode")
                .model(chatModel)
                .systemPrompt(
                    "You are a thoughtful assistant that helps people understand topics more deeply. " +
                    "Your task is to analyze the user's question and ask a fundamental 'why' question about it. " +
                    "Provide a brief response to the original question first, and then ask your 'why' question."
                )
                .build();

            var secondWhyNode = dev.agents4j.impl.StringLangChain4JAgentNode.builder()
                .name("SecondWhyNode")
                .model(chatModel)
                .systemPrompt(
                    "You are a thoughtful assistant continuing a chain of inquiry. " +
                    "The input will contain an initial question, an analysis, and a first 'why' question. " +
                    "Your task is to answer the first 'why' question thoroughly and then ask a deeper, second 'why' question " +
                    "that explores the underlying principles or causes."
                )
                .build();

            var thirdWhyNode = dev.agents4j.impl.StringLangChain4JAgentNode.builder()
                .name("ThirdWhyNode")
                .model(chatModel)
                .systemPrompt(
                    "You are a thoughtful assistant continuing a chain of inquiry. " +
                    "The input will contain an initial question, previous analyses, and a second 'why' question. " +
                    "Your task is to answer the second 'why' question thoroughly and then ask a third, even deeper 'why' question " +
                    "that explores the most fundamental aspects of the topic."
                )
                .build();

            var finalNode = dev.agents4j.impl.StringLangChain4JAgentNode.builder()
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

            return dev.agents4j.workflow.ChainWorkflow.<String, String>builder()
                .name("ThreeWhysWorkflow")
                .firstNode(firstWhyNode)
                .node(secondWhyNode)
                .node(thirdWhyNode)
                .node(finalNode)
                .build();
        }
    }
}