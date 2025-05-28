package dev.agents4j.api.workflow;

import java.util.Map;

/**
 * Interface for workflow metadata and configuration.
 */
public interface WorkflowMetadata {
    
    /**
     * Get the name of this workflow.
     *
     * @return The name of the workflow
     */
    String getName();
    
    /**
     * Get the configuration properties of this workflow.
     *
     * @return A map of configuration properties
     */
    Map<String, Object> getConfiguration();
    
    /**
     * Get a specific configuration property.
     *
     * @param key The property key
     * @param defaultValue The default value if property is not found
     * @param <T> The type of the property value
     * @return The property value or default value
     */
    <T> T getConfigurationProperty(String key, T defaultValue);
}