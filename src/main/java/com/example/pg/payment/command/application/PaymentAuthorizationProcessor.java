package com.example.pg.payment.command.application;

import com.example.pg.payment.command.application.port.CardCompanyPort;
import com.example.pg.payment.command.domain.enumerate.PaymentStatus;
import com.example.pg.payment.command.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.command.domain.vo.PaymentId;
import com.example.pg.payment.command.infrastructure.persistence.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAuthorizationProcessor {

    private final PaymentRepository paymentRepository;
    private final CardCompanyPort cardCompanyPort;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 카드사에 결제 승인 요청(POST /api/pg/payments/approve)을 보내고 결과를 반영한다.
     * 동일 paymentId 재요청 시 카드사는 멱등 처리하여 기존 승인 결과를 반환할 수 있음.
     */
    @Async
    @Transactional
    public void processAuthorization(String paymentIdValue, String billingKey) {
        PaymentId paymentId = PaymentId.from(paymentIdValue);

        paymentRepository.load(paymentId).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.AUTHORIZING) {
                return;
            }

            CardCompanyPort.ApproveResult result = cardCompanyPort.approve(
                    payment.getId(),
                    payment.getAmount(),
                    billingKey
            );

            if (result.success()) {
                payment.authorizeSuccess(
                        result.approvalNumber(),
                        result.transactionId(),
                        result.approvedAt()
                );
                paymentRepository.save(payment);
                eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.AUTHORIZED));
                log.info("결제 승인 성공 paymentId={}, approvalNumber={}, transactionId={}",
                        paymentIdValue, result.approvalNumber(), result.transactionId());
            } else {
                payment.authorizeFail();
                eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.FAILED));
                log.warn("결제 승인 실패 paymentId={}, resultCode={}, message={}",
                        paymentIdValue, result.resultCode(), result.message());
            }
        });
    }
}

