package com.example.e_commerce.claim.domain.repository;

import com.example.e_commerce.claim.domain.model.ClaimHistory;

import java.util.List;
import java.util.Optional;

public interface ClaimHistoryRepository {

    ClaimHistory save(ClaimHistory claimHistory);

    void delete(ClaimHistory claimHistory);

    Optional<ClaimHistory> findById(Long id);

    List<ClaimHistory> findByClaimId(Long claimId);
}
