package gg.nextforge.core.data.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PrimaryKey {

    String name() default "";
    boolean mongoId() default true;
    boolean autoGenerate() default false;
}
