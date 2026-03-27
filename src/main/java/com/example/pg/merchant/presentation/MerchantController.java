package com.example.pg.merchant.presentation;

import com.example.pg.merchant.application.MerchantService;
import com.example.pg.merchant.application.dto.RegenerateSecretResultDto;
import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.merchant.presentation.dto.CredentialsRequest;
import com.example.pg.merchant.presentation.dto.MerchantCredentialsResponse;
import com.example.pg.merchant.presentation.dto.RegenerateSecretResponse;
import com.example.pg.common.config.filter.MerchantAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가맹점 API (인증된 가맹점 전용).
 * 가맹점 등록은 POST /api/merchant-applications (신청) 후 관리자 승인을 거친다.
 */
@Slf4j
@RestController
@RequestMapping("/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 사업자번호·비밀번호로 인증 후, 승인된 신청에 한해 apiKey·apiSecret 조회.
     * API 키 없이 호출 가능. MerchantApplication에서 인증 → applicationId로 본 도메인에서 키 반환.
     */
    @PostMapping("/credentials")
    public ResponseEntity<MerchantCredentialsResponse> getCredentials(@Valid @RequestBody CredentialsRequest request) {
        log.debug("[Merchant] API getCredentials businessNumber={}", request.businessNumber());
        Merchant merchant = merchantService.findKeyAndSecretResponse(request.businessNumber(), request.password());
        return ResponseEntity.ok(new MerchantCredentialsResponse(merchant.getApiKey(), merchant.getApiSecret()));
    }

    /**
     * API Secret 재발급
     * 기존 Secret은 즉시 무효화된다.
     * X-API-KEY, X-API-SECRET 헤더로 인증 필요. (merchantId 불필요)
     * 가맹점 페이지와 통신
     */
    @PostMapping("/regenerate-secret")
    public ResponseEntity<RegenerateSecretResponse> regenerateSecret(HttpServletRequest request) {
        String merchantId = (String) request.getAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE);
        log.debug("[Merchant] API regenerateSecret merchantId={}", merchantId);
        RegenerateSecretResultDto result = merchantService.regenerateSecret(merchantId);
        return ResponseEntity.ok(new RegenerateSecretResponse(result.apiKey(), result.apiSecret()));
    }

    /**
     * 가맹점 삭제 (API 키 폐기)
     * X-API-KEY, X-API-SECRET 헤더로 인증 필요.
     * 삭제 시 해당 API 키로 더 이상 인증할 수 없다.
     * PG 페이지와 통신
     */
    @DeleteMapping
    public ResponseEntity<Void> delete(HttpServletRequest request) {
        String merchantId = (String) request.getAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE);
        log.debug("[Merchant] API delete merchantId={}", merchantId);
        merchantService.deleteMerchant(merchantId);
        return ResponseEntity.noContent().build();
    }
}
