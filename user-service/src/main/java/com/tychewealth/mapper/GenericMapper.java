package com.tychewealth.mapper;

import org.mapstruct.MappingTarget;

public interface GenericMapper<D, E, C, U> {

    D toDto(E entity);

    E toEntity(D dto);

    E create(C createBody);

    void update(U updateBody, @MappingTarget E entity);
}
