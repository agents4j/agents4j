/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Metadata information about a workflow type.
 * This class provides descriptive information about workflow capabilities,
 * requirements, and characteristics.
 */
public class WorkflowTypeMetadata {
    
    private final String name;
    private final String description;
    private final String version;
    private final Set<String> capabilities;
    private final Map<String, String> properties;
    private final Set<String> requiredProperties;
    private final Set<String> optionalProperties;
    
    /**
     * Creates a new WorkflowTypeMetadata with basic information.
     *
     * @param name The display name of the workflow type
     * @param description A description of what this workflow type does
     */
    public WorkflowTypeMetadata(String name, String description) {
        this(name, description, "1.0.0", Set.of(), Map.of(), Set.of(), Set.of());
    }
    
    /**
     * Creates a new WorkflowTypeMetadata with detailed information.
     *
     * @param name The display name of the workflow type
     * @param description A description of what this workflow type does
     * @param version The version of this workflow type
     * @param capabilities Set of capabilities this workflow type supports
     * @param properties Additional properties and their descriptions
     * @param requiredProperties Set of required configuration property names
     * @param optionalProperties Set of optional configuration property names
     */
    public WorkflowTypeMetadata(
        String name,
        String description,
        String version,
        Set<String> capabilities,
        Map<String, String> properties,
        Set<String> requiredProperties,
        Set<String> optionalProperties
    ) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.capabilities = Set.copyOf(capabilities != null ? capabilities : Set.of());
        this.properties = Map.copyOf(properties != null ? properties : Map.of());
        this.requiredProperties = Set.copyOf(requiredProperties != null ? requiredProperties : Set.of());
        this.optionalProperties = Set.copyOf(optionalProperties != null ? optionalProperties : Set.of());
    }
    
    /**
     * Gets the display name of the workflow type.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the description of the workflow type.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the version of the workflow type.
     *
     * @return The version
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Gets the capabilities supported by this workflow type.
     *
     * @return Set of capability names
     */
    public Set<String> getCapabilities() {
        return capabilities;
    }
    
    /**
     * Gets additional properties and their descriptions.
     *
     * @return Map of property names to descriptions
     */
    public Map<String, String> getProperties() {
        return properties;
    }
    
    /**
     * Gets the required configuration property names.
     *
     * @return Set of required property names
     */
    public Set<String> getRequiredProperties() {
        return requiredProperties;
    }
    
    /**
     * Gets the optional configuration property names.
     *
     * @return Set of optional property names
     */
    public Set<String> getOptionalProperties() {
        return optionalProperties;
    }
    
    /**
     * Checks if this workflow type has a specific capability.
     *
     * @param capability The capability to check
     * @return true if the capability is supported
     */
    public boolean hasCapability(String capability) {
        return capabilities.contains(capability);
    }
    
    /**
     * Checks if a property is required.
     *
     * @param propertyName The property name
     * @return true if the property is required
     */
    public boolean isPropertyRequired(String propertyName) {
        return requiredProperties.contains(propertyName);
    }
    
    /**
     * Checks if a property is optional.
     *
     * @param propertyName The property name
     * @return true if the property is optional
     */
    public boolean isPropertyOptional(String propertyName) {
        return optionalProperties.contains(propertyName);
    }
    
    /**
     * Gets the description of a specific property.
     *
     * @param propertyName The property name
     * @return The property description, or null if not found
     */
    public String getPropertyDescription(String propertyName) {
        return properties.get(propertyName);
    }
    
    /**
     * Builder for creating WorkflowTypeMetadata instances.
     */
    public static class Builder {
        private String name;
        private String description;
        private String version = "1.0.0";
        private final Set<String> capabilities = new java.util.HashSet<>();
        private final Map<String, String> properties = new HashMap<>();
        private final Set<String> requiredProperties = new java.util.HashSet<>();
        private final Set<String> optionalProperties = new java.util.HashSet<>();
        
        /**
         * Sets the workflow type name.
         *
         * @param name The name
         * @return This builder for method chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        /**
         * Sets the workflow type description.
         *
         * @param description The description
         * @return This builder for method chaining
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets the workflow type version.
         *
         * @param version The version
         * @return This builder for method chaining
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        /**
         * Adds a capability.
         *
         * @param capability The capability name
         * @return This builder for method chaining
         */
        public Builder capability(String capability) {
            this.capabilities.add(capability);
            return this;
        }
        
        /**
         * Adds multiple capabilities.
         *
         * @param capabilities The capability names
         * @return This builder for method chaining
         */
        public Builder capabilities(String... capabilities) {
            Collections.addAll(this.capabilities, capabilities);
            return this;
        }
        
        /**
         * Adds a property with description.
         *
         * @param name The property name
         * @param description The property description
         * @return This builder for method chaining
         */
        public Builder property(String name, String description) {
            this.properties.put(name, description);
            return this;
        }
        
        /**
         * Adds a required property.
         *
         * @param name The property name
         * @param description The property description
         * @return This builder for method chaining
         */
        public Builder requiredProperty(String name, String description) {
            this.properties.put(name, description);
            this.requiredProperties.add(name);
            return this;
        }
        
        /**
         * Adds an optional property.
         *
         * @param name The property name
         * @param description The property description
         * @return This builder for method chaining
         */
        public Builder optionalProperty(String name, String description) {
            this.properties.put(name, description);
            this.optionalProperties.add(name);
            return this;
        }
        
        /**
         * Builds the WorkflowTypeMetadata instance.
         *
         * @return A new WorkflowTypeMetadata instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public WorkflowTypeMetadata build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Workflow type name is required");
            }
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("Workflow type description is required");
            }
            
            return new WorkflowTypeMetadata(
                name,
                description,
                version,
                capabilities,
                properties,
                requiredProperties,
                optionalProperties
            );
        }
    }
    
    /**
     * Creates a new builder for WorkflowTypeMetadata.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public String toString() {
        return String.format("WorkflowTypeMetadata{name='%s', description='%s', version='%s'}", 
            name, description, version);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        WorkflowTypeMetadata that = (WorkflowTypeMetadata) obj;
        return java.util.Objects.equals(name, that.name) &&
               java.util.Objects.equals(description, that.description) &&
               java.util.Objects.equals(version, that.version);
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, description, version);
    }
}