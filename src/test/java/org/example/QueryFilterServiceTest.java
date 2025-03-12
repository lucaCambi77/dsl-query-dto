package org.example;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.CollectionExpression;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Operation;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.PathType;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import org.example.dos.QDoClass;
import org.example.query.Join;
import org.example.query.QueryFilter;
import org.example.query.QueryFilterException;
import org.example.query.service.QueryFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.example.QueryFilterServiceImpl.FAKE_DO;
import static org.example.QueryFilterServiceImpl.ONE_TO_MANY_DO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class QueryFilterServiceTest {

    QueryFilterService queryFilterService;

    @Mock
    private JPAQuery<?> mockQuery;

    @Mock
    private EntityPathBase<Object> mockRootEntity;

    @Mock
    private QueryMetadata mockMetadata;

    private static final int totalFiltersNumber = 12;
    private static final int totalFiltersDate = 10;
    private static final int totalFiltersString = 6;

    @BeforeEach
    void setUp() {
        queryFilterService = new QueryFilterServiceImpl();
        when(mockQuery.getMetadata()).thenReturn(mockMetadata);
    }

    @Test
    void createEmptyPredicate() {

        Expression mockProjection = QDoClass.doClass;

        when(mockMetadata.getProjection()).thenReturn(mockProjection);

        Predicate result = queryFilterService.predicateFrom(List.of(), mockRootEntity, mockQuery,
                List.of());

        assertNotNull(result, "Predicate should not be null");
        assertInstanceOf(BooleanBuilder.class, result,
                "Result should be an instance of BooleanBuilder");
        assertFalse(((BooleanBuilder) result).hasValue());
    }

    @Test
    void shouldThrowWhenNoProjection() {

        Expression mockProjection = QDoClass.doClass;

        when(mockMetadata.getProjection()).thenReturn(mockProjection);

        when(mockMetadata.getProjection()).thenReturn(null);

        Executable executable = () ->
                queryFilterService.predicateFrom(List.of(), mockRootEntity, mockQuery,
                        List.of());

        assertThrows(IllegalArgumentException.class, executable);
    }

    @Test
    void shouldThrowWhenInvalidExpand() {

        mockRootPath();
        Executable executable = () ->
                queryFilterService.predicateFrom(List.of(), mockRootEntity, mockQuery,
                        List.of("invalid_expand"));

        assertThrows(QueryFilterException.class, executable);
    }

    @Test
    void shouldThrowWhenFieldNotExists() {

        mockRootPath();
        Executable executable = () ->
                queryFilterService.predicateFrom(
                        List.of(new QueryFilter("not_found_field", List.of(), "value")), mockRootEntity,
                        mockQuery,
                        List.of());

        assertThrows(QueryFilterException.class, executable);
    }

    @Test
    void queryWithExpand() {

        mockRootPath();

        JPAQuery<?> query = mock(JPAQuery.class);

        when(mockQuery.leftJoin(any(CollectionExpression.class), any(Path.class))).thenReturn(query);

        Predicate result = queryFilterService.predicateFrom(List.of(), mockRootEntity, mockQuery,
                List.of(ONE_TO_MANY_DO));

        assertNotNull(result, "Predicate should not be null");
        assertInstanceOf(BooleanBuilder.class, result,
                "Result should be an instance of BooleanBuilder");

        verify(mockQuery, times(1)).leftJoin(any(CollectionExpression.class), any(Path.class));
        verify(query, times(1)).fetchJoin();
    }

    @Test
    void shouldThrowWhenDoNot() {

        mockRootPath();
        Executable executable =
                () -> queryFilterService.predicateFrom(
                        List.of(new QueryFilter("id", List.of(new Join(FAKE_DO)), "value")),
                        mockRootEntity, mockQuery,
                        List.of());

        assertThrows(QueryFilterException.class, executable);
    }

    @Test
    void queryWithJoinAndFetch() {

        mockRootPath();

        JPAQuery<?> query = mock(JPAQuery.class);

        when(mockQuery.leftJoin(any(EntityPath.class), any(Path.class))).thenReturn(query);

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("id", List.of(new Join(ONE_TO_MANY_DO)), "!eq:value")),
                mockRootEntity, mockQuery,
                List.of());

        assertNotNull(result, "Predicate should not be null");
        assertInstanceOf(BooleanBuilder.class, result,
                "Result should be an instance of BooleanBuilder");

        verify(mockQuery, times(1)).leftJoin(any(EntityPath.class), any(Path.class));
        verify(query, times(1)).fetchJoin();
    }

    @Test
    void queryWithJoinAndCount() {

        mockCount();

        JPAQuery<?> query = mock(JPAQuery.class);

        when(mockQuery.leftJoin(any(EntityPath.class), any(Path.class))).thenReturn(query);

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("id", List.of(new Join(ONE_TO_MANY_DO)), "!eq:value")),
                mockRootEntity, mockQuery,
                List.of());

        assertNotNull(result, "Predicate should not be null");
        assertInstanceOf(BooleanBuilder.class, result,
                "Result should be an instance of BooleanBuilder");

        verify(mockQuery, times(1)).leftJoin(any(EntityPath.class), any(Path.class));
        verify(query, times(0)).fetchJoin();
    }

    @Test
    void queryWithStringFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("id", List.of(), "value"),
                        new QueryFilter("id", List.of(), "!eq:value"),
                        new QueryFilter("id", List.of(), "like:value"),
                        new QueryFilter("id", List.of(), "!like:value"),
                        new QueryFilter("id", List.of(), "!in:value,value1"),
                        new QueryFilter("id", List.of(), "in:value,value1")), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertEquals(totalFiltersString, countConditions(((BooleanBuilder) result).getValue()));
    }

    @Test
    void queryWithEnumFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("status", List.of(), "value"),
                        new QueryFilter("status", List.of(), "!eq:value"),
                        new QueryFilter("status", List.of(), "like:value"),
                        new QueryFilter("status", List.of(), "!like:value"),
                        new QueryFilter("status", List.of(), "!in:value,value1"),
                        new QueryFilter("status", List.of(), "in:value,value1")), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertEquals(totalFiltersString, countConditions(((BooleanBuilder) result).getValue()));
    }

    @Test
    void queryWithStringWildCardsFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("id", List.of(), "like:*value*")), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertNotNull(((BooleanBuilder) result).getValue());
        assertTrue(((BooleanBuilder) result).getValue().toString().contains("like %value%"));

        result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("id", List.of(), "like:/*value*")), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertNotNull(((BooleanBuilder) result).getValue());
        assertTrue(((BooleanBuilder) result).getValue().toString().contains("like *value%"));
    }

    @Test
    void queryWithBooleanFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("booleanType", List.of(), "false"),
                        new QueryFilter("booleanType", List.of(), "true")), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertEquals(2, countConditions(((BooleanBuilder) result).getValue()));
    }

    @Test
    void queryWithLongFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("longType", List.of(), "1"),
                        new QueryFilter("longType", List.of(), "like:1"),
                        new QueryFilter("longType", List.of(), "!like:1"),
                        new QueryFilter("longType", List.of(), "!in:1,2"),
                        new QueryFilter("longType", List.of(), "in:1,2"),
                        new QueryFilter("longType", List.of(), "!eq:1"),
                        new QueryFilter("longType", List.of(), "between:1,2"),
                        new QueryFilter("longType", List.of(), "!between:1,2"),
                        new QueryFilter("longType", List.of(), "gt:1"),
                        new QueryFilter("longType", List.of(), "lt:1"),
                        new QueryFilter("longType", List.of(), "!gt:1"),
                        new QueryFilter("longType", List.of(), "!lt:1")

                ), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertEquals(totalFiltersNumber, countConditions(((BooleanBuilder) result).getValue()));
    }

    @Test
    void queryWithFloatFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("floatType", List.of(), "1"),
                        new QueryFilter("floatType", List.of(), "like:1"),
                        new QueryFilter("floatType", List.of(), "!like:1"),
                        new QueryFilter("floatType", List.of(), "!in:1,2"),
                        new QueryFilter("floatType", List.of(), "in:1,2"),
                        new QueryFilter("floatType", List.of(), "!eq:1"),
                        new QueryFilter("floatType", List.of(), "between:1,2"),
                        new QueryFilter("floatType", List.of(), "!between:1,2"),
                        new QueryFilter("floatType", List.of(), "gt:1"),
                        new QueryFilter("floatType", List.of(), "lt:1"),
                        new QueryFilter("floatType", List.of(), "!gt:1"),
                        new QueryFilter("floatType", List.of(), "!lt:1")

                ), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertEquals(totalFiltersNumber, countConditions(((BooleanBuilder) result).getValue()));
    }

    @Test
    void queryWithShortFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("shortType", List.of(), "1"),
                        new QueryFilter("shortType", List.of(), "like:1"),
                        new QueryFilter("shortType", List.of(), "!like:1"),
                        new QueryFilter("shortType", List.of(), "!in:1,2"),
                        new QueryFilter("shortType", List.of(), "in:1,2"),
                        new QueryFilter("shortType", List.of(), "!eq:1"),
                        new QueryFilter("shortType", List.of(), "between:1,2"),
                        new QueryFilter("shortType", List.of(), "!between:1,2"),
                        new QueryFilter("shortType", List.of(), "gt:1"),
                        new QueryFilter("shortType", List.of(), "lt:1"),
                        new QueryFilter("shortType", List.of(), "!gt:1"),
                        new QueryFilter("shortType", List.of(), "!lt:1")

                ), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertEquals(totalFiltersNumber, countConditions(((BooleanBuilder) result).getValue()));
    }

    @Test
    void queryWithIntFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("intType", List.of(), "1"),
                        new QueryFilter("intType", List.of(), "like:1"),
                        new QueryFilter("intType", List.of(), "!like:1"),
                        new QueryFilter("intType", List.of(), "!in:1,2"),
                        new QueryFilter("intType", List.of(), "in:1,2"),
                        new QueryFilter("intType", List.of(), "!eq:1"),
                        new QueryFilter("intType", List.of(), "between:1,2"),
                        new QueryFilter("intType", List.of(), "!between:1,2"),
                        new QueryFilter("intType", List.of(), "gt:1"),
                        new QueryFilter("intType", List.of(), "lt:1"),
                        new QueryFilter("intType", List.of(), "!gt:1"),
                        new QueryFilter("intType", List.of(), "!lt:1")

                ), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertEquals(totalFiltersNumber, countConditions(((BooleanBuilder) result).getValue()));
    }

    @Test
    void queryWithDoubleFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("doubleType", List.of(), "1.0"),
                        new QueryFilter("doubleType", List.of(), "like:1"),
                        new QueryFilter("doubleType", List.of(), "!like:1.0"),
                        new QueryFilter("doubleType", List.of(), "!in:1.0,2.0"),
                        new QueryFilter("doubleType", List.of(), "in:1.0,2.0"),
                        new QueryFilter("doubleType", List.of(), "!eq:1.0"),
                        new QueryFilter("doubleType", List.of(), "between:1.0,2.0"),
                        new QueryFilter("doubleType", List.of(), "!between:1.0,2.0"),
                        new QueryFilter("doubleType", List.of(), "gt:1.0"),
                        new QueryFilter("doubleType", List.of(), "lt:1.0"),
                        new QueryFilter("doubleType", List.of(), "!gt:1.0"),
                        new QueryFilter("doubleType", List.of(), "!lt:1.0")

                ), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertEquals(totalFiltersNumber, countConditions(((BooleanBuilder) result).getValue()));
    }

    @Test
    void queryWithBigDecimalFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("decimalType", List.of(), "1.0"),
                        new QueryFilter("decimalType", List.of(), "like:1"),
                        new QueryFilter("decimalType", List.of(), "!like:1.0"),
                        new QueryFilter("decimalType", List.of(), "!in:1.0,2.0"),
                        new QueryFilter("decimalType", List.of(), "in:1.0,2.0"),
                        new QueryFilter("decimalType", List.of(), "!eq:1.0"),
                        new QueryFilter("decimalType", List.of(), "between:1.0,2.0"),
                        new QueryFilter("decimalType", List.of(), "!between:1.0,2.0"),
                        new QueryFilter("decimalType", List.of(), "gt:1.0"),
                        new QueryFilter("decimalType", List.of(), "lt:1.0"),
                        new QueryFilter("decimalType", List.of(), "!gt:1.0"),
                        new QueryFilter("decimalType", List.of(), "!lt:1.0")

                ), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertEquals(totalFiltersNumber, countConditions(((BooleanBuilder) result).getValue()));
    }

    @Test
    void queryWithBigIntegerFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("bigIntegerType", List.of(), "1"),
                        new QueryFilter("bigIntegerType", List.of(), "like:1"),
                        new QueryFilter("bigIntegerType", List.of(), "!like:1"),
                        new QueryFilter("bigIntegerType", List.of(), "!in:1,2"),
                        new QueryFilter("bigIntegerType", List.of(), "in:1,2"),
                        new QueryFilter("bigIntegerType", List.of(), "!eq:1"),
                        new QueryFilter("bigIntegerType", List.of(), "between:1,2"),
                        new QueryFilter("bigIntegerType", List.of(), "!between:1,2"),
                        new QueryFilter("bigIntegerType", List.of(), "gt:1"),
                        new QueryFilter("bigIntegerType", List.of(), "lt:1"),
                        new QueryFilter("bigIntegerType", List.of(), "!gt:1"),
                        new QueryFilter("bigIntegerType", List.of(), "!lt:1")

                ), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertEquals(totalFiltersNumber, countConditions(((BooleanBuilder) result).getValue()));
    }

    @Test
    void shouldThrowQueryWithWrongDateFormat() {
        mockRootPath();
        Executable executable = () ->
                queryFilterService.predicateFrom(
                        List.of(new QueryFilter("dateType", List.of(), "2020-01")), mockRootEntity,
                        mockQuery,
                        List.of());

        assertThrows(IllegalArgumentException.class, executable);

        executable = () ->
                queryFilterService.predicateFrom(
                        List.of(new QueryFilter("dateType", List.of(), "between:2020-01,2020-01-02")),
                        mockRootEntity,
                        mockQuery,
                        List.of());

        assertThrows(IllegalArgumentException.class, executable);

        executable = () ->
                queryFilterService.predicateFrom(
                        List.of(new QueryFilter("localDateTimeType", List.of(), "2020-01")), mockRootEntity,
                        mockQuery,
                        List.of());

        assertThrows(IllegalArgumentException.class, executable);

        executable = () ->
                queryFilterService.predicateFrom(
                        List.of(new QueryFilter("localDateTimeType", List.of(), "between:2020-01,2020-01-02")),
                        mockRootEntity,
                        mockQuery,
                        List.of());

        assertThrows(IllegalArgumentException.class, executable);

        executable = () ->
                queryFilterService.predicateFrom(
                        List.of(new QueryFilter("localDateType", List.of(), "between:2020-01,2020-01-02")),
                        mockRootEntity,
                        mockQuery,
                        List.of());

        assertThrows(IllegalArgumentException.class, executable);

        executable = () ->
                queryFilterService.predicateFrom(
                        List.of(new QueryFilter("localDateType", List.of(), "2020-01")),
                        mockRootEntity,
                        mockQuery,
                        List.of());

        assertThrows(IllegalArgumentException.class, executable);

        executable = () ->
                queryFilterService.predicateFrom(
                        List.of(new QueryFilter("localDateType", List.of(), "between:2020-01")),
                        mockRootEntity,
                        mockQuery,
                        List.of());

        assertThrows(IllegalArgumentException.class, executable);
    }

    @Test
    void queryWithDateFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("dateType", List.of(), "2020-01-01"),
                        new QueryFilter("dateType", List.of(), "!eq:2020-01-01"),
                        new QueryFilter("dateType", List.of(), "!in:2020-01-01,2020-01-02"),
                        new QueryFilter("dateType", List.of(), "in:2020-01-01,2020-01-02"),
                        new QueryFilter("dateType", List.of(), "between:2020-01-01,2020-01-02"),
                        new QueryFilter("dateType", List.of(), "!between:2020-01-01,2020-01-02"),
                        new QueryFilter("dateType", List.of(), "gt:2020-01-01"),
                        new QueryFilter("dateType", List.of(), "lt:2020-01-01"),
                        new QueryFilter("dateType", List.of(), "!gt:2020-01-01"),
                        new QueryFilter("dateType", List.of(), "!lt:2020-01-01")

                ), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertTrue(countConditions(((BooleanBuilder) result).getValue()) >= totalFiltersDate);
    }

    @Test
    void queryWithLocalDateFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("localDateType", List.of(), "2020-01-01"),
                        new QueryFilter("localDateType", List.of(), "!eq:2020-01-01"),
                        new QueryFilter("localDateType", List.of(), "!in:2020-01-01,2020-01-02"),
                        new QueryFilter("localDateType", List.of(), "in:2020-01-01,2020-01-02"),
                        new QueryFilter("localDateType", List.of(), "between:2020-01-01,2020-01-02"),
                        new QueryFilter("localDateType", List.of(), "!between:2020-01-01,2020-01-02"),
                        new QueryFilter("localDateType", List.of(), "gt:2020-01-01"),
                        new QueryFilter("localDateType", List.of(), "lt:2020-01-01"),
                        new QueryFilter("localDateType", List.of(), "!gt:2020-01-01"),
                        new QueryFilter("localDateType", List.of(), "!lt:2020-01-01")

                ), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertTrue(countConditions(((BooleanBuilder) result).getValue()) >= totalFiltersDate);
    }

    @Test
    void queryWithDateWithTimeFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("dateType", List.of(), "2020-01-01 12:00:00"),
                        new QueryFilter("dateType", List.of(), "!eq:2020-01-01 12:00:00"),
                        new QueryFilter("dateType", List.of(), "!in:2020-01-01 12:00:00,2020-01-02 12:00:00"),
                        new QueryFilter("dateType", List.of(), "in:2020-01-01 12:00:00,2020-01-02 12:00:00"),
                        new QueryFilter("dateType", List.of(),
                                "between:2020-01-01 12:00:00,2020-01-02 12:00:00"),
                        new QueryFilter("dateType", List.of(),
                                "!between:2020-01-01 12:00:00,2020-01-02 12:00:00"),
                        new QueryFilter("dateType", List.of(), "gt:2020-01-01 12:00:00"),
                        new QueryFilter("dateType", List.of(), "lt:2020-01-01 12:00:00"),
                        new QueryFilter("dateType", List.of(), "!gt:2020-01-01 12:00:00"),
                        new QueryFilter("dateType", List.of(), "!lt:2020-01-01 12:00:00")

                ), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertTrue(countConditions(((BooleanBuilder) result).getValue()) >= totalFiltersDate);
    }

    @Test
    void queryWithLocalDateTimeFilter() {

        mockRootPath();

        Predicate result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("localDateTimeType", List.of(), "2020-01-01 12:00:00"),
                        new QueryFilter("localDateTimeType", List.of(), "!eq:2020-01-01 12:00:00"),
                        new QueryFilter("localDateTimeType", List.of(),
                                "!in:2020-01-01 12:00:00,2020-01-02 12:00:00"),
                        new QueryFilter("localDateTimeType", List.of(),
                                "in:2020-01-01 12:00:00,2020-01-02 12:00:00"),
                        new QueryFilter("localDateTimeType", List.of(),
                                "between:2020-01-01 12:00:00,2020-01-02 12:00:00"),
                        new QueryFilter("localDateTimeType", List.of(),
                                "!between:2020-01-01 12:00:00,2020-01-02 12:00:00"),
                        new QueryFilter("localDateTimeType", List.of(), "gt:2020-01-01 12:00:00"),
                        new QueryFilter("localDateTimeType", List.of(), "lt:2020-01-01 12:00:00"),
                        new QueryFilter("localDateTimeType", List.of(), "!gt:2020-01-01 12:00:00"),
                        new QueryFilter("localDateTimeType", List.of(), "!lt:2020-01-01 12:00:00")

                ), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertTrue(countConditions(((BooleanBuilder) result).getValue()) >= totalFiltersDate);

        result = queryFilterService.predicateFrom(
                List.of(new QueryFilter("localDateTimeType", List.of(), "2020-01-01"),
                        new QueryFilter("localDateTimeType", List.of(), "!eq:2020-01-01"),
                        new QueryFilter("localDateTimeType", List.of(), "!in:2020-01-01,2020-01-02"),
                        new QueryFilter("localDateTimeType", List.of(), "in:2020-01-01,2020-01-02"),
                        new QueryFilter("localDateTimeType", List.of(),
                                "between:2020-01-01,2020-01-02"),
                        new QueryFilter("localDateTimeType", List.of(),
                                "!between:2020-01-01,2020-01-02"),
                        new QueryFilter("localDateTimeType", List.of(), "gt:2020-01-01"),
                        new QueryFilter("localDateTimeType", List.of(), "lt:2020-01-01"),
                        new QueryFilter("localDateTimeType", List.of(), "!gt:2020-01-01"),
                        new QueryFilter("localDateTimeType", List.of(), "!lt:2020-01-01")

                ), mockRootEntity, mockQuery,
                List.of());

        assertInstanceOf(BooleanBuilder.class, result);
        assertTrue(countConditions(((BooleanBuilder) result).getValue()) >= totalFiltersDate);
    }

    private int countConditions(Predicate predicate) {
        if (predicate == null) {
            return 0; // No predicate means no conditions
        }

        int count = 0;
        if (predicate instanceof Operation<?> operation) {
            // Recursively count conditions in the operation's arguments
            for (Expression<?> arg : operation.getArgs()) {
                String[] parts = arg.toString().split("&&");

                // Filter out empty parts to avoid counting invalid splits
                count += (int) Arrays.stream(parts)
                        .map(String::trim)
                        .filter(part -> !part.isEmpty())
                        .count();
            }
        }

        return count;
    }

    @Test
    void queryWithCount() {

        mockCount();

        JPAQuery<?> query = mock(JPAQuery.class);

        when(mockQuery.leftJoin(any(CollectionExpression.class), any(Path.class))).thenReturn(query);

        Predicate result = queryFilterService.predicateFrom(List.of(), mockRootEntity, mockQuery,
                List.of(ONE_TO_MANY_DO));

        assertNotNull(result, "Predicate should not be null");
        assertInstanceOf(BooleanBuilder.class, result,
                "Result should be an instance of BooleanBuilder");

        verify(mockQuery, times(1)).leftJoin(any(CollectionExpression.class), any(Path.class));
        verify(query, times(0)).fetchJoin();
    }

    private void mockCount() {
        when(mockRootEntity.getMetadata()).thenReturn(
                new PathMetadata(new PathBuilder<>(Object.class, "parentPath"),
                        QDoClass.class,
                        PathType.VARIABLE));

        Expression mockProjection = mock(Expression.class);
        when(mockProjection.toString()).thenReturn("count");

        when(mockMetadata.getProjection()).thenReturn(mockProjection);
    }

    private void mockRootPath() {
        when(mockRootEntity.getMetadata()).thenReturn(
                new PathMetadata(new PathBuilder<>(Object.class, "parentPath"),
                        QDoClass.class,
                        PathType.VARIABLE));

        Expression mockProjection = QDoClass.doClass;

        when(mockMetadata.getProjection()).thenReturn(mockProjection);

        when(mockRootEntity.getType()).thenReturn(mockProjection.getType());
    }
}
