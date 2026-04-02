package com.example.pg.payment.application;

import com.example.pg.card_company.util.CardCompanyPortRegistry;
import com.example.pg.card_company.presentation.CardCompanyConnect;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import com.example.pg.payment.presentation.dto.PaymentRefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카드사에 환불(취소) 요청을 비동기로 수행하고 결과를 결제 상태에 반영한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRefundProcessor {

    private final PaymentRepository paymentRepository;
    private final CardCompanyPortRegistry portRegistry;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @Transactional
    public void processRefund(String paymentIdValue) {
        PaymentId paymentId = PaymentId.from(paymentIdValue);

        paymentRepository.load(paymentId).ifPresent(payment -> {
            CardCompanyConnect port = portRegistry.getPortOrThrow(payment.getCardCompany().getCode());
            PaymentRefundResponse result = port.requestRefund(payment.getId());

            if (result.success()) {
                int updated = paymentRepository.markCanceled(paymentIdValue);
                if (updated == 1) {
                    eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.CANCELED));
                    log.info("[Payment] event=Canceled paymentId={}", paymentIdValue);
                } else {
                    log.warn("[Payment] skip cancelSuccess due to status race paymentId={}", paymentIdValue);
                }
            } else {
                int updated = paymentRepository.markCancelFailed(paymentIdValue);
                if (updated == 1) {
                    eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.CANCEL_FAILED));
                    log.warn("[Payment] event=RefundRequestFailed paymentId={} status=CANCEL_FAILED resultCode={} message={}",
                            paymentIdValue, result.resultCode(), result.message());
                } else {
                    log.warn("[Payment] skip cancelFail due to status race paymentId={}", paymentIdValue);
                }
            }
        });
    }
}
