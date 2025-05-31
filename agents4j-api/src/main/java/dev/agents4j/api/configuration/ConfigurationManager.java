package dev.agents4j.api.configuration;

import dev.agents4j.api.validation.ValidationResult;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Interface for managing workflow and system configuration.
 * Provides a clean abstraction for configuration access, validation,
 * and dynamic updates while supporting different configuration sources.
 * 
 * <p>This interface enables dependency injection of configuration logic
 * and supports various configuration backends such as files, databases,
 * environment variables, or remote configuration services.</p>
 */
public interface ConfigurationManager {
    
    /**
     * Gets a configuration property value.
     *
     * @param key The configuration key
     * @return The property value, or empty if not found
     */
    Optional<Object> getProperty(String key);
    
    /**
     * Gets a typed configuration property value.
     *
     * @param key The configuration key
     * @param type The expected value type
     * @param <T> The value type
     * @return The typed property value, or empty if not found or wrong type
     */
    <T> Optional<T> getProperty(String key, Class<T> type);
    
    /**
     * Gets a configuration property with a default value.
     *
     * @param key The configuration key
     * @param defaultValue The default value if property is not found
     * @param <T> The value type
     * @return The property value or the default value
     */
    <T> T getProperty(String key, T defaultValue);
    
    /**
     * Gets a configuration property with a type converter.
     *
     * @param key The configuration key
     * @param converter Function to convert the raw value to desired type
     * @param defaultValue The default value if property is not found
     * @param <T> The target type
     * @return The converted property value or the default value
     */
    <T> T getProperty(String key, Function<Object, T> converter, T defaultValue);
    
    /**
     * Sets a configuration property value.
     *
     * @param key The configuration key
     * @param value The property value
     * @return This configuration manager for method chaining
     * @throws UnsupportedOperationException if the manager is read-only
     */
    ConfigurationManager setProperty(String key, Object value);
    
    /**
     * Removes a configuration property.
     *
     * @param key The configuration key to remove
     * @return true if the property was removed, false if it didn't exist
     * @throws UnsupportedOperationException if the manager is read-only
     */
    boolean removeProperty(String key);
    
    /**
     * Gets all configuration properties.
     *
     * @return Map containing all configuration properties
     */
    Map<String, Object> getAllProperties();
    
    /**
     * Gets all configuration properties with a specific prefix.
     *
     * @param prefix The key prefix to filter by
     * @return Map containing properties with the specified prefix
     */
    Map<String, Object> getPropertiesWithPrefix(String prefix);
    
    /**
     * Gets all configuration property keys.
     *
     * @return Set of all configuration keys
     */
    Set<String> getPropertyKeys();
    
    /**
     * Checks if a configuration property exists.
     *
     * @param key The configuration key
     * @return true if the property exists, false otherwise
     */
    boolean hasProperty(String key);
    
    /**
     * Creates a configuration section with a specific prefix.
     * This allows for hierarchical configuration access.
     *
     * @param prefix The section prefix
     * @return A configuration section for the specified prefix
     */
    ConfigurationSection section(String prefix);
    
    /**
     * Validates the current configuration.
     *
     * @return ValidationResult indicating if configuration is valid
     */
    ValidationResult validate();
    
    /**
     * Validates the configuration against specific rules.
     *
     * @param ruleName The name of the validation rule set to apply
     * @return ValidationResult for the specified rules
     */
    ValidationResult validate(String ruleName);
    
    /**
     * Reloads configuration from the underlying source.
     *
     * @return true if reload was successful, false otherwise
     * @throws UnsupportedOperationException if reload is not supported
     */
    default boolean reload() {
        throw new UnsupportedOperationException("Reload not supported by this configuration manager");
    }
    
    /**
     * Saves the current configuration to the underlying storage.
     *
     * @return true if save was successful, false otherwise
     * @throws UnsupportedOperationException if save is not supported
     */
    default boolean save() {
        throw new UnsupportedOperationException("Save not supported by this configuration manager");
    }
    
