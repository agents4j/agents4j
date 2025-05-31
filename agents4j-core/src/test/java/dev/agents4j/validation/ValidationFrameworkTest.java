package dev.agents4j.validation;

import dev.agents4j.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationFrameworkTest {

    @Test
    void shouldValidateNotNullRule() {
        // Given
        ValidationFramework validator = ValidationFramework.<String>builder()
            .notNull("value", input -> input)
            .build();

        // When & Then
        ValidationFramework.ValidationResult result1 = validator.validate("test");
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
        ValidationFramework.ValidationResult result1 = validator.validate("test");
        assertTrue(result1.isValid());

        ValidationFramework.ValidationResult result2 = validator.validate("");
        assertFalse(result2.isValid());

        ValidationFramework.ValidationResult result3 = validator.validate("   ");
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
        ValidationFramework validator = ValidationFramework.<List<String>>builder()
            .size("items", input -> input, 2, 5)
            .build();

        // When & Then
        ValidationFramework.ValidationResult result1 = validator.validate(List.of("a", "b", "c"));
        assertTrue(result1.isValid());

        ValidationFramework.ValidationResult result2 = validator.validate(List.of("a"));
        assertFalse(result2.isValid());

        ValidationFramework.ValidationResult result3 = validator.validate(List.of("a", "b", "c", "d", "e", "f"));
        assertFalse(result3.isValid());
    }

    @Test
    void shouldValidateMultipleRules() {
        // Given
        ValidationFramework validator = ValidationFramework.<TestObject>builder()
            .notEmpty("name", TestObject::getName)
            .positive("age", TestObject::getAge)
            .min("score", TestObject::getScore, 0)
            .max("score", TestObject::getScore, 100)
            .build();

        // When & Then - valid object
        TestObject validObject = new TestObject("John", 25, 85);
        ValidationFramework.ValidationResult result1 = validator.validate(validObject);
        assertTrue(result1.isValid());

        // When & Then - invalid object
        TestObject invalidObject = new TestObject("", -5, 150);
        ValidationFramework.ValidationResult result2 = validator.validate(invalidObject);
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
        ValidationException exception = assertThrows(ValidationException.class, 
            () -> validator.validateAndThrow(""));
        assertEquals(1, exception.getErrorCount());
    }

    @Test
    void shouldUseCommonValidators() {
        // When & Then - STRING_NOT_EMPTY
        ValidationFramework.ValidationResult result1 = 
            ValidationFramework.CommonValidators.STRING_NOT_EMPTY.validate("test");
        assertTrue(result1.isValid());

        ValidationFramework.ValidationResult result2 = 
            ValidationFramework.CommonValidators.STRING_NOT_EMPTY.validate("");
        assertFalse(result2.isValid());

        // When & Then - WORKFLOW_NAME
        ValidationFramework.ValidationResult result3 = 
            ValidationFramework.CommonValidators.WORKFLOW_NAME.validate("valid-workflow_name");
        assertTrue(result3.isValid());

        ValidationFramework.ValidationResult result4 = 
            ValidationFramework.CommonValidators.WORKFLOW_NAME.validate("invalid workflow name!");
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
        ValidationException.ValidationError error = new ValidationException.ValidationError("field", "message");

        // When
        ValidationFramework.ValidationResult success = ValidationFramework.ValidationResult.success();
        ValidationFramework.ValidationResult failure1 = ValidationFramework.ValidationResult.failure(List.of(error));
        ValidationFramework.ValidationResult failure2 = ValidationFramework.ValidationResult.failure("field", "message");

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
            .rule("email", input -> input.contains("@"), "must be a valid email")
            .build();

        // When & Then
        ValidationFramework.ValidationResult result1 = validator.validate("test@example.com");
        assertTrue(result1.isValid());

        ValidationFramework.ValidationResult result2 = validator.validate("invalid-email");
        assertFalse(result2.isValid());
        assertEquals("must be a valid email", result2.getErrors().get(0).getMessage());
    }

    @Test
    void shouldHandleValidationExceptionsDuringValidation() {
        // Given
        ValidationFramework validator = ValidationFramework.<String>builder()
            .rule("test", input -> {
                throw new RuntimeException("Validation error");
            }, "original message")
            .build();

        // When
        ValidationFramework.ValidationResult result = validator.validate("test");

        // Then
        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().contains("Validation error"));
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