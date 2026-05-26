package com.tychewealth.mock;

import static com.tychewealth.testhelper.MockVehicleTestHelper.assertVehicleShape;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.ExternalVehicleDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockVehicleGeneratorTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final MockVehicleGenerator mockVehicleGenerator = new MockVehicleGenerator();

  @Test
  void shouldGenerateVehicleWithExpectedFormat() {
    ExternalVehicleDto vehicle = mockVehicleGenerator.newVehicle();

    assertVehicleShape(vehicle);
  }

  @Test
  void shouldSerializeVehiclesToJson() throws Exception {
    ExternalVehicleDto firstVehicle = mockVehicleGenerator.newVehicle();
    ExternalVehicleDto secondVehicle = mockVehicleGenerator.newVehicle();

    String serializedVehicles =
        mockVehicleGenerator.serializeVehicles(List.of(firstVehicle, secondVehicle));
    List<ExternalVehicleDto> deserializedVehicles =
        OBJECT_MAPPER.readValue(serializedVehicles, new TypeReference<>() {});

    assertThat(deserializedVehicles).containsExactly(firstVehicle, secondVehicle);
  }

  @Test
  void shouldSerializeEmptyVehicleListToEmptyJsonArray() {
    String serializedVehicles = mockVehicleGenerator.serializeVehicles(List.of());

    assertThat(serializedVehicles).isEqualTo("[]");
  }
}
