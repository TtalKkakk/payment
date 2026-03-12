package com.example.pg.payment.command.application;

import com.example.pg.payment.command.application.port.CardCompanyPort;
import com.example.pg.payment.command.application.port.dto.PaymentApproveDto;
import com.example.pg.payment.domain.aggregate.CardCompany;
import com.example.pg.payment.domain.enumerate.PaymentStatus;
import com.example.pg.payment.domain.event.PaymentStatusChangedEvent;
import com.example.pg.payment.domain.vo.PaymentId;
import com.example.pg.payment.infrastructure.persistence.PaymentRepository;
import com.example.pg.common.exception.BusinessException;
import com.example.pg.common.exception.ErrorCode;
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
            if (payment.getStatus() != PaymentStatus.AUTHORIZING) {
                return;
            }

            CardCompany cardCompany = payment.getCardCompany();
            if (cardCompany == null) {
                throw new BusinessException(ErrorCode.PAYMENT_INVALID_STATUS, "결제에 카드사 정보가 없습니다.");
            }
            CardCompanyPort port = portRegistry.getPortOrThrow(cardCompany.getCode());

            PaymentApproveDto result = port.approve(
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

