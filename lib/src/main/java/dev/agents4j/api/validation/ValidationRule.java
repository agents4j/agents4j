package dev.agents4j.api.validation;

/**
 * Interface for validation rules.
 */
public interface ValidationRule<T> {
    
    /**
     * Validate the given object.
     *
     * @param object The object to validate
     * @return The validation result
     */
    ValidationResult validate(T object);
    
    /**
     * Get a description of this validation rule.
     *
     * @return The rule description
     */
    String getDescription();
}