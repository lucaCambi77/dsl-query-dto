package org.example.query;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Set;
import org.example.employee.Employee;
import org.example.employee.QEmployee;

public class QueryFilterService {

  public static <T> BooleanBuilder predicateFrom(
      List<QueryFilter> queryFilters, EntityPathBase<T> rootEntity, JPAQuery<?> query) {

    return predicate(queryFilters, rootEntity, query);
  }

  public static <T> BooleanBuilder predicateFromRoot(
      List<QueryFilter> queryFilters, EntityPathBase<T> rootEntity, JPAQueryFactory factory) {

    JPAQuery<Employee> query = factory.selectFrom(QEmployee.employee);

    return predicate(queryFilters, rootEntity, query);
  }

  private static <T> BooleanBuilder predicate(
      List<QueryFilter> queryFilters, EntityPathBase<T> rootEntity, JPAQuery<?> query) {

    BooleanBuilder booleanBuilder = new BooleanBuilder();

    for (QueryFilter queryFilter : queryFilters) {
      booleanBuilder.and(
          buildPredicatesForObject(
              pathBuilder(query, rootEntity, queryFilter.joins()),
              queryFilter.fieldName(),
              queryFilter.value()));
    }
    return booleanBuilder;
  }

  /**
   * Returns either the join or the root path
   *
   * @param query
   * @param rootEntity
   * @param joins
   * @return
   * @param <T>
   */
  private static <T> PathBuilder<?> pathBuilder(
      JPAQuery<?> query, EntityPathBase<T> rootEntity, Set<Join> joins) {

    PathBuilder<?> path = new PathBuilder<>(rootEntity.getType(), rootEntity.getMetadata());

    for (Join join : joins) {

      String alias = join.alias();

      PathBuilder<Object> aliasPath = new PathBuilder<>(join.value(), alias);
      if (!join.collection().isBlank()) {
        query.leftJoin(path.getSet(join.collection(), Object.class), aliasPath).fetchJoin();
      } else {
        query.leftJoin(path.get(alias, Object.class), aliasPath).fetchJoin();
      }
      path = new PathBuilder<>(join.value(), alias);
    }

    return path;
  }

  private static BooleanExpression buildPredicatesForObject(
      PathBuilder<?> path, String fieldPath, Object value) {
    SimpleExpression<Object> expression = path.get(fieldPath);
    return expression.eq(value);
  }
}
