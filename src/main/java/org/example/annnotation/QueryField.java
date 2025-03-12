package org.example.annnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface QueryField {
    /**
     * Database field to filter on
     */
    String name() default "";

    /**
     * Join path as an array
     */
    JOIN[] joinPath() default {};

    /**
     * Whether it is a prefix for a nested field
     */
    boolean isPrefix() default false;
}
