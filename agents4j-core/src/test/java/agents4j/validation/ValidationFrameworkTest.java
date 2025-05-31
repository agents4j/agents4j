package agents4j.validation;

import dev.agents4j.api.validation.ValidationResult;
import dev.agents4j.api.validation.ValidationRule;
import dev.agents4j.api.validation.Validator;
import dev.agents4j.impl.validation.DefaultValidator;
import dev.agents4j.impl.validation.NotEmptyValidationRule;
import dev.agents4j.impl.validation.NotNullValidationRule;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationFrameworkTest {

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
        ValidationResult result = ValidationResult.withErrorsAndWarnings(errors, warnings);
        
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
        ValidationResult result2 = ValidationResult.withWarnings(Arrays.asList("Warning 1"));
        
        ValidationResult combined = result1.combine(result2);
        
        assertFalse(combined.isValid());
        assertEquals(1, combined.getErrors().size());
        assertEquals("Error 1", combined.getErrors().get(0));
        assertEquals(1, combined.getWarnings().size());
        assertEquals("Warning 1", combined.getWarnings().get(0));
    }

    @Test
    void testNotNullValidationRule() {
        NotNullValidationRule<String> rule = new NotNullValidationRule<>("testField");
        
        assertEquals("Validates that testField is not null", rule.getDescription());
        
        ValidationResult validResult = rule.validate("valid string");
        assertTrue(validResult.isValid());
        
        ValidationResult invalidResult = rule.validate(null);
        assertFalse(invalidResult.isValid());
        assertEquals(1, invalidResult.getErrors().size());
        assertTrue(invalidResult.getErrors().get(0).contains("testField cannot be null"));
    }

    @Test
    void testNotEmptyValidationRule() {
        NotEmptyValidationRule rule = new NotEmptyValidationRule("testField");
        
        assertEquals("Validates that testField is not null or empty", rule.getDescription());
        
        ValidationResult validResult = rule.validate("valid string");
        assertTrue(validResult.isValid());
        
        ValidationResult nullResult = rule.validate(null);
        assertFalse(nullResult.isValid());
        assertTrue(nullResult.getErrors().get(0).contains("testField cannot be null"));
        
        ValidationResult emptyResult = rule.validate("");
        assertFalse(emptyResult.isValid());
        assertTrue(emptyResult.getErrors().get(0).contains("testField cannot be empty"));
        
        ValidationResult whitespaceResult = rule.validate("   ");
        assertFalse(whitespaceResult.isValid());
        assertTrue(whitespaceResult.getErrors().get(0).contains("testField cannot be empty"));
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
        NotNullValidationRule<String> notNullRule = new NotNullValidationRule<>("field");
        NotEmptyValidationRule notEmptyRule = new NotEmptyValidationRule("field");
        
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
        NotNullValidationRule<String> notNullRule = new NotNullValidationRule<>("field");
        NotEmptyValidationRule notEmptyRule = new NotEmptyValidationRule("field");
        
        DefaultValidator<String> validator = DefaultValidator.of(notNullRule, notEmptyRule);
        
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
        ValidationResult resultWithWarnings = ValidationResult.withWarnings(warnings);
        
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
        
        assertEquals("Validates that value is positive", positiveRule.getDescription());
        
        ValidationResult validResult = positiveRule.validate(5);
        assertTrue(validResult.isValid());
        
        ValidationResult invalidResult = positiveRule.validate(-1);
        assertFalse(invalidResult.isValid());
        assertEquals("Value must be positive", invalidResult.getErrors().get(0));
        
        ValidationResult nullResult = positiveRule.validate(null);
        assertFalse(nullResult.isValid());
    }
}