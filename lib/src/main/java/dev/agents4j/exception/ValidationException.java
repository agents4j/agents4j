/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when input validation fails.
 * This exception provides detailed information about validation errors.
 */
public class ValidationException extends RuntimeException {
    
    private final List<ValidationError> errors;
    
    /**
     * Creates a new ValidationException with a single error.
     *
     * @param field The field that failed validation
     * @param message The validation error message
     */
    public ValidationException(String field, String message) {
        this(Collections.singletonList(new ValidationError(field, message)));
    }
    
    /**
     * Creates a new ValidationException with multiple errors.
     *
     * @param errors The list of validation errors
     */
    public ValidationException(List<ValidationError> errors) {
        super(formatMessage(errors));
        this.errors = new ArrayList<>(errors != null ? errors : Collections.emptyList());
    }
    
    /**
     * Gets an unmodifiable view of the validation errors.
     *
     * @return The list of validation errors
     */
    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    /**
     * Gets the number of validation errors.
     *
     * @return The error count
     */
    public int getErrorCount() {
        return errors.size();
    }
    
    /**
     * Checks if there are validation errors for a specific field.
     *
     * @param field The field name
     * @return true if there are errors for the field
     */
    public boolean hasErrorsFor(String field) {
        return errors.stream().anyMatch(error -> field.equals(error.getField()));
    }
    
    /**
     * Gets validation errors for a specific field.
     *
     * @param field The field name
     * @return List of errors for the field
     */
    public List<ValidationError> getErrorsFor(String field) {
        return errors.stream()
            .filter(error -> field.equals(error.getField()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Gets the first validation error.
     *
     * @return The first error, or null if no errors
     */
    public ValidationError getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }
    
    /**
     * Creates a formatted error message from validation errors.
     *
     * @param errors The validation errors
     * @return The formatted message
     */
    private static String formatMessage(List<ValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            return "Validation failed";
        }
        
        if (errors.size() == 1) {
            ValidationError error = errors.get(0);
            return String.format("Validation failed for field '%s': %s", error.getField(), error.getMessage());
        }
        
        StringBuilder sb = new StringBuilder("Validation failed with ");
        sb.append(errors.size()).append(" errors:");
        
        for (ValidationError error : errors) {
            sb.append(String.format("\n  - %s: %s", error.getField(), error.getMessage()));
        }
        
        return sb.toString();
    }
    
    /**
     * Represents a single validation error.
     */
    public static class ValidationError {
        private final String field;
        private final String message;
        
        /**
         * Creates a new ValidationError.
         *
         * @param field The field that failed validation
         * @param message The error message
         */
        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }
        
        /**
         * Gets the field name.
         *
         * @return The field name
         */
        public String getField() {
            return field;
        }
        
        /**
         * Gets the error message.
         *
         * @return The error message
         */
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return String.format("ValidationError{field='%s', message='%s'}", field, message);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            ValidationError that = (ValidationError) obj;
            return java.util.Objects.equals(field, that.field) && 
                   java.util.Objects.equals(message, that.message);
        }
        
        @Override
        public int hashCode() {
            return java.util.Objects.hash(field, message);
        }
    }
}