    /**
     * Checks if this configuration manager supports write operations.
     *
     * @return true if write operations are supported, false otherwise
     */
    boolean isWritable();
    
    /**
     * Checks if this configuration manager supports dynamic reloading.
     *
     * @return true if reload operations are supported, false otherwise
     */
    default boolean supportsReload() {
        return false;
    }
    
    /**
     * Checks if this configuration manager supports persistence.
     *
     * @return true if save operations are supported, false otherwise
     */
    default boolean supportsPersistence() {
        return false;
    }
    
    /**
     * Gets metadata about this configuration manager.
     *
     * @return Map containing manager capabilities and information
     */
    default Map<String, Object> getManagerInfo() {
        return Map.of(
            "managerType", getClass().getSimpleName(),
            "writable", isWritable(),
            "supportsReload", supportsReload(),
            "supportsPersistence", supportsPersistence(),
            "propertyCount", getPropertyKeys().size()
        );
    }
    
    /**
     * Adds a configuration change listener.
     *
     * @param listener The listener to add
     * @return This configuration manager for method chaining
     */
    default ConfigurationManager addChangeListener(ConfigurationChangeListener listener) {
        // Default implementation is no-op for managers that don't support listeners
        return this;
    }
    
    /**
     * Removes a configuration change listener.
     *
     * @param listener The listener to remove
     * @return true if the listener was removed, false if it wasn't found
     */
    default boolean removeChangeListener(ConfigurationChangeListener listener) {
        // Default implementation is no-op for managers that don't support listeners
        return false;
    }
}

/**
 * Interface for configuration sections that provide scoped access to properties.
 */
interface ConfigurationSection {
    
    /**
     * Gets the section prefix.
     *
     * @return The prefix for this configuration section
     */
    String getPrefix();
    
    /**
     * Gets a property value within this section.
     *
     * @param key The property key (relative to section prefix)
     * @return The property value, or empty if not found
     */
    Optional<Object> getProperty(String key);
    
    /**
     * Gets a typed property value within this section.
     *
     * @param key The property key (relative to section prefix)
     * @param type The expected value type
     * @param <T> The value type
     * @return The typed property value, or empty if not found or wrong type
     */
    <T> Optional<T> getProperty(String key, Class<T> type);
    
    /**
     * Gets a property with a default value within this section.
     *
     * @param key The property key (relative to section prefix)
     * @param defaultValue The default value if property is not found
     * @param <T> The value type
     * @return The property value or the default value
     */
    <T> T getProperty(String key, T defaultValue);
    
    /**
     * Sets a property value within this section.
     *
     * @param key The property key (relative to section prefix)
     * @param value The property value
     * @return This configuration section for method chaining
     */
    ConfigurationSection setProperty(String key, Object value);
    
    /**
     * Gets all properties within this section.
     *
     * @return Map containing all properties in this section
     */
    Map<String, Object> getAllProperties();
    
    /**
     * Creates a nested section within this section.
     *
     * @param subPrefix The sub-section prefix
     * @return A configuration section for the nested prefix
     */
    ConfigurationSection section(String subPrefix);
    
    /**
     * Gets the parent configuration manager.
     *
     * @return The parent configuration manager
     */
    ConfigurationManager getParent();
}

/**
 * Interface for listening to configuration changes.
 */
interface ConfigurationChangeListener {
    
    /**
     * Called when a configuration property is added or updated.
     *
     * @param key The property key
     * @param oldValue The previous value (null if property was added)
     * @param newValue The new value
     */
    void onPropertyChanged(String key, Object oldValue, Object newValue);
    
    /**
     * Called when a configuration property is removed.
     *
     * @param key The property key
     * @param oldValue The previous value
     */
    void onPropertyRemoved(String key, Object oldValue);
    
    /**
     * Called when the configuration is reloaded.
     *
     * @param changedKeys Set of keys that changed during reload
     */
    default void onConfigurationReloaded(Set<String> changedKeys) {
        // Default implementation is no-op
    }
}