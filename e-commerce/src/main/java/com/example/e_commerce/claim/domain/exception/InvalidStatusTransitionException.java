package com.example.e_commerce.claim.domain.exception;

import com.example.e_commerce.claim.domain.enums.EnumStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(EnumStatus from, EnumStatus to) {
        super("Invalid status transition: " + from + " -> " + to);
    }
}
