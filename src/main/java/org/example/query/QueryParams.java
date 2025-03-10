package org.example.query;

import org.example.query.annnotation.JOIN;
import org.example.query.annnotation.QueryField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record QueryParams(List<QueryFilter> queryFilter, PageRequest pageRequest,
                          List<String> expandList) {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryParams.class);

    public QueryParams(List<QueryFilter> queryFilter, PageRequest pageRequest,
                       List<String> expandList) {
        this.queryFilter = new ArrayList<>(queryFilter);
        this.pageRequest = pageRequest;
        // Implicitly expand the joins
        this.expandList = Stream.concat(
                queryFilter.stream()
                        .flatMap(qf -> qf.joins().stream().map(Join::entityToJoin)),
                Optional.ofNullable(expandList).orElse(Collections.emptyList()).stream()
        ).toList();
    }

    /**
     * Create a query filter list from the request. The request has the table fields, and we use
     * reflection to access the class field corresponding to the one of the table. The table field can
     * be built by concatenation of nested fields if required. For example, location.cod can be
     * locationCod by stating the query field location is a prefix. For nested objects, we recursively
     * scan the class and get all fields.
     *
     * @param filterRequest containing the mapping for the tables objects
     * @param pageRequest   with limit, page number and sort
     * @param expand        list of tables to expand and fetch the attributes for
     * @return QueryParams from a filter request with all relevant query filters, pagination request
     * and the expand list
     */
    public static QueryParams from(Object filterRequest, PageRequest pageRequest,
                                   List<String> expand) {

        List<QueryFilter> queryFilters = new ArrayList<>();

        if (null != filterRequest) {

            for (Field field : getAllFields(filterRequest.getClass())) {
                field.setAccessible(true); //NOSONAR

                QueryField queryFieldAnnotation = field.getAnnotation(QueryField.class);

                JOIN[] joinPath =
                        queryFieldAnnotation == null ? new JOIN[]{} : queryFieldAnnotation.joinPath();

                try {
                    populateQueryFilters(field, queryFieldAnnotation, joinPath,
                            fieldName(field, queryFieldAnnotation),
                            field.get(filterRequest), queryFilters);
                } catch (IllegalAccessException e) {
                    LOGGER.error("Unable to parse query field {}", field);
                    throw new QueryFilterException(e.getMessage(), e);
                }

            }
        }

        return new QueryParams(queryFilters, pageRequest,
                expand);
    }

    private static void populateQueryFilters(Field field, QueryField rootQueryFieldAnnotation,
                                             JOIN[] joinPath,
                                             String fieldName, Object value,
                                             List<QueryFilter> queryFilters) throws IllegalAccessException {
        if (value != null) {

            Class<?> fieldType = field.getType();

            if (List.class.isAssignableFrom(fieldType)) {
                List<?> rawList = (List<?>) value;
                List<String> list = new ArrayList<>();

                for (Object o : rawList) {
                    list.add((String) o);
                }

                list.forEach(v ->
                        queryFilters.add(
                                new QueryFilter(
                                        fieldName,
                                        Arrays.stream(joinPath)
                                                .map(jp -> new Join(jp.entityToJoin()))
                                                .collect(Collectors.toCollection(LinkedList::new)),
                                        v)));

            } else if (Boolean.class.isAssignableFrom(fieldType)) {
                queryFilters.add(
                        new QueryFilter(
                                fieldName,
                                Arrays.stream(joinPath)
                                        .map(jp -> new Join(jp.entityToJoin()))
                                        .collect(Collectors.toCollection(LinkedList::new)),
                                value));
            } else if (!fieldType.isPrimitive()) {
                for (Field fieldsFromComplexObject : getAllFields(value.getClass())) {
                    fieldsFromComplexObject.setAccessible(true); // NOSONAR

                    String segment = fieldName(fieldsFromComplexObject,
                            fieldsFromComplexObject.getAnnotation(QueryField.class));

                    String finalName = getFinalName(rootQueryFieldAnnotation, fieldName, segment);

                    QueryField queryField = fieldsFromComplexObject.getAnnotation(QueryField.class);

                    JOIN[] join =
                            queryField == null ? new JOIN[]{}
                                    : queryField.joinPath();

                    populateQueryFilters(fieldsFromComplexObject,
                            fieldsFromComplexObject.getAnnotation(QueryField.class),
                            Stream.concat(Stream.of(joinPath), Stream.of(join))
                                    .toArray(JOIN[]::new),
                            finalName,
                            fieldsFromComplexObject.get(value),
                            queryFilters);
                }
            }
        }
    }

    private static String getFinalName(QueryField rootQueryFieldAnnotation, String fieldName,
                                       String segment) {
        if (null != rootQueryFieldAnnotation && rootQueryFieldAnnotation.isPrefix()) {
            return
                    fieldName + segment.substring(0, 1).toUpperCase() + segment.substring(1);
        } else {
            return segment;
        }
    }

    private static String fieldName(Field field, QueryField queryFieldAnnotation) {
        return queryFieldAnnotation == null || queryFieldAnnotation.name().isBlank()
                ? field.getName()
                : queryFieldAnnotation.name();
    }

    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();

        while (clazz != null) {
            Field[] declaredFields = clazz.getDeclaredFields();
            fields.addAll(Arrays.asList(declaredFields));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    @Override
    public String toString() {

        String attributes = Stream.of(
                        Optional.ofNullable(queryFilter).map(q -> "queryFilter=" + q).orElse(null),
                        Optional.ofNullable(pageRequest).map(p -> "pageRequest=" + p).orElse(null),
                        Optional.ofNullable(expandList).map(e -> "expandList=" + e).orElse(null)
                )
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        return "QueryParams{" + attributes + "}";
    }
}
