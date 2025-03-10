package org.example.query.service;

import static com.querydsl.core.types.dsl.Expressions.dateTemplate;
import static java.util.stream.Collectors.toSet;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQuery;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.example.query.Join;
import org.example.query.QueryFilter;
import org.example.query.QueryFilterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface QueryFilterService {

  Logger LOGGER = LoggerFactory.getLogger(QueryFilterService.class);

  DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  Pattern datePattern = Pattern.compile(
      "^\\d{4}-\\d{2}-\\d{2}$"); // Matches "yyyy-MM-dd"
  Pattern dateTimePattern = Pattern.compile(
      "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$"); // Matches "yyyy-MM-dd HH:mm:ss"
  Pattern inSplitPattern = Pattern.compile("(?<!/),");
  String OPERATOR_REGEX = "(eq|!eq|like|!like|gt|!gt|lt|!lt|between|!between|in|!in)";
  String ESCAPE = "/";
  String OPERATOR_SPLITTER = ":";
  String VARCHAR_CAST = "CAST({0} AS VARCHAR)";


  List<Class<?>> shortClasses = List.of(Short.class, short.class);
  List<Class<?>> floatClasses = List.of(Float.class, float.class);
  List<Class<?>> doubleClasses = List.of(Double.class, double.class);
  List<Class<?>> longClasses = List.of(Long.class, long.class);
  List<Class<?>> integerClasses = List.of(Integer.class, int.class);
  List<Class<?>> bigIntegerClasses = List.of(BigInteger.class);
  List<Class<?>> bigDecimalClasses = List.of(BigDecimal.class);

  List<Class<?>> numberClasses = Stream.of(
          shortClasses,
          floatClasses,
          doubleClasses,
          longClasses,
          integerClasses,
          bigIntegerClasses,
          bigDecimalClasses
      ).
      flatMap(List::stream).
      toList();

  default <T> Predicate predicateFrom(
      List<QueryFilter> queryFilters,
      EntityPathBase<T> rootEntity,
      JPAQuery<?> query,
      List<String> expandList) {

    return predicate(queryFilters, rootEntity, query, expandList);
  }

  /**
   * Create a Predicate with fields in and conditions for the QueryFilter created from the request
   * parameters.
   *
   * @param queryFilters list of QueryFilter from the request
   * @param rootEntity   DO root entity
   * @param query        JPA query from root entity
   * @param expandList   list of mandatory entity to expand. It might contain already joined tables
   * @return Predicate with fields in and conditions
   */
  private <T> Predicate predicate(
      List<QueryFilter> queryFilters,
      EntityPathBase<T> rootEntity,
      JPAQuery<?> query,
      List<String> expandList) {

    Expression<?> projection = query.getMetadata().getProjection();

    if (projection == null) {
      throw new IllegalArgumentException("Query should have an entity or a count projection");
    }

    var booleanBuilder = new BooleanBuilder();

    Set<String> appliedJoins = new HashSet<>();

    for (QueryFilter queryFilter : queryFilters) {

      List<Join> joins = queryFilter.joins();
      joins.forEach(j -> appliedJoins.add(j.entityToJoin()));

      booleanBuilder.and(
          buildPredicatesForObject(
              pathBuilder(query, rootEntity, joins, projection), queryFilter.fieldName(),
              queryFilter.value()
          ));
    }

    Set<String> finalJoinList = Stream.concat(expandList.stream(), defaultJoins().stream())
        .collect(toSet());

    for (String expand : finalJoinList) {
      if (!appliedJoins.contains(expand)) {
        applyJoinFromExpand(query, rootEntity, expand, projection);
      }
    }

    return booleanBuilder.and(defaultPredicate(query, rootEntity));
  }

  /**
   * Create a path given the root entity and the joins. Joins are built by getting the DO entity
   * class from the input join string
   *
   * @param query      JPA query from root
   * @param rootEntity is the root entity path from DO
   * @param joins      set of join strings from the request
   * @return the path for the current entity from the root to the joins if any
   */
  private <T> PathBuilder<?> pathBuilder(
      JPAQuery<?> query, EntityPathBase<T> rootEntity, List<Join> joins, Expression<?> projection) {

    PathBuilder<?> path = new PathBuilder<>(rootEntity.getType(), rootEntity.getMetadata());

    for (Join join : joins) {
      if (isRoot(join.entityToJoin())) {
        continue;
      }

      Class<?> doClass = doClass(join.entityToJoin());

      String alias =
          alias(join.entityToJoin()) == null ? join.entityToJoin() : alias(join.entityToJoin());

      PathBuilder<Object> aliasPath;

      aliasPath = new PathBuilder<>(doClass, alias);

      // check that the entity has not already a join by class or by alias
      if (query.getMetadata().getJoins().stream()
          .noneMatch(j -> j.getTarget().getType().equals(doClass)) || query.getMetadata()
          .getJoins().stream()
          .noneMatch(j -> j.getTarget().toString().contains("as " + alias))) {

        String entityPath = entityPath(join);

        PathBuilder<Object> target = path.get(entityPath, Object.class);

        if (projection.toString().contains("count")) {
          query.leftJoin(target, aliasPath);
        } else {
          query.leftJoin(target, aliasPath)
              .fetchJoin();
        }
      }
      path = aliasPath;
    }

    return path;
  }

  private String entityPath(Join join) {
    String entityPath = entityFrom(join.entityToJoin());

    if (entityPath == null) {
      throw new QueryFilterException(
          String.format("Entity Path is not defined for %s", join.entityToJoin()));
    }
    return entityPath;
  }

  private Class<?> doClass(String entityToJoin) {
    Class<?> doClass = doFrom(entityToJoin);

    if (doClass == null) {
      throw new QueryFilterException(
          String.format("DO class is not defined for %s", entityToJoin));
    }
    return doClass;
  }

  private <T> void applyJoinFromExpand(
      JPAQuery<?> query, EntityPathBase<T> rootEntity, String expand, Expression<?> projection) {
    List<String> expandsFrom = expandsFrom(expand);

    PathBuilder<?> path = new PathBuilder<>(rootEntity.getType(), rootEntity.getMetadata());

    for (String exp : expandsFrom) {

      Class<?> doClass = doClass(exp);

      PathBuilder<Object> aliasPath = new PathBuilder<>(doClass, exp);

      if (projection.toString().contains("count")) {
        query.leftJoin(path.getSet(entityFrom(exp), Object.class), aliasPath);
      } else {
        query.leftJoin(path.getSet(entityFrom(exp), Object.class), aliasPath)
            .fetchJoin();
      }
      path = aliasPath;
    }
  }

  /**
   * Builds a Predicate based on the given path, field path, and condition specified in the value.
   * Supports conditions such as "eq", "like", "gt", "goe", "lt", "loe", in", "between", etc
   *
   * @param path      the PathBuilder instance representing the entity path.
   * @param fieldPath the field path within the entity to apply the condition to.
   * @param value     the value specifying the condition and filter, e.g., "eq:value".
   * @return a Predicate representing the condition applied to the field.
   */
  private Predicate buildPredicatesForObject(
      PathBuilder<?> path, String fieldPath, Object value) {

    String[] parts = parseConditionAndValue(value.toString());
    ConditionType condition = ConditionType.EQ; // Default to EQ
    String filter = parts[0];

    if (parts.length == 2) {
      condition = ConditionType.from(parts[1].toLowerCase());
    }

    return buildExpression(path, fieldPath, condition, filter);
  }

  /**
   * Parses the input to extract the condition type and the filter value. The input format should be
   * "condition:filter", e.g., "eq:value".
   *
   * @param input the input string to parse.
   * @return an array where the first element is the filter value and the second is the condition
   * type.
   */
  private String[] parseConditionAndValue(String input) {
    var matcher = Pattern.compile("^" + OPERATOR_REGEX + OPERATOR_SPLITTER + "(.*)$",
            Pattern.CASE_INSENSITIVE)
        .matcher(input);
    if (matcher.find()) {
      return new String[]{matcher.group(2), matcher.group(1)};
    }
    return new String[]{input}; // Default to "eq" without condition prefix
  }

  /**
   * Constructs a Predicate based on the field type and condition. Supports types such as String,
   * Boolean, Number types, Date, LocalDate, and LocalDateTime.
   *
   * @param path      the PathBuilder instance.
   * @param fieldPath the path to the field on which to apply the condition.
   * @param condition the ConditionType to apply.
   * @param value     the value to compare against.
   * @return a Predicate representing the condition applied to the field.
   */
  private Predicate buildExpression(PathBuilder<?> path, String fieldPath,
      ConditionType condition,
      String value) {

    Class<?> fieldType = resolveFieldType(path.getType(), fieldPath);

    if (fieldType == String.class) {
      return buildStringPredicate(path.getString(fieldPath), condition, value);
    } else if (fieldType.isEnum()) {
      return buildEnumPredicate(path.getEnum(fieldPath, (Class<Enum>) fieldType), condition,
          value);
    } else if (fieldType == Boolean.class || fieldType == boolean.class) {
      return path.getBoolean(fieldPath).eq(Boolean.valueOf(value));
    } else if (numberClasses.contains(fieldType)) {
      return buildNumberPredicate(path, fieldType, fieldPath,
          condition, value);
    } else if (fieldType == LocalDate.class) {
      return localDateExpression(path, fieldPath, condition, value);
    } else if (fieldType == LocalDateTime.class) {
      return localDateTimeExpression(path, fieldPath, condition, value);
    } else if (fieldType == Date.class) {
      return dateExpression(path, fieldPath, condition, value);
    } else {
      throw new IllegalArgumentException("Unsupported field type for path: " + fieldType);
    }
  }

  private Predicate buildStringPredicate(StringPath stringPath, ConditionType condition,
      String value) {
    return switch (condition) {
      case NEQ -> stringPath.eq(cleanString(value)).not();
      case LIKE -> stringPath.like(cleanString(value));
      case NLIKE -> stringPath.like(cleanString(value)).not();
      case IN -> stringPath.in(inSplitValues(value));
      case NIN -> stringPath.in(inSplitValues(value)).not();
      default -> stringPath.eq(cleanString(value));
    };
  }

  private Predicate buildEnumPredicate(EnumPath<?> stringPath, ConditionType condition,
      String value) {
    return switch (condition) {
      case NEQ -> stringPath.stringValue().eq(cleanString(value)).not();
      case LIKE -> stringPath.stringValue().like(cleanString(value));
      case NLIKE -> stringPath.stringValue().like(cleanString(value)).not();
      case IN -> stringPath.stringValue().in(inSplitValues(value));
      case NIN -> stringPath.stringValue().in(inSplitValues(value)).not();
      default -> stringPath.stringValue().eq(cleanString(value));
    };
  }

  private Predicate buildNumberPredicate(PathBuilder<?> numberPath, Class<?> clazz,
      String fieldPath, ConditionType condition, String value) {
    Predicate predicate;

    if (integerClasses.contains(clazz)) {
      predicate = buildIntegerPredicate(numberPath.getNumber(fieldPath, Integer.class), condition,
          value);
    } else if (longClasses.contains(clazz)) {
      predicate = buildLongPredicate(numberPath.getNumber(fieldPath, Long.class), condition,
          value);
    } else if (doubleClasses.contains(clazz)) {
      predicate = buildDoublePredicate(numberPath.getNumber(fieldPath, Double.class), condition,
          value);
    } else if (floatClasses.contains(clazz)) {
      predicate = buildFloatPredicate(numberPath.getNumber(fieldPath, Float.class), condition,
          value);
    } else if (shortClasses.contains(clazz)) {
      predicate = buildShortPredicate(numberPath.getNumber(fieldPath, Short.class), condition,
          value);
    } else if (bigDecimalClasses.contains(clazz)) {
      predicate = buildBigDecimalPredicate(numberPath.getNumber(fieldPath, BigDecimal.class),
          condition,
          value);
    } else if (bigIntegerClasses.contains(clazz)) {
      predicate = buildBigIntegerPredicate(numberPath.getNumber(fieldPath, BigInteger.class),
          condition,
          value);
    } else {
      throw new QueryFilterException("Unsupported number type: " + numberPath.getType());
    }

    return predicate;
  }

  private Predicate buildIntegerPredicate(
      NumberPath<Integer> numberPath,
      ConditionType condition,
      String value) {
    return switch (condition) {
      case EQ -> numberPath.eq(Integer.valueOf(value));
      case NEQ -> numberPath.eq(Integer.valueOf(value)).not();
      case IN -> numberInPredicate(numberPath,
          inSplitValues(value).stream().map(Integer::valueOf).toList());
      case NIN -> numberInPredicate(numberPath,
          inSplitValues(value).stream().map(Integer::valueOf).toList()).not();
      case BETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(Integer.valueOf(range.get(0)), Integer.valueOf(range.get(1)));
      }
      case NBETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(Integer.valueOf(range.get(0)), Integer.valueOf(range.get(1)))
            .not();
      }
      default -> buildCommonNumberPredicate(numberPath, condition, Integer.valueOf(value), value);
    };
  }

  private Predicate buildLongPredicate(NumberPath<Long> numberPath, ConditionType condition,
      String value) {
    return switch (condition) {
      case EQ -> numberPath.eq(Long.valueOf(value));
      case NEQ -> numberPath.eq(Long.valueOf(value)).not();
      case IN -> numberInPredicate(numberPath,
          (inSplitValues(value).stream().map(Long::valueOf).toList()));
      case NIN -> numberInPredicate(numberPath,
          (inSplitValues(value).stream().map(Long::valueOf).toList())).not();
      case BETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(Long.valueOf(range.get(0)), Long.valueOf(range.get(1)));
      }
      case NBETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(Long.valueOf(range.get(0)), Long.valueOf(range.get(1))).not();
      }
      default -> buildCommonNumberPredicate(numberPath, condition, Long.valueOf(value), value);
    };
  }

  private Predicate buildDoublePredicate(NumberPath<Double> numberPath,
      ConditionType condition,
      String value) {
    return switch (condition) {
      case EQ -> numberPath.eq(Double.valueOf(value));
      case NEQ -> numberPath.eq(Double.valueOf(value)).not();
      case IN -> numberInPredicate(numberPath,
          (inSplitValues(value).stream().map(Double::valueOf).toList()));
      case NIN -> numberInPredicate(numberPath,
          (inSplitValues(value).stream().map(Double::valueOf).toList())).not();
      case BETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(Double.valueOf(range.get(0)), Double.valueOf(range.get(1)));
      }
      case NBETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(Double.valueOf(range.get(0)), Double.valueOf(range.get(1)))
            .not();
      }
      default -> buildCommonNumberPredicate(numberPath, condition, Double.valueOf(value), value);
    };
  }

  private Predicate buildFloatPredicate(NumberPath<Float> numberPath,
      ConditionType condition,
      String value) {
    return switch (condition) {
      case EQ -> numberPath.eq(Float.valueOf(value));
      case NEQ -> numberPath.eq(Float.valueOf(value)).not();
      case IN -> numberInPredicate(numberPath,
          (inSplitValues(value).stream().map(Float::valueOf).toList()));
      case NIN -> numberInPredicate(numberPath,
          (inSplitValues(value).stream().map(Float::valueOf).toList())).not();
      case BETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(Float.valueOf(range.get(0)), Float.valueOf(range.get(1)));
      }
      case NBETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(Float.valueOf(range.get(0)), Float.valueOf(range.get(1))).not();
      }
      default -> buildCommonNumberPredicate(numberPath, condition, Float.valueOf(value), value);
    };
  }

  private Predicate buildShortPredicate(NumberPath<Short> numberPath,
      ConditionType condition,
      String value) {
    return switch (condition) {
      case EQ -> numberPath.eq(Short.valueOf(value));
      case NEQ -> numberPath.eq(Short.valueOf(value)).not();
      case IN -> numberInPredicate(numberPath,
          (inSplitValues(value).stream().map(Short::valueOf).toList()));
      case NIN -> numberInPredicate(numberPath,
          (inSplitValues(value).stream().map(Short::valueOf).toList())).not();
      case BETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(Short.valueOf(range.get(0)), Short.valueOf(range.get(1)));
      }
      case NBETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(Short.valueOf(range.get(0)), Short.valueOf(range.get(1))).not();
      }
      default -> buildCommonNumberPredicate(numberPath, condition, Short.valueOf(value), value);
    };
  }

  private Predicate buildBigDecimalPredicate(NumberPath<BigDecimal> numberPath,
      ConditionType condition,
      String value) {
    return switch (condition) {
      case EQ -> numberPath.eq(new BigDecimal(value));
      case NEQ -> numberPath.eq(new BigDecimal(value)).not();
      case IN -> numberInPredicate(numberPath,
          (inSplitValues(value).stream().map(BigDecimal::new).toList()));
      case NIN -> numberInPredicate(numberPath,
          (inSplitValues(value).stream().map(BigDecimal::new).toList())).not();
      case BETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(new BigDecimal(range.get(0)), new BigDecimal(range.get(1)));
      }
      case NBETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(new BigDecimal(range.get(0)), new BigDecimal(range.get(1)))
            .not();
      }
      default -> buildCommonNumberPredicate(numberPath, condition, new BigDecimal(value), value);
    };
  }

  private Predicate buildBigIntegerPredicate(NumberPath<BigInteger> numberPath,
      ConditionType condition,
      String value) {
    return switch (condition) {
      case EQ -> numberPath.eq(new BigInteger(value));
      case NEQ -> numberPath.eq(new BigInteger(value)).not();
      case IN -> numberInPredicate(numberPath,
          (inSplitValues(value).stream().map(BigInteger::new).toList()));
      case NIN -> numberInPredicate(numberPath,
          (inSplitValues(value).stream().map(BigInteger::new).toList())).not();
      case BETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(new BigInteger(range.get(0)), new BigInteger(range.get(1)));
      }
      case NBETWEEN -> {
        List<String> range = betweenValues(value);
        yield numberPath.between(new BigInteger(range.get(0)), new BigInteger(range.get(1)))
            .not();
      }
      default -> buildCommonNumberPredicate(numberPath, condition, new BigInteger(value), value);
    };
  }

  private <T extends Number & Comparable<?>> Predicate buildCommonNumberPredicate(
      NumberPath<T> numberPath,
      ConditionType condition,
      T value, String stringValue) {
    return switch (condition) {
      case LIKE -> numberPath.like(Expressions.stringTemplate(VARCHAR_CAST, stringValue));
      case NLIKE -> numberPath.like(Expressions.stringTemplate(VARCHAR_CAST, stringValue)).not();
      case GT -> numberPath.gt(value);
      case GOE -> numberPath.goe(value);
      case LT -> numberPath.lt(value);
      default -> numberPath.loe(value);
    };
  }

  /**
   * With the current QueryDsl implementation, the IN condition is not working for some Number types
   * such as BigInteger or BigDecimal, giving the exception - java.lang.IllegalStateException:
   * Binding is multi-valued; illegal call to #getBindValue Therefore, we create an or condition for
   * the numerberPath equals to each one of the list values.
   *
   * @param numberPath generic numberPath
   * @param list       of values for the in condition
   * @param <T>        the generic Number type
   * @return an OR condition with the numberPath equals to each of the in values
   */
  private <T extends Number & Comparable<?>> Predicate
  numberInPredicate(NumberPath<T> numberPath,
      List<T> list) {
    var booleanBuilder = new BooleanBuilder();

    for (T date : list) {
      booleanBuilder.or(numberPath.eq(date));
    }

    return booleanBuilder;
  }

  private Predicate localDateTimeExpression(PathBuilder<?> path, String fieldPath,
      ConditionType conditionType,
      String value) {
    if (isNotBetweenOrIn(conditionType)) {
      if (datePattern.matcher(value).matches()) { // "yyyy-MM-dd"
        var date = LocalDate.parse(value, dateFormatter);

        return localDateTimeExpression(path, fieldPath, conditionType,
            List.of(date.atStartOfDay(),
                date.atTime(LocalTime.MAX))); // equal case

      } else if (dateTimePattern.matcher(value).matches()) { // "yyyy-MM-dd HH:mm:ss"
        var dateTime = LocalDateTime.parse(value, dateTimeFormatter);

        return localDateTimeExpression(path, fieldPath, conditionType,
            List.of(dateTime,
                dateTime.plusSeconds(1))); // equal case

      } else {
        throw new IllegalArgumentException("Invalid date format: " + value);
      }
    } else {
      List<String> dates = inSplitValues(value);

      if (dates.stream()
          .allMatch(d -> dateTimePattern.matcher(d).matches())) { // "yyyy-MM-dd HH:mm:ss"
        if (isBetween(conditionType)) {
          dates = betweenValues(value);

          var start = LocalDateTime.parse(dates.get(0), dateTimeFormatter);
          var end = LocalDateTime.parse(dates.get(1), dateTimeFormatter);

          return localDateTimeExpression(path, fieldPath, conditionType,
              List.of(start, end));
        } else {

          var booleanBuilder = new BooleanBuilder();

          dates.forEach(date -> {

            var dateTime = LocalDateTime.parse(date, dateTimeFormatter);

            booleanBuilder.or(localDateTimeExpression(path, fieldPath, ConditionType.EQ,
                List.of(dateTime,
                    dateTime.plusSeconds(1))));
          });

          return conditionType == ConditionType.IN ? booleanBuilder : booleanBuilder.not();
        }
      } else if (dates.stream()
          .allMatch(d -> datePattern.matcher(d).matches())) {
        if (isBetween(conditionType)) {
          dates = betweenValues(value);

          var start = LocalDate.parse(dates.get(0), dateFormatter);
          var end = LocalDate.parse(dates.get(1), dateFormatter);

          return localDateTimeExpression(path, fieldPath, conditionType,
              List.of(start.atStartOfDay(), end.atTime(LocalTime.MAX)));

        } else {
          var booleanBuilder = new BooleanBuilder();

          dates.forEach(date -> {

            var dateTime = LocalDate.parse(date, dateFormatter);

            booleanBuilder.or(localDateTimeExpression(path, fieldPath, ConditionType.EQ,
                List.of(dateTime.atStartOfDay(),
                    dateTime.plusDays(1).atStartOfDay())));
          });

          return conditionType == ConditionType.IN ? booleanBuilder : booleanBuilder.not();
        }
      } else {
        throw new IllegalArgumentException("Invalid range date format: " + value);
      }
    }
  }

  private BooleanExpression localDateTimeExpression(PathBuilder<?> path,
      String fieldPath,
      ConditionType conditionType, List<LocalDateTime> dates) {
    DateExpression<LocalDateTime> dateExpression = dateTemplate(LocalDateTime.class, "{0}",
        path.get(fieldPath));

    return switch (conditionType) {
      case EQ -> dateExpression.goe(dates.get(0)).and(dateExpression.lt(dates.get(1)));
      case NEQ -> dateExpression.goe(dates.get(0)).and(dateExpression.lt(dates.get(1))).not();
      case GT -> dateExpression.gt(dates.get(0));
      case GOE -> dateExpression.goe(dates.get(0));
      case LT -> dateExpression.lt(dates.get(0));
      case LOE -> dateExpression.loe(dates.get(0));
      case NBETWEEN -> dateExpression.between(dates.get(0), dates.get(1)).not();
      default -> dateExpression.between(dates.get(0), dates.get(1));
    };
  }

  private Predicate localDateExpression(PathBuilder<?> path, String fieldPath,
      ConditionType conditionType,
      String value) {
    if (isNotBetweenOrIn(conditionType)) {

      if (datePattern.matcher(value).matches()) { // "yyyy-MM-dd"
        var date = LocalDate.parse(value, dateFormatter);

        return localDateExpression(path, fieldPath, conditionType,
            List.of(date,
                date.plusDays(1)));

      } else {
        throw new IllegalArgumentException("Invalid date format: " + value);
      }
    } else {
      List<String> dates = inSplitValues(value);

      if (dates.stream().allMatch(d -> datePattern.matcher(d).matches())) {

        if (isBetween(conditionType)) { // "yyyy-MM-dd"
          dates = betweenValues(value);

          return localDateExpression(path, fieldPath, conditionType,
              List.of(LocalDate.parse(dates.get(0), dateFormatter),
                  LocalDate.parse(dates.get(1), dateFormatter)));
        } else {

          var booleanBuilder = new BooleanBuilder();

          dates.forEach(date -> {

            var dateTime = LocalDate.parse(date, dateFormatter);

            booleanBuilder.or(localDateExpression(path, fieldPath, ConditionType.EQ,
                List.of(dateTime,
                    dateTime.plusDays(1))));
          });

          return conditionType == ConditionType.IN ? booleanBuilder : booleanBuilder.not();
        }
      } else {
        throw new IllegalArgumentException("Invalid range date format: " + value);
      }
    }
  }

  private BooleanExpression localDateExpression(PathBuilder<?> path, String fieldPath,
      ConditionType conditionType,
      List<LocalDate> dates) {

    DateExpression<LocalDate> dateExpression = dateTemplate(LocalDate.class, "{0}",
        path.get(fieldPath));

    return switch (conditionType) {
      case EQ -> dateExpression.goe(dates.get(0)).and(dateExpression.lt(dates.get(1)));
      case NEQ -> dateExpression.goe(dates.get(0)).and(dateExpression.lt(dates.get(1))).not();
      case GT -> dateExpression.gt(dates.get(0));
      case GOE -> dateExpression.goe(dates.get(0));
      case LT -> dateExpression.lt(dates.get(0));
      case LOE -> dateExpression.loe(dates.get(0));
      case NBETWEEN -> dateExpression.between(dates.get(0), dates.get(1)).not();
      default -> dateExpression.between(dates.get(0), dates.get(1));
    };
  }

  private Predicate dateExpression(PathBuilder<?> path, String fieldPath,
      ConditionType conditionType,
      String value) {
    if (isNotBetweenOrIn(conditionType)) {

      if (datePattern.matcher(value).matches()) { // "yyyy-MM-dd"
        var localDate = LocalDate.parse(value, dateFormatter);

        return dateExpression(path, fieldPath, conditionType,
            List.of(java.sql.Date.valueOf(localDate),
                java.sql.Date.valueOf(localDate.plusDays(1))));

      } else if (dateTimePattern.matcher(value).matches()) { // "yyyy-MM-dd HH:mm:ss"
        var dateTime = LocalDateTime.parse(value, dateTimeFormatter);

        return dateExpression(path, fieldPath, conditionType,
            List.of(java.sql.Timestamp.valueOf(dateTime),
                java.sql.Timestamp.valueOf(dateTime.plusSeconds(1))));

      } else {
        throw new IllegalArgumentException("Invalid date format: " + value);
      }
    } else {
      List<String> dates = inSplitValues(value);

      if (dates.stream().allMatch(d -> datePattern.matcher(d).matches())) { // "yyyy-MM-dd"
        if (isBetween(conditionType)) {
          dates = betweenValues(value);

          var start = LocalDate.parse(dates.get(0), dateFormatter);
          var end = LocalDate.parse(dates.get(1), dateFormatter);

          return dateExpression(path, fieldPath, conditionType,
              List.of(java.sql.Date.valueOf(start),
                  java.sql.Date.valueOf(end)));
        } else {

          var booleanBuilder = new BooleanBuilder();

          dates.forEach(date -> {

            var dateTime = LocalDate.parse(date, dateFormatter);

            booleanBuilder.or(dateExpression(path, fieldPath, ConditionType.EQ,
                List.of(java.sql.Date.valueOf(dateTime),
                    java.sql.Date.valueOf(dateTime.plusDays(1)))));
          });

          return conditionType == ConditionType.IN ? booleanBuilder : booleanBuilder.not();
        }
      } else if (dates.stream()
          .allMatch(d -> dateTimePattern.matcher(d).matches())) { // "yyyy-MM-dd HH:mm:ss"
        if (isBetween(conditionType)) {
          dates = betweenValues(value);

          var start = LocalDateTime.parse(dates.get(0), dateTimeFormatter);
          var end = LocalDateTime.parse(dates.get(1), dateTimeFormatter);

          return dateExpression(path, fieldPath, conditionType,
              List.of(java.sql.Timestamp.valueOf(start),
                  java.sql.Timestamp.valueOf(end)));
        } else {

          var booleanBuilder = new BooleanBuilder();

          dates.forEach(date -> {

            var dateTime = LocalDateTime.parse(date, dateTimeFormatter);

            booleanBuilder.or(dateExpression(path, fieldPath, ConditionType.EQ,
                List.of(java.sql.Timestamp.valueOf(dateTime),
                    java.sql.Timestamp.valueOf(dateTime.plusSeconds(1)))));
          });

          return conditionType == ConditionType.IN ? booleanBuilder : booleanBuilder.not();
        }
      } else {
        throw new IllegalArgumentException("Invalid range date format: " + value);
      }
    }
  }

  private boolean isNotBetweenOrIn(ConditionType conditionType) {
    return !isBetween(conditionType) && conditionType != ConditionType.IN
        && conditionType != ConditionType.NIN;
  }

  private boolean isBetween(ConditionType conditionType) {
    return conditionType == ConditionType.BETWEEN || conditionType == ConditionType.NBETWEEN;
  }

  private BooleanExpression dateExpression(PathBuilder<?> path, String fieldPath,
      ConditionType conditionType,
      List<Date> dates) {

    DateExpression<Date> dateExpression = dateTemplate(Date.class, "{0}", path.get(fieldPath));

    return switch (conditionType) {
      case EQ -> dateExpression.goe(dates.get(0)).and(dateExpression.lt(dates.get(1)));
      case NEQ -> dateExpression.goe(dates.get(0)).and(dateExpression.lt(dates.get(1))).not();
      case GT -> dateExpression.gt(dates.get(0));
      case GOE -> dateExpression.goe(dates.get(0));
      case LT -> dateExpression.lt(dates.get(0));
      case LOE -> dateExpression.loe(dates.get(0));
      case NBETWEEN -> dateExpression.between(dates.get(0), dates.get(1)).not();
      default -> dateExpression.between(dates.get(0), dates.get(1));
    };
  }

  private List<String> betweenValues(String value) {
    List<String> values = inSplitValues(value);
    if (values.size() != 2) {
      throw new IllegalArgumentException(
          "BETWEEN condition requires two values separated by a comma.");
    }
    return values;
  }

  private List<String> inSplitValues(String input) {
    LinkedList<String> result = new LinkedList<>();

    String[] parts = inSplitPattern.split(input);

    for (String part : parts) {
      result.add(part.replace("/,", ","));
    }

    return result;
  }

  /**
   * @return A clean string in case the user is forced to escape conditional operator. We also
   * replace the * with % for the like conditions
   */
  private String cleanString(String input) {
    var pattern = Pattern.compile("/\\*|\\*");
    var matcher = pattern.matcher(input);
    var result = new StringBuilder();

    while (matcher.find()) {
      String replacement;
      if ("/*".equals(matcher.group())) {
        replacement = "*"; // Literal asterisk
      } else {
        replacement = "%"; // Wildcard asterisk
      }
      matcher.appendReplacement(result, replacement);
    }
    matcher.appendTail(result);

    return result.toString()
        .replaceAll(OPERATOR_REGEX + ESCAPE + OPERATOR_SPLITTER, "$1" + OPERATOR_SPLITTER)
        .trim();
  }

  enum ConditionType {
    EQ("eq"), NEQ("!eq"), LIKE("like"), NLIKE("!like"), GT("gt"), GOE("!lt"), LT("lt"), LOE(
        "!gt"), BETWEEN("between"), NBETWEEN("!between"), IN("in"), NIN("!in");

    private final String operator;

    ConditionType(String operator) {
      this.operator = operator;
    }

    public static ConditionType from(String input) {
      return Arrays.stream(ConditionType.values()).filter(t -> t.operator.equals(input))
          .findFirst()
          .orElse(ConditionType.EQ);
    }
  }

  /**
   * Resolve the class type of fields so we can set the expression accordingly. The field path can
   * be composite, e.g. including joins or id, so we need to recursively split by //. to get the
   * final field type
   *
   * @param rootType  generic class of the path builder
   * @param fieldPath name of the field we want to get the class type
   * @return the actual class of the field from the field path
   */
  private Class<?> resolveFieldType(Class<?> rootType, String fieldPath) {

    if (rootType == null) {
      throw new IllegalArgumentException("Root type cannot be null.");
    }

    String[] pathParts = fieldPath.split("\\.");

    Class<?> currentType = rootType;

    for (String part : pathParts) {
      Field field = null;

      while (currentType != null) {
        try {
          field = currentType.getDeclaredField(part);
          break;
        } catch (NoSuchFieldException e) {
          LOGGER.debug("Field {} not found in class {} {}", part, currentType.getSimpleName(),
              e.getMessage());
          currentType = currentType.getSuperclass();
        }
      }

      if (field == null) {
        throw new QueryFilterException(
            String.format("Field %s not found in %s", part, rootType.getName()));
      }

      field.setAccessible(true); // NOSONAR
      currentType = field.getType();
    }

    return currentType;
  }

  default <T> Predicate defaultPredicate(JPAQuery<?> query, EntityPathBase<T> rootEntity) {
    return null;
  }

  String entityFrom(String expand);

  /**
   * If we use only the expand parameter in a request, we might need to use multiple joins in an n-m
   * relation
   *
   * @param expand string from the request
   * @return a list of tables to join. We need to implement this method in the service for multiple
   * joins if we need to, otherwise fall back to standard relation
   */
  default List<String> expandsFrom(String expand) {
    return List.of(expand);
  }

  Class<?> doFrom(String relation);

  default List<String> defaultJoins() {
    return List.of();
  }

  /**
   * Alias for a table in order to reuse the same table multiple times
   *
   * @param entityToJoin table we want the alias for
   * @return alias if present
   */
  default String alias(String entityToJoin) {
    return null;
  }

  /**
   * In n-m relations we use both ends as joins. Therefore, when we filter on the root table side,
   * we skip adding the join condition
   *
   * @param entityToJoin we want to check if it is root or not
   * @return true if we are on the root entity
   */
  default boolean isRoot(String entityToJoin) {
    return false;
  }
}