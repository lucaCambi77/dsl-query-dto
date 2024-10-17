package org.example.query;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.example.query.annnotation.JOIN;
import org.example.query.annnotation.QueryField;

public class QueryFieldConverter {

  public static List<QueryFilter> convert(FilterRequest filterRequest) {
    List<QueryFilter> queryParams = new ArrayList<>();

    for (Field field : filterRequest.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      QueryField queryFieldAnnotation = field.getAnnotation(QueryField.class);

      if (queryFieldAnnotation != null) {
        try {
          Object value = field.get(filterRequest);
          if (value != null) {
            String fieldName = queryFieldAnnotation.fieldName();
            JOIN[] joinPath = queryFieldAnnotation.joinPath();

            queryParams.add(
                new QueryFilter(
                    fieldName,
                    Arrays.stream(joinPath)
                        .map(
                            jp ->
                                new Join(
                                    jp.collection(),
                                    jp.entityClass(),
                                    jp.entityClass().getSimpleName().toLowerCase()))
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                    value));
          }
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return queryParams;
  }
}
