package dev.agents4j.api.context;

import java.util.*;

/**
 * Specialized context for validation operations.
 * Provides additional validation-specific functionality while maintaining type safety.
 */
public final class ValidationContext implements WorkflowContext {
    
    private final WorkflowContext delegate;
    private final Set<String> validationErrors;
    private final Set<String> validationWarnings;
    
    private ValidationContext(WorkflowContext delegate, Set<String> errors, Set<String> warnings) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate context cannot be null");
        this.validationErrors = Collections.unmodifiableSet(new HashSet<>(errors));
        this.validationWarnings = Collections.unmodifiableSet(new HashSet<>(warnings));
    }
    
    /**
     * Creates an empty validation context.
     *
     * @return An empty ValidationContext
     */
    public static ValidationContext empty() {
        return new ValidationContext(ExecutionContext.empty(), Collections.emptySet(), Collections.emptySet());
    }
    
    /**
     * Creates a validation context from an existing workflow context.
     *
     * @param context The base context
     * @return A new ValidationContext wrapping the provided context
     */
    public static ValidationContext from(WorkflowContext context) {
        return new ValidationContext(context, Collections.emptySet(), Collections.emptySet());
    }
    
    /**
     * Creates a validation context with a single key-value pair.
     *
     * @param key The typed key
     * @param value The value
     * @param <T> The type of the value
     * @return A new ValidationContext with the single entry
     */
    public static <T> ValidationContext of(ContextKey<T> key, T value) {
        return new ValidationContext(ExecutionContext.of(key, value), Collections.emptySet(), Collections.emptySet());
    }
    
    @Override
    public <T> Optional<T> get(ContextKey<T> key) {
        return delegate.get(key);
    }
    
    @Override
    public <T> WorkflowContext with(ContextKey<T> key, T value) {
        return new ValidationContext(delegate.with(key, value), validationErrors, validationWarnings);
    }
    
    @Override
    public WorkflowContext without(ContextKey<?> key) {
        return new ValidationContext(delegate.without(key), validationErrors, validationWarnings);
    }
    
    @Override
    public boolean contains(ContextKey<?> key) {
        return delegate.contains(key);
    }
    
    @Override
    public Set<ContextKey<?>> keys() {
        return delegate.keys();
    }
    
    @Override
    public int size() {
        return delegate.size();
    }
    
    @Override
    public WorkflowContext merge(WorkflowContext other) {
        if (other instanceof ValidationContext validationOther) {
            // Merge validation data as well
            Set<String> mergedErrors = new HashSet<>(this.validationErrors);
            mergedErrors.addAll(validationOther.validationErrors);
            
            Set<String> mergedWarnings = new HashSet<>(this.validationWarnings);
            mergedWarnings.addAll(validationOther.validationWarnings);
            
            return new ValidationContext(delegate.merge(validationOther.delegate), mergedErrors, mergedWarnings);
        } else {
            return new ValidationContext(delegate.merge(other), validationErrors, validationWarnings);
        }
    }
    
    /**
     * Adds a validation error to this context.
     *
     * @param error The error message
     * @return A new ValidationContext with the error added
     */
    public ValidationContext withError(String error) {
        Objects.requireNonNull(error, "Error message cannot be null");
        Set<String> newErrors = new HashSet<>(validationErrors);
        newErrors.add(error);
        return new ValidationContext(delegate, newErrors, validationWarnings);
    }
    
    /**
     * Adds multiple validation errors to this context.
     *
     * @param errors The error messages
     * @return A new ValidationContext with the errors added
     */
    public ValidationContext withErrors(Collection<String> errors) {
        Objects.requireNonNull(errors, "Errors collection cannot be null");
        Set<String> newErrors = new HashSet<>(validationErrors);
        newErrors.addAll(errors);
        return new ValidationContext(delegate, newErrors, validationWarnings);
    }
    
    /**
     * Adds a validation warning to this context.
     *
     * @param warning The warning message
     * @return A new ValidationContext with the warning added
     */
    public ValidationContext withWarning(String warning) {
        Objects.requireNonNull(warning, "Warning message cannot be null");
        Set<String> newWarnings = new HashSet<>(validationWarnings);
        newWarnings.add(warning);
        return new ValidationContext(delegate, validationErrors, newWarnings);
    }
    
    /**
     * Adds multiple validation warnings to this context.
     *
     * @param warnings The warning messages
     * @return A new ValidationContext with the warnings added
     */
    public ValidationContext withWarnings(Collection<String> warnings) {
        Objects.requireNonNull(warnings, "Warnings collection cannot be null");
        Set<String> newWarnings = new HashSet<>(validationWarnings);
        newWarnings.addAll(warnings);
        return new ValidationContext(delegate, validationErrors, newWarnings);
    }
    
    /**
     * Gets all validation errors.
     *
     * @return An unmodifiable set of validation errors
     */
    public Set<String> getErrors() {
        return validationErrors;
    }
    
    /**
     * Gets all validation warnings.
     *
     * @return An unmodifiable set of validation warnings
     */
    public Set<String> getWarnings() {
        return validationWarnings;
    }
    
    /**
     * Checks if this context has any validation errors.
     *
     * @return true if there are validation errors
     */
    public boolean hasErrors() {
        return !validationErrors.isEmpty();
    }
    
    /**
     * Checks if this context has any validation warnings.
     *
     * @return true if there are validation warnings
     */
    public boolean hasWarnings() {
        return !validationWarnings.isEmpty();
    }
    
    /**
     * Checks if this context is valid (no errors).
     *
     * @return true if there are no validation errors
     */
    public boolean isValid() {
        return validationErrors.isEmpty();
    }
    
    /**
     * Gets the count of validation errors.
     *
     * @return The number of validation errors
     */
    public int getErrorCount() {
        return validationErrors.size();
    }
    
    /**
     * Gets the count of validation warnings.
     *
     * @return The number of validation warnings
     */
    public int getWarningCount() {
        return validationWarnings.size();
    }
    
    /**
     * Clears all validation errors and warnings.
     *
     * @return A new ValidationContext with no validation messages
     */
    public ValidationContext clearValidation() {
        return new ValidationContext(delegate, Collections.emptySet(), Collections.emptySet());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationContext that = (ValidationContext) o;
        return Objects.equals(delegate, that.delegate) &&
               Objects.equals(validationErrors, that.validationErrors) &&
               Objects.equals(validationWarnings, that.validationWarnings);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(delegate, validationErrors, validationWarnings);
    }
    
    @Override
    public String toString() {
        return String.format("ValidationContext{size=%d, errors=%d, warnings=%d}", 
            delegate.size(), validationErrors.size(), validationWarnings.size());
    }
}