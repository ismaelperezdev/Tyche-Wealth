package com.tychewealth.listing.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Applies the request's {@code fields} parameter to responses marked with {@link FieldFiltered}.
 *
 * <p>Selection supports comma-separated, dot-delimited paths through nested objects and arrays.
 * Responses without the marker, without a non-empty selection, or with a null body are unchanged.
 */
@ControllerAdvice
public class FieldFilterAdvice implements ResponseBodyAdvice<Object> {

  static final String FIELDS_PARAM = "fields";
  static final String FILTER_ID = FieldFiltered.FILTER_ID;

  private final ObjectMapper objectMapper;

  public FieldFilterAdvice(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean supports(
      final MethodParameter returnType,
      final Class<? extends HttpMessageConverter<?>> converterType) {
    return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
  }

  @Override
  public Object beforeBodyWrite(
      final Object body,
      final MethodParameter returnType,
      final MediaType selectedContentType,
      final Class<? extends HttpMessageConverter<?>> selectedConverterType,
      final ServerHttpRequest request,
      final ServerHttpResponse response) {
    if (body == null) {
      return null;
    }

    String requestedFields = getRequestedFields(request.getURI().getRawQuery());
    if (!StringUtils.hasText(requestedFields)) {
      return body;
    }

    Class<?> bodyType = resolveBodyType(body);
    if (bodyType == null || !AnnotatedElementUtils.hasAnnotation(bodyType, FieldFiltered.class)) {
      return body;
    }

    Set<String> fields =
        Arrays.stream(requestedFields.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .collect(Collectors.toUnmodifiableSet());
    if (fields.isEmpty()) {
      return body;
    }

    try {
      byte[] serialized =
          objectMapper
              .writer(
                  new SimpleFilterProvider()
                      .addFilter(FILTER_ID, SimpleBeanPropertyFilter.serializeAll())
                      .setFailOnUnknownId(false))
              .writeValueAsBytes(body);
      JsonNode responseTree = objectMapper.readTree(serialized);
      return NestedPropertyFilter.project(responseTree, fields);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to apply response field selection", exception);
    }
  }

  private Class<?> resolveBodyType(Object body) {
    if (body instanceof Collection<?> collection) {
      return collection.stream()
          .filter(Objects::nonNull)
          .findFirst()
          .map(Object::getClass)
          .orElse(null);
    }
    if (body.getClass().isArray()) {
      return body.getClass().getComponentType();
    }
    return body.getClass();
  }

  private String getRequestedFields(String rawQuery) {
    if (rawQuery == null) {
      return null;
    }
    return Arrays.stream(rawQuery.split("&"))
        .map(parameter -> parameter.split("=", 2))
        .filter(parameter -> parameter.length == 2 && FIELDS_PARAM.equals(parameter[0]))
        .map(parameter -> URLDecoder.decode(parameter[1], StandardCharsets.UTF_8))
        .findFirst()
        .orElse(null);
  }
}
