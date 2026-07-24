package com.example.e_commerce.user.application.service;

import com.example.e_commerce.user.application.dto.request.UserRequest;
import com.example.e_commerce.user.domain.enums.EnumRole;
import com.example.e_commerce.user.domain.exception.DuplicateEmailException;
import com.example.e_commerce.user.domain.model.User;
import com.example.e_commerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCreationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserCreationService service;

    @Test
    void shouldCreateUserWithCustomerRole_WhenValidRequest() {
        // Given
        UserRequest request = new UserRequest("Juan", "juan@mail.com");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        User result = service.createUser(request);

        // Then
        assertEquals("Juan", result.getName());
        assertEquals("juan@mail.com", result.getEmail());
        assertEquals(EnumRole.CUSTOMER, result.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldThrowDuplicateEmail_WhenEmailAlreadyExists() {
        // Given
        UserRequest request = new UserRequest("Juan", "juan@mail.com");
        when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("unique"));

        // When & Then
        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> service.createUser(request));
        assertTrue(ex.getMessage().contains("juan@mail.com"));
    }

    @Test
    void shouldAlwaysSetRoleToCustomer() {
        // Given
        UserRequest request = new UserRequest("Maria", "maria@mail.com");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        User result = service.createUser(request);

        // Then
        assertEquals(EnumRole.CUSTOMER, result.getRole());
    }
}
