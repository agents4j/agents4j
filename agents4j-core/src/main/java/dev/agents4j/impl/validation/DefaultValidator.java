package dev.agents4j.impl.validation;

import dev.agents4j.api.validation.ValidationResult;
import dev.agents4j.api.validation.ValidationRule;
import dev.agents4j.api.validation.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of the Validator interface.
 * Manages a collection of validation rules and combines their results.
 */
public class DefaultValidator<T> implements Validator<T> {
    
    private final List<ValidationRule<T>> rules;
    
    public DefaultValidator() {
        this.rules = new ArrayList<>();
    }
    
    public DefaultValidator(List<ValidationRule<T>> rules) {
        this.rules = new ArrayList<>(rules);
    }
    
    @Override
    public Validator<T> addRule(ValidationRule<T> rule) {
        if (rule != null) {
            rules.add(rule);
        }
        return this;
    }
    
    @Override
    public ValidationResult validate(T object) {
        ValidationResult combinedResult = ValidationResult.success();
        
        for (ValidationRule<T> rule : rules) {
            ValidationResult ruleResult = rule.validate(object);
            combinedResult = combinedResult.combine(ruleResult);
        }
        
        return combinedResult;
    }
    
    @Override
    public List<ValidationRule<T>> getRules() {
        return Collections.unmodifiableList(rules);
    }
    
    /**
     * Create a new validator with the given rules.
     *
     * @param rules The validation rules
     * @param <T> The type to validate
     * @return A new validator instance
     */
    @SafeVarargs
    public static <T> DefaultValidator<T> of(ValidationRule<T>... rules) {
        DefaultValidator<T> validator = new DefaultValidator<>();
        for (ValidationRule<T> rule : rules) {
            validator.addRule(rule);
        }
        return validator;
    }
}