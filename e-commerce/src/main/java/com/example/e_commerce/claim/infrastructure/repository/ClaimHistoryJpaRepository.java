package com.example.e_commerce.claim.infrastructure.repository;

import com.example.e_commerce.claim.infrastructure.entity.ClaimHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimHistoryJpaRepository extends JpaRepository<ClaimHistoryEntity, Long> {
    List<ClaimHistoryEntity> findByClaim_Id(Long claimId);
}
