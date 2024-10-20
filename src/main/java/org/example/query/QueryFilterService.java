package org.example.query;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.SimpleExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public interface QueryFilterService {

    default <T> Predicate predicateFrom(
            List<QueryFilter> queryFilters,
            EntityPathBase<T> rootEntity,
            JPAQuery<?> query,
            List<String> expandList) {

        return defaultPredicate(queryFilters, rootEntity, query, expandList);
    }

    default <T> Predicate predicateFromRoot(
            List<QueryFilter> queryFilters,
            EntityPathBase<T> rootEntity,
            JPAQueryFactory factory,
            List<String> expandList) {

        JPAQuery<?> query = factory.selectFrom(rootEntity);

        return defaultPredicate(queryFilters, rootEntity, query, expandList);
    }

    private <T> Predicate defaultPredicate(
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

        return booleanBuilder.and(defaultPredicate(query, rootEntity));
    }

    private <T> PathBuilder<?> pathBuilder(
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

    private <T> void applyJoinFromExpand(
            JPAQuery<?> query, EntityPathBase<T> rootEntity, String expand) {
        PathBuilder<?> path = new PathBuilder<>(rootEntity.getType(), rootEntity.getMetadata());
        query.leftJoin(path.getSet(expand, Object.class)).fetchJoin();
    }

    private Predicate buildPredicatesForObject(
            PathBuilder<?> path, String fieldPath, Object value) {
        SimpleExpression<Object> expression = path.get(fieldPath);
        return expression.eq(value);
    }

    <T> Predicate defaultPredicate(JPAQuery<?> query, EntityPathBase<T> rootEntity);
}
