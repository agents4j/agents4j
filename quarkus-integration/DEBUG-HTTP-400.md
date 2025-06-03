# HTTP 400 Error Debugging Guide

This guide provides comprehensive debugging information for troubleshooting HTTP 400 errors in the Agents4J Quarkus Integration service.

## Overview

HTTP 400 (Bad Request) errors typically occur due to:
1. Invalid JSON format in request body
2. Missing required fields
3. Incorrect Content-Type headers
4. Request validation failures
5. Serialization/deserialization issues

## Quick Debugging Steps

### 1. Enable Debug Logging

The service includes comprehensive debug logging. Ensure these settings are active:

```properties
# In application.properties
quarkus.log.console.level=DEBUG
quarkus.log.category."dev.agents4j".level=DEBUG
quarkus.log.category."io.quarkus.resteasy".level=DEBUG
quarkus.log.category."org.jboss.resteasy".level=DEBUG
quarkus.log.category."com.fasterxml.jackson".level=DEBUG
```

### 2. Use Debug Endpoints

The service includes several debugging endpoints:

```bash
# Health check
curl http://localhost:8080/api/debug/health

# System information
curl http://localhost:8080/api/debug/info

# ChatModel status
curl http://localhost:8080/api/debug/chatmodel-status

# Request validation (without external API calls)
curl -X POST http://localhost:8080/api/debug/validate-request \
  -H "Content-Type: application/json" \
  -d '{"question": "test question"}'

# Comprehensive system check
curl http://localhost:8080/api/debug/system-check
```

### 3. Run Automated Tests

Execute the comprehensive test suite:

```bash
./gradlew test
```

The test suite includes:
- Health checks
- Request validation tests
- Content-type validation
- Malformed JSON handling
- HTTP 400 specific debugging tests

### 4. Use the Test Script

Run the automated testing script:

```bash
./test-api.sh
```

This script tests all endpoints with various valid and invalid inputs.

## Common HTTP 400 Scenarios

### 1. Invalid JSON Format

**Problem**: Malformed JSON in request body
**Example**: Missing comma, unclosed brackets, etc.

```json
{
  "question": "test"
  "invalid": true  // Missing comma
}
```

**Solution**: Validate JSON format using tools like `jq` or online validators

### 2. Missing Required Fields

**Problem**: The `question` field is required but missing

```json
{
  "notQuestion": "This will fail"
}
```

**Solution**: Ensure request includes required `question` field:

```json
{
  "question": "Your question here"
}
```

### 3. Null or Empty Values

**Problem**: Required fields are null or empty

```json
{
  "question": null
}
```

or

```json
{
  "question": ""
}
```

**Solution**: Provide non-empty string values:

```json
{
  "question": "A valid question"
}
```

### 4. Incorrect Content-Type

**Problem**: Missing or incorrect Content-Type header

```bash
# Wrong
curl -H "Content-Type: text/plain" ...

# Correct
curl -H "Content-Type: application/json" ...
```

### 5. Empty Request Body

**Problem**: POST request with no body

```bash
# Wrong
curl -X POST http://localhost:8080/api/snarky

# Correct
curl -X POST http://localhost:8080/api/snarky \
  -H "Content-Type: application/json" \
  -d '{"question": "test"}'
```

## Debugging Tools and Features

### 1. Global Exception Mapper

The `GlobalExceptionMapper` catches all exceptions and provides detailed error responses:

```json
{
  "error": "JSON Parse Error",
  "message": "Invalid JSON format in request body",
  "details": "Unexpected character...",
  "line": 2,
  "column": 15,
  "debugInfo": {
    "exceptionClass": "com.fasterxml.jackson.core.JsonParseException",
    "timestamp": "2024-01-01T12:00:00Z",
    "stackTrace": "...",
    "rootCause": {...}
  }
}
```

### 2. Request/Response Logging Filter

The `LoggingFilter` logs all HTTP traffic:

```
=== HTTP REQUEST START ===
Method: POST
URI: http://localhost:8080/api/snarky
Headers:
  Content-Type = [application/json]
  Accept = [application/json]
Request Body:
{"question": "test"}
=== HTTP REQUEST END ===
```

### 3. Enhanced Error Handling

The main endpoint includes comprehensive validation:

