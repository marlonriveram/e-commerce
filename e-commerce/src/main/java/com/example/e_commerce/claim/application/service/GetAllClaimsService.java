package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.exception.ClaimNotFoundException;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAllClaimsService {

    private final ClaimRepository claimRepository;

    public List<Claim> findAll(EnumStatus status) {
        if (status != null) {
            return claimRepository.findByStatus(status);
        }
        return claimRepository.findAll();
    }

    public Claim findById(Long claimId) {
        return claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException(claimId));
    }
}
