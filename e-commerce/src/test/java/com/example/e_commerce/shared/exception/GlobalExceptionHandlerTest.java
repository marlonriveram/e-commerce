package com.example.e_commerce.shared.exception;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.exception.ClaimNotFoundException;
import com.example.e_commerce.claim.domain.exception.InvalidRoleForTransitionException;
import com.example.e_commerce.claim.domain.exception.InvalidStatusTransitionException;
import com.example.e_commerce.user.domain.enums.EnumRole;
import com.example.e_commerce.user.domain.exception.DuplicateEmailException;
import com.example.e_commerce.user.domain.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    void shouldReturn404_WhenClaimNotFound() {
        ResponseEntity<ApiError> response = handler.handleClaimNotFound(
                new ClaimNotFoundException(99L), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("99"));
    }

    @Test
    void shouldReturn404_WhenUserNotFound() {
        ResponseEntity<ApiError> response = handler.handleUserNotFound(
                new UserNotFoundException(50L), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("50"));
    }

    @Test
    void shouldReturn400_WhenInvalidStatusTransition() {
        ResponseEntity<ApiError> response = handler.handleInvalidTransition(
                new InvalidStatusTransitionException(EnumStatus.PENDING, EnumStatus.REFUNDED), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("PENDING"));
        assertTrue(response.getBody().getMessage().contains("REFUNDED"));
    }

    @Test
    void shouldReturn403_WhenInvalidRoleForTransition() {
        ResponseEntity<ApiError> response = handler.handleInvalidRole(
                new InvalidRoleForTransitionException(EnumRole.CUSTOMER, EnumStatus.IN_REVIEW), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getStatus());
    }

    @Test
    void shouldReturn409_WhenDuplicateEmail() {
        ResponseEntity<ApiError> response = handler.handleDuplicateEmail(
                new DuplicateEmailException("test@mail.com"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("test@mail.com"));
    }

    @Test
    void shouldReturn400_WhenMalformedJson() throws Exception {
        org.springframework.http.HttpInputMessage mockMsg = mock(org.springframework.http.HttpInputMessage.class);
        when(mockMsg.getBody()).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));
        when(mockMsg.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
        ResponseEntity<ApiError> response = handler.handleMalformedJson(
                new org.springframework.http.converter.HttpMessageNotReadableException("bad json", mockMsg), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed JSON request", response.getBody().getMessage());
    }

    @Test
    void shouldReturn409_WhenDataIntegrityViolation() {
        ResponseEntity<ApiError> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("constraint"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Data integrity violation", response.getBody().getMessage());
    }

    @Test
    void shouldReturn500_WhenGenericException() {
        ResponseEntity<ApiError> response = handler.handleGeneral(
                new RuntimeException("secret detail"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal server error", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("secret"));
    }

    @Test
    void shouldReturnCorrectPath_WhenErrorOccurs() {
        ResponseEntity<ApiError> response = handler.handleClaimNotFound(
                new ClaimNotFoundException(1L), request);

        assertEquals("/api/v1/test", response.getBody().getPath());
    }

    @Test
    void shouldReturnNullSubErrors_WhenNotValidationError() {
        ResponseEntity<ApiError> response = handler.handleClaimNotFound(
                new ClaimNotFoundException(1L), request);

        assertNull(response.getBody().getSubErrors());
    }
}
