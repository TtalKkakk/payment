package com.example.pg.card_company.application;

import com.example.pg.card_company.application.dto.BillingKeyRegisteredWebhookDto;
import com.example.pg.card_company.presentation.dto.BillingKeyTokenResponse;
import com.example.pg.merchant.presentation.port.MerchantPort;
import com.example.pg.common.presentation.FranchiseConnect;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 카드사로부터 빌링키를 받은 뒤 가맹점 서버로 웹훅을 보낸다.
 * <p>
 * 결제 웹훅({@link com.example.pg.payment.application.PaymentService#sendWebhook})과 동일하게
 * JSON 본문 전체에 대해 apiSecret으로 HMAC-SHA256 → Base64 → {@code X-PG-Signature} 헤더.
 * <p>
 * {@code app.billing-key.webhook.enabled=false} 이면 아무 것도 하지 않는다(기존 code 교환·리다이렉트만 유지).
 */
@Slf4j
@Service
public class BillingKeyWebhookNotifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final FranchiseConnect franchiseConnect;
    private final MerchantPort merchantPort;
    private final ObjectMapper objectMapper;
    private final boolean webhookEnabled;

    public BillingKeyWebhookNotifier(
            FranchiseConnect franchiseConnect,
            MerchantPort merchantPort,
            ObjectMapper objectMapper,
            @Value("${app.billing-key.webhook.enabled:false}") boolean webhookEnabled
    ) {
        this.franchiseConnect = franchiseConnect;
        this.merchantPort = merchantPort;
        this.objectMapper = objectMapper;
        this.webhookEnabled = webhookEnabled;
    }

    /**
     * 빌링키 등록 웹훅을 보낸다. 비활성화이거나 URL이 비어 있으면 no-op.
     *
     * @param webhookUrl 가맹점이 받을 POST URL (브라우저 returnUrl과 분리하려면 세션/토큰에 별도 필드 추가)
     */
    public void notifyBillingKeyRegisteredIfEnabled(
            String merchantId,
            String webhookUrl,
            String cardCompanyCode,
            BillingKeyTokenResponse token
    ) {
        if (!webhookEnabled) {
            log.debug("[BillingKeyWebhook] skipped (disabled)");
            return;
        }
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("[BillingKeyWebhook] skipped empty webhookUrl merchantId={}", merchantId);
            return;
        }

        BillingKeyRegisteredWebhookDto payload = BillingKeyRegisteredWebhookDto.of(merchantId, cardCompanyCode, token);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("[BillingKeyWebhook] serialize failed merchantId={}", merchantId, e);
            return;
        }

        String apiSecret = merchantPort.getApiSecret(merchantId);
        if (apiSecret == null || apiSecret.isBlank()) {
            log.warn("[BillingKeyWebhook] skipped no apiSecret merchantId={}", merchantId);
            return;
        }

        String signature;
        try {
            signature = computeHmacSha256(json, apiSecret);
        } catch (Exception e) {
            log.warn("[BillingKeyWebhook] signature failed merchantId={}", merchantId, e);
            return;
        }

        franchiseConnect.send(webhookUrl, json, signature);
        log.info("[BillingKeyWebhook] dispatch finished merchantId={} url={}", merchantId, webhookUrl);
    }

    private static String computeHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
