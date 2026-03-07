package com.tychewealth.mapper;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserCreateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.utils.FixtureLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);
    private UserEntity baseEntity;
    private UserResponseDto responseFixture;
    private UserCreateRequestDto createFixture;
    private UserUpdateRequestDto updateFixture;

    @BeforeEach
    void setUp() {
        baseEntity = new UserEntity();
        baseEntity.setEmail("before@tyche.com");
        baseEntity.setUsername("before");
        baseEntity.setPassword("hash");

        responseFixture = FixtureLoader.read("/fixtures/user/user-response.json", UserResponseDto.class);
        createFixture = FixtureLoader.read("/fixtures/user/user-create-request.json", UserCreateRequestDto.class);
        updateFixture = FixtureLoader.read("/fixtures/user/user-update-request.json", UserUpdateRequestDto.class);
    }

    @Test
    void toDtoMapsEntityFields() {
        UserEntity entity = new UserEntity();
        entity.setId(responseFixture.getId());
        entity.setEmail(responseFixture.getEmail());
        entity.setUsername(responseFixture.getUsername());
        entity.setPassword("hashed");
        entity.setCreatedAt(responseFixture.getCreatedAt());

        UserResponseDto dto = mapper.toDto(entity);

        assertNotNull(dto);
        assertEquals(responseFixture.getId(), dto.getId());
        assertEquals(responseFixture.getEmail(), dto.getEmail());
        assertEquals(responseFixture.getUsername(), dto.getUsername());
        assertEquals(responseFixture.getCreatedAt(), dto.getCreatedAt());
    }

    @Test
    void toEntityMapsDtoFields() {
        UserEntity entity = mapper.toEntity(responseFixture);

        assertNotNull(entity);
        assertEquals(responseFixture.getId(), entity.getId());
        assertEquals(responseFixture.getEmail(), entity.getEmail());
        assertEquals(responseFixture.getUsername(), entity.getUsername());
        assertEquals(responseFixture.getCreatedAt(), entity.getCreatedAt());
        assertNull(entity.getPassword());
    }

    @Test
    void createMapsCreateBodyToEntity() {
        UserEntity entity = mapper.create(createFixture);

        assertNotNull(entity);
        assertEquals(createFixture.getEmail(), entity.getEmail());
        assertEquals(createFixture.getUsername(), entity.getUsername());
        assertEquals(createFixture.getPassword(), entity.getPassword());
    }

    @Test
    void updateOnlyChangesNonNullFields() {
        mapper.update(updateFixture, baseEntity);

        assertEquals("before@tyche.com", baseEntity.getEmail());
        assertEquals(updateFixture.getUsername(), baseEntity.getUsername());
        assertEquals("hash", baseEntity.getPassword());
    }
}
