package com.example.e_commerce.user.application.mapper;

import com.example.e_commerce.user.application.dto.response.UserResponse;
import com.example.e_commerce.user.domain.model.User;
import com.example.e_commerce.user.infrastructure.entity.UserEntity;

public class UserMapper {

    public static UserEntity toEntity(User domain) {
        if (domain == null) return null;
        return UserEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .email(domain.getEmail())
                .role(domain.getRole())
                .build();
    }

    public static User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .role(entity.getRole())
                .build();
    }

    public static UserResponse toResponse(User domain) {
        if (domain == null) return null;
        return UserResponse.builder()
                .id(domain.getId())
                .name(domain.getName())
                .email(domain.getEmail())
                .role(domain.getRole())
                .build();
    }
}