```java
// Validates request is not null
// Validates question field is not null
// Validates question field is not empty
// Logs all validation failures with details
```

## Troubleshooting Workflow

### Step 1: Check Service Health

```bash
curl http://localhost:8080/api/debug/health
```

Expected: HTTP 200 with status "UP"

### Step 2: Verify Configuration

```bash
curl http://localhost:8080/api/debug/info
```

Check:
- `configuration.chatModelAvailable: true`
- `configuration.apiKeyConfigured: true/false`

### Step 3: Test Request Validation

```bash
curl -X POST http://localhost:8080/api/debug/validate-request \
  -H "Content-Type: application/json" \
  -d '{"question": "test"}'
```

Expected: HTTP 200 with status "VALID"

### Step 4: Test Main Endpoint

```bash
curl -X POST http://localhost:8080/api/snarky \
  -H "Content-Type: application/json" \
  -d '{"question": "test"}'
```

Expected: HTTP 500 (if no API key) or HTTP 200 (if API key configured)
NOT Expected: HTTP 400 (indicates request parsing issue)

### Step 5: Check Logs

Monitor application logs for detailed error information:

```bash
# If running with Gradle
./gradlew quarkusDev

# Look for log entries from:
# - GlobalExceptionMapper
# - LoggingFilter
# - SnarkyResponseResource
# - ChatModelProducer
```

## Configuration Issues

### OpenAI API Key

Set the API key:

```bash
export OPENAI_API_KEY="your-actual-api-key"
```

Or in `application.properties`:

```properties
openai.api-key=your-actual-api-key
```

### Logging Configuration

Ensure debug logging is enabled:

```properties
quarkus.log.category."dev.agents4j".level=DEBUG
quarkus.http.access-log.enabled=true
```

## Testing with Different Tools

### cURL Examples

```bash
# Valid request
curl -X POST http://localhost:8080/api/snarky \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"question": "What is AI?"}' \
  -v

# Test with malformed JSON
curl -X POST http://localhost:8080/api/snarky \
  -H "Content-Type: application/json" \
  -d '{"question": "test"' \
  -v

# Test with wrong content type
curl -X POST http://localhost:8080/api/snarky \
  -H "Content-Type: text/plain" \
  -d '{"question": "test"}' \
  -v
```

### Postman/REST Client

1. Set URL: `http://localhost:8080/api/snarky`
2. Set Method: POST
3. Set Headers:
   - `Content-Type: application/json`
   - `Accept: application/json`
4. Set Body (raw JSON):
   ```json
   {
     "question": "Your question here"
   }
   ```

### HTTPie Examples

```bash
# Valid request
http POST localhost:8080/api/snarky question="What is AI?"

# Test validation
http POST localhost:8080/api/debug/validate-request question="test"
```

## Common Fixes

### Fix 1: JSON Validation

Before sending requests, validate JSON:

```bash
echo '{"question": "test"}' | jq .
```

### Fix 2: Header Validation

Always include proper headers:

```bash
-H "Content-Type: application/json"
-H "Accept: application/json"
```

### Fix 3: Request Body Validation

Ensure request body matches expected schema:

```json
{
  "question": "string (required, non-empty)"
}
```

### Fix 4: Service Dependencies

Verify all dependencies are available:

```bash
curl http://localhost:8080/api/debug/system-check
```

## Advanced Debugging

### Enable JAX-RS Debug Logging

```properties
quarkus.log.category."org.jboss.resteasy.resteasy_jaxrs.i18n".level=DEBUG
quarkus.log.category."org.jboss.resteasy.core".level=DEBUG
```

### Enable Jackson Debug Logging

```properties
quarkus.log.category."com.fasterxml.jackson".level=DEBUG
```

### Monitor Network Traffic

Use tools like Wireshark or tcpdump to inspect actual HTTP traffic.

### JVM Debugging

Enable JVM debugging:

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar app.jar
```

## Getting Help

If you continue experiencing HTTP 400 errors:

1. Run the test script: `./test-api.sh`
2. Check all debug endpoints
3. Review application logs with DEBUG level enabled
4. Verify JSON format with online validators
5. Test with simple tools like cURL first
6. Compare working vs non-working requests

The enhanced debugging features in this service provide comprehensive information to identify and resolve HTTP 400 errors quickly.