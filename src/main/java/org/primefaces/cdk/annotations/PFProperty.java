package org.primefaces.cdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target( ElementType.FIELD )
@Retention( RetentionPolicy.SOURCE)
public @interface PFProperty {

    boolean required() default false;

    Class<?> type() default String.class;

    String defaultValue() default "";

    String description();

    boolean ignore() default false;
}
