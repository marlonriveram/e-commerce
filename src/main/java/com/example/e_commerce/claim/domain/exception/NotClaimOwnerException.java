package com.example.e_commerce.claim.domain.exception;

public class NotClaimOwnerException extends RuntimeException {

    public NotClaimOwnerException(Long userId, Long claimId) {
        super("User " + userId + " is not the owner of claim " + claimId);
    }
}
