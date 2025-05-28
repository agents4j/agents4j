package dev.agents4j.api.validation;

import java.util.List;

/**
 * Interface for validators that can validate objects using multiple rules.
 */
public interface Validator<T> {
    
    /**
     * Add a validation rule.
     *
     * @param rule The validation rule to add
     * @return This validator for method chaining
     */
    Validator<T> addRule(ValidationRule<T> rule);
    
    /**
     * Validate the given object using all registered rules.
     *
     * @param object The object to validate
     * @return The combined validation result
     */
    ValidationResult validate(T object);
    
    /**
     * Get all registered validation rules.
     *
     * @return The list of validation rules
     */
    List<ValidationRule<T>> getRules();
}