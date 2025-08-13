package gg.nextforge.core.commands.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {
    String name();
    String[] aliases() default {};
    String permission() default "";
    String descriptionKey() default ""; // i18n key (optional)
}
