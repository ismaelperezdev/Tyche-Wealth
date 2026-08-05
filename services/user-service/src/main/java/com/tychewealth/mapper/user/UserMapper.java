package com.tychewealth.mapper.user;

import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.mapper.GenericMapper;
import com.tychewealth.mapper.GenericMapperConfig;
import org.mapstruct.Mapper;

/**
 * Maps user registration and update requests to {@link UserEntity} and exposes user responses as
 * {@link UserResponseDto} instances.
 *
 * <p>Inherits the shared conversion contract from {@link GenericMapper} and applies the common
 * MapStruct configuration defined by {@link GenericMapperConfig}.
 */
@Mapper(config = GenericMapperConfig.class)
public interface UserMapper
    extends GenericMapper<UserResponseDto, UserEntity, RegisterRequestDto, UserUpdateRequestDto> {}
