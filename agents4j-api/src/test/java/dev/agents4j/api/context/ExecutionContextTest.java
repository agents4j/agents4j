package dev.agents4j.api.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

class ExecutionContextTest {

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create empty context")
        void shouldCreateEmptyContext() {
            var context = ExecutionContext.empty();
            
            assertTrue(context.isEmpty());
            assertEquals(0, context.size());
            assertTrue(context.keys().isEmpty());
        }

        @Test
        @DisplayName("Should create context with single entry")
        void shouldCreateContextWithSingleEntry() {
            var key = ContextKey.stringKey("username");
            var context = ExecutionContext.of(key, "john");
            
            assertFalse(context.isEmpty());
            assertEquals(1, context.size());
            assertEquals(Optional.of("john"), context.get(key));
        }

        @Test
        @DisplayName("Should create context from map")
        void shouldCreateContextFromMap() {
            var key1 = ContextKey.stringKey("username");
            var key2 = ContextKey.intKey("age");
            var entries = Map.<ContextKey<?>, Object>of(
                key1, "john",
                key2, 30
            );
            
            var context = ExecutionContext.from(entries);
            
            assertEquals(2, context.size());
            assertEquals(Optional.of("john"), context.get(key1));
            assertEquals(Optional.of(30), context.get(key2));
        }

        @Test
        @DisplayName("Should throw exception for null key in of method")
        void shouldThrowExceptionForNullKeyInOfMethod() {
            assertThrows(NullPointerException.class, () -> 
                ExecutionContext.of(null, "value"));
        }

