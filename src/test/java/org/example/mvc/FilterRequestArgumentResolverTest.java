package org.example.mvc;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.annnotation.FilterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilterRequestArgumentResolverTest {

    private FilterRequestArgumentResolver resolver;

    @Mock
    private NativeWebRequest webRequest;

    @Mock
    private MethodParameter methodParameter;

    @Mock
    private ModelAndViewContainer mavContainer;

    @Mock
    private WebDataBinderFactory binderFactory;

    @BeforeEach
    void setUp() {
        resolver = new FilterRequestArgumentResolver();
    }

    @Test
    void testSupportsParameter() {
        // Arrange
        MethodParameter methodParameter = mock(MethodParameter.class);
        when(methodParameter.getParameterType()).thenAnswer(invocation -> FilterRequest.class);
        when(methodParameter.getContainingClass()).thenAnswer(
                invocation -> FilterRequestController.class);

        // Act
        boolean result = resolver.supportsParameter(methodParameter);

        // Assert
        assertTrue(result);
    }

    @Test
    void testResolveArgument() throws Exception {
        // Arrange
        MethodParameter methodParameter = mock(MethodParameter.class);
        when(methodParameter.getParameterType()).thenAnswer(invocation -> EventFilter.class);

        when(webRequest.getParameterValues(anyString())).thenReturn(new String[]{"value1", "value2"});

        // Act
        Object resolvedArgument = resolver.resolveArgument(methodParameter, mavContainer, webRequest,
                binderFactory);

        // Assert
        assertNotNull(resolvedArgument);
        assertInstanceOf(EventFilter.class, resolvedArgument);
        EventFilter filter = (EventFilter) resolvedArgument;
        assertEquals(List.of("value1", "value2"), filter.getSomeField());
    }

    @Test
    void testResolveArgumentWithDotNotation() throws Exception {
        // Arrange
        MethodParameter methodParameter = mock(MethodParameter.class);
        when(methodParameter.getParameterType()).thenAnswer(invocation -> EventFilter.class);

        // Mocking request parameters with dot notation (e.g., field.nestedField)
        when(webRequest.getParameterValues("someField")).thenReturn(new String[]{"value1", "value2"});
        when(webRequest.getParameterValues("nestedFilter")).thenReturn(null);
        when(webRequest.getParameterValues("nestedFilter.nestedField")).thenReturn(
                new String[]{"nestedValue"});

        // Act
        Object resolvedArgument = resolver.resolveArgument(methodParameter, mavContainer, webRequest,
                binderFactory);

        // Assert
        assertNotNull(resolvedArgument);
        assertInstanceOf(EventFilter.class, resolvedArgument);
        EventFilter filter = (EventFilter) resolvedArgument;
        assertEquals(List.of("value1", "value2"), filter.getSomeField());
        assertNotNull(filter.getNestedFilter());
        assertEquals(List.of("nestedValue"), filter.getNestedFilter().getNestedField());
    }

    @Test
    void testPopulateFieldsWithNullParameter() throws Exception {
        // Arrange
        EventFilter filter = new EventFilter();
        when(webRequest.getParameterValues(anyString())).thenReturn(
                null); // Simulating missing parameter

        // Act
        resolver.populateFields(filter, webRequest, "");

        // Assert
        assertNull(filter.getSomeField()); // Ensure no default value is set if parameter is missing
    }

    // Example FilterRequest implementation for testing
    public static class EventFilter implements FilterRequest {

        @JsonProperty("someField")
        private List<String> someField;

        @JsonProperty("nestedFilter")
        private NestedFilter nestedFilter;

        public List<String> getSomeField() {
            return someField;
        }

        public NestedFilter getNestedFilter() {
            return nestedFilter;
        }
    }

    // Example nested object
    public static class NestedFilter {

        @JsonProperty("nestedField")
        private List<String> nestedField;

        public List<String> getNestedField() {
            return nestedField;
        }
    }

    // Dummy Controller for testing
    @RestController
    public static class FilterRequestController {

    }
}
