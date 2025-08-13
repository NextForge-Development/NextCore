package gg.nextforge.core.plugin.dependency;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IsolatedDependencyLoader implements AutoCloseable {

    private final Map<String, URLClassLoader> groups = new ConcurrentHashMap<>();

    /** Registers a classloader for a group name; replaces if exists. */
    public void registerGroup(String name, List<Path> jars, ClassLoader parent) {
        URL[] urls = jars.stream().map(p -> {
            try { return p.toUri().toURL(); } catch (Exception e) { throw new RuntimeException(e); }
        }).toArray(URL[]::new);
        URLClassLoader cl = new URLClassLoader(urls, parent);
        URLClassLoader old = groups.put(name, cl);
        if (old != null) try { old.close(); } catch (Exception ignored) {}
    }

    /** Loads a class from a specific group. */
    public Class<?> loadClass(String group, String className) throws ClassNotFoundException {
        URLClassLoader cl = groups.get(group);
        if (cl == null) throw new ClassNotFoundException("No classloader for group: " + group);
        return cl.loadClass(className);
    }

    public Optional<URLClassLoader> getGroup(String name) {
        return Optional.ofNullable(groups.get(name));
    }

    @Override public void close() throws Exception {
        for (URLClassLoader cl : groups.values()) { try { cl.close(); } catch (Exception ignored) {} }
        groups.clear();
    }
}
