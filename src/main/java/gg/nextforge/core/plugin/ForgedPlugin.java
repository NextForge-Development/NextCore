// src/main/java/gg/nextforge/core/plugin/ForgedPlugin.java
package gg.nextforge.core.plugin;

import dev.mzcy.LicensedPlugin;
import gg.nextforge.core.plugin.annotation.NextForgePlugin;
import gg.nextforge.core.plugin.dependency.DependencyManager;
import gg.nextforge.core.plugin.dependency.model.DependencyArtifact;
import gg.nextforge.core.plugin.dependency.model.DependencyRepository;
import gg.nextforge.core.plugin.inject.Initializable;
import gg.nextforge.core.plugin.inject.Injector;
import gg.nextforge.core.plugin.inject.ServiceRegistry;

import java.nio.file.Path;
import java.util.*;

public abstract class ForgedPlugin extends LicensedPlugin {

    private DependencyManager depManager;
    private ServiceRegistry services;

    @Override
    public void enablePlugin() {
        if (!this.getClass().isAnnotationPresent(NextForgePlugin.class))
            throw new IllegalStateException("Plugin must be annotated with @NextForgePlugin");

        Path libDir = this.getDataFolder().toPath().resolve("dependencies");
        Path cache = Path.of(System.getProperty("user.home"), ".nextforge-cache");

        // 1) Dependency Manager + Repositories/Groups
        depManager = new DependencyManager(cache, libDir, repositories(), Math.max(4, Runtime.getRuntime().availableProcessors()));
        try {
            for (var e : groups().entrySet()) {
                depManager.prepareGroup(e.getKey(), e.getValue(), /*verifyChecksums*/ true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Dependency prepare failed", e);
        }

        // 2) Hook: onLoadDependencies
        onLoadDependencies(depManager);

        // 3) Services registrieren (DI-Container)
        services = new ServiceRegistry();
        services.register(DependencyManager.class, depManager);
        services.register(ForgedPlugin.class, this);
        // weitere Standard-Services hier registrieren (Logger, Config, etc.)

        // 4) Automatisches Wiring (@Inject) im Plugin (und ggf. Subsystemen)
        Injector.wire(this, services);

        // 5) Hook: beforeEnable (z. B. Config laden, Services starten)
        beforeEnable(services);

        // 6) Plugin-spezifische Enable-Logik
        enable();
    }

    @Override
    public void disablePlugin() {
        try {
            disable();           // Plugin-spezifisch
            afterDisable();      // Hook
        } finally {
            try { if (depManager != null) depManager.close(); } catch (Exception ignored) {}
        }
    }

    /* --------- Lifecycle Hooks --------- */
    /** Wird nach dem Laden der Dependencies, aber vor DI/Lifecycle aufgerufen. */
    protected void onLoadDependencies(DependencyManager manager) {}
    /** Wird nach DI, aber vor enable() aufgerufen. */
    protected void beforeEnable(ServiceRegistry services) {
        for (var entry : services.getAll().entrySet()) {
            Class<?> type = entry.getKey();
            Object instance = entry.getValue();
            getSLF4JLogger().info("Service loaded: {} ({})", type.getSimpleName(), instance);

            // Falls du Initialisierung willst:
            if (instance instanceof Initializable init) {
                init.initialize();
            }
        }
    }
    /** Wird nach disable() aufgerufen (Cleanup). */
    protected void afterDisable() {}

    /* --------- Plugin API --------- */
    public abstract void enable();
    public abstract void disable();

    /** Repositories (öffentlich/privat) */
    public abstract List<DependencyRepository> repositories();
    /** Artefakte gruppiert nach Isolations-Loader */
    public abstract Map<String, List<DependencyArtifact>> groups();

    /* Optionaler Getter, falls Plugins Services brauchen */
    protected ServiceRegistry services() { return services; }
    protected DependencyManager dependencyManager() { return depManager; }
}
