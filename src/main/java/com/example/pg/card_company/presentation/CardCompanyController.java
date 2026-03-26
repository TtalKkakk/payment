package com.example.pg.card_company.presentation;

import com.example.pg.card_company.application.CardCompanyService;
import com.example.pg.common.config.filter.MerchantAuthFilter;
import com.example.pg.card_company.presentation.dto.BillingKeyExchangeRequest;
import com.example.pg.card_company.presentation.dto.BillingKeyExchangeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/billing-keys")
@RequiredArgsConstructor
public class CardCompanyController {
    private final CardCompanyService cardCompanyService;

    @PostMapping("/exchange")
    public ResponseEntity<BillingKeyExchangeResponse> exchange(
            @RequestAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE) String merchantId,
            @RequestBody BillingKeyExchangeRequest request
    ) {
        log.debug("[Payment] API exchange billingKey code merchantId={}", merchantId);
        BillingKeyExchangeResponse exchanged = cardCompanyService.getBillingKeyAndCardInfo(request);
        return ResponseEntity.ok(exchanged);
    }
}
