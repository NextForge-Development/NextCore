package gg.nextforge.core.commands.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TabComplete {
    String value() default ""; // "" = root, or sub-name like "hello"
}
