package com.example.e_commerce.claim.infrastructure.repository;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.infrastructure.entity.ClaimEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimJpaRepository extends JpaRepository<ClaimEntity, Long> {
    List<ClaimEntity> findByUserId(Long userId);

    List<ClaimEntity> findByStatus(EnumStatus status);
}
