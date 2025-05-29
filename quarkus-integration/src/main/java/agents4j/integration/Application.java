package agents4j.integration;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jboss.logging.Logger;

/**
 * Main application class for the Agents4J Quarkus REST API integration.
 * This application provides REST endpoints for demonstrating and using
 * the Agents4J library workflows and patterns.
 */
@QuarkusMain
public class Application implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(Application.class);

    public static void main(String... args) {
        LOG.info("Starting Agents4J REST API Server");
        Quarkus.run(Application.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        LOG.info("Agents4J REST API Server is running");
        LOG.info("Access the API documentation at: http://localhost:8080/q/swagger-ui/");
        LOG.info("Health check available at: http://localhost:8080/q/health");
        
        // Keep the application running
        Quarkus.waitForExit();
        return 0;
    }
}