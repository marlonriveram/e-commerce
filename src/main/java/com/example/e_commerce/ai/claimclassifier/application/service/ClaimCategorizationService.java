package com.example.e_commerce.ai.claimclassifier.application.service;

import com.example.e_commerce.ai.claimclassifier.domain.model.ClaimAiMetadata;
import com.example.e_commerce.ai.claimclassifier.domain.repository.ClaimClassifier;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimCategorizationService {

    private final ClaimRepository claimRepository;
    private final ClaimClassifier claimClassifier;

    @Transactional
    public void categorize(Long claimId, String description) {
        Claim claim = claimRepository.findById(claimId).orElse(null);
        if (claim == null) {
            log.warn("Claim {} no encontrado. Se consume el evento sin categorizar.", claimId);
            return;
        }

        ClaimAiMetadata metadata = claimClassifier.classify(description);

        claim.setCategory(metadata.category());
        claim.setUrgency(metadata.urgency());
        claim.setSummary(metadata.summary());
        claimRepository.save(claim);

        log.info("Claim {} categorizado: category={}, urgency={}, summary={}",
                claimId, metadata.category(), metadata.urgency(), metadata.summary());
    }
}
