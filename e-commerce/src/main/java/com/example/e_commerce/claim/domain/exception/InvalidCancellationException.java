package com.example.e_commerce.claim.domain.exception;

import com.example.e_commerce.claim.domain.enums.EnumStatus;

public class InvalidCancellationException extends RuntimeException {

    public InvalidCancellationException(EnumStatus current) {
        super("Claim cannot be cancelled because it is not in PENDING status (current: " + current + ")");
    }
}
