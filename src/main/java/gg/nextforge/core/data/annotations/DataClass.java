package gg.nextforge.core.data.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DataClass {
    String value() default "";
    String table() default "";
    String collection() default "";
    String file() default "";
}