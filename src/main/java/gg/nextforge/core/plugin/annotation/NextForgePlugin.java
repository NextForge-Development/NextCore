package gg.nextforge.core.plugin.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface NextForgePlugin {

    String name();
    String version() default "1.0.0";
    String description() default "";
    String author() default "NextForge Team";

}
