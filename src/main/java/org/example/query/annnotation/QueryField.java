package org.example.query.annnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface QueryField {
  String fieldName(); // Field to filter on

  JOIN[] joinPath() default {}; // Join path as an array
}
