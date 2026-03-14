package com.example.pg.receipt.query.application;

import com.example.pg.receipt.domain.aggregate.Receipt;
import com.example.pg.receipt.infrastructure.persistence.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 영수증 조회 전용 서비스 (상태 변경 없음).
 */
@Service
@RequiredArgsConstructor
public class ReceiptQueryService {

    private final ReceiptRepository receiptRepository;

    /**
     * 결제 ID로 영수증 조회.
     * 결제당 영수증은 1건이므로 단건 반환.
     *
     * @param paymentId 결제 ID
     * @return 해당 결제로 발급된 영수증, 없으면 empty
     */
    @Transactional(readOnly = true)
    public Optional<Receipt> getByPaymentId(String paymentId) {
        return receiptRepository.findByPaymentId(paymentId);
    }
}
