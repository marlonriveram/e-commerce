package com.example.e_commerce.claim.domain.repository;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.model.Claim;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository {

    Claim save(Claim claim);

    void delete(Claim claim);

    Optional<Claim> findById(Long id);

    List<Claim> findAll();

    List<Claim> findByUserId(Long userId);

    List<Claim> findByStatus(EnumStatus status);
}
