package com.tychewealth.mapper;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserCreateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.mapper.user.UserMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toDtoMapsEntityFields() {
        UserEntity entity = new UserEntity();
        entity.setId(10L);
        entity.setEmail("john@tyche.com");
        entity.setUsername("john");
        entity.setPassword("hashed");
        entity.setCreatedAt(LocalDateTime.of(2026, 3, 7, 12, 0));

        UserResponseDto dto = mapper.toDto(entity);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("john@tyche.com", dto.getEmail());
        assertEquals("john", dto.getUsername());
        assertEquals(LocalDateTime.of(2026, 3, 7, 12, 0), dto.getCreatedAt());
    }

    @Test
    void toEntityMapsDtoFields() {
        UserResponseDto dto = new UserResponseDto(5L, "ana@tyche.com", "ana", LocalDateTime.of(2026, 3, 7, 10, 0));

        UserEntity entity = mapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals(5L, entity.getId());
        assertEquals("ana@tyche.com", entity.getEmail());
        assertEquals("ana", entity.getUsername());
        assertEquals(LocalDateTime.of(2026, 3, 7, 10, 0), entity.getCreatedAt());
        assertNull(entity.getPassword());
    }

    @Test
    void createMapsCreateBodyToEntity() {
        UserCreateRequestDto createBody = new UserCreateRequestDto("new@tyche.com", "newuser", "secret");

        UserEntity entity = mapper.create(createBody);

        assertNotNull(entity);
        assertEquals("new@tyche.com", entity.getEmail());
        assertEquals("newuser", entity.getUsername());
        assertEquals("secret", entity.getPassword());
    }

    @Test
    void updateOnlyChangesNonNullFields() {
        UserEntity entity = new UserEntity();
        entity.setEmail("before@tyche.com");
        entity.setUsername("before");
        entity.setPassword("hash");

        UserUpdateRequestDto updateBody = new UserUpdateRequestDto("after");
        mapper.update(updateBody, entity);

        assertEquals("before@tyche.com", entity.getEmail());
        assertEquals("after", entity.getUsername());
        assertEquals("hash", entity.getPassword());
    }
}
