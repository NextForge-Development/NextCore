package gg.nextforge.bridge.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a PluginBridge for automatic discovery and registration.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Bridge {

    /**
     * Whether to enable this bridge by default.
     */
    boolean enabled() default true;

    /**
     * Optional description of the bridge.
     */
    String description() default "";
}
