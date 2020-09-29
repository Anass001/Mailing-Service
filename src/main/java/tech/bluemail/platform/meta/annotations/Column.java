package tech.bluemail.platform.meta.annotations;

import java.lang.annotation.*;

@Documented
@Target({ ElementType.FIELD })
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String name() default "";
    
    boolean autoincrement() default false;
    
    boolean primary() default false;
    
    String type() default "integer";
    
    boolean nullable() default false;
    
    int length() default 0;
}
