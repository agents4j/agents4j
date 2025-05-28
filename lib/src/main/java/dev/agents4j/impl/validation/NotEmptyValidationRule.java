package dev.agents4j.impl.validation;

import dev.agents4j.api.validation.ValidationResult;
import dev.agents4j.api.validation.ValidationRule;

/**
 * Validation rule that checks if a string is not null and not empty.
 */
public class NotEmptyValidationRule implements ValidationRule<String> {
    
    private final String fieldName;
    
    public NotEmptyValidationRule(String fieldName) {
        this.fieldName = fieldName;
    }
    
    @Override
    public ValidationResult validate(String value) {
        if (value == null) {
            return ValidationResult.failure(fieldName + " cannot be null");
        }
        if (value.trim().isEmpty()) {
            return ValidationResult.failure(fieldName + " cannot be empty");
        }
        return ValidationResult.success();
    }
    
    @Override
    public String getDescription() {
        return "Validates that " + fieldName + " is not null or empty";
    }
}