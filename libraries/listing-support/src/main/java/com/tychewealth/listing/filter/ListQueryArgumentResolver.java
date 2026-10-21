package com.tychewealth.listing.filter;

import com.tychewealth.listing.Listable;
import com.tychewealth.listing.enums.ErrorDefinition;
import com.tychewealth.listing.exception.FilterException;
import com.tychewealth.listing.properties.FilterProperties;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Adapts a {@link Listable} handler's HTTP parameters to a {@link ListQuery} method argument.
 *
 * <p>The legacy {@code page} parameter is converted to an offset when no offset was supplied.
 * Common response wrappers are unwrapped so the parser can inspect the endpoint's DTO metadata.
 */
@Component
public class ListQueryArgumentResolver implements HandlerMethodArgumentResolver {

  private static final String PAGE_PARAM = "page";
  private static final String OFFSET_PARAM = "offset";

  private final FilterParser filterParser;
  private final FilterProperties filterProperties;

  public ListQueryArgumentResolver(FilterParser filterParser, FilterProperties filterProperties) {
    this.filterParser = filterParser;
    this.filterProperties = filterProperties;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return ListQuery.class.equals(parameter.getParameterType())
        && parameter.getMethod() != null
        && AnnotatedElementUtils.hasAnnotation(parameter.getMethod(), Listable.class);
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    Map<String, String> requestParameters = getRequestParameters(webRequest);
    String requestedPage = requestParameters.remove(PAGE_PARAM);
    boolean pageBased = requestedPage != null && !requestParameters.containsKey(OFFSET_PARAM);

    if (pageBased) {
      requestParameters.put(OFFSET_PARAM, "0");
    }

    ListQuery parsed =
        filterParser.parse(
            requestParameters,
            pageBased ? null : webRequest.getHeader("Range"),
            resolveResponseDtoType(parameter));

    if (!pageBased) {
      return parsed;
    }

    int page = parsePage(requestedPage);
    long requestedOffset = (long) page * parsed.pageCriteria().limit();
    int offset = (int) Math.min(requestedOffset, filterProperties.getMaxOffset());
    return new ListQuery(
        parsed.filter(),
        new com.tychewealth.listing.domain.PageCriteria(
            offset, parsed.pageCriteria().limit(), parsed.pageCriteria().sorts()),
        parsed.range());
  }

  private Map<String, String> getRequestParameters(NativeWebRequest webRequest) {
    Map<String, String> parameters = new HashMap<>();
    webRequest
        .getParameterMap()
        .forEach((name, values) -> parameters.put(name, values.length == 0 ? "" : values[0]));
    return parameters;
  }

  private Class<?> resolveResponseDtoType(MethodParameter parameter) {
    Method method = parameter.getMethod();
    if (method == null) {
      throw new IllegalStateException(
          "Cannot determine the response DTO type without a handler method");
    }

    ResolvableType responseType = ResolvableType.forMethodReturnType(method);
    Class<?> rawType = responseType.resolve();

    while (rawType != null && responseType.hasGenerics()) {
      if (ResponseEntity.class.isAssignableFrom(rawType)
          || Page.class.isAssignableFrom(rawType)
          || Collection.class.isAssignableFrom(rawType)) {
        responseType = responseType.getGeneric(0);
      } else {
        break;
      }
      rawType = responseType.resolve();
    }

    if (rawType != null && rawType.isArray()) {
      rawType = rawType.getComponentType();
    }
    if (rawType == null || rawType == Object.class) {
      throw new IllegalStateException(
          "Cannot determine the response DTO type for @Listable endpoint "
              + method.toGenericString());
    }
    return rawType;
  }

  private int parsePage(String rawPage) {
    try {
      return Math.max(0, Integer.parseInt(rawPage.trim()));
    } catch (NumberFormatException exception) {
      throw new FilterException(
          ErrorDefinition.FILTER_PARSE_ERROR,
          new Object[] {rawPage, "Integer"},
          HttpStatus.BAD_REQUEST);
    }
  }
}
