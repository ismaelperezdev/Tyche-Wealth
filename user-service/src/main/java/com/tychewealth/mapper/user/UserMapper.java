package com.tychewealth.mapper.user;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.RegisterRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.mapper.GenericMapper;
import com.tychewealth.mapper.GenericMapperConfig;
import org.mapstruct.Mapper;

@Mapper(config = GenericMapperConfig.class)
public interface UserMapper
    extends GenericMapper<UserResponseDto, UserEntity, RegisterRequestDto, UserUpdateRequestDto> {}
