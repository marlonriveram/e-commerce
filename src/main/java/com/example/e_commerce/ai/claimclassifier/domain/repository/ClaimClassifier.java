package com.example.e_commerce.ai.claimclassifier.domain.repository;

import com.example.e_commerce.ai.claimclassifier.domain.model.ClaimAiMetadata;

/**
 * Port del dominio ai: clasifica la descripción de un claim y devuelve
 * su categoría, urgencia y resumen. La implementación concreta (Groq)
 * vive en infrastructure/.
 */
public interface ClaimClassifier {

    ClaimAiMetadata classify(String description);
}
