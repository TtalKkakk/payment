package com.example.pg.payment.command.application;

import com.example.pg.payment.command.domain.enumerate.PaymentStatus;
import com.example.pg.payment.command.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.command.domain.repository.PaymentCommandRepository;
import com.example.pg.payment.command.domain.vo.PaymentId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PaymentAuthorizationProcessor {

    private final PaymentCommandRepository paymentCommandRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 카드사에 빌링키로 청구 요청을 보내고 결과를 반영한다.
     * 빌링키 유효성 검증은 카드사에서 수행하며, PG는 요청을 전달만 한다.
     * 비동기 시뮬레이션: 일정 시간 대기 후, 카드사 응답을 랜덤으로 흉내낸다.
     * (실제 연동 시 BillingKeyChargePort 등으로 카드사 API 호출)
     */
    @Async
    @Transactional
    public void processAuthorization(String paymentIdValue, String billingKey) {
        try {
            // 외부 카드사와 통신하는 시간을 흉내내기 위한 지연
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        PaymentId paymentId = PaymentId.from(paymentIdValue);

        paymentCommandRepository.load(paymentId).ifPresent(payment -> {
            // 이미 다른 상태로 변경된 경우(취소 등)는 무시
            if (payment.getStatus() != PaymentStatus.AUTHORIZING) {
                return;
            }

            // 카드사 응답 시뮬레이션 (실제 연동 시 카드사가 빌링키 유효성 검증 후 승인/거절 반환)
            boolean success = ThreadLocalRandom.current().nextBoolean();

            if (success) {
                payment.authorizeSuccess();
                eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.AUTHORIZED));
            } else {
                payment.authorizeFail();
                eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.FAILED));
            }
        });
    }
}

