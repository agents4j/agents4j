package dev.agents4j.api.validation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for providing validation services across the workflow system.
 * Separates validation concerns from business logic and enables pluggable
 * validation strategies.
 * 
 * <p>This interface allows for different validation implementations such as
 * schema-based validation, rule-based validation, or external service validation.</p>
 * 
 * @param <T> The type of objects this provider can validate
 */
public interface ValidationProvider<T> {
    
    /**
     * Validates the given object and returns a comprehensive result.
     *
     * @param object The object to validate
     * @return ValidationResult containing success/failure status and details
     */
    ValidationResult validate(T object);
    
    /**
     * Validates the object with additional context information.
     *
     * @param object The object to validate
     * @param context Additional context that may influence validation
     * @return ValidationResult containing success/failure status and details
     */
    ValidationResult validate(T object, Map<String, Object> context);
    
    /**
     * Validates only specific aspects or fields of the object.
     *
     * @param object The object to validate
     * @param aspects Set of aspect names to validate (implementation-specific)
     * @return ValidationResult for the specified aspects only
     */
    ValidationResult validateAspects(T object, Set<String> aspects);
    
    /**
     * Performs a quick validation check, potentially with reduced thoroughness
     * for performance-sensitive scenarios.
     *
     * @param object The object to validate
     * @return true if the object passes basic validation, false otherwise
     */
    boolean isValid(T object);
    
    /**
     * Gets the supported validation aspects for this provider.
     *
     * @return Set of aspect names that can be validated
     */
    Set<String> getSupportedAspects();
    
    /**
     * Gets the validation rules currently applied by this provider.
     *
     * @return List of validation rules
     */
    List<ValidationRule<T>> getValidationRules();
    
    /**
     * Adds a validation rule to this provider.
     *
     * @param rule The validation rule to add
     * @return This provider for method chaining
     */
    ValidationProvider<T> addRule(ValidationRule<T> rule);
    
    /**
     * Removes a validation rule from this provider.
     *
     * @param rule The validation rule to remove
     * @return true if the rule was removed, false if it wasn't found
     */
    boolean removeRule(ValidationRule<T> rule);
    
    /**
     * Clears all validation rules from this provider.
     *
     * @return This provider for method chaining
     */
    ValidationProvider<T> clearRules();
    
    /**
     * Checks if this provider can validate objects of the given type.
     *
     * @param type The class type to check
     * @return true if this provider supports the type, false otherwise
     */
    boolean supports(Class<?> type);
    
    /**
     * Gets the primary type this provider is designed to validate.
     *
     * @return The target validation type
     */
    Class<T> getTargetType();
    
    /**
     * Gets configuration information about this validation provider.
     *
     * @return Map containing provider configuration and capabilities
     */
    default Map<String, Object> getProviderInfo() {
        return Map.of(
            "providerType", getClass().getSimpleName(),
            "targetType", getTargetType().getSimpleName(),
            "ruleCount", getValidationRules().size(),
            "supportedAspects", getSupportedAspects()
        );
    }
    
    /**
     * Creates a validation context for complex validation scenarios.
     *
     * @param rootObject The root object being validated
     * @return A validation context for this validation session
     */
    default ValidationContext createContext(T rootObject) {
        return new SimpleValidationContext(rootObject);
    }
    
    /**
     * Simple implementation of ValidationContext for basic scenarios.
     */
    class SimpleValidationContext implements ValidationContext {
        private final Object rootObject;
        private final Map<String, Object> metadata;
        private String currentPath;
        
        public SimpleValidationContext(Object rootObject) {
            this.rootObject = rootObject;
            this.metadata = new java.util.HashMap<>();
            this.currentPath = "";
        }
        
        @Override
        public String getObjectPath() {
            return currentPath;
        }
        
        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        @Override
        public ValidationContext navigate(String fieldName) {
            SimpleValidationContext newContext = new SimpleValidationContext(rootObject);
            newContext.currentPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;
            newContext.metadata.putAll(this.metadata);
            return newContext;
        }
        
        @Override
        public Object getRootObject() {
            return rootObject;
        }
        
        @Override
        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }
        
        @Override
        public <V> Optional<V> getMetadata(String key, Class<V> type) {
            Object value = metadata.get(key);
            if (type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
            return Optional.empty();
        }
    }
}

/**
 * Interface for validation context that provides information about
 * the current validation state and path.
 */
interface ValidationContext {
    
    /**
     * Gets the current path in the object being validated.
     *
     * @return The object path (e.g., "user.address.street")
     */
    String getObjectPath();
    
    /**
     * Gets metadata associated with this validation context.
     *
     * @return Map of metadata
     */
    Map<String, Object> getMetadata();
    
    /**
     * Navigates to a field or property within the current context.
     *
     * @param fieldName The field name to navigate to
     * @return A new validation context for the specified field
     */
    ValidationContext navigate(String fieldName);
    
    /**
     * Gets the root object being validated.
     *
     * @return The root object
     */
    Object getRootObject();
    
    /**
     * Sets metadata for this validation context.
     *
     * @param key The metadata key
     * @param value The metadata value
     */
    void setMetadata(String key, Object value);
    
    /**
     * Gets typed metadata from this validation context.
     *
     * @param key The metadata key
     * @param type The expected type
     * @param <V> The value type
     * @return The metadata value if present and of correct type
     */
    <V> Optional<V> getMetadata(String key, Class<V> type);
}