package com.tychewealth.mapper;

import com.tychewealth.dto.ExternalVehicleDto;
import com.tychewealth.model.Vehicle;
import com.tychewealth.model.VehicleLocation;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

  @Mapping(target = "type", source = "resourceType")
  @Mapping(target = "location", source = ".", qualifiedByName = "toLocation")
  Vehicle toDomain(ExternalVehicleDto externalVehicleDto);

  List<Vehicle> toDomainList(List<ExternalVehicleDto> externalVehicles);

  @Named("toLocation")
  default VehicleLocation toLocation(ExternalVehicleDto externalVehicleDto) {
    return new VehicleLocation(externalVehicleDto.y(), externalVehicleDto.x());
  }
}
