package com.example.e_commerce.user.domain.repository;

import com.example.e_commerce.user.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    void delete(User user);

    Optional<User> findById(Long id);
}
