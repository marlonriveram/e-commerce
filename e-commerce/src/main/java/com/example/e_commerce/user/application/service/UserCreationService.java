package com.example.e_commerce.user.application.service;

import com.example.e_commerce.user.application.dto.request.UserRequest;
import com.example.e_commerce.user.domain.enums.EnumRole;
import com.example.e_commerce.user.domain.exception.DuplicateEmailException;
import com.example.e_commerce.user.domain.model.User;
import com.example.e_commerce.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCreationService {

    private final UserRepository userRepository;

    public User createUser(UserRequest request) {
        try {
            User user = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .role(EnumRole.CUSTOMER)
                    .build();

            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException(request.getEmail());
        }
    }
}
