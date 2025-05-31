/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Configuration object for creating workflows.
 * This class provides a type-safe way to pass configuration parameters
 * to workflow providers.
 */
public class WorkflowConfig {
    
    private final String workflowName;
    private final Map<String, Object> properties;
    
    /**
     * Creates a new WorkflowConfig.
     *
     * @param workflowName The name of the workflow to create
     * @param properties Configuration properties
     */
    public WorkflowConfig(String workflowName, Map<String, Object> properties) {
        if (workflowName == null || workflowName.trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow name cannot be null or empty");
        }
        this.workflowName = workflowName;
        this.properties = new HashMap<>(properties != null ? properties : Map.of());
    }
    
    /**
     * Gets the workflow name.
     *
     * @return The workflow name
     */
    public String getWorkflowName() {
        return workflowName;
    }
    
    /**
     * Gets all configuration properties.
     *
     * @return Unmodifiable map of properties
     */
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }
    
    /**
     * Gets a configuration property value.
     *
     * @param key The property key
     * @param <T> The expected type of the value
     * @return The property value wrapped in an Optional, or empty if not found
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key) {
        return Optional.ofNullable((T) properties.get(key));
    }
    
    /**
     * Gets a configuration property value with a default.
     *
     * @param key The property key
     * @param defaultValue The default value if the property is not found
     * @param <T> The expected type of the value
     * @return The property value, or defaultValue if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        return (T) properties.getOrDefault(key, defaultValue);
    }
    
    /**
     * Gets a required configuration property value.
     *
     * @param key The property key
     * @param <T> The expected type of the value
     * @return The property value
     * @throws IllegalArgumentException if the property is not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getRequiredProperty(String key) {
        T value = (T) properties.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required property '" + key + "' is not present");
        }
        return value;
    }
    
    /**
     * Gets a string property value.
     *
     * @param key The property key
     * @return The string value wrapped in an Optional, or empty if not found
     */
    public Optional<String> getStringProperty(String key) {
        return getProperty(key);
    }
    
    /**
     * Gets a string property value with a default.
     *
     * @param key The property key
     * @param defaultValue The default value if the property is not found
     * @return The string value, or defaultValue if not found
     */
    public String getStringProperty(String key, String defaultValue) {
        return getProperty(key, defaultValue);
    }
    
    /**
     * Gets an integer property value.
     *
     * @param key The property key
     * @return The integer value wrapped in an Optional, or empty if not found
     */
    public Optional<Integer> getIntProperty(String key) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return Optional.of(((Number) value).intValue());
        }
        return Optional.empty();
    }
    
    /**
     * Gets an integer property value with a default.
     *
     * @param key The property key
     * @param defaultValue The default value if the property is not found
     * @return The integer value, or defaultValue if not found
     */
    public int getIntProperty(String key, int defaultValue) {
        return getIntProperty(key).orElse(defaultValue);
    }
    
    /**
     * Gets a boolean property value.
     *
     * @param key The property key
     * @return The boolean value wrapped in an Optional, or empty if not found
     */
    public Optional<Boolean> getBooleanProperty(String key) {
        Object value = properties.get(key);
        if (value instanceof Boolean) {
            return Optional.of((Boolean) value);
        }
        return Optional.empty();
    }
    
    /**
     * Gets a boolean property value with a default.
     *
     * @param key The property key
     * @param defaultValue The default value if the property is not found
     * @return The boolean value, or defaultValue if not found
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        return getBooleanProperty(key).orElse(defaultValue);
    }
    
    /**
     * Checks if a property exists.
     *
     * @param key The property key
     * @return true if the property exists
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
    
    /**
     * Gets all property keys.
     *
     * @return Set of property keys
     */
    public Set<String> getPropertyKeys() {
        return Collections.unmodifiableSet(properties.keySet());
    }
    
    /**
     * Creates a new WorkflowConfig with an additional property.
     *
     * @param key The property key
     * @param value The property value
     * @return A new WorkflowConfig instance with the added property
     */
    public WorkflowConfig withProperty(String key, Object value) {
        Map<String, Object> newProperties = new HashMap<>(properties);
        newProperties.put(key, value);
        return new WorkflowConfig(workflowName, newProperties);
    }
    
    /**
     * Creates a new WorkflowConfig with additional properties.
     *
     * @param additionalProperties The properties to add
     * @return A new WorkflowConfig instance with the added properties
     */
    public WorkflowConfig withProperties(Map<String, Object> additionalProperties) {
        Map<String, Object> newProperties = new HashMap<>(properties);
        if (additionalProperties != null) {
            newProperties.putAll(additionalProperties);
        }
        return new WorkflowConfig(workflowName, newProperties);
    }
    
    /**
     * Creates a new WorkflowConfig with a different name.
     *
     * @param newName The new workflow name
     * @return A new WorkflowConfig instance with the new name
     */
    public WorkflowConfig withName(String newName) {
        return new WorkflowConfig(newName, properties);
    }
    
    /**
     * Builder for creating WorkflowConfig instances.
     */
    public static class Builder {
        private String workflowName;
        private final Map<String, Object> properties = new HashMap<>();
        
        /**
         * Sets the workflow name.
         *
         * @param workflowName The workflow name
         * @return This builder for method chaining
         */
        public Builder name(String workflowName) {
            this.workflowName = workflowName;
            return this;
        }
        
        /**
         * Adds a property.
         *
         * @param key The property key
         * @param value The property value
         * @return This builder for method chaining
         */
        public Builder property(String key, Object value) {
            properties.put(key, value);
            return this;
        }
        
        /**
         * Adds multiple properties.
         *
         * @param properties The properties to add
         * @return This builder for method chaining
         */
        public Builder properties(Map<String, Object> properties) {
            if (properties != null) {
                this.properties.putAll(properties);
            }
            return this;
        }
        
        /**
         * Builds the WorkflowConfig instance.
         *
         * @return A new WorkflowConfig instance
         * @throws IllegalArgumentException if workflow name is null or empty
         */
        public WorkflowConfig build() {
            return new WorkflowConfig(workflowName, properties);
        }
    }
    
    /**
     * Creates a new builder for WorkflowConfig.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a new WorkflowConfig with just a name.
     *
     * @param workflowName The workflow name
     * @return A new WorkflowConfig instance
     */
    public static WorkflowConfig of(String workflowName) {
        return new WorkflowConfig(workflowName, Map.of());
    }
    
    /**
     * Creates a new WorkflowConfig with a name and properties.
     *
     * @param workflowName The workflow name
     * @param properties The configuration properties
     * @return A new WorkflowConfig instance
     */
    public static WorkflowConfig of(String workflowName, Map<String, Object> properties) {
        return new WorkflowConfig(workflowName, properties);
    }
    
    @Override
    public String toString() {
        return String.format("WorkflowConfig{name='%s', properties=%s}", workflowName, properties);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        WorkflowConfig that = (WorkflowConfig) obj;
        return workflowName.equals(that.workflowName) && properties.equals(that.properties);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(workflowName, properties);
    }
}