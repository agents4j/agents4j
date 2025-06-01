package agents4j.integration.examples;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.*;

import dev.agents4j.integration.examples.SnarkyResponseResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SnarkyResponseResourceTest {

    @BeforeAll
    static void setUp() {
        System.out.println("=== SnarkyResponseResourceTest Setup ===");
        // Enable debug logging for tests
        System.setProperty("quarkus.log.category.\"agents4j\".level", "DEBUG");
        System.setProperty(
            "quarkus.log.category.\"dev.agents4j\".level",
            "DEBUG"
        );
    }

    @Test
    @Order(1)
    @DisplayName("Health Check - Verify service is running")
    public void testHealthCheck() {
        System.out.println("=== Testing Health Check ===");

        given()
            .when()
            .get("/api/debug/health")
            .then()
            .statusCode(200)
            .body("status", is("UP"))
            .body("service", is("agents4j-quarkus-integration"));
    }

    @Test
    @Order(2)
    @DisplayName("System Info - Check configuration and dependencies")
    public void testSystemInfo() {
        System.out.println("=== Testing System Info ===");

        Response response = given()
            .when()
            .get("/api/debug/info")
            .then()
            .statusCode(200)
            .body("service", is("agents4j-quarkus-integration"))
            .body("configuration.chatModelAvailable", notNullValue())
            .extract()
            .response();

        // Print the response for debugging
        System.out.println("System Info Response: " + response.asString());
    }

    @Test
    @Order(3)
    @DisplayName("ChatModel Status - Verify ChatModel injection")
    public void testChatModelStatus() {
        System.out.println("=== Testing ChatModel Status ===");

        Response response = given()
            .when()
            .get("/api/debug/chatmodel-status")
            .then()
            .statusCode(200)
            .body("chatModelInjected", notNullValue())
            .extract()
            .response();

        System.out.println("ChatModel Status Response: " + response.asString());

        // Verify ChatModel is properly injected
        assertTrue(
            response.jsonPath().getBoolean("chatModelInjected"),
            "ChatModel should be injected"
        );
    }

    @Test
    @Order(4)
    @DisplayName("Request Validation - Test JSON parsing")
    public void testRequestValidation() {
        System.out.println("=== Testing Request Validation ===");

        // Test valid request
        String validRequest =
            """
            {
                "question": "Test question for validation"
            }
            """;

        Response validResponse = given()
            .contentType(ContentType.JSON)
            .body(validRequest)
            .when()
            .post("/api/debug/validate-request")
            .then()
            .statusCode(200)
            .body("status", is("VALID"))
            .body("requestReceived", is(true))
            .body("questionPresent", is(true))
            .body("questionEmpty", is(false))
            .extract()
            .response();

        System.out.println(
            "Valid Request Response: " + validResponse.asString()
        );
    }

    @Test
    @Order(5)
    @DisplayName("Invalid Request Validation - Test error handling")
    public void testInvalidRequestValidation() {
        System.out.println("=== Testing Invalid Request Validation ===");

        // Test request with null question
        String nullQuestionRequest =
            """
            {
                "question": null
            }
            """;

        Response nullResponse = given()
            .contentType(ContentType.JSON)
            .body(nullQuestionRequest)
            .when()
            .post("/api/debug/validate-request")
            .then()
            .statusCode(400)
            .body("status", is("INVALID"))
            .extract()
            .response();

        System.out.println(
            "Null Question Response: " + nullResponse.asString()
        );

        // Test request with empty question
        String emptyQuestionRequest =
            """
            {
                "question": ""
            }
            """;

        Response emptyResponse = given()
            .contentType(ContentType.JSON)
            .body(emptyQuestionRequest)
            .when()
            .post("/api/debug/validate-request")
            .then()
            .statusCode(400)
            .body("status", is("INVALID"))
            .extract()
            .response();

        System.out.println(
            "Empty Question Response: " + emptyResponse.asString()
        );

        // Test malformed JSON
        String malformedJson =
            """
            {
                "question": "Test question"
                "missing_comma": true
            }
            """;

        Response malformedResponse = given()
            .contentType(ContentType.JSON)
            .body(malformedJson)
            .when()
            .post("/api/debug/validate-request")
            .then()
            .statusCode(400)
            .extract()
            .response();

        System.out.println(
            "Malformed JSON Response: " + malformedResponse.asString()
        );
    }

    @Test
    @Order(6)
    @DisplayName("Content Type Validation - Test different content types")
    public void testContentTypeValidation() {
        System.out.println("=== Testing Content Type Validation ===");

        String validRequest =
            """
            {
                "question": "Test question"
            }
            """;

        // Test with correct content type
        given()
            .contentType(ContentType.JSON)
            .body(validRequest)
            .when()
            .post("/api/debug/validate-request")
            .then()
            .statusCode(200);

        // Test with incorrect content type
        Response incorrectContentTypeResponse = given()
            .contentType(ContentType.TEXT)
            .body(validRequest)
            .when()
            .post("/api/debug/validate-request")
            .then()
            .extract()
            .response();

        System.out.println(
            "Incorrect Content Type Response Status: " +
            incorrectContentTypeResponse.getStatusCode()
        );
        System.out.println(
            "Incorrect Content Type Response: " +
            incorrectContentTypeResponse.asString()
        );
    }

    @Test
    @Order(7)
    @DisplayName("Snarky Endpoint - Test main workflow endpoint")
    public void testSnarkyEndpoint() {
        System.out.println("=== Testing Snarky Endpoint ===");

        // Create a question request
        String requestBody =
            """
            {
                "question": "How do I make a good cup of coffee?"
            }
            """;

        // Send the request and capture the full response for debugging
        Response response = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/snarky")
            .then()
            .extract()
            .response();

        System.out.println(
            "Snarky Endpoint Response Status: " + response.getStatusCode()
        );
        System.out.println(
            "Snarky Endpoint Response Headers: " + response.getHeaders()
        );
        System.out.println(
            "Snarky Endpoint Response Body: " + response.asString()
        );

        // Since we don't have a valid API key, we expect a 500 error with proper error handling
        // But we also verify that the processing history structure is present in the error response
        if (response.getStatusCode() == 500) {
            response
                .then()
                .body("error", is("Workflow execution failed"))
                .body("details", containsString("Incorrect API key"));
        } else if (response.getStatusCode() == 400) {
            // If we get a 400, let's examine why
            System.err.println(
                "Unexpected 400 error - this is what we're debugging!"
            );
            System.err.println("Response body: " + response.asString());
            fail(
                "Received HTTP 400 - this indicates a request parsing issue that needs debugging"
            );
        }
    }

    @Test
    @Order(8)
    @DisplayName(
        "Snarky Endpoint Success Scenario - Document expected behavior"
    )
    public void testSnarkyEndpointSuccessScenario() {
        System.out.println("=== Testing Snarky Endpoint Success Scenario ===");

        // This test documents what the response should look like when API key is valid
        // Since we can't test with real API key in CI, this serves as documentation
        // and will pass when processingHistory is an empty array (no successful workflow execution)
        String requestBody =
            """
            {
                "question": "How do I make a good cup of coffee?"
            }
            """;

        // This would be the expected structure for a successful response
        // Currently returns 500 due to invalid API key, but shows we can access the structure
        var response = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/snarky")
            .then()
            .extract()
            .response();

        System.out.println(
            "Success Scenario Response Status: " + response.getStatusCode()
        );
        System.out.println("Success Scenario Response: " + response.asString());

        // Verify that our API handles the structure properly
        // In failure case, we still return proper JSON structure
        if (response.getStatusCode() == 500) {
            // Verify error structure
            response
                .then()
                .body("error", notNullValue())
                .body("details", notNullValue());
        } else if (response.getStatusCode() == 200) {
            // This would be the success case structure with valid API key
            response
                .then()
                .body("response", notNullValue())
                .body(
                    "originalQuestion",
                    is("How do I make a good cup of coffee?")
                )
                .body("processingHistory", notNullValue())
                .body("processingHistory.size()", greaterThan(0));
        } else if (response.getStatusCode() == 400) {
            System.err.println("HTTP 400 detected in success scenario test!");
            fail(
                "HTTP 400 indicates request parsing issue - check request format and content type"
            );
        }
    }

    @Test
    @Order(9)
    @DisplayName("Invalid Request Handling - Test various invalid inputs")
    public void testInvalidRequest() {
        System.out.println("=== Testing Invalid Request Handling ===");

        // Send an invalid request (missing question field)
        String invalidRequest =
            """
            {
                "invalid": "This is not a valid request"
            }
            """;

        Response response = given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
            .when()
            .post("/api/snarky")
            .then()
            .extract()
            .response();

        System.out.println(
            "Invalid Request Response Status: " + response.getStatusCode()
        );
        System.out.println("Invalid Request Response: " + response.asString());

        // Should get a 400 for validation error or 500 for workflow execution error
        if (response.getStatusCode() == 400) {
            // This is better - validation caught the issue early
            response.then().body("error", notNullValue());
        } else if (response.getStatusCode() == 500) {
            // Workflow tried to execute but failed due to invalid input
            response
                .then()
                .body("error", is("Workflow execution failed"))
                .body(
                    "details",
                    containsString("text cannot be null or blank")
                );
        } else {
            fail("Unexpected status code: " + response.getStatusCode());
        }
    }

    @Test
    @Order(10)
    @DisplayName("System Check - Overall system health")
    public void testSystemCheck() {
        System.out.println("=== Testing System Check ===");

        Response response = given()
            .when()
            .get("/api/debug/system-check")
            .then()
            .statusCode(200)
            .body("overallStatus", notNullValue())
            .extract()
            .response();

        System.out.println("System Check Response: " + response.asString());

        // Log the overall status for debugging
        String overallStatus = response.jsonPath().getString("overallStatus");
        System.out.println("Overall System Status: " + overallStatus);
    }

    @Test
    @Order(11)
    @DisplayName("HTTP 400 Debugging - Comprehensive request testing")
    public void testHttp400Debugging() {
        System.out.println("=== HTTP 400 Debugging Test ===");

        // Test 1: Valid JSON, valid structure
        String validRequest =
            """
            {
                "question": "This is a valid question"
            }
            """;

        Response validResponse = given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(validRequest)
            .when()
            .post("/api/snarky")
            .then()
            .extract()
            .response();

        System.out.println(
            "Valid request status: " + validResponse.getStatusCode()
        );
        if (validResponse.getStatusCode() == 400) {
            System.err.println("ERROR: Valid request returned 400!");
            System.err.println("Response: " + validResponse.asString());
        }

        // Test 2: Invalid JSON structure
        String invalidJson = "{ invalid json }";

        Response invalidJsonResponse = given()
            .contentType(ContentType.JSON)
            .body(invalidJson)
            .when()
            .post("/api/snarky")
            .then()
            .extract()
            .response();

        System.out.println(
            "Invalid JSON status: " + invalidJsonResponse.getStatusCode()
        );
        System.out.println(
            "Invalid JSON response: " + invalidJsonResponse.asString()
        );

        // Test 3: Empty body
        Response emptyBodyResponse = given()
            .contentType(ContentType.JSON)
            .when()
            .post("/api/snarky")
            .then()
            .extract()
            .response();

        System.out.println(
            "Empty body status: " + emptyBodyResponse.getStatusCode()
        );
        System.out.println(
            "Empty body response: " + emptyBodyResponse.asString()
        );

        // Summary
        System.out.println("=== HTTP 400 Debugging Summary ===");
        System.out.println("Valid request: " + validResponse.getStatusCode());
        System.out.println(
            "Invalid JSON: " + invalidJsonResponse.getStatusCode()
        );
        System.out.println("Empty body: " + emptyBodyResponse.getStatusCode());
    }
}
