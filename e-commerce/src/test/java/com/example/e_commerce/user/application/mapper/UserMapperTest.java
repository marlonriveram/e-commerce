package com.example.e_commerce.user.application.mapper;

import com.example.e_commerce.user.application.dto.response.UserResponse;
import com.example.e_commerce.user.domain.enums.EnumRole;
import com.example.e_commerce.user.domain.model.User;
import com.example.e_commerce.user.infrastructure.entity.UserEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    @Test
    void shouldMapToEntity_WhenValidDomain() {
        // Given
        User domain = User.builder().id(1L).name("Juan").email("juan@mail.com").role(EnumRole.SUPPORT).build();

        // When
        UserEntity entity = UserMapper.toEntity(domain);

        // Then
        assertEquals(1L, entity.getId());
        assertEquals("Juan", entity.getName());
        assertEquals("juan@mail.com", entity.getEmail());
        assertEquals(EnumRole.SUPPORT, entity.getRole());
    }

    @Test
    void shouldReturnNull_WhenToEntityWithNull() {
        assertNull(UserMapper.toEntity(null));
    }

    @Test
    void shouldMapToDomain_WhenValidEntity() {
        // Given
        UserEntity entity = UserEntity.builder().id(2L).name("Maria").email("maria@mail.com").role(EnumRole.FINANCE).build();

        // When
        User domain = UserMapper.toDomain(entity);

        // Then
        assertEquals(2L, domain.getId());
        assertEquals("Maria", domain.getName());
        assertEquals("maria@mail.com", domain.getEmail());
        assertEquals(EnumRole.FINANCE, domain.getRole());
    }

    @Test
    void shouldReturnNull_WhenToDomainWithNull() {
        assertNull(UserMapper.toDomain(null));
    }

    @Test
    void shouldMapToResponse_WhenValidDomain() {
        // Given
        User domain = User.builder().id(3L).name("Pedro").email("pedro@mail.com").role(EnumRole.CUSTOMER).build();

        // When
        UserResponse response = UserMapper.toResponse(domain);

        // Then
        assertEquals(3L, response.getId());
        assertEquals("Pedro", response.getName());
        assertEquals("pedro@mail.com", response.getEmail());
        assertEquals(EnumRole.CUSTOMER, response.getRole());
    }

    @Test
    void shouldReturnNull_WhenToResponseWithNull() {
        assertNull(UserMapper.toResponse(null));
    }
}
