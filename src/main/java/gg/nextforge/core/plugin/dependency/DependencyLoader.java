package gg.nextforge.core.plugin.dependency;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

public class DependencyLoader {

    private final URLClassLoader classLoader;

    public DependencyLoader(List<Path> jarFiles) {
        URL[] urls = jarFiles.stream()
                .map(path -> {
                    try {
                        return path.toUri().toURL();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(URL[]::new);

        classLoader = new URLClassLoader(urls, getClass().getClassLoader());
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }
}
