package com.example.pg.payment.presentation;

import com.example.pg.common.config.filter.MerchantAuthFilter;
import com.example.pg.payment.command.application.BillingKeyExchangeService;
import com.example.pg.payment.presentation.dto.BillingKeyExchangeRequest;
import com.example.pg.payment.presentation.dto.BillingKeyExchangeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * billingKeyToken 교환 API (서버-서버).
 * MerchantAuthFilter(/api/**) 인증 후, 1회용 code를 billingKeyToken으로 교환한다.
 */
@Slf4j
@RestController
@RequestMapping("/billing-keys")
@RequiredArgsConstructor
public class BillingKeyExchangeController {

    private final BillingKeyExchangeService exchangeService;

    @PostMapping("/exchange")
    public ResponseEntity<BillingKeyExchangeResponse> exchange(
            @RequestAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE) String merchantId,
            @RequestBody BillingKeyExchangeRequest request
    ) {
        log.debug("[Payment] API exchange billingKey code merchantId={}", merchantId);
        var exchanged = exchangeService.exchangeOrThrow(merchantId, request.code());
        return ResponseEntity.ok(new BillingKeyExchangeResponse(
                exchanged.billingKeyToken(),
                exchanged.cardCompanyCode(),
                exchanged.cardBrand(),
                exchanged.cardNumberMasked(),
                exchanged.expiryMasked()
        ));
    }
}
