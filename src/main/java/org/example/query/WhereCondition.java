package org.example.query;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface WhereCondition {
  String collection() default "";

  String
      alias(); // an alias for the entity, it is mandatory otherwise we can't create dynamic joins

  Class<?> value(); // refers to the entity class we want to join.
}
