package dev.agents4j.api.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a validation operation.
 */
public class ValidationResult {
    
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    
    private ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>(warnings);
    }
    
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList(), Collections.emptyList());
    }
    
    public static ValidationResult failure(String error) {
        return new ValidationResult(false, List.of(error), Collections.emptyList());
    }
    
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors, Collections.emptyList());
    }
    
    public static ValidationResult withWarnings(List<String> warnings) {
        return new ValidationResult(true, Collections.emptyList(), warnings);
    }
    
    public static ValidationResult withErrorsAndWarnings(List<String> errors, List<String> warnings) {
        return new ValidationResult(false, errors, warnings);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public ValidationResult combine(ValidationResult other) {
        List<String> combinedErrors = new ArrayList<>(this.errors);
        combinedErrors.addAll(other.errors);
        
        List<String> combinedWarnings = new ArrayList<>(this.warnings);
        combinedWarnings.addAll(other.warnings);
        
        return new ValidationResult(this.valid && other.valid, combinedErrors, combinedWarnings);
    }
}