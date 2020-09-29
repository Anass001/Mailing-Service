/*
 * Decompiled with CFR <Could not determine version>.
 */
package tech.bluemail.platform.meta.annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(value={ElementType.FIELD})
@Inherited
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Column {
    public String name() default "";

    public boolean autoincrement() default false;

    public boolean primary() default false;

    public String type() default "integer";

    public boolean nullable() default false;

    public int length() default 0;
}

