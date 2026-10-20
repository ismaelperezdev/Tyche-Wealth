package com.tychewealth.listing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tychewealth.listing.shared.FilterFieldMapping;
import org.junit.jupiter.api.Test;

class ListableTest {

  @Test
  void declaresTheEndpointFieldMappingAtRuntime() throws Exception {
    Listable annotation = Controller.class.getDeclaredMethod("list").getAnnotation(Listable.class);

    assertEquals(ExampleMapping.class, annotation.mapping());
  }

  private static class Controller {
    @Listable(mapping = ExampleMapping.class)
    void list() {
      // This fixture exists only to expose annotation metadata for reflection.
    }
  }

  private static class ExampleMapping extends FilterFieldMapping {}
}
