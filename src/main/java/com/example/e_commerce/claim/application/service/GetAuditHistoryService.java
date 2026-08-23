package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.domain.exception.ClaimNotFoundException;
import com.example.e_commerce.claim.domain.model.ClaimHistory;
import com.example.e_commerce.claim.domain.repository.ClaimHistoryRepository;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAuditHistoryService {

    private final ClaimHistoryRepository claimHistoryRepository;
    private final ClaimRepository claimRepository;

    public List<ClaimHistory> findHistoryByClaimId(Long claimId) {
        claimRepository.findById(claimId)
                .orElseThrow(() -> new ClaimNotFoundException(claimId));

        return claimHistoryRepository.findByClaimId(claimId);
    }
}
