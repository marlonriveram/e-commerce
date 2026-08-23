package com.example.e_commerce.user.web.controller;

import com.example.e_commerce.user.application.dto.request.UserRequest;
import com.example.e_commerce.user.application.dto.response.UserResponse;
import com.example.e_commerce.user.application.service.UserCreationService;
import com.example.e_commerce.user.domain.enums.EnumRole;
import com.example.e_commerce.user.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserCreationService userCreationService;

    @InjectMocks
    private UserController controller;

    @Test
    void shouldCreateUser_With201() {
        UserRequest request = new UserRequest("Juan", "juan@mail.com");
        User user = User.builder().id(1L).name("Juan").email("juan@mail.com").role(EnumRole.CUSTOMER).build();
        when(userCreationService.createUser(any())).thenReturn(user);

        ResponseEntity<?> response = controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Juan", ((UserResponse) response.getBody()).getName());
    }
}
