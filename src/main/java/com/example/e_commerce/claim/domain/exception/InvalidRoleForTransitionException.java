package com.example.e_commerce.claim.domain.exception;

import com.example.e_commerce.user.domain.enums.EnumRole;
import com.example.e_commerce.claim.domain.enums.EnumStatus;

public class InvalidRoleForTransitionException extends RuntimeException {

    public InvalidRoleForTransitionException(EnumRole role, EnumStatus newStatus) {
        super("Role " + role + " is not allowed to set status: " + newStatus);
    }
}
