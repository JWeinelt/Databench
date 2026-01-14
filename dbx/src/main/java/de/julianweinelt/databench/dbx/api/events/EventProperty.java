package de.julianweinelt.databench.dbx.api.events;

import org.jetbrains.annotations.ApiStatus;

/**
 * Represents a generic event property that can hold a value of any type.
 * Provides utility methods to retrieve the value as different data types.
 * <p>
 * This class is useful for handling dynamic event properties where the type is not known beforehand.
 */

public class EventProperty {
    private final Object value;
    private final Class<?> valueClass;

    /**
     * Constructs an EventProperty with the specified value.
     *
     * @param value {@link Object} The value to store in this event property. Can be of any type.
     * <p>
     * Example:
     * <pre>
     *     EventProperty property = new EventProperty(42);
     * </pre>
     */
    public EventProperty(Object value) {
        this.value = value;
        this.valueClass = value.getClass();
    }

    /**
     *
     * @param expectedType The {@code Class<T>} that is expected to be returned
     * @param <T> The type of object
     * @return The stored value cast to the expected type
     * @throws ClassCastException if the stored is not a type of {@code expectedType}
     */
    public <T> T asValue(Class<T> expectedType) {
        if (!expectedType.isInstance(value)) {
            throw new ClassCastException(
                    "Expected " + expectedType.getName() +
                            " but was " + valueClass.getName()
            );
        }
        return expectedType.cast(value);
    }

    /**
     * Retrieves the stored value as the original type using Java's type casting.
     * <p>
     * Important: This method is experimental, as there are no checks done.
     * If you want to use the type-safe variant, please use {@link #asValue(Class)}.
     * @return The stored value cast to their original type.
     * @param <T> The type of the class
     * <p></p>
     */
    @ApiStatus.Experimental
    @SuppressWarnings("unchecked")
    public <T> T asValue() {
        return (T) value;
    }

    /**
     * Retrieves the stored value as a specified type using Java's type casting.
     * <p></p>
     * Example:
     * <pre>{@code
     *     EventProperty property = new EventProperty(42);
     *     int intValue = property.getAs(Integer.class); // Returns 42
     * }</pre>
     * <p></p>
     * This method is unsafe to use, as it may throw a {@code ClassCastException}.
     * You may use {@link #asValue(Class)} for type-safe casting instead.
     *
     * @param type The class of the expected return type.
     * @return The stored value cast to the specified type.
     * @throws ClassCastException if the stored value is not of the requested type.
     */
    @Deprecated
    public <T> T getAs(Class<T> type) {
        return type.cast(value);
    }

    /**
     * Retrieves the stored value as a String.
     * Equivalent to calling `getAs(String.class)`.
     *
     * @return The stored value as a String.
     * @throws ClassCastException if the value cannot be cast to a String.
     * <p></p>
     * Example:
     * <pre>{@code
     *     EventProperty property = new EventProperty("Hello");
     *     String text = property.asString(); // Returns "Hello"
     * }</pre>
     */
    public String asString() {
        return getAs(String.class);
    }
    /**
     * Retrieves the stored value as an int.
     * Equivalent to calling `getAs(Integer.class)`.
     *
     * @return The stored value as an int.
     * @throws ClassCastException if the value cannot be cast to an Integer.
     *
     * Example:
     * <pre>{@code
     *     EventProperty property = new EventProperty(100);
     *     int number = property.asInt(); // Returns 100
     * }</pre>
     */
    public int asInt() {
        return getAs(Integer.class);
    }
    /**
     * Retrieves the stored value as a boolean.
     * Equivalent to calling `getAs(Boolean.class)`.
     *
     * @return The stored value as a boolean.
     * @throws ClassCastException if the value cannot be cast to a Boolean.
     *
     * Example:
     * <pre>{@code
     *     EventProperty property = new EventProperty(true);
     *     boolean flag = property.asBoolean(); // Returns true
     * }</pre>
     */
    public boolean asBoolean() {
        return getAs(Boolean.class);
    }
    /**
     * Retrieves the stored value as a float.
     * Equivalent to calling `getAs(Float.class)`.
     *
     * @return The stored value as a float.
     * @throws ClassCastException if the value cannot be cast to a Float.
     *
     * Example:
     * <pre>{@code
     *     EventProperty property = new EventProperty(3.14f);
     *     float pi = property.asFloat(); // Returns 3.14f
     * }</pre>
     */
    public float asFloat() {
        return getAs(Float.class);
    }
    /**
     * Retrieves the stored value as a double.
     * Equivalent to calling `getAs(Double.class)`.
     *
     * @return The stored value as a double.
     * @throws ClassCastException if the value cannot be cast to a Double.
     *
     * Example:
     * <pre>{@code
     *     EventProperty property = new EventProperty(2.718);
     *     double e = property.asDouble(); // Returns 2.718
     * }</pre>
     */
    public double asDouble() {
        return getAs(Double.class);
    }
    /**
     * Retrieves the raw stored value without any type casting.
     *
     * @return The stored value as an Object.
     *
     * Example:
     * <pre>{@code
     *     EventProperty property = new EventProperty("Dynamic Value");
     *     Object rawValue = property.getRaw(); // Returns "Dynamic Value"
     * }</pre>
     */
    public Object getRaw() {
        return value;
    }
}