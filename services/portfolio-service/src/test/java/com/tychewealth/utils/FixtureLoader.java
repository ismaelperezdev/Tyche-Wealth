package com.tychewealth.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class FixtureLoader {

  private static final String CANNOT_READ_FIXTURE = "Cannot read fixture: ";
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  private FixtureLoader() {}

  public static <T> T read(String path, Class<T> type) {
    try (InputStream input = openFixture(path)) {
      return OBJECT_MAPPER.readValue(input, type);
    } catch (IOException exception) {
      throw new IllegalStateException(CANNOT_READ_FIXTURE + path, exception);
    }
  }

  public static String readString(String path) {
    try (InputStream input = openFixture(path)) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException(CANNOT_READ_FIXTURE + path, exception);
    }
  }

  private static InputStream openFixture(String path) {
    InputStream resource = FixtureLoader.class.getResourceAsStream(path);
    if (resource == null) {
      throw new IllegalStateException("Fixture not found: " + path);
    }
    return resource;
  }
}
