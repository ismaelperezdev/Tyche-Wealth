package com.tychewealth.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Shared MapStruct configuration for Spring-managed mappers.
 *
 * <p>Uses the Spring component model and ignores {@code null} source properties during update
 * mappings, allowing partial-update requests to preserve values that were not provided.
 */
@MapperConfig(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface GenericMapperConfig {}
