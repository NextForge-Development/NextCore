package gg.nextforge.core.data.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
@Repeatable(Indexes.class)
public @interface Index {
    String name() default "";      // falls leer -> auto
    String[] columns() default {}; // bei TYPE verwenden; bei FIELD ignoriert
    boolean unique() default false;
}
