package com.example.e_commerce.claim.domain.validator;

import com.example.e_commerce.user.domain.enums.EnumRole;
import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.exception.InvalidRoleForTransitionException;
import com.example.e_commerce.claim.domain.exception.InvalidStatusTransitionException;
import com.example.e_commerce.user.domain.exception.UserNotFoundException;
import com.example.e_commerce.user.domain.repository.UserRepository;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class ClaimValidator {

    private static final Map<EnumStatus, Set<EnumStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(EnumStatus.class);
    private static final Map<EnumRole, Set<EnumStatus>> ROLE_ALLOWED_STATUSES = new EnumMap<>(EnumRole.class);

    static {
        ALLOWED_TRANSITIONS.put(EnumStatus.PENDING,   Set.of(EnumStatus.IN_REVIEW, EnumStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(EnumStatus.IN_REVIEW, Set.of(EnumStatus.APPROVED,  EnumStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(EnumStatus.APPROVED,  Set.of(EnumStatus.REFUNDED,  EnumStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(EnumStatus.REJECTED,  Set.of());
        ALLOWED_TRANSITIONS.put(EnumStatus.REFUNDED,  Set.of());

        ROLE_ALLOWED_STATUSES.put(EnumRole.SUPPORT, Set.of(EnumStatus.IN_REVIEW, EnumStatus.APPROVED, EnumStatus.REJECTED));
        ROLE_ALLOWED_STATUSES.put(EnumRole.FINANCE, Set.of(EnumStatus.REFUNDED));
    }

    private ClaimValidator() {
    }

    public static void validateUserExists(UserRepository userRepository, Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    public static void validateStatusTransition(EnumStatus current, EnumStatus newStatus) {
        Set<EnumStatus> allowed = ALLOWED_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new InvalidStatusTransitionException(current, newStatus);
        }
    }

    public static void validateRoleForTransition(EnumRole role, EnumStatus newStatus) {
        Set<EnumStatus> allowed = ROLE_ALLOWED_STATUSES.get(role);
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new InvalidRoleForTransitionException(role, newStatus);
        }
    }
}
