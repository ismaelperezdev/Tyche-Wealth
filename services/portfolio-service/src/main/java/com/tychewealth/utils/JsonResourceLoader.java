package com.tychewealth.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

public final class JsonResourceLoader {

  private JsonResourceLoader() {}

  public static List<String> readStringList(ObjectMapper objectMapper, String resourcePath) {
    return read(objectMapper, resourcePath, new TypeReference<>() {});
  }

  public static <E extends Enum<E>> Map<E, List<String>> readEnumKeyedStringListMap(
      ObjectMapper objectMapper, String resourcePath, Class<E> enumType) {
    Map<String, List<String>> rawMap = read(objectMapper, resourcePath, new TypeReference<>() {});
    return rawMap.entrySet().stream()
        .collect(
            java.util.stream.Collectors.toUnmodifiableMap(
                entry -> Enum.valueOf(enumType, entry.getKey()), Map.Entry::getValue));
  }

  private static <T> T read(
      ObjectMapper objectMapper, String resourcePath, TypeReference<T> typeReference) {
    try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
      return objectMapper.readValue(inputStream, typeReference);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to read JSON resource: " + resourcePath, ex);
    }
  }
}
