package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import com.example.e_commerce.claim.domain.validator.ClaimValidator;
import com.example.e_commerce.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCustomerClaimsService {
    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;

    public List<Claim> findAllCustomersClaims(Long userId) {
        ClaimValidator.validateUserExists(userRepository, userId);

        return claimRepository.findByUserId(userId);
    }
}
