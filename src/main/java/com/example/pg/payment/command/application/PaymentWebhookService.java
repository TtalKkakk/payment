package com.example.pg.payment.command.application;

import com.example.pg.merchant.domain.aggregate.Merchant;
import com.example.pg.payment.command.application.result.PaymentWebhookPayload;
import com.example.pg.payment.command.domain.aggregate.Payment;
import com.example.pg.merchant.infrastructure.persistence.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 결제 결과 웹훅 발송 서비스.
 * 가맹점 callbackUrl로 POST 요청을 보내며, X-PG-Signature 헤더로 서명을 포함한다.
 * 가맹점은 apiSecret으로 서명 검증 후 처리할 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private static final String SIGNATURE_HEADER = "X-PG-Signature";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final MerchantRepository merchantRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 결제 상태 변경 시 웹훅 발송.
     * callbackUrl이 없으면 발송하지 않는다.
     */
    public void sendWebhook(Payment payment) {
        String callbackUrl = payment.getCallbackUrl();
        if (callbackUrl == null || callbackUrl.isBlank()) {
            log.debug("웹훅 callbackUrl이 없어 발송 생략 paymentId={}", payment.getId());
            return;
        }

        String apiSecret = merchantRepository.findById(payment.getMerchantId())
                .map(Merchant::getApiSecret)
                .orElse(null);
        if (apiSecret == null) {
            log.warn("가맹점을 찾을 수 없어 웹훅 서명 생략 merchantId={}", payment.getMerchantId());
        }

        PaymentWebhookPayload payload = PaymentWebhookPayload.from(payment);
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("웹훅 payload 직렬화 실패", e);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiSecret != null) {
            try {
                String signature = computeHmacSha256(payloadJson, apiSecret);
                headers.set(SIGNATURE_HEADER, signature);
            } catch (Exception e) {
                log.warn("웹훅 서명 생성 실패", e);
            }
        }

        HttpEntity<String> entity = new HttpEntity<>(payloadJson, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    callbackUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            log.info("웹훅 발송 완료 paymentId={}, status={}, responseStatus={}",
                    payment.getId(), payment.getStatus(), response.getStatusCode());
        } catch (Exception e) {
            log.error("웹훅 발송 실패 paymentId={}, callbackUrl={}", payment.getId(), callbackUrl, e);
            // 재시도는 별도 구현 (재시도 큐 등)
        }
    }

    private String computeHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
