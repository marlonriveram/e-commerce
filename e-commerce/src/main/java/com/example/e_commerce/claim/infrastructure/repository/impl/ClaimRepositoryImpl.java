package com.example.e_commerce.claim.infrastructure.repository.impl;

import com.example.e_commerce.claim.application.mapper.ClaimMapper;
import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import com.example.e_commerce.claim.infrastructure.entity.ClaimEntity;
import com.example.e_commerce.claim.infrastructure.repository.ClaimJpaRepository;
import com.example.e_commerce.user.infrastructure.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClaimRepositoryImpl implements ClaimRepository {

    private final ClaimJpaRepository jpaRepository;
    private final UserJpaRepository userRepository;

    @Override
    public Claim save(Claim claim) {
        ClaimEntity entity = ClaimMapper.toEntity(claim);
        //getReferenceById obtiene una referencia del id, no toda la entidad
        entity.setUser(userRepository.getReferenceById(claim.getUserId()));
        return ClaimMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public void delete(Claim claim) {
        jpaRepository.deleteById(claim.getId());
    }

    @Override
    public Optional<Claim> findById(Long id) {
        return jpaRepository.findById(id).map(ClaimMapper::toDomain);
    }

    @Override
    public List<Claim> findAll() {
        return jpaRepository.findAll().stream()
                .map(ClaimMapper::toDomain)
                .toList();
    }

    @Override
    public List<Claim> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(ClaimMapper::toDomain)
                .toList();
    }

    @Override
    public List<Claim> findByStatus(EnumStatus status) {
        return jpaRepository.findByStatus(status).stream()
                .map(ClaimMapper::toDomain)
                .toList();
    }
}
