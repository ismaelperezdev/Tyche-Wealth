package com.tychewealth.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.InputStream;

public final class FixtureLoader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private FixtureLoader() {
    }

    public static <T> T read(String path, Class<T> type) {
        try (InputStream input = FixtureLoader.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Fixture not found: " + path);
            }
            return OBJECT_MAPPER.readValue(input, type);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read fixture: " + path, exception);
        }
    }
}
