package com.example.e_commerce.claim.web.controller;

import com.example.e_commerce.claim.application.dto.request.ClaimRequest;
import com.example.e_commerce.claim.application.dto.request.RefundRequest;
import com.example.e_commerce.claim.application.dto.request.StatusUpdateRequest;
import com.example.e_commerce.claim.application.dto.response.ClaimResponse;
import com.example.e_commerce.claim.application.service.ClaimCreationService;
import com.example.e_commerce.claim.application.service.ClaimRefundService;
import com.example.e_commerce.claim.application.service.ClaimReviewService;
import com.example.e_commerce.claim.application.service.GetAllClaimsService;
import com.example.e_commerce.claim.application.service.GetAuditHistoryService;
import com.example.e_commerce.claim.application.service.GetCustomerClaimsService;
import com.example.e_commerce.claim.domain.enums.EnumStatus;
import com.example.e_commerce.claim.domain.exception.ClaimNotFoundException;
import com.example.e_commerce.claim.domain.model.Claim;
import com.example.e_commerce.claim.domain.model.ClaimHistory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    @Mock
    private ClaimCreationService claimCreationService;
    @Mock
    private ClaimReviewService claimReviewService;
    @Mock
    private ClaimRefundService claimRefundService;
    @Mock
    private GetAllClaimsService getAllClaimsService;
    @Mock
    private GetCustomerClaimsService getCustomerClaimsService;
    @Mock
    private GetAuditHistoryService getAuditHistoryService;

    @InjectMocks
    private ClaimController controller;

    private Claim sampleClaim(Long id, EnumStatus status) {
        return Claim.builder().id(id).orderId(100L + id).description("Desc " + id)
                .status(status).userId(10L).createdAt(LocalDateTime.now()).build();
    }

    @Test
    void shouldCreateClaim_With201() {
        ClaimRequest request = new ClaimRequest(100L, "Damaged item", 1L);
        when(claimCreationService.createClaim(any())).thenReturn(sampleClaim(1L, EnumStatus.PENDING));

        ResponseEntity<?> response = controller.createClaim(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void shouldReturnAllClaims_WhenNoStatusFilter() {
        List<Claim> claims = List.of(sampleClaim(1L, EnumStatus.PENDING), sampleClaim(2L, EnumStatus.APPROVED));
        when(getAllClaimsService.findAll(null)).thenReturn(claims);

        ResponseEntity<?> response = controller.getAllClaims(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnFilteredClaims_WhenStatusProvided() {
        when(getAllClaimsService.findAll(EnumStatus.PENDING))
                .thenReturn(List.of(sampleClaim(1L, EnumStatus.PENDING)));

        ResponseEntity<?> response = controller.getAllClaims(EnumStatus.PENDING);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnClaimById() {
        when(getAllClaimsService.findById(1L)).thenReturn(sampleClaim(1L, EnumStatus.IN_REVIEW));

        ResponseEntity<?> response = controller.getClaimById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(EnumStatus.IN_REVIEW, ((ClaimResponse) response.getBody()).getStatus());
    }

    @Test
    void shouldReturn404_WhenClaimNotFoundById() {
        when(getAllClaimsService.findById(99L)).thenThrow(new ClaimNotFoundException(99L));

        assertThrows(ClaimNotFoundException.class, () -> controller.getClaimById(99L));
    }

    @Test
    void shouldReturnClaimsByUserId() {
        when(getCustomerClaimsService.findAllCustomersClaims(10L))
                .thenReturn(List.of(sampleClaim(1L, EnumStatus.PENDING)));

        ResponseEntity<?> response = controller.getClaimsByUserId(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnHistoryForClaim() {
        List<ClaimHistory> history = List.of(
                ClaimHistory.builder().id(1L).claimId(1L)
                        .previousStatus(EnumStatus.PENDING).newStatus(EnumStatus.IN_REVIEW).changedByUser(2L).build()
        );
        when(getAuditHistoryService.findHistoryByClaimId(1L)).thenReturn(history);

        ResponseEntity<?> response = controller.getHistoryClaims(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReviewClaim() {
        StatusUpdateRequest request = new StatusUpdateRequest(EnumStatus.IN_REVIEW, 2L);
        when(claimReviewService.reviewClaim(eq(1L), eq(EnumStatus.IN_REVIEW), eq(2L)))
                .thenReturn(sampleClaim(1L, EnumStatus.IN_REVIEW));

        ResponseEntity<?> response = controller.updateStatusClaim(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(EnumStatus.IN_REVIEW, ((ClaimResponse) response.getBody()).getStatus());
    }

    @Test
    void shouldRefundClaim() {
        RefundRequest request = new RefundRequest(5L);
        when(claimRefundService.refundClaim(1L, 5L))
                .thenReturn(sampleClaim(1L, EnumStatus.REFUNDED));

        ResponseEntity<?> response = controller.refund(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(EnumStatus.REFUNDED, ((ClaimResponse) response.getBody()).getStatus());
    }
}
