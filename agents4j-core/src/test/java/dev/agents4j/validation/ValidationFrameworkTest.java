package dev.agents4j.validation;

import static org.junit.jupiter.api.Assertions.*;

import dev.agents4j.api.validation.ValidationResult;
import dev.agents4j.api.validation.ValidationRule;
import dev.agents4j.api.validation.Validator;
import dev.agents4j.exception.ValidationException;
import dev.agents4j.impl.validation.DefaultValidator;
import dev.agents4j.impl.validation.NotEmptyValidationRule;
import dev.agents4j.impl.validation.NotNullValidationRule;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidationFrameworkTest {

    @Test
    void shouldValidateNotNullRule() {
        // Given
        ValidationFramework validator = ValidationFramework.<String>builder()
            .notNull("value", input -> input)
            .build();

        // When & Then
        ValidationFramework.ValidationResult result1 = validator.validate(
            "test"
        );
        assertTrue(result1.isValid());

        ValidationFramework.ValidationResult result2 = validator.validate(null);
        assertFalse(result2.isValid());
        assertEquals(1, result2.getErrors().size());
        assertEquals("value", result2.getErrors().get(0).getField());
    }

    @Test
    void shouldValidateNotEmptyRule() {
        // Given
        ValidationFramework validator = ValidationFramework.<String>builder()
            .notEmpty("value", input -> input)
            .build();

        // When & Then
        ValidationFramework.ValidationResult result1 = validator.validate(
            "test"
        );
        assertTrue(result1.isValid());

        ValidationFramework.ValidationResult result2 = validator.validate("");
        assertFalse(result2.isValid());

        ValidationFramework.ValidationResult result3 = validator.validate(
            "   "
        );
        assertFalse(result3.isValid());

        ValidationFramework.ValidationResult result4 = validator.validate(null);
        assertFalse(result4.isValid());
    }

    @Test
    void shouldValidatePositiveRule() {
        // Given
        ValidationFramework validator = ValidationFramework.<Integer>builder()
            .positive("value", input -> input)
            .build();

        // When & Then
        ValidationFramework.ValidationResult result1 = validator.validate(5);
        assertTrue(result1.isValid());

        ValidationFramework.ValidationResult result2 = validator.validate(0);
        assertFalse(result2.isValid());

        ValidationFramework.ValidationResult result3 = validator.validate(-1);
        assertFalse(result3.isValid());
    }

    @Test
    void shouldValidateMinRule() {
        // Given
        ValidationFramework validator = ValidationFramework.<Integer>builder()
            .min("value", input -> input, 10)
            .build();

        // When & Then
        ValidationFramework.ValidationResult result1 = validator.validate(15);
        assertTrue(result1.isValid());

        ValidationFramework.ValidationResult result2 = validator.validate(10);
        assertTrue(result2.isValid());

        ValidationFramework.ValidationResult result3 = validator.validate(5);
        assertFalse(result3.isValid());
    }

    @Test
    void shouldValidateMaxRule() {
        // Given
        ValidationFramework validator = ValidationFramework.<Integer>builder()
            .max("value", input -> input, 100)
            .build();

        // When & Then
        ValidationFramework.ValidationResult result1 = validator.validate(50);
        assertTrue(result1.isValid());

        ValidationFramework.ValidationResult result2 = validator.validate(100);
        assertTrue(result2.isValid());

        ValidationFramework.ValidationResult result3 = validator.validate(150);
        assertFalse(result3.isValid());
    }

    @Test
    void shouldValidateSizeRule() {
        // Given
        ValidationFramework validator = ValidationFramework.<
                List<String>
            >builder()
            .size("items", input -> input, 2, 5)
            .build();

        // When & Then
        ValidationFramework.ValidationResult result1 = validator.validate(
            List.of("a", "b", "c")
        );
        assertTrue(result1.isValid());

        ValidationFramework.ValidationResult result2 = validator.validate(
            List.of("a")
        );
        assertFalse(result2.isValid());

        ValidationFramework.ValidationResult result3 = validator.validate(
            List.of("a", "b", "c", "d", "e", "f")
        );
        assertFalse(result3.isValid());
    }

    @Test
    void shouldValidateMultipleRules() {
        // Given
        ValidationFramework validator = ValidationFramework.<
                TestObject
            >builder()
            .notEmpty("name", TestObject::getName)
            .positive("age", TestObject::getAge)
            .min("score", TestObject::getScore, 0)
            .max("score", TestObject::getScore, 100)
            .build();

        // When & Then - valid object
        TestObject validObject = new TestObject("John", 25, 85);
        ValidationFramework.ValidationResult result1 = validator.validate(
            validObject
        );
        assertTrue(result1.isValid());

        // When & Then - invalid object
        TestObject invalidObject = new TestObject("", -5, 150);
        ValidationFramework.ValidationResult result2 = validator.validate(
            invalidObject
        );
        assertFalse(result2.isValid());
        assertEquals(3, result2.getErrors().size());
    }

    @Test
    void shouldValidateAndThrowOnFailure() {
        // Given
        ValidationFramework validator = ValidationFramework.<String>builder()
            .notEmpty("value", input -> input)
            .build();

        // When & Then - valid input
        assertDoesNotThrow(() -> validator.validateAndThrow("test"));

        // When & Then - invalid input
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> validator.validateAndThrow("")
        );
        assertEquals(1, exception.getErrorCount());
    }

    @Test
    void shouldUseCommonValidators() {
        // When & Then - STRING_NOT_EMPTY
        ValidationFramework.ValidationResult result1 =
            ValidationFramework.CommonValidators.STRING_NOT_EMPTY.validate(
                "test"
            );
        assertTrue(result1.isValid());

        ValidationFramework.ValidationResult result2 =
            ValidationFramework.CommonValidators.STRING_NOT_EMPTY.validate("");
        assertFalse(result2.isValid());

        // When & Then - WORKFLOW_NAME
        ValidationFramework.ValidationResult result3 =
            ValidationFramework.CommonValidators.WORKFLOW_NAME.validate(
                "valid-workflow_name"
            );
        assertTrue(result3.isValid());

        ValidationFramework.ValidationResult result4 =
            ValidationFramework.CommonValidators.WORKFLOW_NAME.validate(
                "invalid workflow name!"
            );
        assertFalse(result4.isValid());

        // When & Then - POSITIVE_INTEGER
        ValidationFramework.ValidationResult result5 =
            ValidationFramework.CommonValidators.POSITIVE_INTEGER.validate(5);
        assertTrue(result5.isValid());

        ValidationFramework.ValidationResult result6 =
            ValidationFramework.CommonValidators.POSITIVE_INTEGER.validate(-1);
        assertFalse(result6.isValid());
    }

    @Test
    void shouldCreateValidationResultsCorrectly() {
        // Given
        ValidationException.ValidationError error =
            new ValidationException.ValidationError("field", "message");

        // When
        ValidationFramework.ValidationResult success =
            ValidationFramework.ValidationResult.success();
        ValidationFramework.ValidationResult failure1 =
            ValidationFramework.ValidationResult.failure(List.of(error));
        ValidationFramework.ValidationResult failure2 =
            ValidationFramework.ValidationResult.failure("field", "message");

        // Then
        assertTrue(success.isValid());
        assertTrue(success.getErrors().isEmpty());

        assertFalse(failure1.isValid());
        assertEquals(1, failure1.getErrors().size());

        assertFalse(failure2.isValid());
        assertEquals(1, failure2.getErrors().size());
        assertEquals("field", failure2.getErrors().get(0).getField());
        assertEquals("message", failure2.getErrors().get(0).getMessage());
    }

    @Test
    void shouldHandleCustomRules() {
        // Given
        ValidationFramework validator = ValidationFramework.<String>builder()
            .rule(
                "email",
                input -> input.contains("@"),
                "must be a valid email"
            )
            .build();

        // When & Then
        ValidationFramework.ValidationResult result1 = validator.validate(
            "test@example.com"
        );
        assertTrue(result1.isValid());

        ValidationFramework.ValidationResult result2 = validator.validate(
            "invalid-email"
        );
        assertFalse(result2.isValid());
        assertEquals(
            "must be a valid email",
            result2.getErrors().get(0).getMessage()
        );
    }

    @Test
    void shouldHandleValidationExceptionsDuringValidation() {
        // Given
        ValidationFramework validator = ValidationFramework.<String>builder()
            .rule(
                "test",
                input -> {
                    throw new RuntimeException("Validation error");
                },
                "original message"
            )
            .build();

        // When
        ValidationFramework.ValidationResult result = validator.validate(
            "test"
        );

        // Then
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(
            result.getErrors().get(0).getMessage().contains("Validation error")
        );
    }

    @Test
    void testValidationResultSuccess() {
        ValidationResult result = ValidationResult.success();

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
        assertFalse(result.hasWarnings());
    }

    @Test
    void testValidationResultFailureWithSingleError() {
        String error = "Field is required";
        ValidationResult result = ValidationResult.failure(error);

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(error, result.getErrors().get(0));
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void testValidationResultFailureWithMultipleErrors() {
        List<String> errors = Arrays.asList("Error 1", "Error 2", "Error 3");
        ValidationResult result = ValidationResult.failure(errors);

        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
        assertEquals(errors, result.getErrors());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void testValidationResultWithWarnings() {
        List<String> warnings = Arrays.asList("Warning 1", "Warning 2");
        ValidationResult result = ValidationResult.withWarnings(warnings);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(2, result.getWarnings().size());
        assertEquals(warnings, result.getWarnings());
        assertTrue(result.hasWarnings());
    }

    @Test
    void testValidationResultWithErrorsAndWarnings() {
        List<String> errors = Arrays.asList("Error 1");
        List<String> warnings = Arrays.asList("Warning 1", "Warning 2");
        ValidationResult result = ValidationResult.withErrorsAndWarnings(
            errors,
            warnings
        );

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals(2, result.getWarnings().size());
        assertEquals(errors, result.getErrors());
        assertEquals(warnings, result.getWarnings());
        assertTrue(result.hasWarnings());
    }

    @Test
    void testValidationResultCombine() {
        ValidationResult result1 = ValidationResult.failure("Error 1");
        ValidationResult result2 = ValidationResult.withWarnings(
            Arrays.asList("Warning 1")
        );

        ValidationResult combined = result1.combine(result2);

        assertFalse(combined.isValid());
        assertEquals(1, combined.getErrors().size());
        assertEquals("Error 1", combined.getErrors().get(0));
        assertEquals(1, combined.getWarnings().size());
        assertEquals("Warning 1", combined.getWarnings().get(0));
    }

    @Test
    void testNotNullValidationRule() {
        NotNullValidationRule<String> rule = new NotNullValidationRule<>(
            "testField"
        );

        assertEquals(
            "Validates that testField is not null",
            rule.getDescription()
        );

        ValidationResult validResult = rule.validate("valid string");
        assertTrue(validResult.isValid());

        ValidationResult invalidResult = rule.validate(null);
        assertFalse(invalidResult.isValid());
        assertEquals(1, invalidResult.getErrors().size());
        assertTrue(
            invalidResult
                .getErrors()
                .get(0)
                .contains("testField cannot be null")
        );
    }

    @Test
    void testNotEmptyValidationRule() {
        NotEmptyValidationRule rule = new NotEmptyValidationRule("testField");

        assertEquals(
            "Validates that testField is not null or empty",
            rule.getDescription()
        );

        ValidationResult validResult = rule.validate("valid string");
        assertTrue(validResult.isValid());

        ValidationResult nullResult = rule.validate(null);
        assertFalse(nullResult.isValid());
        assertTrue(
            nullResult.getErrors().get(0).contains("testField cannot be null")
        );

        ValidationResult emptyResult = rule.validate("");
        assertFalse(emptyResult.isValid());
        assertTrue(
            emptyResult.getErrors().get(0).contains("testField cannot be empty")
        );

        ValidationResult whitespaceResult = rule.validate("   ");
        assertFalse(whitespaceResult.isValid());
        assertTrue(
            whitespaceResult
                .getErrors()
                .get(0)
                .contains("testField cannot be empty")
        );
    }

    @Test
    void testDefaultValidatorEmpty() {
        DefaultValidator<String> validator = new DefaultValidator<>();

        ValidationResult result = validator.validate("test");
        assertTrue(result.isValid());
        assertTrue(validator.getRules().isEmpty());
    }

    @Test
    void testDefaultValidatorWithRules() {
        DefaultValidator<String> validator = new DefaultValidator<>();
        NotNullValidationRule<String> notNullRule = new NotNullValidationRule<>(
            "field"
        );
        NotEmptyValidationRule notEmptyRule = new NotEmptyValidationRule(
            "field"
        );

        validator.addRule(notNullRule);
        validator.addRule(notEmptyRule);

        assertEquals(2, validator.getRules().size());
        assertTrue(validator.getRules().contains(notNullRule));
        assertTrue(validator.getRules().contains(notEmptyRule));

        ValidationResult validResult = validator.validate("valid string");
        assertTrue(validResult.isValid());

        ValidationResult invalidResult = validator.validate(null);
        assertFalse(invalidResult.isValid());
        assertEquals(2, invalidResult.getErrors().size());
    }

    @Test
    void testDefaultValidatorStaticFactory() {
        NotNullValidationRule<String> notNullRule = new NotNullValidationRule<>(
            "field"
        );
        NotEmptyValidationRule notEmptyRule = new NotEmptyValidationRule(
            "field"
        );

        DefaultValidator<String> validator = DefaultValidator.of(
            notNullRule,
            notEmptyRule
        );

        assertEquals(2, validator.getRules().size());

        ValidationResult result = validator.validate("valid");
        assertTrue(result.isValid());
    }

    @Test
    void testDefaultValidatorWithNullRule() {
        DefaultValidator<String> validator = new DefaultValidator<>();
        validator.addRule(null);

        assertTrue(validator.getRules().isEmpty());
    }

    @Test
    void testValidationResultListsAreImmutable() {
        List<String> errors = Arrays.asList("Error 1");
        ValidationResult result = ValidationResult.failure(errors);

        assertThrows(UnsupportedOperationException.class, () -> {
            result.getErrors().add("Error 2");
        });

        List<String> warnings = Arrays.asList("Warning 1");
        ValidationResult resultWithWarnings = ValidationResult.withWarnings(
            warnings
        );

        assertThrows(UnsupportedOperationException.class, () -> {
            resultWithWarnings.getWarnings().add("Warning 2");
        });
    }

    @Test
    void testValidatorRulesListIsImmutable() {
        DefaultValidator<String> validator = new DefaultValidator<>();
        validator.addRule(new NotNullValidationRule<>("field"));

        assertThrows(UnsupportedOperationException.class, () -> {
            validator.getRules().add(new NotEmptyValidationRule("field"));
        });
    }

    @Test
    void testCustomValidationRule() {
        ValidationRule<Integer> positiveRule = new ValidationRule<Integer>() {
            @Override
            public ValidationResult validate(Integer value) {
                if (value != null && value > 0) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Value must be positive");
            }

            @Override
            public String getDescription() {
                return "Validates that value is positive";
            }
        };

        assertEquals(
            "Validates that value is positive",
            positiveRule.getDescription()
        );

        ValidationResult validResult = positiveRule.validate(5);
        assertTrue(validResult.isValid());

        ValidationResult invalidResult = positiveRule.validate(-1);
        assertFalse(invalidResult.isValid());
        assertEquals(
            "Value must be positive",
            invalidResult.getErrors().get(0)
        );

        ValidationResult nullResult = positiveRule.validate(null);
        assertFalse(nullResult.isValid());
    }

    // Helper class for testing
    private static class TestObject {

        private final String name;
        private final int age;
        private final double score;

        public TestObject(String name, int age, double score) {
            this.name = name;
            this.age = age;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public double getScore() {
            return score;
        }
    }
}
