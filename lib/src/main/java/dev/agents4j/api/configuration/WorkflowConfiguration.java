package dev.agents4j.api.configuration;

import dev.agents4j.api.validation.ValidationResult;
import dev.agents4j.api.validation.Validator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration for workflows with validation support.
 */
public class WorkflowConfiguration {
    
    private final Map<String, Object> properties;
    private final Validator<WorkflowConfiguration> validator;
    
    private WorkflowConfiguration(Map<String, Object> properties, Validator<WorkflowConfiguration> validator) {
        this.properties = new HashMap<>(properties);
        this.validator = validator;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }
    
    public <T> T getProperty(String key, Class<T> type, T defaultValue) {
        return getProperty(key, type).orElse(defaultValue);
    }
    
    public Map<String, Object> getAllProperties() {
        return Collections.unmodifiableMap(properties);
    }
    
    public ValidationResult validate() {
        if (validator != null) {
            return validator.validate(this);
        }
        return ValidationResult.success();
    }
    
    public static class Builder {
        private final Map<String, Object> properties = new HashMap<>();
        private Validator<WorkflowConfiguration> validator;
        
        public Builder withProperty(String key, Object value) {
            properties.put(key, value);
            return this;
        }
        
        public Builder withProperties(Map<String, Object> props) {
            properties.putAll(props);
            return this;
        }
        
        public Builder withValidator(Validator<WorkflowConfiguration> validator) {
            this.validator = validator;
            return this;
        }
        
        public WorkflowConfiguration build() {
            return new WorkflowConfiguration(properties, validator);
        }
    }
}