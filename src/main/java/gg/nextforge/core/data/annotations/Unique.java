package gg.nextforge.core.data.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
@Repeatable(Uniques.class)
public @interface Unique {
    String name() default "";
    String[] columns() default {}; // bei TYPE verwenden; bei FIELD ignoriert
}
