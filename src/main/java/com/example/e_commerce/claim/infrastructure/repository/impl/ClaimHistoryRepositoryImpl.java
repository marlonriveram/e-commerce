package com.example.e_commerce.claim.infrastructure.repository.impl;

import com.example.e_commerce.claim.application.mapper.ClaimHistoryMapper;
import com.example.e_commerce.claim.domain.model.ClaimHistory;
import com.example.e_commerce.claim.domain.repository.ClaimHistoryRepository;
import com.example.e_commerce.claim.infrastructure.repository.ClaimHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClaimHistoryRepositoryImpl implements ClaimHistoryRepository {

    private final ClaimHistoryJpaRepository jpaRepository;

    @Override
    public ClaimHistory save(ClaimHistory claimHistory) {
        return ClaimHistoryMapper.toDomain(jpaRepository.save(ClaimHistoryMapper.toEntity(claimHistory)));
    }

    @Override
    public void delete(ClaimHistory claimHistory) {
        jpaRepository.deleteById(claimHistory.getId());
    }

    @Override
    public Optional<ClaimHistory> findById(Long id) {
        return jpaRepository.findById(id).map(ClaimHistoryMapper::toDomain);
    }

    @Override
    public List<ClaimHistory> findByClaimId(Long claimId) {
        return jpaRepository.findByClaim_Id(claimId).stream()
                .map(ClaimHistoryMapper::toDomain)
                .toList();
    }
}
