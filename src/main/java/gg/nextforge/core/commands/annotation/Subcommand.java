package gg.nextforge.core.commands.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subcommand {
    String value();              // e.g. "reload", "hello"
    String permission() default "";
    String descriptionKey() default ""; // i18n key (optional)
}
