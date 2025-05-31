package dev.agents4j.api.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

class ContextKeyTest {

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create context key with valid name and type")
        void shouldCreateContextKeyWithValidNameAndType() {
            var key = ContextKey.of("test", String.class);
            assertEquals("test", key.name());
            assertEquals(String.class, key.type());
        }

        @Test
        @DisplayName("Should create string key using convenience method")
        void shouldCreateStringKeyUsingConvenienceMethod() {
            var key = ContextKey.stringKey("username");
            assertEquals("username", key.name());
            assertEquals(String.class, key.type());
        }

        @Test
        @DisplayName("Should create integer key using convenience method")
        void shouldCreateIntegerKeyUsingConvenienceMethod() {
            var key = ContextKey.intKey("count");
            assertEquals("count", key.name());
            assertEquals(Integer.class, key.type());
        }

        @Test
        @DisplayName("Should create long key using convenience method")
        void shouldCreateLongKeyUsingConvenienceMethod() {
            var key = ContextKey.longKey("id");
            assertEquals("id", key.name());
            assertEquals(Long.class, key.type());
        }

        @Test
        @DisplayName("Should create boolean key using convenience method")
        void shouldCreateBooleanKeyUsingConvenienceMethod() {
            var key = ContextKey.booleanKey("enabled");
            assertEquals("enabled", key.name());
            assertEquals(Boolean.class, key.type());
        }

        @Test
        @DisplayName("Should throw exception for null name")
        void shouldThrowExceptionForNullName() {
            assertThrows(NullPointerException.class, () -> 
                ContextKey.of(null, String.class));
        }

        @Test
        @DisplayName("Should throw exception for null type")
        void shouldThrowExceptionForNullType() {
            assertThrows(NullPointerException.class, () -> 
                ContextKey.of("test", null));
        }

        @Test
        @DisplayName("Should throw exception for empty name")
        void shouldThrowExceptionForEmptyName() {
            assertThrows(IllegalArgumentException.class, () -> 
                ContextKey.of("", String.class));
        }

        @Test
        @DisplayName("Should throw exception for whitespace-only name")
        void shouldThrowExceptionForWhitespaceOnlyName() {
            assertThrows(IllegalArgumentException.class, () -> 
                ContextKey.of("   ", String.class));
        }
    }

    @Nested
    @DisplayName("Type Safety Tests")
    class TypeSafetyTests {

        @Test
        @DisplayName("Should cast compatible value correctly")
        void shouldCastCompatibleValueCorrectly() {
            var key = ContextKey.of("test", String.class);
            String value = "hello";
            
            String result = key.cast(value);
            assertEquals("hello", result);
        }

        @Test
        @DisplayName("Should return null for incompatible value")
        void shouldReturnNullForIncompatibleValue() {
            var key = ContextKey.of("test", String.class);
            Integer value = 42;
            
            String result = key.cast(value);
            assertNull(result);
        }

        @Test
        @DisplayName("Should return null for null value")
        void shouldReturnNullForNullValue() {
            var key = ContextKey.of("test", String.class);
            
            String result = key.cast(null);
            assertNull(result);
        }

        @Test
        @DisplayName("Should check compatibility correctly for compatible value")
        void shouldCheckCompatibilityCorrectlyForCompatibleValue() {
            var key = ContextKey.of("test", String.class);
            String value = "hello";
            
            assertTrue(key.isCompatible(value));
        }

        @Test
        @DisplayName("Should check compatibility correctly for incompatible value")
        void shouldCheckCompatibilityCorrectlyForIncompatibleValue() {
            var key = ContextKey.of("test", String.class);
            Integer value = 42;
            
            assertFalse(key.isCompatible(value));
        }

        @Test
        @DisplayName("Should consider null value compatible")
        void shouldConsiderNullValueCompatible() {
            var key = ContextKey.of("test", String.class);
            
            assertTrue(key.isCompatible(null));
        }

        @Test
        @DisplayName("Should handle inheritance correctly")
        void shouldHandleInheritanceCorrectly() {
            var key = ContextKey.of("test", Number.class);
            Integer value = 42;
            
            assertTrue(key.isCompatible(value));
            assertNotNull(key.cast(value));
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when name and type are same")
        void shouldBeEqualWhenNameAndTypeAreSame() {
            var key1 = ContextKey.of("test", String.class);
            var key2 = ContextKey.of("test", String.class);
            
            assertEquals(key1, key2);
            assertEquals(key1.hashCode(), key2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when names differ")
        void shouldNotBeEqualWhenNamesDiffer() {
            var key1 = ContextKey.of("test1", String.class);
            var key2 = ContextKey.of("test2", String.class);
            
            assertNotEquals(key1, key2);
        }

        @Test
        @DisplayName("Should not be equal when types differ")
        void shouldNotBeEqualWhenTypesDiffer() {
            var key1 = ContextKey.of("test", String.class);
            var key2 = ContextKey.of("test", Integer.class);
            
            assertNotEquals(key1, key2);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            var key = ContextKey.of("test", String.class);
            
            assertNotEquals(key, null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            var key = ContextKey.of("test", String.class);
            
            assertNotEquals(key, "test");
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should have meaningful toString")
        void shouldHaveMeaningfulToString() {
            var key = ContextKey.of("username", String.class);
            
            String result = key.toString();
            
            assertTrue(result.contains("username"));
            assertTrue(result.contains("String"));
            assertTrue(result.contains("ContextKey"));
        }

        @Test
        @DisplayName("Should include type simple name in toString")
        void shouldIncludeTypeSimpleNameInToString() {
            var key = ContextKey.of("config", java.util.Map.class);
            
            String result = key.toString();
            
            assertTrue(result.contains("Map"));
            assertFalse(result.contains("java.util.Map"));
        }
    }

    @Nested
    @DisplayName("Generic Type Tests")
    class GenericTypeTests {

        @Test
        @DisplayName("Should work with generic types")
        void shouldWorkWithGenericTypes() {
            var key = ContextKey.of("list", java.util.List.class);
            java.util.List<String> value = java.util.Arrays.asList("a", "b", "c");
            
            assertTrue(key.isCompatible(value));
            assertNotNull(key.cast(value));
        }

        @Test
        @DisplayName("Should work with complex types")
        void shouldWorkWithComplexTypes() {
            var key = ContextKey.of("map", java.util.Map.class);
            java.util.Map<String, Integer> value = java.util.Map.of("key", 42);
            
            assertTrue(key.isCompatible(value));
            assertNotNull(key.cast(value));
        }

        @Test
        @DisplayName("Should work with custom classes")
        void shouldWorkWithCustomClasses() {
            var key = ContextKey.of("person", Person.class);
            Person value = new Person("John", 30);
            
            assertTrue(key.isCompatible(value));
            assertEquals(value, key.cast(value));
        }
    }

    // Helper class for testing
    static class Person {
        private final String name;
        private final int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return age == person.age && java.util.Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, age);
        }
    }
}