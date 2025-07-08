package gg.nextforge.protocol.reflect;

import java.lang.reflect.Field;

/**
 * A utility class that wraps around Java's reflection API to provide
 * cleaner and more convenient access to fields. This class handles
 * common reflection operations such as getting and setting field values
 * while providing better error handling.
 *
 * @param <T>   The type of the field being accessed.
 * @param field The underlying Java reflection Field object.
 */
public record FieldAccessor<T>(Field field) {

    /**
     * Constructs a FieldAccessor instance for the given field.
     * Sets the field to be accessible to bypass Java's access checks.
     *
     * @param field The Field object to wrap.
     */
    public FieldAccessor(Field field) {
        this.field = field;
        field.setAccessible(true);
    }

    /**
     * Retrieves the value of the field from the specified target object.
     *
     * @param target The object from which to retrieve the field value.
     * @return The value of the field.
     * @throws RuntimeException If the field value cannot be read.
     */
    @SuppressWarnings("unchecked")
    public T get(Object target) {
        try {
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to read field " + field.getName() + " from " +
                            (target != null ? target.getClass().getName() : "null"), e
            );
        }
    }

    /**
     * Sets the value of the field on the specified target object.
     *
     * @param target The object on which to set the field value.
     * @param value  The value to set.
     * @throws RuntimeException If the field value cannot be written.
     */
    public void set(Object target, T value) {
        try {
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to write field " + field.getName() + " on " +
                            (target != null ? target.getClass().getName() : "null"), e
            );
        }
    }

    /**
     * Retrieves the underlying Field object.
     *
     * @return The Field object wrapped by this class.
     */
    @Override
    public Field field() {
        return field;
    }

    /**
     * Retrieves the name of the field.
     *
     * @return The name of the field.
     */
    public String getName() {
        return field.getName();
    }

    /**
     * Retrieves the type of the field.
     *
     * @return The Class object representing the type of the field.
     */
    public Class<?> getType() {
        return field.getType();
    }

    /**
     * Retrieves the underlying Field object.
     *
     * @return The Field object.
     */
    public Field getField() {
        return field;
    }
}