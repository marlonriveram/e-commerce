package com.example.e_commerce.claim.domain.exception;

public class ClaimNotFoundException extends RuntimeException {

    public ClaimNotFoundException(Long id) {
        super("Claim not found with id: " + id);
    }
}
