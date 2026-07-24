package com.example.e_commerce.claim.application.service;

import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.exception.ClaimNotFoundException;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.model.ClaimHistory;
import com.example.e_commerce.claim.domain.repository.ClaimHistoryRepository;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import com.example.e_commerce.claim.domain.validator.ClaimValidator;
import com.example.e_commerce.user.domain.exception.UserNotFoundException;
import com.example.e_commerce.user.domain.model.User;
import com.example.e_commerce.user.domain.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClaimReviewService {

    private final ClaimRepository claimRepository;
    private final ClaimHistoryRepository claimHistoryRepository;
    private final UserRepository userRepository;

    private Claim findClaimById(Long id) {
        return claimRepository.findById(id)
                .orElseThrow(() -> new ClaimNotFoundException(id));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public Claim reviewClaim(Long id, EnumStatus newStatus, Long changedByUser) {
        Claim claim = findClaimById(id);
        User user = findUserById(changedByUser);

        ClaimValidator.validateRoleForTransition(user.getRole(), newStatus);
        ClaimValidator.validateStatusTransition(claim.getStatus(), newStatus);

        return updateStatus(claim, newStatus, changedByUser);
    }

    private Claim updateStatus(Claim claim, EnumStatus newStatus, Long changedByUser) {
        EnumStatus previousStatus = claim.getStatus();
        claim.setStatus(newStatus);
        claimRepository.save(claim);

        ClaimHistory history = ClaimHistory.builder()
                .claimId(claim.getId())
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changedByUser(changedByUser)
                .build();
        claimHistoryRepository.save(history);

        return claim;
    }
}
