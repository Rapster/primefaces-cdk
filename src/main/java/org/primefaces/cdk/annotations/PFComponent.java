package org.primefaces.cdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target( ElementType.TYPE )
@Retention( RetentionPolicy.SOURCE )
public @interface PFComponent {

    String tagName();

    String description();

    boolean widget() default false;

    boolean rtl() default false;

    Class<?> componentHandlerClass() default void.class;
}
