package com.example.pg.receipt.presentation.port;

import com.example.pg.receipt.domain.aggregate.Receipt;

/**
 * 영수증 PDF 생성 포트.
 * Receipt 애그리거트를 PDF 바이트로 변환한다.
 */
public interface ReceiptPdfPort {

    /**
     * 영수증 데이터로 PDF 바이트 생성.
     *
     * @param receipt 영수증 (무효화 시 PDF에 취소 표시 가능)
     * @return PDF 바이트 (application/pdf)
     */
    byte[] generate(Receipt receipt);
}
