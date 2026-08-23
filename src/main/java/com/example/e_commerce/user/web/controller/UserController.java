package com.example.e_commerce.user.web.controller;

import com.example.e_commerce.user.application.dto.request.UserRequest;
import com.example.e_commerce.user.application.dto.response.UserResponse;
import com.example.e_commerce.user.application.mapper.UserMapper;
import com.example.e_commerce.user.application.service.UserCreationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCreationService userCreationService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestBody @Valid UserRequest request) {
        var user = userCreationService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toResponse(user));
    }
}
