/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.validation;

import dev.agents4j.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Validation framework for input validation with fluent API.
 */
public interface ValidationFramework {

    /**
     * Validates an input object and returns a validation result.
     *
     * @param input The input to validate
     * @param <T> The input type
     * @return ValidationResult containing validation status and errors
     */
    <T> ValidationResult validate(T input);

    /**
     * Validates an input object and throws ValidationException if invalid.
     *
     * @param input The input to validate
     * @param <T> The input type
     * @throws ValidationException if validation fails
     */
    default <T> void validateAndThrow(T input) throws ValidationException {
        ValidationResult result = validate(input);
        if (!result.isValid()) {
            throw new ValidationException(result.getErrors());
        }
    }

    /**
     * Creates a new validator builder.
     *
     * @param <T> The type to validate
     * @return A new ValidatorBuilder instance
     */
    static <T> ValidatorBuilder<T> builder() {
        return new ValidatorBuilder<>();
    }

    /**
     * Validation result containing status and errors.
     */
    class ValidationResult {
        private final boolean valid;
        private final List<ValidationException.ValidationError> errors;

        public ValidationResult(boolean valid, List<ValidationException.ValidationError> errors) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors != null ? errors : List.of());
        }

        public boolean isValid() {
            return valid;
        }

        public List<ValidationException.ValidationError> getErrors() {
            return List.copyOf(errors);
        }

        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(List<ValidationException.ValidationError> errors) {
            return new ValidationResult(false, errors);
        }

        public static ValidationResult failure(String field, String message) {
            return new ValidationResult(false, List.of(new ValidationException.ValidationError(field, message)));
        }
    }

    /**
     * Builder for creating validators with fluent API.
     *
     * @param <T> The type to validate
     */
    class ValidatorBuilder<T> {
        private final List<ValidationRule<T>> rules = new ArrayList<>();

        /**
         * Adds a validation rule.
         *
         * @param field The field name
         * @param predicate The validation predicate
         * @param message The error message if validation fails
         * @return This builder for method chaining
         */
        public ValidatorBuilder<T> rule(String field, Predicate<T> predicate, String message) {
            rules.add(new ValidationRule<>(field, predicate, message));
            return this;
        }

        /**
         * Adds a not-null validation rule.
         *
         * @param field The field name
         * @param extractor Function to extract value from input
         * @param <U> The field type
         * @return This builder for method chaining
         */
        public <U> ValidatorBuilder<T> notNull(String field, java.util.function.Function<T, U> extractor) {
            return rule(field, input -> extractor.apply(input) != null, field + " cannot be null");
        }

        /**
         * Adds a not-empty string validation rule.
         *
         * @param field The field name
         * @param extractor Function to extract string value from input
         * @return This builder for method chaining
         */
        public ValidatorBuilder<T> notEmpty(String field, java.util.function.Function<T, String> extractor) {
            return rule(field, input -> {
                String value = extractor.apply(input);
                return value != null && !value.trim().isEmpty();
            }, field + " cannot be null or empty");
        }

        /**
         * Adds a positive number validation rule.
         *
         * @param field The field name
         * @param extractor Function to extract number value from input
         * @return This builder for method chaining
         */
        public ValidatorBuilder<T> positive(String field, java.util.function.Function<T, Number> extractor) {
            return rule(field, input -> {
                Number value = extractor.apply(input);
                return value != null && value.doubleValue() > 0;
            }, field + " must be positive");
        }

        /**
         * Adds a minimum value validation rule.
         *
         * @param field The field name
         * @param extractor Function to extract number value from input
         * @param min The minimum allowed value
         * @return This builder for method chaining
         */
        public ValidatorBuilder<T> min(String field, java.util.function.Function<T, Number> extractor, double min) {
            return rule(field, input -> {
                Number value = extractor.apply(input);
                return value != null && value.doubleValue() >= min;
            }, field + " must be at least " + min);
        }

        /**
         * Adds a maximum value validation rule.
         *
         * @param field The field name
         * @param extractor Function to extract number value from input
         * @param max The maximum allowed value
         * @return This builder for method chaining
         */
        public ValidatorBuilder<T> max(String field, java.util.function.Function<T, Number> extractor, double max) {
            return rule(field, input -> {
                Number value = extractor.apply(input);
                return value != null && value.doubleValue() <= max;
            }, field + " must be at most " + max);
        }

        /**
         * Adds a collection size validation rule.
         *
         * @param field The field name
         * @param extractor Function to extract collection from input
         * @param minSize The minimum collection size
         * @param maxSize The maximum collection size
         * @return This builder for method chaining
         */
        public ValidatorBuilder<T> size(String field, java.util.function.Function<T, java.util.Collection<?>> extractor, int minSize, int maxSize) {
            return rule(field, input -> {
                java.util.Collection<?> value = extractor.apply(input);
                if (value == null) return false;
                int size = value.size();
                return size >= minSize && size <= maxSize;
            }, field + " size must be between " + minSize + " and " + maxSize);
        }

        /**
         * Builds the validator.
         *
         * @return A new ValidationFramework instance
         */
        public ValidationFramework build() {
            return new DefaultValidationFramework<>(List.copyOf(rules));
        }
    }

    /**
     * Represents a single validation rule.
     *
     * @param <T> The input type
     */
    class ValidationRule<T> {
        private final String field;
        private final Predicate<T> predicate;
        private final String message;

        public ValidationRule(String field, Predicate<T> predicate, String message) {
            this.field = field;
            this.predicate = predicate;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public Predicate<T> getPredicate() {
            return predicate;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Default implementation of ValidationFramework.
     *
     * @param <T> The input type
     */
    class DefaultValidationFramework<T> implements ValidationFramework {
        private final List<ValidationRule<T>> rules;

        public DefaultValidationFramework(List<ValidationRule<T>> rules) {
            this.rules = new ArrayList<>(rules);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> ValidationResult validate(U input) {
            List<ValidationException.ValidationError> errors = new ArrayList<>();

            for (ValidationRule<T> rule : rules) {
                try {
                    if (!rule.getPredicate().test((T) input)) {
                        errors.add(new ValidationException.ValidationError(rule.getField(), rule.getMessage()));
                    }
                } catch (Exception e) {
                    errors.add(new ValidationException.ValidationError(
                        rule.getField(),
                        "Validation error: " + e.getMessage()
                    ));
                }
            }

            return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
        }
    }

    /**
     * Common validators for frequently used types.
     */
    class CommonValidators {

        /**
         * Validator for String inputs.
         */
        public static final ValidationFramework STRING_NOT_EMPTY = ValidationFramework.<String>builder()
            .notEmpty("value", input -> input)
            .build();

        /**
         * Validator for workflow names.
         */
        public static final ValidationFramework WORKFLOW_NAME = ValidationFramework.<String>builder()
            .notEmpty("name", input -> input)
            .rule("name", input -> input.length() <= 100, "name must not exceed 100 characters")
            .rule("name", input -> input.matches("^[a-zA-Z0-9_-]+$"), "name must contain only alphanumeric characters, underscores, and hyphens")
            .build();

        /**
         * Validator for positive integers.
         */
        public static final ValidationFramework POSITIVE_INTEGER = ValidationFramework.<Integer>builder()
            .notNull("value", input -> input)
            .positive("value", input -> input)
            .build();
    }
}