// src/main/java/gg/nextforge/core/plugin/dependency/DependencyManager.java
package gg.nextforge.core.plugin.dependency;

import gg.nextforge.core.plugin.dependency.aether.AetherDependencyResolver;
import gg.nextforge.core.plugin.dependency.model.DependencyArtifact;
import gg.nextforge.core.plugin.dependency.model.DependencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class DependencyManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DependencyManager.class);

    private final Path cacheDir;     // ~/.nextforge-cache
    private final Path pluginLibDir; // <plugin-data>/dependencies
    private final List<DependencyRepository> repositories;
    private final AetherDependencyResolver resolver;
    private final IsolatedDependencyLoader loader;

    public DependencyManager(Path cacheDir, Path pluginLibDir, List<DependencyRepository> repositories, int threads) {
        this.cacheDir = cacheDir;
        this.pluginLibDir = pluginLibDir;
        this.repositories = repositories;
        this.resolver = new AetherDependencyResolver(cacheDir, repositories, threads);
        this.loader = new IsolatedDependencyLoader();
    }

    /** Resolves all artifacts (with transitives), mirrors them into plugin lib dir (hardlink/copy), creates group loader. */
    public List<Path> prepareGroup(String groupName,
                                   Collection<DependencyArtifact> artifacts,
                                   boolean verifyChecksums) throws Exception {
        Files.createDirectories(pluginLibDir);
        List<AetherDependencyResolver.ResolvedJar> resolved = resolver.resolveTransitiveJars(artifacts);
        if (verifyChecksums) resolver.verifyChecksums(resolved, java.time.Duration.ofSeconds(5));
        List<Path> jarPaths = resolved.stream()
                .map(AetherDependencyResolver.ResolvedJar::file)
                .toList();

        // 4) In pluginLibDir spiegeln (Hardlink, sonst Copy)
        List<Path> mirrored = new ArrayList<>(jarPaths.size());
        for (Path src : jarPaths) {
            Path dst = pluginLibDir.resolve(src.getFileName().toString());
            try {
                Files.createLink(dst, src); // Hardlink
            } catch (Exception e) {
                Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            }
            mirrored.add(dst);
        }
        loader.registerGroup(groupName, mirrored, getClass().getClassLoader());
        log.info("Group '{}' prepared with {} jars", groupName, mirrored.size());
        return mirrored;
    }


    public Class<?> loadFromGroup(String groupName, String fqcn) throws ClassNotFoundException {
        return loader.loadClass(groupName, fqcn);
    }

    @Override public void close() throws Exception {
        resolver.shutdown();
        loader.close();
    }
}
