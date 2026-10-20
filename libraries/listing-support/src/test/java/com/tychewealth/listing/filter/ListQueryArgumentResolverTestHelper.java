package com.tychewealth.listing.filter;

import com.tychewealth.listing.Listable;
import com.tychewealth.listing.dto.Filterable;
import com.tychewealth.listing.properties.FilterProperties;
import com.tychewealth.listing.shared.FilterFieldMapping;
import java.lang.reflect.Method;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

/** Test fixture and invocation helpers for {@link ListQueryArgumentResolverTest}. */
final class ListQueryArgumentResolverTestHelper {

  private final FilterProperties properties = new FilterProperties();
  private final ListQueryArgumentResolver resolver =
      new ListQueryArgumentResolver(new FilterParser(properties), properties);

  boolean supportsListableEndpoint() {
    return resolver.supportsParameter(parameter("listable"));
  }

  boolean supportsNonListableEndpoint() {
    return resolver.supportsParameter(parameter("notListable"));
  }

  ListQuery resolve(MockHttpServletRequest request) {
    return (ListQuery)
        resolver.resolveArgument(parameter("listable"), null, new ServletWebRequest(request), null);
  }

  private MethodParameter parameter(String methodName) {
    try {
      Method method = Controller.class.getDeclaredMethod(methodName, ListQuery.class);
      return new MethodParameter(method, 0);
    } catch (NoSuchMethodException exception) {
      throw new IllegalStateException("Test endpoint method not found: " + methodName, exception);
    }
  }

  private static class Controller {
    @Listable(mapping = ExampleMapping.class)
    ResponseEntity<List<AssetResponse>> listable(ListQuery query) {
      return ResponseEntity.ok(responseFor(query));
    }

    ResponseEntity<List<AssetResponse>> notListable(ListQuery query) {
      return ResponseEntity.ok(responseFor(query));
    }

    private List<AssetResponse> responseFor(ListQuery query) {
      return List.of(new AssetResponse(query.filter().toString(), query.pageCriteria().toString()));
    }
  }

  @Filterable
  private record AssetResponse(String name, String symbol) {}

  private static class ExampleMapping extends FilterFieldMapping {}
}
