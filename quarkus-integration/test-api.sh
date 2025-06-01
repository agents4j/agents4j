#!/bin/bash

# Agents4J Quarkus Integration API Testing Script
# This script helps debug HTTP 400 errors and test various API endpoints

set -e

# Configuration
BASE_URL="http://localhost:8080"
CONTENT_TYPE="Content-Type: application/json"
ACCEPT="Accept: application/json"

echo "=== Agents4J Quarkus Integration API Testing ==="
echo "Base URL: $BASE_URL"
echo

# Function to print test headers
print_test_header() {
    echo "=================================================="
    echo "TEST: $1"
    echo "=================================================="
}

# Function to make HTTP request with debugging
debug_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo "--- $description ---"
    echo "Method: $method"
    echo "Endpoint: $BASE_URL$endpoint"
    
    if [ ! -z "$data" ]; then
        echo "Request Body:"
        echo "$data" | jq . 2>/dev/null || echo "$data"
        echo
    fi
    
    echo "Response:"
    if [ "$method" = "GET" ]; then
        curl -s -w "\nHTTP Status: %{http_code}\nTotal Time: %{time_total}s\n" \
             -H "$ACCEPT" \
             "$BASE_URL$endpoint" | jq . 2>/dev/null || cat
    else
        curl -s -w "\nHTTP Status: %{http_code}\nTotal Time: %{time_total}s\n" \
             -X "$method" \
             -H "$CONTENT_TYPE" \
             -H "$ACCEPT" \
             -d "$data" \
             "$BASE_URL$endpoint" | jq . 2>/dev/null || cat
    fi
    echo
    echo
}

# Test 1: Health Check
print_test_header "Health Check"
debug_request "GET" "/api/debug/health" "" "Basic health check"

# Test 2: System Information
print_test_header "System Information"
debug_request "GET" "/api/debug/info" "" "System configuration info"

# Test 3: ChatModel Status
print_test_header "ChatModel Status"
debug_request "GET" "/api/debug/chatmodel-status" "" "ChatModel injection status"

# Test 4: Request Validation - Valid Request
print_test_header "Request Validation - Valid"
VALID_REQUEST='{"question": "How do I make a good cup of coffee?"}'
debug_request "POST" "/api/debug/validate-request" "$VALID_REQUEST" "Valid request validation"

# Test 5: Request Validation - Null Question
print_test_header "Request Validation - Null Question"
NULL_QUESTION='{"question": null}'
debug_request "POST" "/api/debug/validate-request" "$NULL_QUESTION" "Null question validation"

# Test 6: Request Validation - Empty Question
print_test_header "Request Validation - Empty Question"
EMPTY_QUESTION='{"question": ""}'
debug_request "POST" "/api/debug/validate-request" "$EMPTY_QUESTION" "Empty question validation"

# Test 7: Request Validation - Missing Question Field
print_test_header "Request Validation - Missing Field"
MISSING_FIELD='{"notQuestion": "This is wrong"}'
debug_request "POST" "/api/debug/validate-request" "$MISSING_FIELD" "Missing question field"

# Test 8: Malformed JSON
print_test_header "Malformed JSON Test"
MALFORMED_JSON='{"question": "test" "missing": "comma"}'
debug_request "POST" "/api/debug/validate-request" "$MALFORMED_JSON" "Malformed JSON test"

# Test 9: Main Snarky Endpoint - Valid Request
print_test_header "Snarky Endpoint - Valid Request"
SNARKY_REQUEST='{"question": "What is the meaning of life?"}'
debug_request "POST" "/api/snarky" "$SNARKY_REQUEST" "Main snarky endpoint test"

# Test 10: Main Snarky Endpoint - Invalid Request
print_test_header "Snarky Endpoint - Invalid Request"
INVALID_SNARKY='{"notAQuestion": "This will fail"}'
debug_request "POST" "/api/snarky" "$INVALID_SNARKY" "Invalid request to snarky endpoint"

# Test 11: Content Type Issues
print_test_header "Content Type Test"
echo "--- Testing with incorrect Content-Type ---"
echo "Method: POST"
echo "Endpoint: $BASE_URL/api/debug/validate-request"
echo "Content-Type: text/plain (incorrect)"
echo "Request Body: $VALID_REQUEST"
echo
echo "Response:"
curl -s -w "\nHTTP Status: %{http_code}\nTotal Time: %{time_total}s\n" \
     -X POST \
     -H "Content-Type: text/plain" \
     -H "$ACCEPT" \
     -d "$VALID_REQUEST" \
     "$BASE_URL/api/debug/validate-request" | jq . 2>/dev/null || cat
echo
echo

# Test 12: Empty Body Test
print_test_header "Empty Body Test"
echo "--- Testing with empty request body ---"
echo "Method: POST"
echo "Endpoint: $BASE_URL/api/snarky"
echo "Request Body: [empty]"
echo
echo "Response:"
curl -s -w "\nHTTP Status: %{http_code}\nTotal Time: %{time_total}s\n" \
     -X POST \
     -H "$CONTENT_TYPE" \
     -H "$ACCEPT" \
     "$BASE_URL/api/snarky" | jq . 2>/dev/null || cat
echo
echo

# Test 13: System Check
print_test_header "System Check"
debug_request "GET" "/api/debug/system-check" "" "Comprehensive system check"

# Test 14: Large Request Test
print_test_header "Large Request Test"
LARGE_QUESTION="What is the meaning of life, the universe, and everything? Please provide a comprehensive answer that covers philosophical, scientific, and practical perspectives. "
LARGE_QUESTION="${LARGE_QUESTION}${LARGE_QUESTION}${LARGE_QUESTION}"  # Triple the size
LARGE_REQUEST="{\"question\": \"$LARGE_QUESTION\"}"
debug_request "POST" "/api/debug/validate-request" "$LARGE_REQUEST" "Large request validation"

# Test 15: Special Characters Test
print_test_header "Special Characters Test"
SPECIAL_CHARS='{"question": "What about émojis 🤔 and spéciàl chars & symbols? <test>"}'
debug_request "POST" "/api/debug/validate-request" "$SPECIAL_CHARS" "Special characters test"

echo "=================================================="
echo "ALL TESTS COMPLETED"
echo "=================================================="
echo
echo "If you're seeing HTTP 400 errors, check:"
echo "1. JSON format is valid"
echo "2. Content-Type header is 'application/json'"
echo "3. Required fields are present and not null/empty"
echo "4. Request body is not empty"
echo "5. Check the application logs for detailed error messages"
echo
echo "For detailed debugging, check the Quarkus application logs."
echo "To run with verbose curl output, set: export CURL_VERBOSE=1"