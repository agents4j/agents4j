package agents4j.integration.examples;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WorkflowSequenceFixTest {

    @Test
    @DisplayName("Verify that workflow sequence no longer fails with 'Node not found: next' error")
    public void testWorkflowSequenceFixApplied() {
        System.out.println("=== Testing Workflow Sequence Fix ===");

        // Test that the main endpoint returns proper error handling
        // instead of "Node not found: next" error
        String requestBody = """
            {
                "question": "Test question to verify workflow sequencing"
            }
            """;

        Response response = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/snarky")
            .then()
            .extract()
            .response();

        System.out.println("Response Status: " + response.getStatusCode());
        System.out.println("Response Body: " + response.asString());

        // We expect either:
        // 1. HTTP 500 with proper API key error (expected in tests without real API key)
        // 2. HTTP 200 with successful response (if API key was configured)
        // 
        // We should NOT get:
        // - HTTP 400 with "Node not found: next" error
        
        int statusCode = response.getStatusCode();
        String responseBody = response.asString();

        // Verify we don't get the old "Node not found: next" error
        assertFalse(
            responseBody.contains("Node not found: next"),
            "Workflow should not fail with 'Node not found: next' error"
        );

        // Verify we get expected error handling
        if (statusCode == 500) {
            // Expected API key error in test environment
            assertTrue(
                responseBody.contains("Workflow execution failed") || 
                responseBody.contains("API key") ||
                responseBody.contains("Authentication"),
                "Should get proper API key error, not workflow structure error"
            );
        } else if (statusCode == 200) {
            // Success case (if API key was configured)
            assertTrue(
                responseBody.contains("response") || responseBody.contains("processingHistory"),
                "Successful response should contain expected structure"
            );
        } else {
            fail("Unexpected status code: " + statusCode + ". Response: " + responseBody);
        }

        System.out.println("✓ Workflow sequence fix verified - no 'Node not found: next' error");
    }

    @Test
    @DisplayName("Verify that request validation still works correctly")
    public void testRequestValidationStillWorks() {
        System.out.println("=== Testing Request Validation ===");

        // Test valid request validation
        String validRequest = """
            {
                "question": "Valid test question"
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

        System.out.println("Valid request validation: " + validResponse.asString());

        // Test invalid request validation
        String invalidRequest = """
            {
                "question": null
            }
            """;

        Response invalidResponse = given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
            .when()
            .post("/api/debug/validate-request")
            .then()
            .statusCode(400)
            .body("status", is("INVALID"))
            .extract()
            .response();

        System.out.println("Invalid request validation: " + invalidResponse.asString());
        System.out.println("✓ Request validation works correctly");
    }

    @Test
    @DisplayName("Verify system health after workflow fix")
    public void testSystemHealthAfterFix() {
        System.out.println("=== Testing System Health ===");

        // Health check
        given()
            .when()
            .get("/api/debug/health")
            .then()
            .statusCode(200)
            .body("status", is("UP"))
            .body("service", is("agents4j-quarkus-integration"));

        // System info
        Response systemInfo = given()
            .when()
            .get("/api/debug/info")
            .then()
            .statusCode(200)
            .body("service", is("agents4j-quarkus-integration"))
            .body("configuration.chatModelAvailable", notNullValue())
            .extract()
            .response();

        System.out.println("System info: " + systemInfo.asString());

        // ChatModel status
        Response chatModelStatus = given()
            .when()
            .get("/api/debug/chatmodel-status")
            .then()
            .statusCode(200)
            .body("chatModelInjected", is(true))
            .extract()
            .response();

        System.out.println("ChatModel status: " + chatModelStatus.asString());
        System.out.println("✓ System health verified after workflow fix");
    }
}