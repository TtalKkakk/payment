package com.example.pg.merchantapplication.presentation;

import com.example.pg.merchantapplication.command.application.MerchantApplicationService;
import com.example.pg.merchantapplication.domain.aggregate.MerchantApplication;
import com.example.pg.merchantapplication.presentation.dto.ApplicationStatusRequest;
import com.example.pg.merchantapplication.presentation.dto.ApplicationStatusResponse;
import com.example.pg.merchantapplication.presentation.dto.ApplyMerchantRequest;
import com.example.pg.merchantapplication.presentation.dto.DeleteMerchantApplicationRequest;
import com.example.pg.merchantapplication.presentation.dto.ApplyMerchantResponse;
import com.example.pg.merchantapplication.query.application.MerchantApplicationQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 가맹점(신청자) 관점의 API.
 * - 신청 접수 (POST)
 * - PG사 페이지에서 자신의 신청 승인 여부 조회 (POST)
 */
@Slf4j
@RestController
@RequestMapping("/merchant-applications")
@RequiredArgsConstructor
public class MerchantApplicationController {

    private final MerchantApplicationService merchantApplicationService;
    private final MerchantApplicationQueryService merchantApplicationQueryService;

    @PostMapping
    public ResponseEntity<ApplyMerchantResponse> apply(@Valid @RequestBody ApplyMerchantRequest request) {
        log.debug("[MerchantApplication] API apply businessNumber={} name={}", request.businessNumber(), request.name());
        MerchantApplication merchantApplication = merchantApplicationService.apply(
                request.name(),
                request.businessNumber(),
                request.phone(),
                request.email(),
                request.password()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApplyMerchantResponse.from(merchantApplication));
    }

    /**
     * 사업자번호 + 비밀번호로 심사 상세 조회.
     */
    @PostMapping("/business-number")
    public ResponseEntity<ApplicationStatusResponse> getStatusByBusinessNumber(
            @Valid @RequestBody ApplicationStatusRequest request) {
        log.debug("[MerchantApplication] API getStatusByBusinessNumber businessNumber={}", request.businessNumber());
        MerchantApplication merchantApplication = merchantApplicationQueryService.getByBusinessNumberAndPassword(
                request.businessNumber(), request.password());
        return ResponseEntity.ok(ApplicationStatusResponse.from(merchantApplication));
    }

    /**
     * 사업자번호 + 비밀번호로 신청 삭제.
     */
    @PostMapping("/delete")
    public ResponseEntity<Void> deleteByBusinessNumberAndPassword(
            @Valid @RequestBody DeleteMerchantApplicationRequest request) {
        log.debug("[MerchantApplication] API delete businessNumber={}", request.businessNumber());
        merchantApplicationService.deleteByBusinessNumberAndPassword(request.businessNumber(), request.password());
        return ResponseEntity.noContent().build();
    }
}
