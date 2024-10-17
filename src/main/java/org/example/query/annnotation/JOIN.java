package org.example.query.annnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface JOIN {
  /** Collection name in case of one to many, many to many */
  String collection() default "";

  /** Entity class we want to join */
  Class<?> entityClass();
}
