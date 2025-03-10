package org.example.query.mvc;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.example.query.annnotation.FilterRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * A custom argument resolver for handling request parameters and mapping them to complex objects
 * that implement the {@code FilterRequest} interface. This resolver supports deeply nested fields,
 * inheritance, and allows flexible parameter binding based on request parameters with names
 * specified by {@code @JsonProperty} annotations.
 * <p>
 * This resolver is intended for use in Spring controllers annotated with {@code @RestController}.
 * It dynamically constructs and populates instances of classes implementing {@code FilterRequest},
 * making it reusable across different microservices for custom query filtering.
 *
 * <p><b>Usage:</b> Register the {@code FilterRequestArgumentResolver} in the Spring MVC
 * configuration by overriding {@code addArgumentResolvers} in a {@code WebMvcConfigurer}
 * implementation:
 *
 * <pre>
 * {@code
 * @Configuration
 * public class WebConfig implements WebMvcConfigurer {
 *     @Override
 *     public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
 *         resolvers.add(new FilterRequestArgumentResolver());
 *     }
 * }
 * }
 * </pre>
 *
 * <p><b>Example Controller:</b> With the resolver registered, it can be used in a controller as
 * follows:
 *
 * <pre>
 * {@code
 * @RestController
 * public class EventController {
 *     @GetMapping("/events")
 *     public ResponseEntity<Object> getEvents(EventFilter filter) {
 *         // The filter object will contain data from request parameters
 *         return ResponseEntity.ok("Event Filter Processed");
 *     }
 * }
 * }
 * </pre>
 */
@Component
public class FilterRequestArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return FilterRequest.class.isAssignableFrom(parameter.getParameterType())
        && parameter.getContainingClass().isAnnotationPresent(RestController.class);
  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

    Class<?> filterClass = parameter.getParameterType();
    Object filterRequest = filterClass.getDeclaredConstructor().newInstance();

    populateFields(filterRequest, webRequest, "");

    return filterRequest;
  }

  /**
   * Recursively populates fields of an object using request parameters. This includes handling
   * nested objects and inherited fields. Fields annotated with {@code @JsonProperty} are populated
   * based on matching parameter names, with nested properties using dot notation.
   *
   * @param filterRequest the object to populate
   * @param webRequest    the current web request
   * @param prefix        the prefix to use for nested parameters
   * @throws Exception if an error occurs during field population
   */
  protected void populateFields(Object filterRequest, NativeWebRequest webRequest,
      String prefix)
      throws Exception {
    Class<?> currentClass = filterRequest.getClass();

    while (currentClass != null && currentClass != Object.class) {
      for (Field field : currentClass.getDeclaredFields()) {
        field.setAccessible(true); // NOSONAR
        JsonProperty annotation = field.getAnnotation(JsonProperty.class);

        if (annotation != null) {
          setParamValue(filterRequest, webRequest, prefix, field, annotation);
        }
      }

      currentClass = currentClass.getSuperclass();
    }
  }

  private void setParamValue(Object filterRequest, NativeWebRequest webRequest, String prefix,
      Field field, JsonProperty annotation) throws Exception {
    String jsonName = annotation.value();
    String[] paramValue = webRequest.getParameterValues(prefix + jsonName);

    if (paramValue != null) {
      if (List.class.isAssignableFrom(field.getType())) {
        List<String> listValues = new ArrayList<>(Arrays.asList(paramValue));
        field.set(filterRequest, listValues); // NOSONAR
      } else if (Boolean.class.isAssignableFrom(field.getType())) {
        field.set(filterRequest, Boolean.valueOf(paramValue[0])); // NOSONAR
      }
    } else if (!List.class.isAssignableFrom(field.getType()) && !field.getType()
        .isAssignableFrom(Boolean.class) && !field.getType()
        .isAssignableFrom(String.class)) {
      Object nestedObject = field.getType().getDeclaredConstructor().newInstance();
      populateFields(nestedObject, webRequest, prefix + jsonName + ".");
      field.set(filterRequest, nestedObject); // NOSONAR
    }
  }
}

