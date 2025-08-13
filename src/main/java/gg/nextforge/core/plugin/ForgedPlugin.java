// src/main/java/gg/nextforge/core/plugin/ForgedPlugin.java
package gg.nextforge.core.plugin;

import dev.mzcy.LicensedPlugin;
import gg.nextforge.core.events.EventBus;
import gg.nextforge.core.plugin.annotation.NextForgePlugin;
import gg.nextforge.core.plugin.dependency.DependencyManager;
import gg.nextforge.core.plugin.dependency.model.DependencyArtifact;
import gg.nextforge.core.plugin.dependency.model.DependencyRepository;
import gg.nextforge.core.plugin.inject.Initializable;
import gg.nextforge.core.plugin.inject.Injector;
import gg.nextforge.core.plugin.inject.ServiceRegistry;
import gg.nextforge.core.i18n.I18n;
import gg.nextforge.core.i18n.YamlMessageSource;
import gg.nextforge.core.i18n.LocaleResolver;
import gg.nextforge.core.i18n.DefaultLocaleResolver;
import gg.nextforge.core.scheduler.NextForgeScheduler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.nio.file.Path;
import java.util.*;

@Getter
@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class ForgedPlugin extends LicensedPlugin {

    private DependencyManager depManager;
    private ServiceRegistry services;
    private NextForgeScheduler scheduler;
    private EventBus eventBus;

    @Override
    public void enablePlugin() {
        if (!this.getClass().isAnnotationPresent(NextForgePlugin.class))
            throw new IllegalStateException("Plugin must be annotated with @NextForgePlugin");

        Path libDir = this.getDataFolder().toPath().resolve("dependencies");
        Path cache = Path.of(System.getProperty("user.home"), ".nextforge-cache");

        // 1) Resolve dependency groups and prepare isolated classloaders
        depManager = new DependencyManager(cache, libDir, repositories(),
                Math.max(4, Runtime.getRuntime().availableProcessors()));
        try {
            for (var e : groups().entrySet()) {
                depManager.prepareGroup(e.getKey(), e.getValue(), true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Dependency preparation failed", e);
        }

        // 2) Hook after dependencies are loaded
        onLoadDependencies(depManager);

        // 3) Build DI container and wire @Inject fields
        services = new ServiceRegistry();
        services.register(DependencyManager.class, depManager);
        services.register(ForgedPlugin.class, this);

        // 3a) Register i18n YAML services (per plugin)
        var source = new YamlMessageSource(getDataFolder().toPath(), pluginDefaultLocale(), pluginSupportedLocales());
        try {
            source.ensureDefaults(this.getClass().getClassLoader()); // expects /messages/<lang>.yml in plugin jar
            source.loadAll();
        } catch (Exception ex) {
            throw new RuntimeException("i18n initialization failed", ex);
        }
        LocaleResolver resolver = pluginLocaleResolver();
        I18n i18n = new I18n(source, resolver);

        // Qualified by plugin id (isolated in multi-plugin environments)
        services.register(YamlMessageSource.class, pluginId().toString(), source);
        services.register(LocaleResolver.class, pluginId().toString(), resolver);
        services.register(I18n.class, pluginId().toString(), i18n);

        // Optional unqualified registrations for convenience
        services.register(YamlMessageSource.class, source);
        services.register(LocaleResolver.class, resolver);
        services.register(I18n.class, i18n);

        Injector.wire(this, services);

        scheduler = new NextForgeScheduler(Math.min(2, Runtime.getRuntime().availableProcessors() / 2), 10);
        scheduler.bindMainThread();

        eventBus = new EventBus();

        // 4) Pre-enable hook (initialize services etc.)
        beforeEnable(services);

        // 5) Plugin-specific enable logic
        enable();
    }

    @Override
    public void disablePlugin() {
        try {
            disable();
            scheduler.close();
            afterDisable();
        } finally {
            try { if (depManager != null) depManager.close(); } catch (Exception ignored) {}
        }
    }

    /** Called after dependency loading, before DI/lifecycle. */
    protected void onLoadDependencies(DependencyManager manager) {}

    /** Called after DI wiring, before enable(). */
    protected void beforeEnable(ServiceRegistry services) {
        for (var entry : services.getAll().entrySet()) {
            Class<?> type = entry.getKey();
            Object instance = entry.getValue();
            getSLF4JLogger().info("Service loaded: {} ({})", type.getSimpleName(), instance);
            if (instance instanceof Initializable init) {
                init.initialize();
            }
        }
    }

    /** Called after disable(). */
    protected void afterDisable() {}

    public abstract void enable();
    public abstract void disable();

    /** Public/private repositories. */
    public abstract List<DependencyRepository> repositories();

    /** Artifacts grouped by isolated classloader. */
    public abstract Map<String, List<DependencyArtifact>> groups();

    /** Per-plugin default locale. */
    protected Locale pluginDefaultLocale() { return Locale.ENGLISH; }

    /** Per-plugin supported locales (files: <data>/messages/<lang>.yml). */
    protected Set<Locale> pluginSupportedLocales() { return Set.of(Locale.ENGLISH); }

    /** Per-plugin audience locale resolver. */
    protected LocaleResolver pluginLocaleResolver() { return new DefaultLocaleResolver(pluginDefaultLocale()); }

    protected ServiceRegistry services() { return services; }
    protected DependencyManager dependencyManager() { return depManager; }
}
