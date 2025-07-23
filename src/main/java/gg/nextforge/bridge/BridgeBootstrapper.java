package gg.nextforge.bridge;

import gg.nextforge.bridge.annotations.Bridge;
import org.reflections.Reflections;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Automatically discovers and registers all PluginBridges annotated with @Bridge.
 */
public class BridgeBootstrapper {

    private static final Logger LOGGER = Logger.getLogger("BridgeBootstrapper");

    private final BridgeManager bridgeManager;

    public BridgeBootstrapper(BridgeManager bridgeManager) {
        this.bridgeManager = bridgeManager;
    }

    /**
     * Scans and registers all bridges annotated with @Bridge.
     */
    public void loadBridges(String basePackage) {
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> bridgeClasses = reflections.getTypesAnnotatedWith(Bridge.class);

        for (Class<?> clazz : bridgeClasses) {
            if (!PluginBridge.class.isAssignableFrom(clazz)) {
                LOGGER.warning("Invalid @Bridge class (not a PluginBridge): " + clazz.getName());
                continue;
            }

            try {
                PluginBridge instance = (PluginBridge) clazz.getDeclaredConstructor().newInstance();
                Bridge annotation = clazz.getAnnotation(Bridge.class);

                if (annotation.enabled()) {
                    bridgeManager.register(instance);
                    LOGGER.info("Registered bridge: " + clazz.getSimpleName());
                } else {
                    LOGGER.info("Bridge disabled (annotation): " + clazz.getSimpleName());
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize bridge: " + clazz.getName(), e);
            }
        }
    }
}
