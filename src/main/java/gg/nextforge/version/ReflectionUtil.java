package gg.nextforge.version;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for reflection operations with caching to improve performance.
 * Provides methods to retrieve classes, methods, fields, and constructors,
 * as well as to invoke methods and manipulate field values.
 */
public class ReflectionUtil {
    // Cache for classes to avoid repeated Class.forName calls
    private static final ConcurrentHashMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

    // Cache for methods to avoid repeated getDeclaredMethod calls
    private static final ConcurrentHashMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    // Cache for fields to avoid repeated getDeclaredField calls
    private static final ConcurrentHashMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    // Cache for constructors to avoid repeated getDeclaredConstructor calls
    private static final ConcurrentHashMap<String, Constructor<?>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();

    /**
     * Retrieves a class by its name, with caching to improve performance.
     *
     * @param className The fully qualified name of the class.
     * @return The Class object corresponding to the given name.
     * @throws RuntimeException If the class cannot be found.
     */
    public static Class<?> getClass(String className) {
        return CLASS_CACHE.computeIfAbsent(className, name -> {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                        "Can't find class: " + name + ". Did you check the version? " +
                                "Current: " + MinecraftVersion.getCurrent(), e
                );
            }
        });
    }

    /**
     * Retrieves an NMS (net.minecraft.server) class by its name.
     * Handles version-specific package naming.
     *
     * @param className The name of the NMS class.
     * @return The Class object for the specified NMS class.
     */
    public static Class<?> getNMSClass(String className) {
        String fullName = "net.minecraft." + className;
        return getClass(fullName);
    }

    /**
     * Retrieves a CraftBukkit class by its name.
     * Handles version-specific package naming.
     *
     * @param className The name of the CraftBukkit class.
     * @return The Class object for the specified CraftBukkit class.
     */
    public static Class<?> getCraftBukkitClass(String className) {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        String fullName = "org.bukkit.craftbukkit." + version + "." + className;
        return getClass(fullName);
    }

    /**
     * Retrieves a method from a class, with caching to improve performance.
     *
     * @param clazz      The class containing the method.
     * @param methodName The name of the method.
     * @param paramTypes The parameter types of the method.
     * @return The Method object for the specified method.
     * @throws RuntimeException If the method cannot be found.
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        String key = clazz.getName() + "#" + methodName + "#" + Arrays.toString(paramTypes);

        return METHOD_CACHE.computeIfAbsent(key, k -> {
            try {
                Method method = clazz.getDeclaredMethod(methodName, paramTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                try {
                    Method method = clazz.getMethod(methodName, paramTypes);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException e2) {
                    throw new RuntimeException(
                            "Can't find method: " + methodName + " in " + clazz.getName() +
                                    " with params " + Arrays.toString(paramTypes), e2
                    );
                }
            }
        });
    }

    /**
     * Retrieves a field from a class, with caching to improve performance.
     *
     * @param clazz     The class containing the field.
     * @param fieldName The name of the field.
     * @return The Field object for the specified field.
     * @throws RuntimeException If the field cannot be found.
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        String key = clazz.getName() + "#" + fieldName;

        return FIELD_CACHE.computeIfAbsent(key, k -> {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                Class<?> superClass = clazz.getSuperclass();
                if (superClass != null && superClass != Object.class) {
                    return getField(superClass, fieldName);
                }
                throw new RuntimeException(
                        "Can't find field: " + fieldName + " in " + clazz.getName(), e
                );
            }
        });
    }

    /**
     * Retrieves a constructor from a class, with caching to improve performance.
     *
     * @param clazz      The class containing the constructor.
     * @param paramTypes The parameter types of the constructor.
     * @return The Constructor object for the specified constructor.
     * @throws RuntimeException If the constructor cannot be found.
     */
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... paramTypes) {
        String key = clazz.getName() + "#<init>#" + Arrays.toString(paramTypes);

        return CONSTRUCTOR_CACHE.computeIfAbsent(key, k -> {
            try {
                Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
                constructor.setAccessible(true);
                return constructor;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                        "Can't find constructor for " + clazz.getName() +
                                " with params " + Arrays.toString(paramTypes), e
                );
            }
        });
    }

    /**
     * Invokes a method on an instance, wrapping exceptions in a RuntimeException.
     *
     * @param method   The method to invoke.
     * @param instance The instance on which to invoke the method (null for static methods).
     * @param args     The arguments to pass to the method.
     * @param <T>      The return type of the method.
     * @return The result of the method invocation.
     * @throws RuntimeException If the method invocation fails.
     */
    @SuppressWarnings("unchecked")
    public static <T> T invoke(Method method, Object instance, Object... args) {
        try {
            return (T) method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to invoke method: " + method.getName() +
                            " on " + (instance != null ? instance.getClass().getName() : "null"), e
            );
        }
    }

    /**
     * Retrieves the value of a field from an instance.
     *
     * @param field    The field to retrieve the value from.
     * @param instance The instance from which to retrieve the field value.
     * @param <T>      The type of the field value.
     * @return The value of the field.
     * @throws RuntimeException If the field value cannot be retrieved.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Field field, Object instance) {
        try {
            return (T) field.get(instance);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to get field value: " + field.getName() +
                            " from " + (instance != null ? instance.getClass().getName() : "null"), e
            );
        }
    }

    /**
     * Sets the value of a field on an instance.
     *
     * @param field    The field to set the value for.
     * @param instance The instance on which to set the field value.
     * @param value    The value to set.
     * @throws RuntimeException If the field value cannot be set.
     */
    public static void setFieldValue(Field field, Object instance, Object value) {
        try {
            field.set(instance, value);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to set field value: " + field.getName() +
                            " on " + (instance != null ? instance.getClass().getName() : "null"), e
            );
        }
    }

    /**
     * Creates a new instance using a constructor.
     *
     * @param constructor The constructor to use for instantiation.
     * @param args        The arguments to pass to the constructor.
     * @param <T>         The type of the instance.
     * @return The newly created instance.
     * @throws RuntimeException If the instance cannot be created.
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Constructor<?> constructor, Object... args) {
        try {
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create instance of " + constructor.getDeclaringClass().getName(), e
            );
        }
    }
}