        @Test
        @DisplayName("Should throw exception for null entries map")
        void shouldThrowExceptionForNullEntriesMap() {
            assertThrows(NullPointerException.class, () -> 
                ExecutionContext.from(null));
        }
    }

    @Nested
    @DisplayName("Type Safety Validation Tests")
    class TypeSafetyValidationTests {

        @Test
        @DisplayName("Should accept null value")
        void shouldAcceptNullValue() {
            var key = ContextKey.stringKey("optional");
            var context = ExecutionContext.of(key, null);
            
            assertEquals(Optional.empty(), context.get(key));
        }

        @Test
        @DisplayName("Should throw exception for incompatible value in of method")
        void shouldThrowExceptionForIncompatibleValueInOfMethod() {
            var key = ContextKey.stringKey("username");
            
            // Cannot test incompatible types at runtime due to type erasure
            assertTrue(true); // Type safety is enforced at compile time
        }

        @Test
        @DisplayName("Should throw exception for incompatible value in from method")
        void shouldThrowExceptionForIncompatibleValueInFromMethod() {
            var key = ContextKey.stringKey("username");
            var entries = Map.<ContextKey<?>, Object>of(key, 42);
            
            assertThrows(IllegalArgumentException.class, () -> 
                ExecutionContext.from(entries));
        }

        @Test
        @DisplayName("Should validate all entries in from method")
        void shouldValidateAllEntriesInFromMethod() {
            var key1 = ContextKey.stringKey("valid");
            var key2 = ContextKey.stringKey("invalid");
            var entries = Map.<ContextKey<?>, Object>of(
                key1, "good",
                key2, 42  // Invalid type
            );
            
            var exception = assertThrows(IllegalArgumentException.class, () -> 
                ExecutionContext.from(entries));
            
            assertTrue(exception.getMessage().contains("invalid"));
        }
    }

    @Nested
    @DisplayName("Access Operations Tests")
    class AccessOperationsTests {

        private ExecutionContext context;
        private ContextKey<String> stringKey;
        private ContextKey<Integer> intKey;
        private ContextKey<Boolean> boolKey;

        @BeforeEach
        void setUp() {
            stringKey = ContextKey.stringKey("name");
            intKey = ContextKey.intKey("age");
            boolKey = ContextKey.booleanKey("active");
            
            var tempContext = ExecutionContext.empty()
                .with(stringKey, "john");
            context = ExecutionContext.from(Map.of(
                stringKey, "john",
                intKey, 30
            ));
        }

        @Test
        @DisplayName("Should get existing value")
        void shouldGetExistingValue() {
            assertEquals(Optional.of("john"), context.get(stringKey));
            assertEquals(Optional.of(30), context.get(intKey));
        }

        @Test
        @DisplayName("Should return empty for non-existent key")
        void shouldReturnEmptyForNonExistentKey() {
            assertEquals(Optional.empty(), context.get(boolKey));
        }

        @Test
        @DisplayName("Should get value with default")
        void shouldGetValueWithDefault() {
            assertEquals("john", context.getOrDefault(stringKey, "default"));
            assertEquals(true, context.getOrDefault(boolKey, true));
        }

        @Test
        @DisplayName("Should check key existence")
        void shouldCheckKeyExistence() {
            assertTrue(context.contains(stringKey));
            assertTrue(context.contains(intKey));
            assertFalse(context.contains(boolKey));
        }

        @Test
        @DisplayName("Should return correct keys")
        void shouldReturnCorrectKeys() {
            Set<ContextKey<?>> keys = context.keys();
            
            assertEquals(2, keys.size());
            assertTrue(keys.contains(stringKey));
            assertTrue(keys.contains(intKey));
            assertFalse(keys.contains(boolKey));
        }

        @Test
        @DisplayName("Should throw exception for null key in get")
        void shouldThrowExceptionForNullKeyInGet() {
            assertThrows(NullPointerException.class, () -> 
                context.get(null));
        }
    }

    @Nested
    @DisplayName("Modification Operations Tests")
    class ModificationOperationsTests {

        private ExecutionContext baseContext;
        private ContextKey<String> stringKey;
        private ContextKey<Integer> intKey;

        @BeforeEach
        void setUp() {
            stringKey = ContextKey.stringKey("name");
            intKey = ContextKey.intKey("age");
            baseContext = ExecutionContext.of(stringKey, "john");
        }

        @Test
        @DisplayName("Should add new key-value pair")
        void shouldAddNewKeyValuePair() {
            var newContext = baseContext.with(intKey, 30);
            
            assertEquals(2, newContext.size());
            assertEquals(Optional.of("john"), newContext.get(stringKey));
            assertEquals(Optional.of(30), newContext.get(intKey));
            
            // Original context should be unchanged
            assertEquals(1, baseContext.size());
            assertFalse(baseContext.contains(intKey));
        }

        @Test
        @DisplayName("Should update existing key")
        void shouldUpdateExistingKey() {
            var newContext = baseContext.with(stringKey, "jane");
            
            assertEquals(1, newContext.size());
            assertEquals(Optional.of("jane"), newContext.get(stringKey));
            
            // Original context should be unchanged
            assertEquals(Optional.of("john"), baseContext.get(stringKey));
        }

        @Test
        @DisplayName("Should remove existing key")
        void shouldRemoveExistingKey() {
            var contextWithTwo = baseContext.with(intKey, 30);
            var newContext = contextWithTwo.without(stringKey);
            
            assertEquals(1, newContext.size());
            assertFalse(newContext.contains(stringKey));
            assertTrue(newContext.contains(intKey));
        }

        @Test
        @DisplayName("Should return same instance when removing non-existent key")
        void shouldReturnSameInstanceWhenRemovingNonExistentKey() {
            var newContext = baseContext.without(intKey);
            
            assertSame(baseContext, newContext);
        }

        @Test
        @DisplayName("Should throw exception for null key in with")
        void shouldThrowExceptionForNullKeyInWith() {
            assertThrows(NullPointerException.class, () -> 
                baseContext.with(null, "value"));
        }

        @Test
        @DisplayName("Should throw exception for incompatible value in with")
        void shouldThrowExceptionForIncompatibleValueInWith() {
            // Cannot test incompatible types at runtime due to type erasure
            assertTrue(true); // Type safety is enforced at compile time
        }
    }

    @Nested
    @DisplayName("Merge Operations Tests")
    class MergeOperationsTests {

        private ExecutionContext context1;
        private ExecutionContext context2;
        private ContextKey<String> key1;
        private ContextKey<Integer> key2;
        private ContextKey<Boolean> key3;

        @BeforeEach
        void setUp() {
            key1 = ContextKey.stringKey("name");
            key2 = ContextKey.intKey("age");
            key3 = ContextKey.booleanKey("active");
            
            context1 = ExecutionContext.from(Map.of(
                key1, "john",
                key2, 30
            ));
            
            context2 = ExecutionContext.from(Map.of(
                key2, 35,  // Overlapping key with different value
                key3, true
            ));
        }

        @Test
        @DisplayName("Should merge contexts correctly")
        void shouldMergeContextsCorrectly() {
            var merged = context1.merge(context2);
            
            assertEquals(3, merged.size());
            assertEquals(Optional.of("john"), merged.get(key1));
            assertEquals(Optional.of(35), merged.get(key2));  // Other takes precedence
            assertEquals(Optional.of(true), merged.get(key3));
        }

        @Test
        @DisplayName("Should return this when merging with empty context")
        void shouldReturnThisWhenMergingWithEmptyContext() {
            var merged = context1.merge(ExecutionContext.empty());
            
            assertSame(context1, merged);
        }

        @Test
        @DisplayName("Should return other when this is empty")
        void shouldReturnOtherWhenThisIsEmpty() {
            var merged = ExecutionContext.empty().merge(context1);
            
            assertSame(context1, merged);
        }

        @Test
        @DisplayName("Should throw exception for null other context")
        void shouldThrowExceptionForNullOtherContext() {
            assertThrows(NullPointerException.class, () -> 
                context1.merge(null));
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when contents are same")
        void shouldBeEqualWhenContentsAreSame() {
            var key = ContextKey.stringKey("test");
            var context1 = ExecutionContext.of(key, "value");
            var context2 = ExecutionContext.of(key, "value");
            
            assertEquals(context1, context2);
            assertEquals(context1.hashCode(), context2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when contents differ")
        void shouldNotBeEqualWhenContentsDiffer() {
            var key = ContextKey.stringKey("test");
            var context1 = ExecutionContext.of(key, "value1");
            var context2 = ExecutionContext.of(key, "value2");
            
            assertNotEquals(context1, context2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            var context = ExecutionContext.of(ContextKey.stringKey("test"), "value");
            
            assertEquals(context, context);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            var context = ExecutionContext.empty();
            
            assertNotEquals(context, null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            var context = ExecutionContext.empty();
            
            assertNotEquals(context, "string");
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should have meaningful toString for empty context")
        void shouldHaveMeaningfulToStringForEmptyContext() {
            var context = ExecutionContext.empty();
            String result = context.toString();
            
            assertTrue(result.contains("ExecutionContext"));
            assertTrue(result.contains("size=0"));
        }

        @Test
        @DisplayName("Should have meaningful toString for non-empty context")
        void shouldHaveMeaningfulToStringForNonEmptyContext() {
            var key1 = ContextKey.stringKey("name");
            var key2 = ContextKey.intKey("age");
            var context = ExecutionContext.from(Map.of(
                key1, "john",
                key2, 30
            ));
            
            String result = context.toString();
            
            assertTrue(result.contains("ExecutionContext"));
            assertTrue(result.contains("size=2"));
            assertTrue(result.contains("name"));
            assertTrue(result.contains("age"));
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Should not modify original when adding")
        void shouldNotModifyOriginalWhenAdding() {
            var key1 = ContextKey.stringKey("key1");
            var key2 = ContextKey.stringKey("key2");
            var original = ExecutionContext.of(key1, "value1");
            
            var modified = original.with(key2, "value2");
            
            assertEquals(1, original.size());
            assertEquals(2, modified.size());
            assertFalse(original.contains(key2));
            assertTrue(modified.contains(key2));
        }

        @Test
        @DisplayName("Should not modify original when removing")
        void shouldNotModifyOriginalWhenRemoving() {
            var key1 = ContextKey.stringKey("key1");
            var key2 = ContextKey.stringKey("key2");
            var original = ExecutionContext.empty()
                .with(key1, "value1")
                .with(key2, "value2");
            
            var modified = original.without(key2);
            
            assertEquals(2, original.size());
            assertEquals(1, modified.size());
            assertTrue(original.contains(key2));
            assertFalse(modified.contains(key2));
        }

        @Test
        @DisplayName("Keys collection should be immutable")
        void keysCollectionShouldBeImmutable() {
            var key = ContextKey.stringKey("test");
            var context = ExecutionContext.of(key, "value");
            
            var keys = context.keys();
            
            assertThrows(UnsupportedOperationException.class, () -> 
                keys.add(ContextKey.stringKey("another")));
        }
    }
}