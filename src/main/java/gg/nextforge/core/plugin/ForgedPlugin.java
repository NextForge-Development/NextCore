package gg.nextforge.core.plugin;

import dev.mzcy.LicensedPlugin;
import gg.nextforge.core.plugin.annotation.NextForgePlugin;
import gg.nextforge.core.plugin.dependency.DependencyLoader;
import gg.nextforge.core.plugin.dependency.DependencyResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public abstract class ForgedPlugin extends LicensedPlugin {

    @Override
    public abstract UUID pluginId();

    @Override
    public void enablePlugin() {
        //Check if this class is annotated with @NextForgePlugin
        if (!this.getClass().isAnnotationPresent(NextForgePlugin.class)) {
            throw new IllegalStateException("Plugin must be annotated with @NextForgePlugin");
        }
        Path libDir = this.getDataFolder().toPath().resolve("dependencies");
        DependencyResolver dependencyResolver = resolveDependencies(DependencyResolver.create());
        dependencyResolver.downloadDependencies(libDir);
        try {
            List<Path> jars = Files.list(libDir).toList();
            DependencyLoader loader = new DependencyLoader(jars);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        enable();
    }

    @Override
    public void disablePlugin() {
        disable();
    }

    public abstract void enable();
    public abstract void disable();
    public abstract DependencyResolver resolveDependencies(DependencyResolver dependencyResolver);
}
