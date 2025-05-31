package dev.agents4j.impl.validation;

import dev.agents4j.api.validation.ValidationResult;
import dev.agents4j.api.validation.ValidationRule;

/**
 * Validation rule that checks if an object is not null.
 */
public class NotNullValidationRule<T> implements ValidationRule<T> {
    
    private final String fieldName;
    
    public NotNullValidationRule(String fieldName) {
        this.fieldName = fieldName;
    }
    
    @Override
    public ValidationResult validate(T object) {
        if (object == null) {
            return ValidationResult.failure(fieldName + " cannot be null");
        }
        return ValidationResult.success();
    }
    
    @Override
    public String getDescription() {
        return "Validates that " + fieldName + " is not null";
    }
}