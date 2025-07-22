package gg.nextforge.database.entity;

import gg.nextforge.database.annotations.Table;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class EntityScanner {

    public List<Class<?>> findEntities(String basePackage) throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        String path = basePackage.replace('.', '/');
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File dir = new File(resource.getFile());
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".class")) {
                    String className = basePackage + '.' + file.getName().replace(".class", "");
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Table.class)) {
                        classes.add(clazz);
                    }
                }
            }
        }
        return classes;
    }
}
