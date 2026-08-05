package com.tychewealth.mapper;

import org.mapstruct.MappingTarget;

/**
 * Defines the common MapStruct operations for converting API models and persistence entities.
 *
 * <p>Separates full object conversion from create and partial-update mappings so concrete mappers
 * can reuse the same contract for their DTO, entity, create-request, and update-request types.
 *
 * @param <D> response DTO type
 * @param <E> persistence entity type
 * @param <C> create-request type
 * @param <U> update-request type
 */
public interface GenericMapper<D, E, C, U> {

  D toDto(E entity);

  E toEntity(D dto);

  E create(C createBody);

  void update(U updateBody, @MappingTarget E entity);
}
