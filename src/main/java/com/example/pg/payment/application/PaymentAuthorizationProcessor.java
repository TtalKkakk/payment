package com.example.pg.payment.application;

import com.example.pg.common.retry.card_company.CardCompanyPaymentRetryExecutor;
import com.example.pg.card_company.util.CardCompanyPortRegistry;
import com.example.pg.card_company.presentation.CardCompanyConnect;
import com.example.pg.payment.presentation.dto.PaymentApproveResponse;
import com.example.pg.payment.domain.aggregate.Payment;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.infrastructure.lock.PaymentProcessDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAuthorizationProcessor {

    private final CardCompanyPortRegistry portRegistry;
    private final PaymentProcessDistributedLock processLock;
    private final CardCompanyPaymentRetryExecutor cardCompanyPaymentRetryExecutor;
    private final PaymentAuthorizationPersister persister;

    /**
     * 카드사에 결제 승인 요청(POST /api/pg/payments/approve)을 보내고 결과를 반영한다.
     *
     * <p>@Transactional을 제거하여 카드사 HTTP 호출(~10s) 중에는 HikariCP 커넥션을 점유하지 않는다.
     * DB 접근이 필요한 load / write 구간은 {@link PaymentAuthorizationPersister}가 각각 짧은 트랜잭션으로 처리한다.
     *
     * <pre>
     * [load TX]  loadPaymentWithCardCompany() → 커넥션 획득 → 조회 → 커넥션 반납
     * [no TX]    cardCompanyPaymentRetryExecutor.approve() → HTTP ~10s (커넥션 미점유)
     * [write TX] applyAuthorizeSuccess/Failure() → 커넥션 획득 → 저장 → 커넥션 반납
     * </pre>
     */
    @Async
    public void processAuthorization(String paymentIdValue, String billingKey) {
        processLock.runWithAuthorizeLock(paymentIdValue, () -> {
            PaymentId paymentId = PaymentId.from(paymentIdValue);
            persister.loadPaymentWithCardCompany(paymentId).ifPresent(payment ->
                    runAuthorization(paymentIdValue, billingKey, payment));
        });
    }

    private void runAuthorization(String paymentIdValue, String billingKey, Payment payment) {
        CardCompanyConnect port = portRegistry.getPortOrThrow(payment.getCardCompany().getCode());

        try {
            PaymentApproveResponse result = cardCompanyPaymentRetryExecutor.approve(
                    port,
                    payment.getId(),
                    payment.getAmount(),
                    billingKey
            );
            if (result.success()) {
                persister.applyAuthorizeSuccess(paymentIdValue, result);
            } else {
                persister.applyAuthorizeBusinessFailure(paymentIdValue, result);
            }
        } catch (Exception e) {
            persister.applyAuthorizeTechnicalFailure(paymentIdValue, e);
        }
    }
}
