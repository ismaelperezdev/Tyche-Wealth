package com.tychewealth.mapper;

import static com.tychewealth.testdata.ClientTestData.COMPANY_ZONE_ID;
import static com.tychewealth.testdata.ClientTestData.HELMETS;
import static com.tychewealth.testdata.ClientTestData.LICENCE_PLATE;
import static com.tychewealth.testdata.ClientTestData.RANGE;
import static com.tychewealth.testdata.ClientTestData.REAL_TIME_DATA;
import static com.tychewealth.testdata.ClientTestData.RESOURCE_TYPE;
import static com.tychewealth.testdata.ClientTestData.VEHICLE_ID;
import static com.tychewealth.testdata.ClientTestData.VEHICLE_NAME;
import static com.tychewealth.testdata.ClientTestData.VEHICLE_X;
import static com.tychewealth.testdata.ClientTestData.VEHICLE_Y;
import static com.tychewealth.testdata.ClientTestData.defaultExternalVehicleDto;
import static org.assertj.core.api.Assertions.assertThat;

import com.tychewealth.dto.ExternalVehicleDto;
import com.tychewealth.model.Vehicle;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class VehicleMapperTest {

  private final VehicleMapper vehicleMapper = Mappers.getMapper(VehicleMapper.class);

  @Test
  void shouldMapExternalVehicleToDomain() {
    ExternalVehicleDto externalVehicleDto = defaultExternalVehicleDto();

    Vehicle vehicle = vehicleMapper.toDomain(externalVehicleDto);

    assertThat(vehicle.id()).isEqualTo(VEHICLE_ID);
    assertThat(vehicle.name()).isEqualTo(VEHICLE_NAME);
    assertThat(vehicle.type()).isEqualTo(RESOURCE_TYPE);
    assertThat(vehicle.location().latitude()).isEqualTo(VEHICLE_Y);
    assertThat(vehicle.location().longitude()).isEqualTo(VEHICLE_X);
    assertThat(vehicle.licencePlate()).isEqualTo(LICENCE_PLATE);
    assertThat(vehicle.range()).isEqualTo(RANGE);
    assertThat(vehicle.helmets()).isEqualTo(HELMETS);
    assertThat(vehicle.realTimeData()).isEqualTo(REAL_TIME_DATA);
    assertThat(vehicle.companyZoneId()).isEqualTo(COMPANY_ZONE_ID);
  }

  @Test
  void shouldMapExternalVehicleListToDomainList() {
    ExternalVehicleDto externalVehicleDto = defaultExternalVehicleDto();

    List<Vehicle> vehicles = vehicleMapper.toDomainList(List.of(externalVehicleDto));

    assertThat(vehicles).hasSize(1);
    assertThat(vehicles.getFirst().id()).isEqualTo(VEHICLE_ID);
  }

  @Test
  void shouldReturnNullWhenExternalVehicleIsNull() {
    assertThat(vehicleMapper.toDomain(null)).isNull();
  }

  @Test
  void shouldReturnNullWhenExternalVehicleListIsNull() {
    assertThat(vehicleMapper.toDomainList(null)).isNull();
  }
}
