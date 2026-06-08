package com.tychewealth.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class VehicleMapperTest {

  private final VehicleMapper vehicleMapper = Mappers.getMapper(VehicleMapper.class);

  @Test
  void shouldReturnNullWhenExternalVehicleIsNull() {
    assertThat(vehicleMapper.toDomain(null)).isNull();
  }

  @Test
  void shouldReturnNullWhenExternalVehicleListIsNull() {
    assertThat(vehicleMapper.toDomainList(null)).isNull();
  }
}
