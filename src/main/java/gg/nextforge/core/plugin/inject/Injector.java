package gg.nextforge.core.plugin.inject;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public final class Injector {
    private Injector() {}

    public static void wire(Object target, ServiceRegistry registry) {
        wire(target, registry, new HashSet<>());
    }

    private static void wire(Object target, ServiceRegistry registry, Set<Class<?>> stack) {
        if (target == null) return;
        Class<?> cls = target.getClass();
        if (!stack.add(cls)) return; // simple cycle guard

        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                Inject ann = f.getAnnotation(Inject.class);
                if (ann == null) continue;

                Class<?> depType = f.getType();
                Object dep = ann.value().isBlank()
                        ? registry.get(depType).orElseGet(() -> registry.getOrCreate(depType))
                        : registry.get(depType, ann.value()).orElseThrow(() ->
                        new IllegalStateException("No service registered für " + depType.getName() + " mit Name '" + ann.value() + "'"));
                f.setAccessible(true);
                try {
                    if (f.get(target) == null) {
                        f.set(target, dep);
                        // optional: deep-wire das neu gesetzte Dependency
                        wire(dep, registry, stack);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Injection failed on " + cls.getName() + "." + f.getName(), e);
                }
            }
        }
        stack.remove(cls);
    }
}