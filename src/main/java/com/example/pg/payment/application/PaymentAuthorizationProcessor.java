package com.example.pg.payment.application;

import com.example.pg.card_company.util.CardCompanyPortRegistry;
import com.example.pg.card_company.presentation.CardCompanyConnect;
import com.example.pg.payment.presentation.dto.PaymentApproveResponse;
import com.example.pg.card_company.domain.aggergate.CardCompany;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
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
    private final CardCompanyPortRegistry portRegistry;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 카드사에 결제 승인 요청(POST /api/pg/payments/approve)을 보내고 결과를 반영한다.
     * 결제에 연결된 카드사(cardCompanyCode)에 맞는 포트를 레지스트리에서 꺼내 사용한다.
     */
    @Async
    @Transactional
    public void processAuthorization(String paymentIdValue, String billingKey) {
        PaymentId paymentId = PaymentId.from(paymentIdValue);

        paymentRepository.load(paymentId).ifPresent(payment -> {
            CardCompany cardCompany = payment.getCardCompany();
            CardCompanyConnect port = portRegistry.getPortOrThrow(cardCompany.getCode());

            PaymentApproveResponse result = port.approve(
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
                eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.AUTHORIZED));
                log.info("[Payment] event=Authorized paymentId={} approvalNumber={} transactionId={}",
                        paymentIdValue, result.approvalNumber(), result.transactionId());
            } else {
                payment.authorizeFail();
                eventPublisher.publishEvent(PaymentStatusChangedEvent.from(paymentIdValue, PaymentStatus.AUTHORIZE_FAILED));
                log.info("[Payment] event=Failed paymentId={} resultCode={} message={}",
                        paymentIdValue, result.resultCode(), result.message());
            }
        });
    }
}

