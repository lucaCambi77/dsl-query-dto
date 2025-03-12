package org.example;

import org.example.annnotation.JOIN;
import org.example.annnotation.QueryField;
import org.example.query.Join;
import org.example.query.QueryFilter;
import org.example.query.QueryParams;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryParamsTest {

    @Test
    void constructor_shouldInitializeFieldsCorrectly() {
        QueryFilter filter1 = new QueryFilter("field1", List.of(new Join("entity1")), "value1");
        QueryFilter filter2 = new QueryFilter("field2", List.of(new Join("entity2")), "value2");
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<String> expandList = List.of("expand1", "expand2");

        QueryParams queryParams = new QueryParams(List.of(filter1, filter2), pageRequest, expandList);

        assertEquals(2, queryParams.queryFilter().size());
        assertEquals(pageRequest, queryParams.pageRequest());
        assertTrue(
                queryParams.expandList().containsAll(List.of("expand1", "expand2", "entity1", "entity2")));
    }

    @Test
    void from_shouldCreateQueryParamsCorrectly() {
        TestFilterRequest filterRequest = new TestFilterRequest();
        filterRequest.field1 = List.of("value1");
        filterRequest.nestedObject = new NestedObject();
        filterRequest.nestedObject.nestedField = List.of("nestedValue");

        PageRequest pageRequest = PageRequest.of(0, 10);
        List<String> expandList = List.of("expand1");

        QueryParams queryParams = QueryParams.from(filterRequest, pageRequest, expandList);

        assertEquals(2, queryParams.queryFilter().size());
        assertEquals(pageRequest, queryParams.pageRequest());
        assertTrue(queryParams.expandList().contains("expand1"));
    }

    @Test
    void getAllFields_shouldReturnAllFieldsIncludingSuperclasses() {
        List<Field> fields = QueryParams.getAllFields(TestFilterRequest.class);

        assertEquals(2, fields.size());
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("field1")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("nestedObject")));
    }

    @Test
    void toString_shouldReturnFormattedString() {
        QueryFilter filter = new QueryFilter("field1", List.of(new Join("entity1")), "value1");
        PageRequest pageRequest = PageRequest.of(0, 10);
        QueryParams queryParams = new QueryParams(List.of(filter), pageRequest, List.of("expand1"));

        String result = queryParams.toString();

        assertTrue(result.contains("queryFilter"));
        assertTrue(result.contains("pageRequest"));
        assertTrue(result.contains("expandList"));
    }

    @Test
    void constructor_shouldHandleBooleanFieldsCorrectly() {
        BooleanFieldRequest filterRequest = new BooleanFieldRequest();
        filterRequest.active = List.of("true");

        PageRequest pageRequest = PageRequest.of(0, 10);
        List<String> expandList = List.of("expand1");

        QueryParams queryParams = QueryParams.from(filterRequest, pageRequest, expandList);

        assertEquals(1, queryParams.queryFilter().size());
        QueryFilter queryFilter = queryParams.queryFilter().get(0);
        assertEquals("active", queryFilter.fieldName());
        assertEquals("true", queryFilter.value());
    }

    @Test
    void constructor_shouldHandleJoinsCorrectly() {
        JoinFieldRequest filterRequest = new JoinFieldRequest();
        filterRequest.userName = List.of("john");
        filterRequest.userAddress = List.of("123 Main St");

        PageRequest pageRequest = PageRequest.of(0, 10);
        List<String> expandList = List.of("expand1");

        QueryParams queryParams = QueryParams.from(filterRequest, pageRequest, expandList);

        assertEquals(2, queryParams.queryFilter().size());

        // Check the first filter has the correct join path
        QueryFilter userNameFilter = queryParams.queryFilter().get(0);
        assertEquals("userName", userNameFilter.fieldName());
        assertTrue(userNameFilter.joins().isEmpty());

        // Check the second filter has the correct join path
        QueryFilter userAddressFilter = queryParams.queryFilter().get(1);
        assertEquals("userAddress", userAddressFilter.fieldName());
        assertEquals(List.of(new Join("address")), userAddressFilter.joins());
    }

    @Test
    void constructor_shouldHandleQueryFieldWithNameCorrectly() {
        NamedFieldRequest filterRequest = new NamedFieldRequest();
        filterRequest.userName = List.of("john");

        PageRequest pageRequest = PageRequest.of(0, 10);
        List<String> expandList = List.of("expand1");

        QueryParams queryParams = QueryParams.from(filterRequest, pageRequest, expandList);

        assertEquals(1, queryParams.queryFilter().size());

        QueryFilter queryFilter = queryParams.queryFilter().get(0);
        assertEquals("user_name", queryFilter.fieldName()); // Since @QueryField(name = "user_name")
    }

    // Helper classes for testing
    static class TestFilterRequest {

        @QueryField(name = "field1")
        List<String> field1;

        @QueryField(isPrefix = true)
        NestedObject nestedObject;
    }

    static class NestedObject {

        @QueryField(name = "nestedField")
        List<String> nestedField;
    }

    static class BooleanFieldRequest {

        @QueryField
        List<String> active;
    }

    static class JoinFieldRequest {

        @QueryField(name = "userName")
        List<String> userName;

        @QueryField(name = "userAddress", joinPath = @JOIN(entityToJoin = "address"))
        List<String> userAddress;
    }

    static class NamedFieldRequest {

        @QueryField(name = "user_name")
        List<String> userName;
    }
}
