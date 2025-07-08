package gg.nextforge.protocol.reflect;

 import java.lang.reflect.Field;
 import java.lang.reflect.Modifier;
 import java.util.*;
 import java.util.concurrent.ConcurrentHashMap;

 /**
  * A utility class for modifying the structure of objects using reflection.
  * This class provides methods to read and write fields of a target class or object,
  * with caching for improved performance.
  *
  * @param <T> The type of the fields being accessed.
  */
 public class StructureModifier<T> {

     private final Class<?> targetClass; // The class whose fields are being modified.
     private final Class<T> fieldType; // The type of the fields to be accessed.
     private final List<FieldAccessor<T>> accessors; // List of field accessors for the target class.
     private final Object target; // The target object instance for field access.

     // Cache for storing field lists by class and field type.
     private static final Map<String, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

     /**
      * Constructs a StructureModifier for the specified class and field type.
      *
      * @param targetClass The class whose fields are being modified.
      * @param fieldType   The type of the fields to be accessed.
      */
     public StructureModifier(Class<?> targetClass, Class<T> fieldType) {
         this(targetClass, fieldType, null);
     }

     /**
      * Constructs a StructureModifier for the specified class, field type, and target object.
      *
      * @param targetClass The class whose fields are being modified.
      * @param fieldType   The type of the fields to be accessed.
      * @param target      The target object instance for field access.
      */
     public StructureModifier(Class<?> targetClass, Class<T> fieldType, Object target) {
         this.targetClass = targetClass;
         this.fieldType = fieldType;
         this.target = target;
         this.accessors = new ArrayList<>();

         // Initialize field accessors by scanning the class hierarchy.
         initializeFields();
     }

     /**
      * Initializes field accessors by scanning the class hierarchy for matching fields.
      * Fields are cached for performance optimization.
      */
     private void initializeFields() {
         String cacheKey = targetClass.getName() + "#" + fieldType.getName();
         List<Field> fields = FIELD_CACHE.computeIfAbsent(cacheKey, k -> {
             List<Field> result = new ArrayList<>();

             // Traverse the class hierarchy to find fields.
             Class<?> current = targetClass;
             while (current != null && current != Object.class) {
                 for (Field field : current.getDeclaredFields()) {
                     // Skip static fields.
                     if (Modifier.isStatic(field.getModifiers())) {
                         continue;
                     }

                     // Check if the field type matches the target type.
                     if (isCompatible(field.getType(), fieldType)) {
                         field.setAccessible(true);
                         result.add(field);
                     }
                 }
                 current = current.getSuperclass();
             }

             return result;
         });

         // Create field accessors for the matching fields.
         for (Field field : fields) {
             accessors.add(new FieldAccessor<>(field));
         }
     }

     /**
      * Checks if a field type is compatible with the target type.
      * Handles primitive and wrapper type conversions.
      *
      * @param fieldType  The type of the field.
      * @param targetType The target type to check compatibility against.
      * @return True if the field type is compatible, false otherwise.
      */
     private boolean isCompatible(Class<?> fieldType, Class<?> targetType) {
         if (targetType.isAssignableFrom(fieldType)) {
             return true;
         }

         // Handle primitive and wrapper type conversions.
         if (targetType == Object.class) {
             return true; // Object matches all types.
         }

         if (isPrimitiveWrapper(targetType) && fieldType.isPrimitive()) {
             return getPrimitive(targetType) == fieldType;
         }

         if (isPrimitiveWrapper(fieldType) && targetType.isPrimitive()) {
             return getPrimitive(fieldType) == targetType;
         }

         return false;
     }

     /**
      * Checks if a type is a primitive wrapper (e.g., Integer, Boolean).
      *
      * @param type The type to check.
      * @return True if the type is a primitive wrapper, false otherwise.
      */
     private boolean isPrimitiveWrapper(Class<?> type) {
         return type == Boolean.class || type == Byte.class ||
                 type == Character.class || type == Short.class ||
                 type == Integer.class || type == Long.class ||
                 type == Float.class || type == Double.class;
     }

     /**
      * Retrieves the primitive type corresponding to a wrapper type.
      *
      * @param wrapper The wrapper type.
      * @return The primitive type corresponding to the wrapper type.
      */
     private Class<?> getPrimitive(Class<?> wrapper) {
         if (wrapper == Boolean.class) return boolean.class;
         if (wrapper == Byte.class) return byte.class;
         if (wrapper == Character.class) return char.class;
         if (wrapper == Short.class) return short.class;
         if (wrapper == Integer.class) return int.class;
         if (wrapper == Long.class) return long.class;
         if (wrapper == Float.class) return float.class;
         if (wrapper == Double.class) return double.class;
         return wrapper;
     }

     /**
      * Reads the value of a field by its index.
      *
      * @param index The index of the field.
      * @return The value of the field.
      * @throws IndexOutOfBoundsException If the index is out of bounds.
      */
     @SuppressWarnings("unchecked")
     public T read(int index) {
         if (index < 0 || index >= accessors.size()) {
             throw new IndexOutOfBoundsException(
                     "Index " + index + " is out of bounds for " + accessors.size() + " fields"
             );
         }

         return accessors.get(index).get(target);
     }

     /**
      * Writes a value to a field by its index.
      *
      * @param index The index of the field.
      * @param value The value to write to the field.
      * @throws IndexOutOfBoundsException If the index is out of bounds.
      */
     public void write(int index, T value) {
         if (index < 0 || index >= accessors.size()) {
             throw new IndexOutOfBoundsException(
                     "Index " + index + " is out of bounds for " + accessors.size() + " fields"
             );
         }

         accessors.get(index).set(target, value);
     }

     /**
      * Retrieves the number of fields in the target class.
      *
      * @return The number of fields.
      */
     public int size() {
         return accessors.size();
     }

     /**
      * Creates a new StructureModifier with a different field type.
      *
      * @param <V>     The new field type.
      * @param newType The class of the new field type.
      * @return A new StructureModifier instance.
      */
     @SuppressWarnings("unchecked")
     public <V> StructureModifier<V> withType(Class<V> newType) {
         return new StructureModifier<>(targetClass, newType, target);
     }

     /**
      * Creates a new StructureModifier with a specific target instance.
      *
      * @param newTarget The new target object instance.
      * @return A new StructureModifier instance.
      */
     public StructureModifier<T> withTarget(Object newTarget) {
         return new StructureModifier<>(targetClass, fieldType, newTarget);
     }

     /**
      * Reads all field values into a list.
      *
      * @return A list of field values.
      */
     public List<T> getValues() {
         List<T> values = new ArrayList<>();
         for (int i = 0; i < size(); i++) {
             values.add(read(i));
         }
         return values;
     }

     /**
      * Retrieves the names of all fields for debugging purposes.
      *
      * @return A list of field names.
      */
     public List<String> getFieldNames() {
         List<String> names = new ArrayList<>();
         for (FieldAccessor<T> accessor : accessors) {
             names.add(accessor.getField().getName());
         }
         return names;
     }
 }