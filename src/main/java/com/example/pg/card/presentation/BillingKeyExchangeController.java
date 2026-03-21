package com.example.pg.card.presentation;

import com.example.pg.card.command.application.CardService;
import com.example.pg.card.command.application.result.BillingKeyExchangeResult;
import com.example.pg.config.filter.MerchantAuthFilter;
import com.example.pg.card.presentation.dto.ExchangeBillingKeyRequest;
import com.example.pg.card.presentation.dto.ExchangeBillingKeyResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing-keys")
@RequiredArgsConstructor
public class BillingKeyExchangeController {

    private final CardService cardService;

    /**
     * 1회용 authCode를 빌링키로 교환한다.
     * X-API-KEY, X-API-SECRET 헤더로 가맹점 인증 필수.
     */
    @PostMapping("/exchange")
    public ResponseEntity<ExchangeBillingKeyResponse> exchange(
            HttpServletRequest request,
            @RequestBody ExchangeBillingKeyRequest body
    ) {
        String merchantId = (String) request.getAttribute(MerchantAuthFilter.MERCHANT_ID_ATTRIBUTE);
        BillingKeyExchangeResult result = cardService.exchangeBillingKey(merchantId, body.authCode());

        return ResponseEntity.ok(new ExchangeBillingKeyResponse(
                result.billingKey(),
                result.ownerId(),
                result.maskedNumber()
        ));
    }
}
