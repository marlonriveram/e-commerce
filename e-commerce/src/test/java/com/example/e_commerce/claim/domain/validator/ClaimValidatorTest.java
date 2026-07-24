package com.example.e_commerce.claim.domain.validator;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.exception.InvalidRoleForTransitionException;
import com.example.e_commerce.claim.domain.exception.InvalidStatusTransitionException;
import com.example.e_commerce.user.domain.enums.EnumRole;
import com.example.e_commerce.user.domain.exception.UserNotFoundException;
import com.example.e_commerce.user.domain.model.User;
import com.example.e_commerce.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimValidatorTest {

    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);

    @Test
    void shouldNotThrow_WhenPendingToInReview() {
        assertDoesNotThrow(() ->
                ClaimValidator.validateStatusTransition(EnumStatus.PENDING, EnumStatus.IN_REVIEW));
    }

    @Test
    void shouldNotThrow_WhenPendingToRejected() {
        assertDoesNotThrow(() ->
                ClaimValidator.validateStatusTransition(EnumStatus.PENDING, EnumStatus.REJECTED));
    }

    @Test
    void shouldNotThrow_WhenInReviewToApproved() {
        assertDoesNotThrow(() ->
                ClaimValidator.validateStatusTransition(EnumStatus.IN_REVIEW, EnumStatus.APPROVED));
    }

    @Test
    void shouldNotThrow_WhenInReviewToRejected() {
        assertDoesNotThrow(() ->
                ClaimValidator.validateStatusTransition(EnumStatus.IN_REVIEW, EnumStatus.REJECTED));
    }

    @Test
    void shouldNotThrow_WhenApprovedToRefunded() {
        assertDoesNotThrow(() ->
                ClaimValidator.validateStatusTransition(EnumStatus.APPROVED, EnumStatus.REFUNDED));
    }

    @Test
    void shouldNotThrow_WhenApprovedToRejected() {
        assertDoesNotThrow(() ->
                ClaimValidator.validateStatusTransition(EnumStatus.APPROVED, EnumStatus.REJECTED));
    }

    @Test
    void shouldThrow_WhenPendingToApproved() {
        assertThrows(InvalidStatusTransitionException.class, () ->
                ClaimValidator.validateStatusTransition(EnumStatus.PENDING, EnumStatus.APPROVED));
    }

    @Test
    void shouldThrow_WhenPendingToRefunded() {
        assertThrows(InvalidStatusTransitionException.class, () ->
                ClaimValidator.validateStatusTransition(EnumStatus.PENDING, EnumStatus.REFUNDED));
    }

    @Test
    void shouldThrow_WhenPendingToPending() {
        assertThrows(InvalidStatusTransitionException.class, () ->
                ClaimValidator.validateStatusTransition(EnumStatus.PENDING, EnumStatus.PENDING));
    }

    @Test
    void shouldThrow_WhenInReviewToRefunded() {
        assertThrows(InvalidStatusTransitionException.class, () ->
                ClaimValidator.validateStatusTransition(EnumStatus.IN_REVIEW, EnumStatus.REFUNDED));
    }

    @Test
    void shouldThrow_WhenInReviewToPending() {
        assertThrows(InvalidStatusTransitionException.class, () ->
                ClaimValidator.validateStatusTransition(EnumStatus.IN_REVIEW, EnumStatus.PENDING));
    }

    @Test
    void shouldThrow_WhenApprovedToPending() {
        assertThrows(InvalidStatusTransitionException.class, () ->
                ClaimValidator.validateStatusTransition(EnumStatus.APPROVED, EnumStatus.PENDING));
    }

    @Test
    void shouldThrow_WhenRejectedToAny() {
        assertThrows(InvalidStatusTransitionException.class, () ->
                ClaimValidator.validateStatusTransition(EnumStatus.REJECTED, EnumStatus.IN_REVIEW));
    }

    @Test
    void shouldThrow_WhenRefundedToAny() {
        assertThrows(InvalidStatusTransitionException.class, () ->
                ClaimValidator.validateStatusTransition(EnumStatus.REFUNDED, EnumStatus.APPROVED));
    }

    @Test
    void shouldNotThrow_WhenSupportSetsInReview() {
        assertDoesNotThrow(() ->
                ClaimValidator.validateRoleForTransition(EnumRole.SUPPORT, EnumStatus.IN_REVIEW));
    }

    @Test
    void shouldNotThrow_WhenSupportSetsApproved() {
        assertDoesNotThrow(() ->
                ClaimValidator.validateRoleForTransition(EnumRole.SUPPORT, EnumStatus.APPROVED));
    }

    @Test
    void shouldNotThrow_WhenSupportSetsRejected() {
        assertDoesNotThrow(() ->
                ClaimValidator.validateRoleForTransition(EnumRole.SUPPORT, EnumStatus.REJECTED));
    }

    @Test
    void shouldNotThrow_WhenFinanceSetsRefunded() {
        assertDoesNotThrow(() ->
                ClaimValidator.validateRoleForTransition(EnumRole.FINANCE, EnumStatus.REFUNDED));
    }

    @Test
    void shouldThrow_WhenSupportSetsRefunded() {
        assertThrows(InvalidRoleForTransitionException.class, () ->
                ClaimValidator.validateRoleForTransition(EnumRole.SUPPORT, EnumStatus.REFUNDED));
    }

    @Test
    void shouldThrow_WhenFinanceSetsInReview() {
        assertThrows(InvalidRoleForTransitionException.class, () ->
                ClaimValidator.validateRoleForTransition(EnumRole.FINANCE, EnumStatus.IN_REVIEW));
    }

    @Test
    void shouldThrow_WhenCustomerTriesAny() {
        assertThrows(InvalidRoleForTransitionException.class, () ->
                ClaimValidator.validateRoleForTransition(EnumRole.CUSTOMER, EnumStatus.IN_REVIEW));
    }

    @Test
    void shouldNotThrow_WhenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        assertDoesNotThrow(() -> ClaimValidator.validateUserExists(userRepository, 1L));
    }

    @Test
    void shouldThrow_WhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () ->
                ClaimValidator.validateUserExists(userRepository, 99L));
    }
}
