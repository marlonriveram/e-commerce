package com.example.e_commerce.claim.web.controller;

import com.example.e_commerce.claim.application.dto.request.ClaimRequest;
import com.example.e_commerce.claim.application.dto.request.RefundRequest;
import com.example.e_commerce.claim.application.dto.request.StatusUpdateRequest;
import com.example.e_commerce.claim.application.dto.response.ClaimHistoryResponse;
import com.example.e_commerce.claim.application.dto.response.ClaimResponse;
import com.example.e_commerce.claim.application.mapper.ClaimHistoryMapper;
import com.example.e_commerce.claim.application.mapper.ClaimMapper;
import com.example.e_commerce.claim.application.service.ClaimCreationService;
import com.example.e_commerce.claim.application.service.ClaimRefundService;
import com.example.e_commerce.claim.application.service.ClaimReviewService;
import com.example.e_commerce.claim.application.service.GetAllClaimsService;
import com.example.e_commerce.claim.application.service.GetAuditHistoryService;
import com.example.e_commerce.claim.application.service.GetCustomerClaimsService;
import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.model.Claim;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimCreationService claimCreationService;
    private final ClaimReviewService claimReviewService;
    private final ClaimRefundService claimRefundService;
    private final GetAllClaimsService getAllClaimsService;
    private final GetCustomerClaimsService getCustomerClaimsService;
    private final GetAuditHistoryService getAuditHistoryService;

    @PostMapping
    public ResponseEntity<ClaimResponse> createClaim(@RequestBody @Valid ClaimRequest request) {
        Claim claim = claimCreationService.createClaim(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClaimMapper.toResponse(claim));
    }

    @GetMapping
    public ResponseEntity<List<ClaimResponse>> getAllClaims(
            @RequestParam(required = false) EnumStatus status) {
        List<ClaimResponse> claims = getAllClaimsService.findAll(status)
                .stream()
                .map(ClaimMapper::toResponse)
                .toList();
        return ResponseEntity.ok(claims);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClaimResponse>> getClaimsByUserId(@PathVariable Long userId) {
        List<ClaimResponse> claims = getCustomerClaimsService.findAllCustomersClaims(userId)
                .stream()
                .map(ClaimMapper::toResponse)
                .toList();
        return ResponseEntity.ok(claims);
    }

    @GetMapping("/{claimId}")
    public ResponseEntity<ClaimResponse> getClaimById(@PathVariable Long claimId) {
        Claim claim = getAllClaimsService.findById(claimId);
        return ResponseEntity.ok(ClaimMapper.toResponse(claim));
    }

    @GetMapping("/{claimId}/history")
    public ResponseEntity<List<ClaimHistoryResponse>> getHistoryClaims(@PathVariable Long claimId) {
        List<ClaimHistoryResponse> history = getAuditHistoryService.findHistoryByClaimId(claimId)
                .stream()
                .map(ClaimHistoryMapper::toResponse)
                .toList();
        return ResponseEntity.ok(history);
    }

    @PatchMapping("/{claimId}/review")
    public ResponseEntity<ClaimResponse> updateStatusClaim(
            @PathVariable Long claimId,
            @RequestBody @Valid StatusUpdateRequest request) {
        Claim claim = claimReviewService.reviewClaim(claimId, request.getNewStatus(), request.getChangedByUser());
        return ResponseEntity.ok(ClaimMapper.toResponse(claim));
    }

    @PatchMapping("/{claimId}/refund")
    public ResponseEntity<ClaimResponse> refund(
            @PathVariable Long claimId,
            @RequestBody @Valid RefundRequest request) {
        Claim claim = claimRefundService.refundClaim(claimId, request.getChangedByUser());
        return ResponseEntity.ok(ClaimMapper.toResponse(claim));
    }
}
