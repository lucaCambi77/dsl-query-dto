package org.example.query;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.example.employee.Employee;
import org.example.employee.QEmployee;

public class QueryFilterService {

  public static <T> BooleanBuilder predicateFrom(
      List<QueryFilter> queryFilters,
      EntityPathBase<T> rootEntity,
      JPAQuery<?> query,
      List<String> expandList) {

    return predicate(queryFilters, rootEntity, query, expandList);
  }

  public static <T> BooleanBuilder predicateFromRoot(
      List<QueryFilter> queryFilters,
      EntityPathBase<T> rootEntity,
      JPAQueryFactory factory,
      List<String> expandList) {

    JPAQuery<Employee> query = factory.selectFrom(QEmployee.employee);

    return predicate(queryFilters, rootEntity, query, expandList);
  }

  private static <T> BooleanBuilder predicate(
      List<QueryFilter> queryFilters,
      EntityPathBase<T> rootEntity,
      JPAQuery<?> query,
      List<String> expandList) {

    BooleanBuilder booleanBuilder = new BooleanBuilder();

    Set<String> appliedJoins = new HashSet<>();

    for (QueryFilter queryFilter : queryFilters) {

      Set<Join> joins = queryFilter.joins();
      joins.forEach(j -> appliedJoins.add(j.alias()));

      booleanBuilder.and(
          buildPredicatesForObject(
              pathBuilder(query, rootEntity, joins), queryFilter.fieldName(), queryFilter.value()));
    }

    for (String expand : expandList) {
      if (!appliedJoins.contains(expand)) {
        applyJoinFromExpand(query, rootEntity, expand);
      }
    }

    return booleanBuilder;
  }

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

  private static <T> void applyJoinFromExpand(
      JPAQuery<?> query, EntityPathBase<T> rootEntity, String expand) {
    PathBuilder<?> path = new PathBuilder<>(rootEntity.getType(), rootEntity.getMetadata());
    PathBuilder<Object> aliasPath = new PathBuilder<>(Object.class, expand);
    query.leftJoin(path.getSet(expand, Object.class), aliasPath).fetchJoin();
  }

  private static BooleanExpression buildPredicatesForObject(
      PathBuilder<?> path, String fieldPath, Object value) {
    SimpleExpression<Object> expression = path.get(fieldPath);
    return expression.eq(value);
  }
}
