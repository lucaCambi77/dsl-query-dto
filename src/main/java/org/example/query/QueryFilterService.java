package org.example.query;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.jpa.impl.JPAQuery;
import java.lang.reflect.Field;
import org.example.EmployeeFilterRequest;

public class QueryFilterService {

  public static <T> BooleanBuilder createDynamicPredicate(
      EmployeeFilterRequest filterRequest, EntityPathBase<T> rootEntity, JPAQuery<?> query) {

    BooleanBuilder booleanBuilder = new BooleanBuilder();

    for (Field field : filterRequest.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      QueryField queryFieldAnnotation = field.getAnnotation(QueryField.class);

      if (queryFieldAnnotation != null) {
        try {
          Object value = field.get(filterRequest);
          if (value != null) {
            String fieldName = queryFieldAnnotation.value();
            WhereCondition[] joinPath = queryFieldAnnotation.joinPath();

            booleanBuilder.and(
                (buildPredicatesForObject(
                    pathBuilder(query, rootEntity, joinPath), fieldName, value)));
          }
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return booleanBuilder;
  }

  private static <T> PathBuilder<?> pathBuilder(
      JPAQuery<?> query, EntityPathBase<T> rootEntity, WhereCondition[] joinPath) {

    PathBuilder<?> path = new PathBuilder<>(rootEntity.getType(), rootEntity.getMetadata());

    for (WhereCondition part : joinPath) {
      PathBuilder<Object> aliasPath = new PathBuilder<>(part.value(), part.alias());
      if (!part.collection().isBlank()) {
        query.leftJoin(path.getSet(part.collection(), Object.class), aliasPath).fetchJoin();
      } else {
        query.leftJoin(path.get(part.alias(), Object.class), aliasPath).fetchJoin();
      }
      path = new PathBuilder<>(part.value(), part.alias());
    }

    return path;
  }

  private static BooleanExpression buildPredicatesForObject(
      PathBuilder<?> path, String fieldPath, Object value) {
    SimpleExpression<Object> expression = path.get(fieldPath);
    return expression.eq(value);
  }
}
