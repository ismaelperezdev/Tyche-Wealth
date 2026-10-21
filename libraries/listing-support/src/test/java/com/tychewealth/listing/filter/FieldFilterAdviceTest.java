package com.tychewealth.listing.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class FieldFilterAdviceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final FieldFilterAdvice advice = new FieldFilterAdvice(objectMapper);

  @Test
  void filtersAnnotatedResponseToRequestedTopLevelFields() throws Exception {
    JsonNode filtered =
        (JsonNode)
            advise(
                new FilteredResponse("Ada", 37),
                "fields=name",
                Controller.class.getDeclaredMethod("response"));

    String json = filtered.toString();

    assertTrue(json.contains("\"name\":\"Ada\""), json);
    assertFalse(json.contains("age"));
  }

  @Test
  void trimsRequestedFieldsAndSupportsCollections() throws Exception {
    JsonNode filtered =
        (JsonNode)
            advise(
                List.of(new FilteredResponse("Ada", 37)),
                "fields=name%2Cage",
                Controller.class.getDeclaredMethod("responses"));

    String json = filtered.toString();

    assertTrue(json.contains("\"name\":\"Ada\""), json);
    assertTrue(json.contains("\"age\":37"), json);
  }

  @Test
  void projectsNestedFieldsAndKeepsAnExplicitlySelectedObjectWhole() throws Exception {
    JsonNode nested =
        (JsonNode)
            advise(
                new NestedResponse("record-1", new Address("London", "UK")),
                "fields=address.city",
                Controller.class.getDeclaredMethod("nestedResponse"));
    JsonNode wholeObject =
        (JsonNode)
            advise(
                new NestedResponse("record-1", new Address("London", "UK")),
                "fields=address",
                Controller.class.getDeclaredMethod("nestedResponse"));

    assertEquals("{\"address\":{\"city\":\"London\"}}", nested.toString());
    assertEquals("{\"address\":{\"city\":\"London\",\"country\":\"UK\"}}", wholeObject.toString());
  }

  @Test
  void projectsNestedFieldsInsideListElements() throws Exception {
    JsonNode filtered =
        (JsonNode)
            advise(
                List.of(new NestedResponse("record-1", new Address("London", "UK"))),
                "fields=address.city",
                Controller.class.getDeclaredMethod("nestedResponses"));

    assertEquals("[{\"address\":{\"city\":\"London\"}}]", filtered.toString());
  }

  @Test
  void leavesUnannotatedResponsesAndRequestsWithoutFieldsUnchanged() throws Exception {
    UnfilteredResponse body = new UnfilteredResponse("visible");

    assertSame(
        body,
        advise(body, "fields=name", Controller.class.getDeclaredMethod("unfilteredResponse")));
    assertSame(body, advise(body, null, Controller.class.getDeclaredMethod("response")));
  }

  private Object advise(Object body, String query, Method method) {
    MethodParameter returnType = new MethodParameter(method, -1);
    MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/api");
    if (query != null) {
      servletRequest.setQueryString(query);
    }
    return advice.beforeBodyWrite(
        body,
        returnType,
        MediaType.APPLICATION_JSON,
        MappingJackson2HttpMessageConverter.class,
        new ServletServerHttpRequest(servletRequest),
        new ServletServerHttpResponse(new MockHttpServletResponse()));
  }

  private static class Controller {
    FilteredResponse response() {
      return null;
    }

    List<FilteredResponse> responses() {
      return List.of();
    }

    UnfilteredResponse unfilteredResponse() {
      return null;
    }

    NestedResponse nestedResponse() {
      return null;
    }

    List<NestedResponse> nestedResponses() {
      return List.of();
    }
  }

  @FieldFiltered
  private record FilteredResponse(String name, int age) {}

  private record UnfilteredResponse(String name) {}

  @FieldFiltered
  private record NestedResponse(String id, Address address) {}

  @FieldFiltered
  private record Address(String city, String country) {}
}
