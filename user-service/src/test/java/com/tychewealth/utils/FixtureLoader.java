package com.tychewealth.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;

public final class FixtureLoader {

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  private FixtureLoader() {}

  public static <T> T read(String path, Class<T> type) {
    InputStream resource = FixtureLoader.class.getResourceAsStream(path);
    if (resource == null) {
      throw new IllegalStateException("Fixture not found: " + path);
    }

    try (InputStream input = resource) {
      return OBJECT_MAPPER.readValue(input, type);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read fixture: " + path, exception);
    }
  }
}
