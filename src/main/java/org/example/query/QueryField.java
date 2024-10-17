package org.example.query;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface QueryField {
  String value(); // Field to filter on

  WhereCondition[] joinPath() default {}; // Join path as an array
}
