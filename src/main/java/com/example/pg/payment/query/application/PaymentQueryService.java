package com.example.pg.payment.query.application;

import com.example.pg.payment.command.domain.repository.PaymentCommandRepository;
import com.example.pg.payment.command.domain.vo.PaymentId;
import com.example.pg.payment.presentation.dto.PaymentDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * CQRS Query: 결제 조회 (상태 변경 없음)
 */
@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentCommandRepository paymentCommandRepository;

    /**
     * 결제 단건 조회.
     * 해당 가맹점의 결제만 조회할 수 있다.
     *
     * @param merchantId 가맹점 ID (인증된 가맹점)
     * @param paymentId  결제 ID
     * @return 해당 가맹점 소유 결제일 때만 응답, 그 외 empty
     */
    @Transactional(readOnly = true)
    public Optional<PaymentDetailResponse> getPayment(String merchantId, String paymentId) {
        return paymentCommandRepository.load(PaymentId.from(paymentId))
                .filter(payment -> payment.getMerchantId().equals(merchantId))
                .map(PaymentDetailResponse::from);
    }
}
