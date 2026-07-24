package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.application.dto.request.ClaimRequest;
import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import com.example.e_commerce.claim.domain.validator.ClaimValidator;
import com.example.e_commerce.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClaimCreationService {

    private final ClaimRepository claimRepository;
    private final UserRepository userRepository;

    public Claim createClaim(ClaimRequest request) {
        ClaimValidator.validateUserExists(userRepository, request.getUserId());

        Claim claim = Claim.builder()
                .orderId(request.getOrderId())
                .description(request.getDescription())
                .status(EnumStatus.PENDING)
                .userId(request.getUserId())
                .createdAt(LocalDateTime.now())
                .build();

        return claimRepository.save(claim);
    }
}
